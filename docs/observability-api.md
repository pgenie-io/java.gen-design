# Observability construction API for `rich-client`

Design spec, assembled from the resolutions of the [Observability construction API for `rich-client`](https://github.com/pgenie-io/java.gen-design/issues/25), [Statement observability surface](https://github.com/pgenie-io/java.gen-design/issues/26), [Transaction observability surface](https://github.com/pgenie-io/java.gen-design/issues/27), and [Move observability into `rich-client`](https://github.com/pgenie-io/java.gen-design/issues/28) sub-tickets of the [Observability design for pgenie Java artifacts](https://github.com/pgenie-io/java.gen-design/issues/24) wayfinder map — and grounded in the **external** instrumentation surface locked in [`docs/observability.md`](observability.md) (its destination artifact, [issue #6](https://github.com/pgenie-io/java.gen-design/issues/6)). This is the destination artifact for the *internal* construction API that realizes that surface inside `vendor/rich-client/` — it defines the three classes in `io.pgenie.java.richclient.observability` (`SessionObservability`, `StatementObservability`, `TransactionObservability` + nested `TransactionObservation` + `SqlOperation`), their construction/lifecycle contracts, and their wiring into `Session`. **Revisiting the locked names, attributes, or shapes of spans, metrics, and SLF4J log sites from `docs/observability.md` is explicitly out of scope — that surface is fixed and this spec conforms to it.** The *internal* construction shape below is not locked in the same sense — see the [#32 revision](#0-32-revision-statementobservability-is-now-per-statement) note just below for the one place it has already changed once.

All three classes are implemented, on branch `more-features` ([#32](https://github.com/pgenie-io/java.gen-design/issues/32), commit `a3eb696`): `SessionObservability` (this ticket), plus `StatementObservability`/`TransactionObservability`, both revised from their original `848a18e` shape as described below. There is no known implementation gap left in this spec.

## 0. #32 revision: `StatementObservability` is now per-statement

While implementing `SessionObservability` ([#32](https://github.com/pgenie-io/java.gen-design/issues/32)), a design review surfaced a mismatch this document's original text (see history) didn't account for: `StatementObservability` was session-scoped and stateless, taking `Statement<?>`/`Span parentSpan` as parameters on every `observeStatement`/`observeBatch` call — unlike `TransactionObservability`, which already splits into a session-scoped factory (`TransactionObservability` itself) and a per-transaction leaf (`TransactionObservation`, bound to one transaction's span and bookkeeping). `StatementObservability` had no equivalent per-statement leaf, so despite the name, it was never actually "the observability for *a* statement."

The fix makes `StatementObservability` itself the per-statement (or per-batch) leaf:

- It's built via package-private static factories, `StatementObservability.forStatement(...)` / `.forBatch(...)`, which extract the statement's `sql`/`statementName`/`operationName`/`collectionName` once and **start the CLIENT span immediately**, at construction.
- The only remaining instance method is `<R> R execute(SqlOperation<R> operation) throws SQLException` — no more re-passing `statement` or `parentSpan` on every call; both are already bound.
- The public constructor is gone; instances only ever come from `forStatement`/`forBatch`.

The session-scoped ingredients (`Tracer`, the shared `db.client.operation.duration` histogram, `Logger`, `dbUser`, `slowQueryLogThreshold`) that `StatementObservability` used to own and that a caller had to thread through on every call now live directly on `SessionObservability` (see [§2.2](#22-child-context-factories)) — there is deliberately **no** separate `StatementObservabilityFactory`-style class, since `SessionObservability` is the only caller.

Ripple effects, all landed in the same commit:

- **`StatementExecutor` deleted** (`vendor/rich-client/src/main/java/io/pgenie/java/richclient/StatementExecutor.java`, plus its test). Once `SessionObservability`/`TransactionObservability` build `StatementObservability` leaves directly, `StatementExecutor` was a pure pass-through wrapper around a `StatementObservability` it built internally — nothing behind its interface didn't already exist one layer down.
- **`TransactionObservability`'s constructor changed** from `(Tracer, Meter, StatementExecutor, Logger)` to `(Tracer, Meter, DoubleHistogram durationHistogram, Logger, String dbUser, Duration slowQueryLogThreshold)`. It now builds a `StatementObservability` leaf per `execute`/`executeBatch` call inside `TransactionObservation`, parented to the transaction span, instead of delegating to a `StatementExecutor`.
- `StatementObservability.buildDurationHistogram(Meter)` is `public` (not package-private) — `TransactionObservability`'s public constructor needs a pre-built histogram, and callers of that constructor (e.g. `TransactionExecutorIT`) live outside the `observability` package.

**The external instrumentation surface is unaffected** — same span names, attributes, metric name/unit/description, and log lines as `docs/observability.md` and the original [#26](https://github.com/pgenie-io/java.gen-design/issues/26)/[#27](https://github.com/pgenie-io/java.gen-design/issues/27) resolutions. Only the Java-level construction/call shape changed; §3 below has been rewritten to match, and is the current source of truth for `StatementObservability`, superseding [#26](https://github.com/pgenie-io/java.gen-design/issues/26)'s original resolution where the two differ.

## Contents

0. [#32 revision: `StatementObservability` is now per-statement](#0-32-revision-statementobservability-is-now-per-statement)
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
| `SessionObservability` | Implemented | Session-level telemetry: opens the only OTel `Tracer`/`Meter` for a session, holds the `db.system.name`/`pool.name`/`db.user` identity plus the shared `db.client.operation.duration` histogram, builds a fresh `StatementObservability` leaf per statement/batch via `forStatement`/`forBatch`, builds the session's single `TransactionObservability` child, owns the `HikariPoolMXBean` plus the four `pgenie.pool.connections.*` gauges, owns the health-check and `session.close` spans, owns the session-opened / closing-session / session-closed / connections-remaining-at-deadline log lines, and is the only path that closes a session. |
| `StatementObservability` | Implemented, **revised under #32** ([§0](#0-32-revision-statementobservability-is-now-per-statement)) | Telemetry for **one particular statement or batch**: bound at construction (via `forStatement`/`forBatch`) to that statement's `sql`/`statementName`/`operationName`/`collectionName` and parent span, with the CLIENT span already started. The one remaining method, `execute(SqlOperation)`, runs the operation under that span, records the `db.client.operation.duration` histogram point, and emits a slow-query warning if needed. |
| `TransactionObservability` | Implemented, constructor revised under #32 ([§0](#0-32-revision-statementobservability-is-now-per-statement)) | Transaction-level telemetry: the single INTERNAL `"transaction"` span, the `pgenie.transaction.retries` counter, commit/rollback attempt bookkeeping, the `committed`/`retries_exhausted`/`non_retryable_failure` outcome classification, and the local retryable-SQLSTATE check. Returns a nested `TransactionObservation` (see below). |
| `TransactionObservation` (nested in `TransactionObservability`) | Implemented | The in-flight transaction handle: implements `TransactionContext` directly (so `Transaction.executeOn(...)` can be passed it unchanged), builds a `StatementObservability` leaf per `execute`/`executeBatch` call with the transaction span as parent, records attempt count and outcome on `markCommitted()` / `markFailed(Throwable)`, and is `AutoCloseable` (idempotent — guarded by `closed`). |
| `SqlOperation` (package-level `@FunctionalInterface`) | Implemented | The execution seam `StatementObservability#execute` decorates: `R execute() throws SQLException`. Lets the leaf stay execution-agnostic — callers supply a lambda or method reference for the real work, the leaf supplies the span/metric/log. |

Implementation status: all three classes are implemented on `more-features` — `StatementObservability` ([#26](https://github.com/pgenie-io/java.gen-design/issues/26)) and `TransactionObservability` ([#27](https://github.com/pgenie-io/java.gen-design/issues/27)) originally at commit `848a18e`, then both revised at commit `a3eb696` as part of implementing `SessionObservability` ([#25](https://github.com/pgenie-io/java.gen-design/issues/25)/[#32](https://github.com/pgenie-io/java.gen-design/issues/32)) — see [§0](#0-32-revision-statementobservability-is-now-per-statement) and [§6](#6-wiring-into-session).

## 2. Construction and lifecycle

### 2.1 `SessionObservability.fromConfig` factory

The single, and only, construction entry point — a static factory, not a public constructor, because it derives the `Tracer`/`Meter` itself from the config's `OpenTelemetry` instance plus scope metadata, and because the session is the *only* class allowed to know about both:

```java
public static SessionObservability fromConfig(
        RichClientConfig config,
        HikariPoolMXBean poolMxBean);
```

What the factory does, in order:

- Derives the `Tracer` once via `config.openTelemetry().getTracer(config.scopeName(), config.scopeVersion())` and the `Meter` once via `config.openTelemetry().getMeter(config.scopeName())`.
- Builds the shared `db.client.operation.duration` histogram once, via `StatementObservability.buildDurationHistogram(meter)`, and holds it — every `StatementObservability` leaf built by `forStatement`/`forBatch` records onto this same instrument (see [§0](#0-32-revision-statementobservability-is-now-per-statement)/[§3](#3-statementobservability)).
- Holds the identity values forwarded to every leaf/child: `db.user` (`config.user()`) and the per-statement `slowQueryLogThreshold` (`config.slowQueryLogThreshold()`). `db.system.name = "postgresql"` stays a `StatementObservability`/`TransactionObservability`-owned string literal.
- Holds the `HikariPoolMXBean` plus the four `ObservableLongGauge` handles for `pgenie.pool.connections.*` (active, idle, pending, total) — registered here, in `fromConfig`. The old standalone `PoolMetrics` class is deleted; its gauge-registration code moved into this factory.
- Builds the session's single `TransactionObservability` (constructor: `Tracer`, `Meter`, the shared `DoubleHistogram`, `Logger`, `dbUser`, `slowQueryLogThreshold` — see [§0](#0-32-revision-statementobservability-is-now-per-statement)).
- Holds the single `Logger` (`LoggerFactory.getLogger(SessionObservability.class)`), forwarded by reference to every `StatementObservability` leaf and to `TransactionObservability` — **one logger per session, shared across the whole package** (nothing else calls `LoggerFactory.getLogger(...)`, except `Session` itself, which keeps its own logger for the one close-deadline warn line that stays in `Session` — see [§6](#6-wiring-into-session)).
- Logs `"Session opened for jdbcUrl={} user={}"` at `info` with the URL passed through the `redactUrl(...)` helper (the `password=...` query param is masked to `password=***`) — this helper now lives on `SessionObservability` itself (package-private), not on `Session`.
- `scopeName`/`scopeVersion`/`artifactName` are used to derive the `Tracer`/`Meter` and are **not** stored as fields. `config.poolName()`, `config.user()`, and `config.slowQueryLogThreshold()` *are* stored (or forwarded into `TransactionObservability`), because leaves/children need them as attribute values.

**No public constructor.** The class is constructed exclusively through `fromConfig`.

### 2.2 Child factories

`SessionObservability` produces per-statement leaves and the session's transaction child via these methods:

```java
public StatementObservability    forStatement(Statement<?> statement, Span parentSpan);
public StatementObservability    forStatement(Statement<?> statement);        // Span.current()

public TransactionObservability  forTransaction(TransactionSettings settings, Span parentSpan);
public TransactionObservability  forTransaction(TransactionSettings settings); // Span.current()
```

These two pairs behave differently, and that difference is the point of [§0](#0-32-revision-statementobservability-is-now-per-statement)'s revision:

- **`forStatement`** returns a *fresh* `StatementObservability`, bound to `statement` and `parentSpan`, with its span **already started**. There is nothing further to pass — `execute(operation)` is the only remaining call. Every invocation builds a new leaf; there is no shared, reusable `StatementObservability` instance anymore.
- **`forTransaction`** returns the session's single, shared `TransactionObservability` (built once in `fromConfig`), internally copied via a package-private `withParentSpan(Span)` wither so that a subsequent `.observe(settings, connection, parentSpan)` call falls back to the bound span if it itself receives `null`. `TransactionObservability` stays a genuine session-scoped, reusable factory — unlike `StatementObservability`, it can't be rebuilt per call, because it owns the `pgenie.transaction.retries` counter and must keep it, and because a transaction's `TransactionObservation` (its per-transaction leaf) is produced separately via `.observe(...)`, not by `forTransaction` itself.

The two-argument overloads take an explicit nullable `Span parentSpan`; the one-argument overloads are convenience shims that call the nullable form with `Span.current()`.

**`Meter` is passed down, not pre-built instruments** (with one exception: the `db.client.operation.duration` histogram is built once by `SessionObservability` and passed down as an already-built `DoubleHistogram`, since it must be shared across every per-statement leaf — see [§2.1](#21-sessionobservabilityfromconfig-factory)). `TransactionObservability` still derives its own `pgenie.transaction.retries` `LongCounter` from the `Meter` it receives; each class keeps ownership of its own metric-name constants.

### 2.3 Two-phase close handle

Close is **not** `AutoCloseable` on `SessionObservability` — the design intentionally rejects the try-with-resources pattern here, because the close work has a *middle*: log "Closing Session", close gauges, drain the pool, then `dataSource.close()`, then emit the `session.close` span and log "Session closed". The session needs to do the middle work itself; only the outer/inner framing is observability's job. The shape is:

```java
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

- `HikariPoolMXBean` and the four `pgenie.pool.connections.*` gauges are owned by `SessionObservability`, registered in `fromConfig`. `PoolMetrics` is deleted.
- The single per-session `Logger` is created in `SessionObservability.fromConfig` and forwarded by reference to every `StatementObservability` leaf and to `TransactionObservability` — neither calls `LoggerFactory.getLogger(...)` itself. (`Session` is the one exception: it keeps its own `Logger` for the single close-deadline warn line that stays in `Session` — see [§6](#6-wiring-into-session).)
- The `Tracer`/`Meter` derivation, and the shared `db.client.operation.duration` histogram construction, happen **once**, in `fromConfig`, then the most-derived thing each child needs is passed down. Children never see `RichClientConfig`.
- `slowQueryLogThreshold` is a session-level config value, owned by `SessionObservability` and forwarded into every `StatementObservability` leaf it builds (directly via `forStatement`/`forBatch`, or indirectly via `TransactionObservability`'s constructor). Nothing downstream has a public way to mutate it.

## 3. `StatementObservability`

Telemetry for **one particular statement or batch execution** — bound at construction, per [§0](#0-32-revision-statementobservability-is-now-per-statement). File: `vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/StatementObservability.java`; test: `vendor/rich-client/src/test/java/io/pgenie/java/richclient/observability/StatementObservabilityTest.java`. Test class lives in the same package so it can reference the package-private constants directly.

### 3.1 Owned names

| Name | Value | Source |
|---|---|---|
| Metric | `db.client.operation.duration` | `METRIC_NAME` |
| Unit | `s` | `METRIC_UNIT` |
| Description | `"Duration of database client operations"` | `METRIC_DESCRIPTION` |
| System | `"postgresql"` | `DB_SYSTEM` |

Instrument type: `DoubleHistogram`, built **once per session** via the public static `StatementObservability.buildDurationHistogram(Meter)` (`meter.histogramBuilder(METRIC_NAME).setUnit(METRIC_UNIT).setDescription(METRIC_DESCRIPTION).build()`) — called by `SessionObservability.fromConfig` and passed into every `forStatement`/`forBatch` call as an already-built instrument, so every statement in a session records onto the same one.

### 3.2 Construction: `forStatement` / `forBatch`

There is no public constructor. Instances come from two package-private static factories, both of which extract the statement's attributes and **start the CLIENT span immediately**:

```java
static StatementObservability forStatement(
        Tracer tracer,
        DoubleHistogram durationHistogram,   // from buildDurationHistogram, shared across the session
        Logger logger,
        String dbUser,
        Duration slowQueryLogThreshold,
        Statement<?> statement,
        Span parentSpan);                    // nullable

static StatementObservability forBatch(
        Tracer tracer,
        DoubleHistogram durationHistogram,
        Logger logger,
        String dbUser,
        Duration slowQueryLogThreshold,
        String batchSql,
        Statement<?> representativeStatement, // used only for db.operation.name / db.collection.name
        int batchSize,
        Span parentSpan);                    // nullable
```

Both callers today are in the same package: `SessionObservability.forStatement(statement, parentSpan)` (for top-level, non-transactional execution) and `TransactionObservability.TransactionObservation.execute`/`executeBatch` (for statements executed inside a transaction, parented to the transaction span).

### 3.3 Span shape — single statement

| Span | Kind | Attributes |
|---|---|---|
| Name = `statement.statementName()` (i.e. the generated statement class's simple name — `Statement#statementName()` defaults to `getClass().getSimpleName()`) | `CLIENT` | `db.system.name` = `"postgresql"`, `db.query.text` = `statement.sql()`, `pgenie.statement.name` = the span name, `pgenie.db.user` = the user from config; plus `db.operation.name` / `db.collection.name` *only* when `statement.operationName()` / `statement.collectionName()` return a value (the vendor's `Statement` defaults to `Optional.empty()`, so plain `SELECT 1`-style statements omit both). |

### 3.4 Span shape — batch

| Span | Kind | Attributes |
|---|---|---|
| Name = `"batch"` (literal) | `CLIENT` | Same as single statement, plus `db.operation.batch.size` (the count). |

### 3.5 Metric shape

| Metric | Instrument | Unit | Attributes |
|---|---|---|---|
| `db.client.operation.duration` | `DoubleHistogram` | `s` | `db.system.name` = `"postgresql"`, `db.query.text` = the SQL, `pgenie.statement.name` = the span name, plus `db.operation.name` / `db.collection.name` when the statement supplies them. **Deliberately omits `db.operation.batch.size`** — kept off the metric on purpose to keep the cardinality shape identical between single-statement and batch calls (so a histogram query against `db.client.operation.duration` works without a join on span kind). The batch size lives on the span only. |

### 3.6 The one execution method

```java
public <R> R execute(SqlOperation<R> operation) throws SQLException;
```

Must be called **exactly once** per instance — there's no separate teardown method; `execute` both runs the operation and ends the span. Runs `operation.execute()` under the already-started span made current, records timing, records the histogram point, emits the slow-query log if needed, and ends the span — all in a `finally` block, so those fire on both success and failure.

### 3.7 Slow-query logging

In `execute`'s `finally` block, after timing is recorded:

- Comparison: `Duration.ofNanos(durationNanos).compareTo(slowQueryLogThreshold) > 0`. A zero threshold means *every* query is logged as slow; a negative threshold is rejected by `RichClientConfig`.
- Log message: `"Slow query detected: {} took {} seconds"` at `warn`, with the statement name and the duration in seconds (`durationNanos / 1_000_000_000.0`).
- Lives on the same shared logger the session forwards in — leaves do not own their own.

### 3.8 `SqlOperation` seam

`SqlOperation<R>` is a package-level `@FunctionalInterface` with one method, `R execute() throws SQLException`. It exists in the same package as `StatementObservability` and is the seam `execute(...)` decorates to remain execution-agnostic: the caller passes a lambda or method reference (e.g. `() -> statement.executeOn(connection)`), the leaf supplies the span/metric/log. The vendor `Statement` and the `StatementBatch` types are untouched.

### 3.9 Parent-span rule

`forStatement`/`forBatch` take a nullable `Span parentSpan`, applied once at construction, when the span is built:

- Non-null: reparent the span builder via `builder.setParent(Context.current().with(parentSpan))`.
- Null: fall through to the OTel default — the builder parents to `Context.current()` / `Span.current()`.

This is what lets `TransactionObservability` nest statement spans under the transaction span (it passes the transaction span in as the parent) and lets a top-level `Session.execute(...)` produce a root CLIENT span (it passes `null`, which is the same as `Span.current()` when no parent is active).

### 3.10 Package-private constants

Every metric/attribute/span-name constant in `StatementObservability` (`DB_SYSTEM`, `METRIC_NAME`, `METRIC_UNIT`, `METRIC_DESCRIPTION`, all `AttributeKey` fields) is `static final` and **package-private**, not `public`. Tests in the same package reference the canonical definition directly. The class is `public final`; the construction factories (`forStatement`, `forBatch`) are package-private, `buildDurationHistogram` and `execute` are `public`.

### 3.11 Exception path

When the wrapped `SqlOperation` throws, `execute(...)`:

- Calls `span.recordException(t)` and `span.setStatus(StatusCode.ERROR, t.getMessage())`.
- Rethrows.
- Still runs the `finally` (timing, slow-query log, `span.end()`).

This is what makes the slow-query log fire on failure paths too, not just successes.

## 4. `TransactionObservability`

Implements transaction-level telemetry and the `TransactionContext` decoration that the `postgresql-jdbc` retry loop calls into. File: `vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/TransactionObservability.java`; test: `vendor/rich-client/src/test/java/io/pgenie/java/richclient/observability/TransactionObservabilityTest.java`. The nested `TransactionObservation` is part of this class's public contract — see [§4.1](#41-transactionobservation-lifecycle).

### 4.1 `TransactionObservation` lifecycle

`TransactionObservability.observe(settings, connection, parentSpan)` returns a new `TransactionObservation`. The observation:

- Holds the started transaction span, the JDBC `Connection`, the `TransactionSettings`, and a private `TransactionContext delegate = TransactionContext.of(connection)`.
- **Implements `TransactionContext` directly.** It does not wrap another decorator; it *is* the decorator. Every `TransactionContext` method is either routed to the delegate (connection-bound methods: `setSavepoint`, `rollback(Savepoint)`, `releaseSavepoint`, `getAutoCommit`, `setAutoCommit`, `getTransactionIsolation`, `setTransactionIsolation`, `isReadOnly`, `setReadOnly`) or, for the data-plane methods (`execute` and `executeBatch`), builds a `StatementObservability` leaf via `forStatement`/`forBatch` (parented to the transaction span) and calls `execute(...)` on it directly — see [§0](#0-32-revision-statementobservability-is-now-per-statement). There is no `StatementExecutor` collaborator anymore; it was deleted once this routing moved to building leaves directly.
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
public Span startHealthCheckSpan();
```

`Session` calls this, then runs its existing `SELECT 1` query on a connection with `setQueryTimeout(2)`, then ends the span. Span name `"healthCheck"` (literal), `CLIENT` kind, `db.system.name` only — no metric, no log line. Failure path records the exception and sets `StatusCode.ERROR` before returning `false`.

### 5.2 Session-close span + close handle

The two-phase close handle from [§2.3](#23-two-phase-close-handle):

- `observability.startClose()` — emits the `"Closing Session"` log line at `info`; closes the four `pgenie.pool.connections.*` gauges.
- `Session` drains the pool (active-connections poll, 10-second deadline) and calls `dataSource.close()`.
- `close.finish(remainingConnections)` — emits the `"session.close"` span (literal name, `INTERNAL` kind) with attribute `pgenie.session.close.connections_remaining` set to the remaining count, sets the span status to `ERROR` if `remainingConnections > 0` else `OK`, and emits the `"Session closed"` log line at `info`. The closing-stage `"{} active connection(s) remained at close deadline"` log line (also at `warn`, only when `remainingConnections > 0`) is emitted by `Session` itself, in the middle of the two-phase handle, between `startClose()` and `finish(...)` — that line is one of the existing `MusicCatalogueSession` call sites preserved unchanged in shape.

### 5.3 Pool gauges

Four `ObservableLongGauge` handles, registered in `SessionObservability.fromConfig`, all carrying the attribute `pool.name` (= `config.poolName()`). Names, units, attribute shape, and `HikariPoolMXBean`-polling mechanism are **as locked in `docs/observability.md` §3** — `pgenie.pool.connections.{active,idle,pending,total}` with `pool.name` only. `PoolMetrics` (the standalone class that used to own these under direct `Session` construction) is deleted; the registration lives in `fromConfig`. `startClose()` is what unregisters the gauges (guarded so repeated calls are a no-op); the gauges' `close()` is the only teardown, there is no separate teardown method on `SessionObservability`.

### 5.4 `pool.name` attribute

The `pool.name` attribute value flows from `config.poolName()` — same source as the HikariCP pool name, same source as the disambiguator in [`docs/observability.md`](observability.md) §5's "multiple instances" note. The four pool gauges and the session-close span's `pool.name` are the only places it appears (the `db.system.name = "postgresql"` constant is a string literal, not a config value).

## 6. Wiring into `Session`

The class that hosts the session's observability — `vendor/rich-client/src/main/java/io/pgenie/java/richclient/Session.java` — is itself out of scope for **naming/shape** per [`docs/observability.md`](observability.md) (it produces no spans/metrics/logs of its own naming — it just hosts `SessionObservability` and calls into it). This section describes the as-landed wiring (commit `a3eb696`).

- `Session`'s constructor builds the HikariCP `HikariDataSource`, then constructs a single `SessionObservability` via `SessionObservability.fromConfig(config, dataSource.getHikariPoolMXBean())`. `Session` no longer derives `Tracer`/`Meter` itself, and no longer references `PoolMetrics` (deleted) or any OTel `AttributeKey` directly.
- `execute(statement, parentSpan)` borrows a connection, then calls `observability.forStatement(statement, parentSpan).execute(() -> statement.executeOn(connection))` — one expression, since `forStatement` returns an already-span-started, single-use leaf (see [§0](#0-32-revision-statementobservability-is-now-per-statement)/[§3](#3-statementobservability)).
- `executeTransaction(transaction, settings, parentSpan)` borrows a connection, then builds a throwaway `TransactionExecutor` wrapping `observability.forTransaction(settings, parentSpan)` and delegates to its existing `execute(transaction, settings, connection, parentSpan)`. `TransactionExecutor` still exists (unlike `StatementExecutor`) because `forTransaction` returns the session's single, reusable `TransactionObservability`, not a per-transaction leaf — see [§0](#0-32-revision-statementobservability-is-now-per-statement).
- The health-check method is `var span = observability.startHealthCheckSpan(); /* run query */; span.end();`, with the failure path calling `span.recordException(e)` and `span.setStatus(StatusCode.ERROR, e.getMessage())` before `span.end()`.
- The close method is the two-phase handle:
  1. `var close = observability.startClose();` (logs `"Closing Session"`, closes gauges).
  2. `Session` drains the pool (active-connections poll, 10-second deadline), logging `"{} active connection(s) remained at close deadline"` at `warn` if any remain — this is the one telemetry call site that stays in `Session` itself, so `Session` keeps its own `private static final Logger logger` for it — and calls `dataSource.close()`.
  3. `close.finish(remainingConnections);` (emits the `session.close` span, logs `"Session closed"`).
- `Session` is reduced to: pool creation, connection borrowing, transaction settings defaults (the `IsolationLevel.SERIALIZABLE` + read-write + `config.transactionRetryAttempts()` defaults from the `executeTransaction(Transaction)` overload), pool draining, and `dataSource.close()`, plus the one warn log line above.

The two non-observability responsibilities that stay in `Session` (pool draining and the `executeTransaction` defaults) are why `Session` still needs a hand in the middle of `startClose()` / `finish(...)` — the drain can't move into `SessionObservability` because it touches `HikariDataSource`, and the `executeTransaction(Transaction)` overload's `TransactionSettings` default can't move because it depends on `config.transactionRetryAttempts()`. The two-phase close handle is the seam that admits that middle work without leaking most telemetry back into `Session`.

## 7. Left open

Nothing. `SessionObservability` is implemented and wired into `Session`; `PoolMetrics` and `StatementExecutor` are deleted. The one known deviation from this document's original text — `StatementObservability` becoming per-statement instead of session-scoped-with-per-call-args — is captured in [§0](#0-32-revision-statementobservability-is-now-per-statement) and folded into [§3](#3-statementobservability), not left as an open question.

## 8. Out of scope

Ruled beyond this map's destination, not part of this spec:

- **pGenie generator template changes and artifact versioning/distribution**, including the codegen work needed to supply `db.operation.name`/`db.collection.name` per statement class (already locked in [`docs/observability.md`](observability.md) §3 / §7).
- **Revisiting the locked *external* instrumentation surface** — span names, attribute keys, metric names, metric units, descriptions, outcome enum values, log messages, and the SQLSTATE set in [§4.5](#45-isretryablefailure--retryable-sqlstates). All of those are locked in [`docs/observability.md`](observability.md) and are conformed-to here, not reopened. (The *internal* construction shape is not covered by this — see [§0](#0-32-revision-statementobservability-is-now-per-statement) for the one place it changed.)

## 9. Sources

Full detail behind every decision above lives in the map's tickets, the locked external surface, and the as-built source on `more-features`:

- [Observability design for pgenie Java artifacts](https://github.com/pgenie-io/java.gen-design/issues/6) — the *external* observability surface that this spec conforms to (its destination artifact is [`docs/observability.md`](observability.md)).
- [Observability design for pgenie Java artifacts (wayfinder map)](https://github.com/pgenie-io/java.gen-design/issues/24) — the map this spec is a sub-ticket of.
- [Observability construction API for `rich-client`](https://github.com/pgenie-io/java.gen-design/issues/25) — `SessionObservability` design (`fromConfig`, child factories, close handle, no `AutoCloseable`, one-logger-forwarded-to-children). Implemented per [#32](https://github.com/pgenie-io/java.gen-design/issues/32); see [§0](#0-32-revision-statementobservability-is-now-per-statement) for the one place implementation diverged from this ticket's original resolution.
- [Statement observability surface](https://github.com/pgenie-io/java.gen-design/issues/26) — `StatementObservability`'s original session-scoped, `observeStatement`/`observeBatch`-per-call shape. **Superseded** by the per-statement redesign in [§0](#0-32-revision-statementobservability-is-now-per-statement)/[§3](#3-statementobservability); the metric/span/log surface it locked (name, attributes, unit, description) is unchanged.
- [Transaction observability surface](https://github.com/pgenie-io/java.gen-design/issues/27) — `TransactionObservability` + nested `TransactionObservation` shape: `pgenie.transaction.retries` metric, attribute table, outcome classification, `isRetryableFailure` SQLSTATE set, retries-exhausted log line, attempt-count bookkeeping rules, `AutoCloseable` + idempotent `close()`. Constructor signature later revised — see [§0](#0-32-revision-statementobservability-is-now-per-statement).
- [Move observability into `rich-client`](https://github.com/pgenie-io/java.gen-design/issues/28) — the refactor: deletion of `AttemptTrackingTransactionContext` / `InstrumentedTransactionContext`, the original `StatementExecutor` / `TransactionExecutor` shrink, test rebalance (`TransactionObservabilityTest` unit, `TransactionExecutorIT` DB-backed, `StatementObservabilityTest` in package). `StatementExecutor` itself was later deleted entirely — see [§0](#0-32-revision-statementobservability-is-now-per-statement).
- [Observability construction API for `rich-client` (this spec)](https://github.com/pgenie-io/java.gen-design/issues/29) — the destination artifact for the construction-side sub-map.
- [Implement SessionObservability and wire it into Session](https://github.com/pgenie-io/java.gen-design/issues/32) — the implementation ticket that landed `SessionObservability` and the `StatementObservability` per-statement redesign, commit `a3eb696` on `more-features`.
- [`docs/observability.md`](observability.md) — the locked *external* observability surface; the source of truth for every span name, attribute, metric, description, outcome enum value, and log message referenced throughout this spec.
- As-built source on `more-features@a3eb696`:
  - [`vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/SessionObservability.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/SessionObservability.java) and [`.../test/.../observability/SessionObservabilityTest.java`](vendor/rich-client/src/test/java/io/pgenie/java/richclient/observability/SessionObservabilityTest.java).
  - [`vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/StatementObservability.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/StatementObservability.java) and [`.../test/.../observability/StatementObservabilityTest.java`](vendor/rich-client/src/test/java/io/pgenie/java/richclient/observability/StatementObservabilityTest.java).
  - [`vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/TransactionObservability.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/TransactionObservability.java) and [`.../test/.../observability/TransactionObservabilityTest.java`](vendor/rich-client/src/test/java/io/pgenie/java/richclient/observability/TransactionObservabilityTest.java).
  - [`vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/SqlOperation.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/observability/SqlOperation.java).
  - [`vendor/rich-client/src/main/java/io/pgenie/java/richclient/Session.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/Session.java) and [`.../TransactionExecutor.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/TransactionExecutor.java) (the one remaining thin wrapper — `StatementExecutor.java` and `PoolMetrics.java` are deleted), [`.../RichClientConfig.java`](vendor/rich-client/src/main/java/io/pgenie/java/richclient/RichClientConfig.java) (the config the `fromConfig` factory consumes).
