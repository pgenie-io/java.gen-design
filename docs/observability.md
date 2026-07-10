# Observability design for pgenie Java artifacts

Locked design spec, assembled from the resolutions of the [Observability design for pgenie Java artifacts](https://github.com/pgenie-io/java.gen-design/issues/6) wayfinder map. This is the destination artifact for that map — it defines the **general**, artifact-agnostic observability pattern for pgenie-generated Java database-client artifacts: module boundaries, the full instrumentation surface (traces, metrics, and SLF4J log statements), the config & wiring surface, and the resulting `postgresql-jdbc` API changes (none, see [§4](#4-postgresqljdbc-api-changes)).

`music-catalogue` (`io.pgenie.artifacts.myspace.musiccatalogue`) is the **reference artifact** this spec was designed against — every concrete name, file, and code shape below is grounded in its current source — but the pattern itself is parameterized: nothing here should require change per generated artifact beyond the values each artifact's generated config supplies. **Implementing this spec — the `rich-client` library, the `music-catalogue` migration, or the pgenie generator template changes needed to emit `db.operation.name`/`db.collection.name` (§3) — is a later effort, out of scope for this document.**

## Contents

1. [Module boundaries](#1-module-boundaries)
2. [The postgresql-jdbc observability seam](#2-the-postgresqljdbc-observability-seam)
3. [Instrumentation surface](#3-instrumentation-surface)
4. [postgresql-jdbc API changes](#4-postgresqljdbc-api-changes)
5. [Config & wiring surface](#5-config--wiring-surface)
6. [Left open](#6-left-open)
7. [Out of scope](#7-out-of-scope)
8. [Sources](#8-sources)

## 1. Module boundaries

Decided in [Library split decision](https://github.com/pgenie-io/java.gen-design/issues/10).

Two modules, not three:

- **`io.codemine:postgresql-jdbc`** (`vendor/postgresql-jdbc.java/`) — stays telemetry-free. Defines the `Statement`/`Transaction`/`TransactionContext`/`ExecutionContext` seam that everything else decorates. No OTel or SLF4J dependency. Unchanged by this spec — see [§4](#4-postgresqljdbc-api-changes).
- **`io.pgenie.java:rich-client`** (proposed location `vendor/rich-client/`) — one shared library covering session management, connection pooling, transaction retry bookkeeping, statement execution, and **all** observability (spans, metrics, SLF4J logging, config, wiring). Every pgenie-generated artifact (e.g. `music-catalogue`) depends on `rich-client` and supplies it a populated config; the generated code itself carries no observability logic of its own.

No third "thin" tier for observability specifically — an application that wants `postgresql-jdbc` with zero pgenie decoration continues to use `io.codemine:postgresql-jdbc` directly (unaffected by anything in this spec); an application that wants the generated, observed experience uses `rich-client` via its generated artifact. There is no supported middle ground where a generated artifact opts out of `rich-client`'s telemetry while keeping its retry/pooling behavior.

## 2. The postgresql-jdbc observability seam

Decided in [postgresql-jdbc observability seam](https://github.com/pgenie-io/java.gen-design/issues/11) and refined by [Instrumentation surface](https://github.com/pgenie-io/java.gen-design/issues/12).

`rich-client` observes `postgresql-jdbc` **entirely through decoration** — wrapping `TransactionContext`/`ExecutionContext` — no new vendor API is needed (see [§4](#4-postgresqljdbc-api-changes)). Every code path that can produce a statement, a batch, or a retryable transaction failure flows through a member the decorator already intercepts: `execute()`, `executeBatch()`, `commit()`, `rollback()`. `postgresql-jdbc`'s own retry loop (`Transaction.executeAttempts`, `private`) calls `commit()`/`rollback()` exactly once per attempt, so decorating those two members is sufficient to observe every attempt without any push-based listener SPI.

Two things the decorator-only approach requires `rich-client` to own, both currently missing or buggy in the reference artifact:

- **Fix the attempt-count race.** `music-catalogue`'s current decorator (`ObservableTransactionContext.rollback()`) increments its rollback counter *after* `delegate.rollback()` returns:
  ```java
  public void rollback() throws SQLException {
      delegate.rollback();
      rollbackCount++;
  }
  ```
  If the delegate call itself throws, the count silently undercounts. `rich-client`'s equivalent decorator must increment regardless of whether the delegate call throws (e.g. in a `finally`, or incrementing before delegating and correcting on the rare success-after-prior-attempts path) — this is a **required fix carried into `rich-client`**, not optional cleanup.
- **Own the retryable-SQLSTATE check locally.** Labeling a captured failure "retryable" (needed for `pgenie.transaction.outcome`, [§3](#3-instrumentation-surface)) requires checking the SQLSTATE against `40001` (serialization failure) / `40P01` (deadlock detected) / `23505` (unique violation, which PostgreSQL can report instead of `40001` under `SERIALIZABLE`). This check is **not exposed** by `postgresql-jdbc` — `Transaction.executeAttempts` inlines it privately (`Transaction.java:112-118`). `rich-client` keeps one locally-owned copy of the same three-SQLSTATE check (documented already in `TransactionSettings`'s javadoc as the retry policy) rather than petitioning the vendor for a public accessor — duplication is bounded to this single place by the module split in [§1](#1-module-boundaries), so growing `postgresql-jdbc`'s public surface to save three string comparisons isn't worth it.

**Adjacent finding, not a required fix but a real anti-pattern to avoid inheriting**: `music-catalogue`'s standalone (non-transactional) `execute()` path (`StatementExecutor.executeStatement()`) currently hand-rolls the same prepare/bind/execute/decode sequence `Statement.executeOn(Connection)` already implements, rather than calling it. `rich-client`'s statement-execution wrapper should call `statement.executeOn(connection)` inside its span/timing decoration rather than reimplementing it.

**Attempt-start timing is moot, not open.** The seam ticket left "eager vs. lazy attempt-start-timing" open as a decorator-mechanism question; [§3](#3-instrumentation-surface) resolves it by never emitting a per-attempt span in the first place, so the question doesn't need an answer.

## 3. Instrumentation surface

Decided in [Instrumentation surface](https://github.com/pgenie-io/java.gen-design/issues/12), grounded in [Survey off-the-shelf OTel instrumentation](https://github.com/pgenie-io/java.gen-design/issues/7) and [DB-client semantic-conventions audit](https://github.com/pgenie-io/java.gen-design/issues/8). Full side-by-side comparison tables and semconv stability citations: [`docs/research/otel-instrumentation-survey.md`](research/otel-instrumentation-survey.md), [`docs/research/db-client-semconv-audit.md`](research/db-client-semconv-audit.md), [`docs/research/otel-library-logging.md`](research/otel-library-logging.md).

### Instrumentation scope

Unchanged: `io.pgenie.artifacts.<group>.<artifact>` (e.g. `io.pgenie.artifacts.myspace.musiccatalogue`) is the **sole** carrier of artifact identity. No span, metric, or `pgenie.*`-namespaced attribute name below ever repeats the artifact name — the artifact/database-instance identity that today leaks into metric-name prefixes (`pgenie.musiccatalogue.*`) moves into attribute *values* only (e.g. `pool.name = "music-catalogue-pool"`), so a dashboard or alert built against a bare `pgenie.*`/`db.*` name works unmodified across every future pgenie-generated artifact.

### Spans

| Span | Kind | Attributes | Notes |
|---|---|---|---|
| Statement — name = statement class simple name (e.g. `InsertAlbum`) | `CLIENT` | `db.system.name`, `db.query.text`, `db.operation.name` (**new**, e.g. `"INSERT"`), `db.collection.name` (**new**, e.g. `"albums"`), `pgenie.statement.name`, `pgenie.db.user` (renamed from `db.user`) | Keeps class-name/literal naming rather than the semconv `{db.operation.name} {target}` template — `pgenie.statement.name` is a more precise identifier for a generated statement than a synthesized name would be. `db.operation.name`/`db.collection.name` give semconv-compatible filtering without renaming the span. |
| Batch — name = `"batch"` (literal) | `CLIENT` | same as statement, plus `db.operation.batch.size` (renamed from `pgenie.statement.batch_size`) | |
| Transaction — name = `"transaction"` (literal) | `INTERNAL` | `db.system.name`, `pgenie.transaction.isolation_level` (renamed from `db.transaction.isolation_level` — that key squatted the reserved `db.*` namespace for a concept semconv doesn't model), `pgenie.transaction.max_attempts`, `pgenie.transaction.read_only`, `pgenie.transaction.attempt_count` (**new**), `pgenie.transaction.outcome` (**new** — enum `committed` \| `retries_exhausted` \| `non_retryable_failure`) | **No per-attempt child spans.** Individual statement spans issued by each attempt remain visible and time-orderable under the transaction span for attempt-level granularity. |
| Health check — name = `"healthCheck"` (literal) | `CLIENT` | `db.system.name` only | No metric. |
| Session close — name **renamed** `"<artifact>.session.close"` → `"session.close"` (artifact segment dropped, see Instrumentation scope above) | `INTERNAL` | `pgenie.session.close.connections_remaining` (**new**) | Status now **meaningful**: `ERROR` when connections remain active at the drain deadline, `OK` otherwise (today always `OK` regardless). |

`db.operation.name`/`db.collection.name` are static per-generated-statement-class metadata, known from the SQL template at codegen time — computing them is a requirement on the pgenie generator's template output, not something `rich-client` derives at runtime by parsing SQL.

**Attempt semantics.** `pgenie.transaction.attempt_count` is set once, after the vendor retry loop returns, from the fixed decorator's commit/rollback bookkeeping ([§2](#2-the-postgresqljdbc-observability-seam)). `pgenie.transaction.outcome` lives on the span only — the `pgenie.transaction.retries` counter below stays a bare, undimensioned count, since a counter's job is throughput and outcome is already queryable via span attributes/status.

### Metrics

| Metric | Instrument | Unit | Attributes | Notes |
|---|---|---|---|---|
| `db.client.operation.duration` (renamed from `pgenie.<artifact>.statement.duration`) | `DoubleHistogram` | `s` | `db.system.name`, `db.operation.name`, `db.collection.name`, `pgenie.statement.name`, `db.query.text` | Renamed to the stable semconv target. `db.query.text` is kept as a dimension deliberately — the spec marks it `opt_in` on this metric for cardinality reasons, but generated statements have one fixed SQL text per statement class, so the cardinality risk the spec guards against doesn't apply here. |
| `pgenie.transaction.retries` (renamed from `pgenie.<artifact>.transaction.retries`, artifact segment dropped) | `LongCounter` | — | none | |
| `pgenie.pool.connections.{active,idle,pending,total}` (renamed from `pgenie.<artifact>.pool.connections.*`, artifact segment dropped) | `ObservableLongGauge` ×4 | — | `pool.name` | **Shape and mechanism deliberately unchanged** — stays hand-rolled `HikariPoolMXBean` polling. Does **not** adopt `db.client.connection.*` naming/shape and does **not** adopt `opentelemetry-hikaricp-3.0`. See rationale below. |

**Pool metrics stay bespoke — deferred, not adopted.** The entire `db.client.connection.*` metric family is `stability: development` in the semconv model — unlike the statement-duration metric, which is stable — and has already changed shape once upstream (units `ms`→`s`, active/idle collapsed into one metric split by a `state` attribute). `opentelemetry-hikaricp-3.0`, the off-the-shelf `MetricsTrackerFactory` implementation that would emit these names, is itself alpha-only with no GA track across every published release. Both facts push this to a future ticket once the family stabilizes upstream, not part of this spec.

### SLF4J logs

Decided in [Idiomatic OTel logging from a library](https://github.com/pgenie-io/java.gen-design/issues/9): **libraries stay on SLF4J at log call sites.** `rich-client` does not call the OTel Logs API directly — the API's own Javadoc and the project's status page both scope it to *appender authors bridging existing frameworks*, not to libraries originating log statements; OTel's own `opentelemetry-jdbc` instrumentation emits zero logs through any framework, reinforcing "libraries emit telemetry, applications own log plumbing." Consuming applications bridge `rich-client`'s SLF4J calls into OTel for free via the javaagent's default-enabled auto-instrumentation, or explicitly via `opentelemetry-log4j-appender-2.17` (or the Logback equivalent) in manual-SDK setups. Trace/span correlation on bridged log records is automatic in the common (same-thread) case — no MDC configuration needed for export, only for rendering trace/span IDs as text in a human-readable log line.

Existing call sites carry over unchanged in shape (5 sites, currently in `music-catalogue`'s `MusicCatalogueSession`/`StatementExecutor`): session-opened (`info`), close-start/close-end (`info`), connections-remaining-at-deadline (`warn`), slow-query (`warn`).

**New**: `logger.warn("Transaction exhausted {} attempts, last failure: {}", maxAttempts, lastCause)`, fired only when `pgenie.transaction.outcome == retries_exhausted` — the one outcome that's genuinely actionable/surprising. `committed` stays log-silent (expected path, would be noise every transaction); `non_retryable_failure` stays log-silent too, since it already propagates as a thrown `SQLException` the caller must handle.

### db.user — final disposition

No semconv replacement exists for `db.user` (removed outright, per the semconv audit — "Removed, no replacement at this time"). Kept, renamed to `pgenie.db.user`, on both statement and batch spans.

### Parameterization rule

Applies to every `pgenie.*` name above: the instrumentation scope name is the **sole** carrier of artifact identity, so no `pgenie.*` span/metric name repeats it (`pgenie.<artifact>.transaction.retries` → `pgenie.transaction.retries`, `pgenie.<artifact>.pool.connections.*` → `pgenie.pool.connections.*`, `"<artifact>.session.close"` span → `"session.close"`). This is what makes a dashboard or alert built against any bare `pgenie.*`/`db.*` name reusable, unmodified, across every future pgenie-generated artifact — the entire point of this spec being general rather than `music-catalogue`-specific.

## 4. postgresql-jdbc API changes

**None.** [§2](#2-the-postgresqljdbc-observability-seam) establishes that the existing `TransactionContext`/`ExecutionContext` decorator surface, once `rich-client` fixes the attempt-count race and owns a local retryable-SQLSTATE check, already makes everything [§3](#3-instrumentation-surface)'s instrumentation surface needs observable. This supersedes an earlier drafted shape from an initial pass at the seam ticket — a push-based `StatementListener`/`TransactionListener` SPI with new `Statement.executeOn(Connection, StatementListener)` / `TransactionContext.of(Connection, TransactionListener)` overloads — dropped in favor of the decorator-only approach once it was confirmed every observable event already flows through a member the decorator can already intercept.

## 5. Config & wiring surface

Decided in [Config & wiring surface](https://github.com/pgenie-io/java.gen-design/issues/13).

**One merged config type, fully owned by `rich-client`** — not split into a connection-config and a separate observability-config. Nothing in the reference artifact's current `MusicCatalogueConfig` field list is artifact-specific in *shape*; every field is just a *value* a given artifact happens to supply. Fields: `jdbcUrl`, `user`, `password`, `maximumPoolSize`, `connectionTimeout`, `statementTimeout`, `transactionRetryAttempts`, `slowQueryLogThreshold`, `openTelemetry`, `scopeName`, `scopeVersion`, `poolName`, `artifactName` (plain `String`, required — every real caller is generated code, which always knows its own artifact name).

**Generated defaults, overridable fields.** Generated code's `defaults(jdbcUrl, user, password)` factory pre-fills `scopeName`/`scopeVersion`/`poolName`/`artifactName` with sensible per-artifact values. These stay plain data fields on the library-owned record — not compile-time-only constants — so an application running multiple instances of the same generated artifact against different databases can override e.g. `poolName` to disambiguate metrics/pools per instance.

**`GlobalOpenTelemetry.get()` stays the default** OTel instance when not explicitly overridden — matching how off-the-shelf OTel instrumentation libraries default. Explicit override remains available via the config's `openTelemetry` field/wither for applications doing explicit SDK wiring.

**Internal wiring rule: derive once, pass the derived thing down.** The session's constructor derives `Tracer`/`Meter` once from `config.openTelemetry()` + `scopeName`/`scopeVersion`, then hands each internal collaborator only the most-derived thing it actually needs — never the raw config threaded through for convenience:

- The statement-execution collaborator receives `Tracer`/`Meter`/instruments directly (not the whole config).
- The transaction-context decorator stays span-only — no config/tracer/meter needed.
- HikariCP pool-metrics registration receives the raw `OpenTelemetry` instance (not a derived `Meter` — `HikariTelemetry.create(openTelemetry)` wants the top-level instance) plus `config.poolName()`.
- The logger stays a plain per-class `LoggerFactory.getLogger(...)`, untouched by config — consistent with [§3](#3-instrumentation-surface)'s SLF4J decision (no config-driven logging wiring).

**HikariCP pool-metrics wiring** swaps today's bespoke `registerPoolGauges()` (four hand-rolled `ObservableLongGauge` callbacks polling `HikariPoolMXBean`) for the upstream `opentelemetry-hikaricp-3.0` integration — `HikariTelemetry.create(config.openTelemetry()).createMetricsTrackerFactory()`, registered on `HikariConfig` before the pool is built. This is a pure implementation swap that needs no config fields beyond the already-listed `openTelemetry` and `poolName`; **it does not change the metric names or shape** — those stay `pgenie.pool.connections.*` per [§3](#3-instrumentation-surface)'s "pool metrics stay bespoke" decision, so the swap is purely about *how* the gauges are read (SPI callback vs. JMX polling), not what they emit.

**Consumer setup, end to end:**

```java
// Application wiring (once, at startup)
OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
        .setTracerProvider(...)
        .setMeterProvider(...)
        .buildAndRegisterGlobal();

// Per-artifact session construction
MusicCatalogueConfig config = MusicCatalogueConfig.defaults(jdbcUrl, user, password);
// .withOpenTelemetry(explicitSdk)             — only if not using the global
// .withPoolName("music-catalogue-secondary")  — only for multi-instance disambiguation

try (MusicCatalogueSession session = new MusicCatalogueSession(config)) {
    var result = session.execute(someGeneratedStatement);
}
```

## 6. Left open

Explicitly deferred by the closed tickets above — not fog, not blocking anything else in this spec, but not resolved by it either:

- **Whether the generated per-artifact config is a subtype of, or just a populated instance of, `rich-client`'s generic config type.** A codegen/instrumentation-surface packaging detail ([Config & wiring surface](https://github.com/pgenie-io/java.gen-design/issues/13)'s resolution), not a runtime-behavior question — revisit when the generator template work that implements this spec is planned.
- **Adoption of the `db.client.connection.*` pool-metric family and `opentelemetry-hikaricp-3.0`'s metric *naming*** (the SPI *wiring swap* itself is decided, [§5](#5-config--wiring-surface)) — tracked-but-deferred until the semconv family stabilizes past `development`.
- **The OTel Events API for the slow-query log line specifically**, if it later graduates to stable — the pattern to reach for at that point is `LogRecordBuilder.setEventName(...)` on the stable base `LogRecordBuilder` interface (available since `opentelemetry-api:1.50.0`), not the incubator `ExtendedLogRecordBuilder` — but this is a future decision informed by re-checking the Events API's stability at that time, not a default to adopt now ([Idiomatic OTel logging from a library](https://github.com/pgenie-io/java.gen-design/issues/9)'s resolution).

## 7. Out of scope

Ruled beyond this map's destination, not part of this spec:

- Performing the refactor/migration itself — extracting `rich-client`, migrating `music-catalogue` onto it, and any interim compatibility concerns are a later effort.
- pGenie generator template changes and artifact versioning/distribution — including the codegen work needed to supply `db.operation.name`/`db.collection.name` per statement class ([§3](#3-instrumentation-surface)).

## 8. Sources

Full detail behind every decision above lives in the map's closed tickets and their linked assets:

- [Survey off-the-shelf OTel instrumentation](https://github.com/pgenie-io/java.gen-design/issues/7) / [`docs/research/otel-instrumentation-survey.md`](research/otel-instrumentation-survey.md)
- [DB-client semantic-conventions audit](https://github.com/pgenie-io/java.gen-design/issues/8) / [`docs/research/db-client-semconv-audit.md`](research/db-client-semconv-audit.md)
- [Idiomatic OTel logging from a library](https://github.com/pgenie-io/java.gen-design/issues/9) / [`docs/research/otel-library-logging.md`](research/otel-library-logging.md)
- [Library split decision](https://github.com/pgenie-io/java.gen-design/issues/10)
- [postgresql-jdbc observability seam](https://github.com/pgenie-io/java.gen-design/issues/11)
- [Instrumentation surface](https://github.com/pgenie-io/java.gen-design/issues/12)
- [Config & wiring surface](https://github.com/pgenie-io/java.gen-design/issues/13)
- Map: [Observability design for pgenie Java artifacts](https://github.com/pgenie-io/java.gen-design/issues/6)
