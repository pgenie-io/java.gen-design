# Observability construction API for `rich-client`

Locked design spec, assembled from the resolutions of the [Observability construction API for `rich-client`](https://github.com/pgenie-io/java.gen-design/issues/25), [Statement observability surface](https://github.com/pgenie-io/java.gen-design/issues/26), [Transaction observability surface](https://github.com/pgenie-io/java.gen-design/issues/27), and [Move observability into `rich-client`](https://github.com/pgenie-io/java.gen-design/issues/28) sub-tickets of the [Observability design for pgenie Java artifacts](https://github.com/pgenie-io/java.gen-design/issues/24) wayfinder map — and grounded in the **external** instrumentation surface locked in [`docs/observability.md`](observability.md) (its destination artifact, [issue #6](https://github.com/pgenie-io/java.gen-design/issues/6)). This is the destination artifact for the *internal* construction API that realizes that surface inside `vendor/rich-client/` — it defines the three classes in `io.pgenie.java.richclient.observability` (`SessionObservability`, `StatementObservability`, `TransactionObservability` + nested `TransactionObservation` + `SqlOperation`), their construction/lifecycle contracts, and their wiring into `Session`. **Revisiting the locked names, attributes, or shapes of spans, metrics, and SLF4J log sites from `docs/observability.md` is explicitly out of scope — that surface is fixed and this spec conforms to it.**

Two of the three classes are implemented on branch `more-features` at commit `848a18e` (`StatementObservability`, `TransactionObservability`); `SessionObservability` is **designed here but not yet implemented** — that implementation gap is the single known leftover, called out in [§6](#6-wiring-into-session) and [§7](#7-left-open).

## Contents

1. [Package and class overview](#1-package-and-class-overview)
2. [Construction and lifecycle](#2-construction-and-lifecycle)
3. [`StatementObservability`](#3-statementobservability)
4. [`TransactionObservability`](#4-transactionobservability)
5. [Session-level telemetry](#5-session-level-telemetry)
6. [Wiring into `Session`](#6-wiring-into-session)
7. [Left open](#7-left-open)
8. [Out of scope](#8-out-of-scope)
9. [Sources](#9-sources)

## 1. Package and class overview

All three observability classes live in `io.pgenie.java.richclient.observability` (under `vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/`). The package's job is to be the **single home for every telemetry concern `rich-client` owns** — OTel traces, OTel metrics, SLF4J log sites, and the construction/lifecycle wiring that binds them to the rest of the library.

| Class | Status | Role |
|---|---|---|
| `SessionObservability` | **Designed, not yet implemented** | Session-level telemetry: opens the only OTel `Tracer`/`Meter` for a session, holds the `db.system.name`/`pool.name`/`db.user` identity, derives per-statement and per-transaction child contexts, owns the `HikariPoolMXBean` plus the four `pgenie.pool.connections.*` gauges, owns the health-check and `session.close` spans, owns the session-opened / closing-session / session-closed / connections-remaining-at-deadline log lines, and is the only path that closes a session. |
| `StatementObservability` | Implemented | Statement-level telemetry: the CLIENT spans, the `db.client.operation.duration` histogram, and slow-query logging. Execution-agnostic — takes a `SqlOperation` and decorates it. |
| `TransactionObservability` | Implemented | Transaction-level telemetry: the single INTERNAL `"transaction"` span, the `pgenie.transaction.retries` counter, commit/rollback attempt bookkeeping, the `committed`/`retries_exhausted`/`non_retryable_failure` outcome classification, and the local retryable-SQLSTATE check. Returns a nested `TransactionObservation` (see below). |
| `TransactionObservation` (nested in `TransactionObservability`) | Implemented | The in-flight transaction handle: implements `TransactionContext` directly (so `Transaction.executeOn(...)` can be passed it unchanged), routes `execute`/`executeBatch` through `StatementExecutor` with the transaction span as parent, records attempt count and outcome on `markCommitted()` / `markFailed(Throwable)`, and is `AutoCloseable` (idempotent — guarded by `closed`). |
| `SqlOperation` (package-level `@FunctionalInterface`) | Implemented | The execution seam `StatementObservability` decorates: `R execute() throws SQLException`. Lets the wrapper stay execution-agnostic — callers supply a lambda or method reference for the real work, the wrapper supplies the spans/metric/log. |

Implementation status of the package's responsibilities, in the order the locked resolutions add them: `StatementObservability` ([#26](https://github.com/pgenie-io/java.gen-design/issues/26)) and `TransactionObservability` ([#27](https://github.com/pgenie-io/java.gen-design/issues/27)) are on `more-features@848a18e`; `SessionObservability` ([#25](https://github.com/pgenie-io/java.gen-design/issues/25)) is the design captured here, with the `Session` refactor that adopts it (per [#28](https://github.com/pgenie-io/java.gen-design/issues/28)) tracked as the single open implementation item — see [§6](#6-wiring-into-session) and [§7](#7-left-open).

## 2. Construction and lifecycle

### 2.1 `SessionObservability.fromConfig` factory

The single, and only, construction entry point — a static factory, not a public constructor, because it derives the `Tracer`/`Meter` itself from the config's `OpenTelemetry` instance plus scope metadata, and because the session is the *only* class allowed to know about both:

```java
// pseudo-signature — SessionObservability is not yet implemented on disk
public static SessionObservability fromConfig(
        RichClientConfig config,
        HikariPoolMXBean poolMxBean);
```

What the factory does, in order:

- Derives the `Tracer` once via `config.openTelemetry().getTracer(config.scopeName(), config.scopeVersion())` and the `Meter` once via `config.openTelemetry().getMeter(config.scopeName())`.
- Holds the four identity constants/attributes used by every child: `db.system.name = "postgresql"`, `pool.name` (taken from `config.poolName()`), `db.user` (taken from `config.user()`), and the per-statement `slowQueryLogThreshold` (taken from `config.slowQueryLogThreshold()` and forwarded to `StatementObservability`).
- Holds the `HikariPoolMXBean` plus the four `ObservableLongGauge` handles for `pgenie.pool.connections.*` (active, idle, pending, total) — the gauges are registered here, in `fromConfig`, not in `PoolMetrics` (the `PoolMetrics` class remains for the current `Session`-side wiring described in [§6](#6-wiring-into-session) and goes away once `SessionObservability` lands).
- Holds the single `Logger` (`LoggerFactory.getLogger(SessionObservability.class)`), forwarded by reference to every child `StatementObservability` and `TransactionObservability` it creates — **one logger per session, shared by all three classes** (children don't construct their own).
- Logs `"Session opened for jdbcUrl={} user={}"` at `info` with the URL passed through the existing `redactUrl(...)` helper (the `password=...` query param is masked to `password=***`). This is the **last** telemetry call site that the design removes from `Session` itself.
- `scopeName`/`scopeVersion`/`artifactName` are used to derive the `Tracer`/`Meter` and are **not** stored as fields. `config.poolName()` and `config.user()` *are* stored, because the children need them as attribute values.

**No public constructor.** The class is constructed exclusively through `fromConfig`; the on-disk package-private fields implied by the resolution are an implementation detail.

### 2.2 Child-context factories

`SessionObservability` produces per-statement and per-transaction child wrappers via four methods. The parent span is **bound at construction** — the children take no further parent-span argument on the execution methods:

```java
public StatementObservability    forStatement(Span parentSpan);
public StatementObservability    forStatement();                 // Span.current()

public TransactionObservability  forTransaction(TransactionSettings settings, Span parentSpan);
public TransactionObservability  forTransaction(TransactionSettings settings); // Span.current()
```

The two-argument overloads take an explicit nullable `Span parentSpan`; the one-argument overloads are convenience shims that call the nullable form with `Span.current()`. Execution methods on the children take only the statement/transaction + connection (and, for `TransactionObservability.observe`, a `parentSpan` for the transaction span itself — see [§4](#4-transactionobservability)).

**`Meter` is passed down, not pre-built instruments.** Each child class holds its own metric-name constants (`db.client.operation.duration` in `StatementObservability`, `pgenie.transaction.retries` in `TransactionObservability`) and constructs its own `DoubleHistogram` / `LongCounter` from the `Meter` it receives. Children keep ownership of their own metric names; the session owns ownership of the `Meter` only.

### 2.3 Two-phase close handle

Close is **not** `AutoCloseable` on `SessionObservability` — the design intentionally rejects the try-with-resources pattern here, because the close work has a *middle*: log "Closing Session", close gauges, drain the pool, then `dataSource.close()`, then emit the `session.close` span and log "Session closed". The session needs to do the middle work itself; only the outer/inner framing is observability's job. The shape is:

```java
// pseudo-signature — design, not yet implemented
public CloseHandle startClose();
public static final class CloseHandle {
    public void finish(int remainingConnections);   // emits "session.close" span + "Session closed" log
}
```

Used as:

```java
// pseudo-code
var close = observability.startClose(); // logs "Closing Session", closes gauges
// Session drains the pool and calls dataSource.close() here
close.finish(remainingConnections);     // emits "session.close" span, logs "Session closed"
```

`SessionObservability` does **not** implement `AutoCloseable`. The `CloseHandle` is the only path that produces the `session.close` span and the closing/closed log lines.

### 2.4 Ownership rules (recap)

- `HikariPoolMXBean` and the four `pgenie.pool.connections.*` gauges are owned by `SessionObservability`. The current `PoolMetrics` class (under `vendor/rich-client/src/main/java/io/pgenie/java/richclient/PoolMetrics.java`) holds them today and is what `Session` constructs directly; under full adoption that construction moves into `SessionObservability.fromConfig` and `PoolMetrics` is deleted.
- The single per-session `Logger` is created in `SessionObservability.fromConfig` and forwarded by reference to every child — children **do not** call `LoggerFactory.getLogger(...)` themselves.
- The `Tracer`/`Meter` derivation happens **once**, in `fromConfig`, then the most-derived thing each child needs is passed down. Children never see `RichClientConfig`.
- `slowQueryLogThreshold` is a session-level config value, owned by `SessionObservability` and forwarded into each `StatementObservability` it builds. The child has no public way to mutate it.

## 3. `StatementObservability`

Implements statement-level telemetry for both single-statement and batch execution. File: `vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/StatementObservability.java`; test: `vendor/rich-client/src/test/java/io/pgenie/java/richclient/observability/StatementObservabilityTest.java`. Test class lives in the same package so it can reference the package-private constants directly.

### 3.1 Owned names

| Name | Value | Source |
|---|---|---|
| Metric | `db.client.operation.duration` | `METRIC_NAME` |
| Unit | `s` | `METRIC_UNIT` |
| Description | `"Duration of database client operations"` | `METRIC_DESCRIPTION` |
| System | `"postgresql"` | `DB_SYSTEM` |

Instrument type: `DoubleHistogram`, built from the `Meter` passed in at construction (`meter.histogramBuilder(METRIC_NAME).setUnit(METRIC_UNIT).setDescription(METRIC_DESCRIPTION).build()`).

### 3.2 Span shape — single statement

| Span | Kind | Attributes |
|---|---|---|
| Name = `statement.statementName()` (i.e. the generated statement class's simple name — `Statement#statementName()` defaults to `getClass().getSimpleName()`) | `CLIENT` | `db.system.name` = `"postgresql"`, `db.query.text` = `statement.sql()`, `pgenie.statement.name` = the span name, `pgenie.db.user` = the user from config; plus `db.operation.name` / `db.collection.name` *only* when `statement.operationName()` / `statement.collectionName()` return a value (the vendor's `Statement` defaults to `Optional.empty()`, so plain `SELECT 1`-style statements omit both). |

### 3.3 Span shape — batch

| Span | Kind | Attributes |
|---|---|---|
| Name = `"batch"` (literal) | `CLIENT` | Same as single statement, plus `db.operation.batch.size` (the count). |

### 3.4 Metric shape

| Metric | Instrument | Unit | Attributes |
|---|---|---|---|
| `db.client.operation.duration` | `DoubleHistogram` | `s` | `db.system.name` = `"postgresql"`, `db.query.text` = the SQL, `pgenie.statement.name` = the span name, plus `db.operation.name` / `db.collection.name` when the statement supplies them. **Deliberately omits `db.operation.batch.size`** — kept off the metric on purpose to keep the cardinality shape identical between single-statement and batch calls (so a histogram query against `db.client.operation.duration` works without a join on span kind). The batch size lives on the span only. |

### 3.5 Two execution methods

```java
public <R> R observeStatement(
        Statement<?> statement,    // postgresql-jdbc vendor type
        Span parentSpan,           // nullable
        SqlOperation<R> operation) // the actual work
        throws SQLException;

public <R> List<R> observeBatch(
        String batchSql,
        Statement<?> representativeStatement, // used only for db.operation.name / db.collection.name
        int batchSize,
        Span parentSpan,           // nullable
        SqlOperation<List<R>> operation)
        throws SQLException;
```

Both methods funnel through one private `observe(...)` helper that records timing, emits the slow-query log, and ends the span — all in a `finally` block, so they fire on both success and failure.

### 3.6 Slow-query logging

In the shared `finally` block, after timing is recorded:

- Comparison: `Duration.ofNanos(durationNanos).compareTo(slowQueryLogThreshold) > 0`. A zero threshold means *every* query is logged as slow; a negative threshold is rejected by `RichClientConfig`.
- Log message: `"Slow query detected: {} took {} seconds"` at `warn`, with the statement name and the duration in seconds (`durationNanos / 1_000_000_000.0`).
- Lives on the same shared logger the session forwards in — children do not own their own.

### 3.7 `SqlOperation` seam

`SqlOperation<R>` is a package-level `@FunctionalInterface` with one method, `R execute() throws SQLException`. It exists in the same package as `StatementObservability` and is the seam the wrapper uses to remain execution-agnostic: the caller passes a lambda or method reference (e.g. `() -> statement.executeOn(connection)`), the wrapper supplies the spans/metric/log. The vendor `Statement` and the `StatementBatch` types are untouched.

### 3.8 Parent-span rule

Every method takes a nullable `Span parentSpan`. The handling is uniform:

- Non-null: reparent the span builder via `builder.setParent(Context.current().with(parentSpan))`.
- Null: fall through to the OTel default — the builder parents to `Context.current()` / `Span.current()`.

This is the parameterization that lets `TransactionObservability` nest statement spans under the transaction span (it passes the transaction span in as the parent) and lets a top-level `Session.execute(...)` produce a root CLIENT span (it passes `null`, which is the same as `Span.current()` when no parent is active).

### 3.9 Package-private constants

Every metric/attribute/span-name constant in `StatementObservability` (`DB_SYSTEM`, `METRIC_NAME`, `METRIC_UNIT`, `METRIC_DESCRIPTION`, all `AttributeKey` fields) is `static final` and **package-private**, not `public`. Tests in the same package reference the canonical definition directly. The class is `public final` with a public constructor; only the *constants* are package-private.

### 3.10 Exception path

When the wrapped `SqlOperation` throws, the `observe(...)` helper:

- Calls `span.recordException(t)` and `span.setStatus(StatusCode.ERROR, t.getMessage())`.
- Rethrows.
- Still runs the `finally` (timing, slow-query log, `span.end()`).

This is what makes the slow-query log fire on failure paths too, not just successes.

## 4. `TransactionObservability`

Implements transaction-level telemetry and the `TransactionContext` decoration that the `postgresql-jdbc` retry loop calls into. File: `vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/TransactionObservability.java`; test: `vendor/rich-client/src/test/java/io/pgenie/java/richclient/observability/TransactionObservabilityTest.java`. The nested `TransactionObservation` is part of this class's public contract — see [§4.1](#41-transactionobservation-lifecycle).

### 4.1 `TransactionObservation` lifecycle

`TransactionObservability.observe(settings, connection, parentSpan)` returns a new `TransactionObservation`. The observation:

- Holds the started transaction span, the JDBC `Connection`, the `TransactionSettings`, and a private `TransactionContext delegate = TransactionContext.of(connection)`.
- **Implements `TransactionContext` directly.** It does not wrap another decorator; it *is* the decorator. Every `TransactionContext` method is either routed to the delegate (connection-bound methods: `setSavepoint`, `rollback(Savepoint)`, `releaseSavepoint`, `getAutoCommit`, `setAutoCommit`, `getTransactionIsolation`, `setTransactionIsolation`, `isReadOnly`, `setReadOnly`) or routed through `StatementExecutor` (data-plane methods: `execute` and `executeBatch`).
- Tracks two pieces of bookkeeping, both absorbed from the deleted `AttemptTrackingTransactionContext`:
  - `boolean commitCalled` — flipped to `true` in `commit()`, **before** delegating. The "increment regardless of whether the delegate throws" rule from `docs/observability.md` §2 (the attempt-count race fix) is implemented by incrementing in the body before the call to `delegate.commit()` / `delegate.rollback()`, so a thrown delegate doesn't lose the count.
  - `int rollbackCount` — incremented in `rollback()`, **before** delegating. Same fix.
- **Implements `AutoCloseable`.** `close()` ends the span, guarded by a `closed` boolean so duplicate calls are no-ops (idempotent).

### 4.2 `markCommitted` vs `markFailed`

Both are called by `TransactionExecutor` from the `try` / `catch` / `finally` of its `execute` wrapper (see [§6](#6-wiring-into-session) for the current state). The two diverge on outcome classification:

- `markCommitted()`:
  - `pgenie.transaction.attempt_count` = `rollbackCount + 1`.
  - `pgenie.transaction.outcome` = `"committed"`.
  - `retriesCounter.add(Math.max(0, attempts - 1))`.
  - `span.setStatus(StatusCode.OK)`.
  - No log line.
- `markFailed(Throwable failure)`:
  - `pgenie.transaction.attempt_count` = `Math.max(1, rollbackCount)`. The `max(1, ...)` is what stops a single-`rollback`-then-`commit`-then-fail sequence from recording `attempt_count = 0` (the unit test `failedTransactionDoesNotOvercountWhenCommitWasAttempted` pins this).
  - `pgenie.transaction.outcome` = `"retries_exhausted"` if `isRetryableFailure(failure)` is `true`, else `"non_retryable_failure"`.
  - `retriesCounter.add(Math.max(0, attempts - 1))`.
  - `span.recordException(failure)`.
  - `span.setStatus(StatusCode.ERROR, failure.getMessage())`.
  - **Log line, only on `retries_exhausted`**: `logger.warn("Transaction exhausted {} attempts, last failure: {}", settings.maxAttempts(), failure)`. The other two outcomes stay log-silent — `committed` is the expected path and would be noise; `non_retryable_failure` already propagates as a thrown `SQLException` the caller must handle.

### 4.3 Span shape

| Span | Kind | Attributes |
|---|---|---|
| Name = `"transaction"` (literal, from `SPAN_NAME`) | `INTERNAL` | `db.system.name` = `"postgresql"`; `pgenie.transaction.isolation_level` = `settings.isolationLevel().name()`; `pgenie.transaction.max_attempts` = `settings.maxAttempts()` (long); `pgenie.transaction.read_only` = `settings.readOnly()` — all set at span start, from `TransactionSettings`. `pgenie.transaction.attempt_count` and `pgenie.transaction.outcome` are set once at the end via `markCommitted` / `markFailed`. |

### 4.4 Metric shape

| Metric | Instrument | Unit | Attributes |
|---|---|---|---|
| `pgenie.transaction.retries` | `LongCounter` | — | **none.** Undimensioned. |

Built from the `Meter` passed in at construction (`meter.counterBuilder(RETRIES_METRIC_NAME).setDescription(RETRIES_METRIC_DESCRIPTION).build()`). Description: `"Number of transaction retries"`.

### 4.5 `isRetryableFailure` — retryable SQLSTATEs

```java
public static boolean isRetryableFailure(Throwable failure);
```

`static` on `TransactionObservability`, not on `TransactionObservation` (so it's reachable without an instance). The check is:

| SQLSTATE | Meaning |
|---|---|
| `40001` | Serialization failure |
| `40P01` | Deadlock detected |
| `23505` | Unique violation (PostgreSQL can raise this instead of `40001` under `SERIALIZABLE`) |

Two rules the implementation has to honor:

- **Unwraps one level of cause.** The static `extractSqlException(Throwable t)` helper returns the throwable itself if it's already an `SQLException`, otherwise it returns the throwable's `getCause()` if that's an `SQLException`, otherwise `null`. This is what makes the unit test `isRetryableFailureUnwrapsWrappedCause` pass — a `RuntimeException("wrapper", sqlException)` whose cause is the real `SQLException("...", "40001")` is recognized as retryable.
- **Explicitly null-tolerant.** `failure == null` returns `false`. (Was NPE'd before the #27 grilling.) A non-`SQLException` with a non-`SQLException` cause returns `false`. A `SQLException` with `getSQLState() == null` returns `false`.

### 4.6 Outcome classification decision table

| `rollbackCount` at end | `commit()` called? | `markCommitted` or `markFailed`? | Failure retryable? | `attempt_count` | `outcome` | Log line? |
|---|---|---|---|---|---|---|
| 0 | yes (or no) | `markCommitted` | — | `1` | `committed` | no |
| ≥ 1 | yes | `markCommitted` | — | `rollbackCount + 1` | `committed` | no |
| 0 | no | `markFailed` | yes | `1` | `retries_exhausted` | yes (`warn`) |
| 0 | no | `markFailed` | no | `1` | `non_retryable_failure` | no |
| ≥ 1 | no | `markFailed` | yes | `rollbackCount` | `retries_exhausted` | yes (`warn`) |
| ≥ 1 | no | `markFailed` | no | `rollbackCount` | `non_retryable_failure` | no |
| ≥ 1, `commit()` was called | no | `markFailed` | either | `max(1, rollbackCount)` — `commit` does not add to the count, so the rollback count stands, but never below 1 | per `isRetryableFailure` | per outcome |

The `retriesCounter.add(Math.max(0, attempts - 1))` line is symmetric across both marks — successful retried transactions (`markCommitted` after rollbacks) and exhausted retries (`markFailed` with `retries_exhausted`) both add to the counter, since "retries" means attempts beyond the first in both cases.

## 5. Session-level telemetry

The `session.*`/`pgenie.pool.connections.*`/`healthCheck` telemetry listed in [`docs/observability.md`](observability.md) §3 is *realized* by `SessionObservability` per [#25](https://github.com/pgenie-io/java.gen-design/issues/25). This section captures the construction-side shape; the *names, attributes, and meanings* of every span/metric/attribute below are locked in `docs/observability.md` §3 and are not restated here.

### 5.1 Health-check span

```java
// pseudo-signature
public Span startHealthCheckSpan();
```

`Session` calls this, then runs its existing `SELECT 1` query on a connection with `setQueryTimeout(2)`, then ends the span. Span name `"healthCheck"` (literal), `CLIENT` kind, `db.system.name` only — no metric, no log line. Failure path records the exception and sets `StatusCode.ERROR` before returning `false`.

### 5.2 Session-close span + close handle

The two-phase close handle from [§2.3](#23-two-phase-close-handle):

- `observability.startClose()` — emits the `"Closing Session"` log line at `info`; closes the four `pgenie.pool.connections.*` gauges.
- `Session` drains the pool (active-connections poll, 10-second deadline) and calls `dataSource.close()`.
- `close.finish(remainingConnections)` — emits the `"session.close"` span (literal name, `INTERNAL` kind) with attribute `pgenie.session.close.connections_remaining` set to the remaining count, sets the span status to `ERROR` if `remainingConnections > 0` else `OK`, and emits the `"Session closed"` log line at `info`. The closing-stage `"{} active connection(s) remained at close deadline"` log line (also at `warn`, only when `remainingConnections > 0`) is emitted by `Session` itself, in the middle of the two-phase handle, between `startClose()` and `finish(...)` — that line is one of the existing `MusicCatalogueSession` call sites preserved unchanged in shape.

### 5.3 Pool gauges

Four `ObservableLongGauge` handles, registered in `SessionObservability.fromConfig`, all carrying the attribute `pool.name` (= `config.poolName()`). Names, units, attribute shape, and `HikariPoolMXBean`-polling mechanism are **as locked in `docs/observability.md` §3** — `pgenie.pool.connections.{active,idle,pending,total}` with `pool.name` only. The current `PoolMetrics` class ([#28](https://github.com/pgenie-io/java.gen-design/issues/28) status) is what actually emits these today under direct `Session` ownership; once `SessionObservability` lands, `PoolMetrics` is deleted and the registration moves into `fromConfig`. The `CloseHandle.finish(...)` path is what unregisters the gauges (the gauges' `close()` is the only teardown; there is no separate teardown method on `SessionObservability`).

### 5.4 `pool.name` attribute

The `pool.name` attribute value flows from `config.poolName()` — same source as the HikariCP pool name, same source as the disambiguator in [`docs/observability.md`](observability.md) §5's "multiple instances" note. The four pool gauges and the session-close span's `pool.name` are the only places it appears (the `db.system.name = "postgresql"` constant is a string literal, not a config value).

## 6. Wiring into `Session`

The class that hosts all three observability wrappers — `vendor/rich-client/src/main/java/io/pgenie/java/richclient/Session.java` — is itself out of scope for **naming/shape** per [`docs/observability.md`](observability.md) (it produces no spans/metrics/logs of its own naming — it just hosts the wrappers). But it *is* the only place that knows when to construct a wrapper, when to call into it, and when to close it, so the current state of the wiring matters.

### 6.1 Current state (as built on `more-features@848a18e`)

What is already wired:

- `Session` derives `Tracer`/`Meter` itself from `config.openTelemetry()` + `config.scopeName()` + `config.scopeVersion()` once, in the constructor, and passes them to its two direct collaborators: `StatementExecutor` and `TransactionExecutor`.
- `StatementExecutor(tracer, meter, logger, config.user(), config.slowQueryLogThreshold())` — constructs a `StatementObservability` internally with those five arguments. All statement execution in `rich-client` flows through this path.
- `TransactionExecutor(new TransactionObservability(tracer, meter, statementExecutor, logger))` — constructs a `TransactionObservability` internally and delegates to it. All transaction execution in `rich-client` flows through this path.
- The two SLF4J log sites owned by `StatementObservability` (slow-query) and `TransactionObservability` (retries-exhausted) are working in the as-built code.
- `PoolMetrics` is constructed directly by `Session` and emits the four `pgenie.pool.connections.*` gauges — still tied to the session lifecycle, but via a separate class rather than via `SessionObservability`.

What still lives in `Session` today (and is the single open implementation item):

- The `HikariPoolMXBean` ownership and the `PoolMetrics` construction.
- The session-opened log line (`"Session opened for jdbcUrl={} user={}"` with URL redaction).
- The health-check span (`"healthCheck"`, `CLIENT` kind, `db.system.name` only).
- The close path: the `"Closing Session"` log line, the `poolMetrics.close()` call, the drain-deadline loop, the `"{} active connection(s) remained at close deadline"` warn line, the `dataSource.close()` call, the `session.close` span, the `"Session closed"` log line.
- Derivation of `Tracer`/`Meter` (the design moves this into `SessionObservability.fromConfig`).
- The `redactUrl(...)` URL-redaction helper (kept as a `static` package-private helper, but called from `SessionObservability` per the new design, not from `Session`).

### 6.2 Target end state under full `SessionObservability` adoption

Per [#25](https://github.com/pgenie-io/java.gen-design/issues/25) and [#28](https://github.com/pgenie-io/java.gen-design/issues/28), the end state is:

- `Session` constructs a single `SessionObservability` via `SessionObservability.fromConfig(config, dataSource.getHikariPoolMXBean())` in its constructor. The `Tracer`/`Meter` derivation, the `HikariPoolMXBean` ownership, the four gauges, the session-opened log line, and the URL redaction all move into that factory.
- `Session` no longer references `Tracer`, `Meter`, `Logger`, `PoolMetrics`, or any OTel `AttributeKey` directly. Every telemetry call site leaves `Session`.
- For each `execute(statement)` / `executeTransaction(...)` call, `Session` borrows a connection from the pool and calls `observability.forStatement(parentSpan)` / `observability.forTransaction(settings, parentSpan)` to obtain the child wrapper, then delegates execution to that wrapper (the wrappers take the connection). `Span.current()` overloads cover the no-explicit-parent case.
- The health-check method becomes `var span = observability.startHealthCheckSpan(); /* run query */; span.end();` with the failure path calling `span.recordException(e)` and `span.setStatus(StatusCode.ERROR, e.getMessage())` before `span.end()`.
- The close method becomes:
  1. `var close = observability.startClose();` (logs `"Closing Session"`, closes gauges).
  2. `Session` drains the pool and calls `dataSource.close()`.
  3. `close.finish(remainingConnections);` (emits `session.close` span, logs `"Session closed"`).
- `StatementExecutor` and `TransactionExecutor` shrink to thin wrappers that just construct / delegate (the unit test `TransactionExecutorTest` and the integration test `TransactionRetryIT` are already deleted on `more-features@848a18e`, replaced by `TransactionObservabilityTest` for the unit layer and the existing `TransactionExecutorIT` for the DB-backed layer — that test rebalance was the [#28](https://github.com/pgenie-io/java.gen-design/issues/28) work, and is part of the as-built state). `StatementObservabilityTest` moved into `observability/` and now lives in the same package as the class it tests (so it can reference package-private constants).
- `PoolMetrics` is deleted; its four gauges are owned by `SessionObservability` directly.
- `Session` is reduced to: pool creation, connection borrowing, transaction settings defaults (the `IsolationLevel.SERIALIZABLE` + read-write + `config.transactionRetryAttempts()` defaults from the `executeTransaction(Transaction)` overload), pool draining, and `dataSource.close()`. Nothing else.

The two non-observability responsibilities that stay in `Session` (pool draining and the `executeTransaction` defaults) are why `Session` still needs a hand in the middle of `startClose()` / `finish(...)` — the drain can't move into `SessionObservability` because it touches `HikariDataSource`, and the `executeTransaction(Transaction)` overload's `TransactionSettings` default can't move because it depends on `config.transactionRetryAttempts()`. The two-phase close handle is the seam that admits that middle work without leaking telemetry back into `Session`.

## 7. Left open

Not fog, not blocking anything else in this spec, but the single implementation gap this spec calls out:

- **Full `SessionObservability` adoption in `Session`.** The class is designed per [§2](#2-construction-and-lifecycle) and [§5](#5-session-level-telemetry); it is not yet implemented on `more-features@848a18e`. The refactor that moves the health-check span, the `session.close` span, the pool gauges, the session-opened / closing-session / session-closed / connections-remaining-at-deadline log lines, and the `Tracer`/`Meter` derivation out of `Session` and into `SessionObservability` (per [§6.2](#62-target-end-state-under-full-sessionobservability-adoption)) is the only remaining implementation work. Once it lands, `PoolMetrics` is deleted, the `Tracer`/`Meter`/`Logger`/`AttributeKey` imports leave `Session.java`, and the only collaborators `Session` constructs are `SessionObservability` and the borrowed-connection plumbing.

## 8. Out of scope

Ruled beyond this map's destination, not part of this spec:

- **Performing the refactor itself.** The `SessionObservability` adoption is a later implementation effort (the only remaining piece of the [#28](https://github.com/pgenie-io/java.gen-design/issues/28) refactor; the `StatementObservability` and `TransactionObservability` moves are already done). The test rebalance that came with [#28](https://github.com/pgenie-io/java.gen-design/issues/28) — `TransactionExecutorTest` + `TransactionRetryIT` deleted, replaced by `TransactionObservabilityTest` (DB-less, in-memory OTel) + the existing `TransactionExecutorIT` (DB-backed), and `StatementObservabilityTest` moved into the `observability` package — is part of the as-built state, not a future task.
- **pGenie generator template changes and artifact versioning/distribution**, including the codegen work needed to supply `db.operation.name`/`db.collection.name` per statement class (already locked in [`docs/observability.md`](observability.md) §3 / §7).
- **Revisiting the locked instrumentation surface** — span names, attribute keys, metric names, metric units, descriptions, outcome enum values, log messages, and the SQLSTATE set in [§4.5](#45-isretryablefailure--retryable-sqlstates). All of those are locked in [`docs/observability.md`](observability.md) and in [#26](https://github.com/pgenie-io/java.gen-design/issues/26) / [#27](https://github.com/pgenie-io/java.gen-design/issues/27) and are conformed-to here, not reopened.

## 9. Sources

Full detail behind every decision above lives in the map's tickets, the locked external surface, and the as-built source on `more-features@848a18e`:

- [Observability design for pgenie Java artifacts](https://github.com/pgenie-io/java.gen-design/issues/6) — the *external* observability surface that this spec conforms to (its destination artifact is [`docs/observability.md`](observability.md)).
- [Observability design for pgenie Java artifacts (wayfinder map)](https://github.com/pgenie-io/java.gen-design/issues/24) — the map this spec is a sub-ticket of.
- [Observability construction API for `rich-client`](https://github.com/pgenie-io/java.gen-design/issues/25) — `SessionObservability` design (`fromConfig`, child factories, close handle, no `AutoCloseable`, one-logger-forwarded-to-children).
- [Statement observability surface](https://github.com/pgenie-io/java.gen-design/issues/26) — `StatementObservability` shape: `db.client.operation.duration` metric, span attribute table, `SqlOperation` seam, parent-span rule, package-private constants, batch-size-is-span-only.
- [Transaction observability surface](https://github.com/pgenie-io/java.gen-design/issues/27) — `TransactionObservability` + nested `TransactionObservation` shape: `pgenie.transaction.retries` metric, attribute table, outcome classification, `isRetryableFailure` SQLSTATE set, retries-exhausted log line, attempt-count bookkeeping rules, `AutoCloseable` + idempotent `close()`.
- [Move observability into `rich-client`](https://github.com/pgenie-io/java.gen-design/issues/28) — the refactor: deletion of `AttemptTrackingTransactionContext` / `InstrumentedTransactionContext`, shrinking of `StatementExecutor` / `TransactionExecutor`, test rebalance (`TransactionObservabilityTest` unit, `TransactionExecutorIT` DB-backed, `StatementObservabilityTest` in package), `Session` not yet wired.
- [Observability construction API for `rich-client` (this spec)](https://github.com/pgenie-io/java.gen-design/issues/29) — the destination artifact for the construction-side sub-map.
- [`docs/observability.md`](observability.md) — the locked *external* observability surface; the source of truth for every span name, attribute, metric, description, outcome enum value, and log message referenced throughout this spec.
- As-built source on `more-features@848a18e`:
  - [`vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/StatementObservability.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/StatementObservability.java) and [`.../test/.../observability/StatementObservabilityTest.java`](vendor/rich-client/src/test/java/io/pgenie/java/richclient/observability/StatementObservabilityTest.java).
  - [`vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/TransactionObservability.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/TransactionObservability.java) and [`.../test/.../observability/TransactionObservabilityTest.java`](vendor/rich-client/src/test/java/io/pgenie/java/richclient/observability/TransactionObservabilityTest.java).
  - [`vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/SqlOperation.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/SqlOperation.java).
  - [`vendor/rich-client/src/main/java/io/pgenie/java/richclient/Session.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/Session.java) (current wiring state), [`.../PoolMetrics.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/PoolMetrics.java) (current pool-gauge implementation, deleted under full adoption), [`.../StatementExecutor.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/StatementExecutor.java) and [`.../TransactionExecutor.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/TransactionExecutor.java) (thin wrappers post-#28), [`.../RichClientConfig.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/RichClientConfig.java) (the config the `fromConfig` factory consumes).
