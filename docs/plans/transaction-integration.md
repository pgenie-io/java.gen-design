# Plan: Integrate vendor `Transaction` into music-catalogue

## Goal

Replace the custom transaction abstraction in `music-catalogue` (`MusicCatalogueSession.transaction(...)` / `TransactionWork`) with the vendored `io.codemine.java.postgresql.jdbc.Transaction` API, while preserving existing OpenTelemetry instrumentation, retry counting, and test coverage.

## Decisions

| Topic | Decision |
|-------|----------|
| Scope | Replace only the transaction methods in `MusicCatalogueSession`; keep the session as pool owner and statement executor. |
| Public API | `MusicCatalogueSession.executeTransaction(Transaction<R>)` and `executeTransaction(Transaction<R>, TransactionSettings)`. The existing `execute(Statement<R>)` is unchanged. |
| Old API | Remove `MusicCatalogueSession.transaction(...)` and `TransactionWork` outright (no deprecated shims). |
| Vendor modifications | None. Treat `vendor/postgresql-jdbc.java` as a fixed dependency. |
| Retry states | Use vendor defaults: retries on SQLSTATE `40001`, `40P01`, and `23505`. |
| Retry attempts | Map `MusicCatalogueConfig.transactionRetryAttempts()` (default 3) to `TransactionSettings.maxAttempts()`. Clamp to at least 1. |
| Retry delay | Accept vendor's immediate retry; drop the old exponential-backoff/jitter logic. |
| Statement execution inside transactions | Implemented by an `ObservableExecutionContext` / `ObservableTransactionContext` that delegates to a shared `StatementExecutor`. |
| Statement observability | Statement spans/metrics inside transactions are identical to non-transactional execution and are children of the transaction span. |
| Batch execution | Single span/metric per batch, with a `pgenie.statement.batch_size` attribute. |
| Transaction span attributes | `db.system=postgresql`, `db.transaction.isolation_level`, `pgenie.transaction.max_attempts`, `pgenie.transaction.read_only`. |
| Retry counting | `ObservableTransactionContext` tracks `commit()` and no-arg `rollback()` calls. After `executeOn` returns: `retries = commitCalled ? rollbackCount : max(0, rollbackCount - 1)`. |
| Exception recording | The shared `StatementExecutor` records **all** `Throwable`s on the statement span, not only `SQLException`. |
| Tests | Rewrite `TransactionIT` and `TransactionRetryIT` for the new API with equivalent coverage plus a retry-counter metric assertion. |
| New classes | `StatementExecutor`, `ObservableTransactionContext` as package-private classes in `io.pgenie.artifacts.myspace.musiccatalogue`. |

## Implementation steps

1. **Create `StatementExecutor`**
   - Instance class created in `MusicCatalogueSession` constructor.
   - Holds `MusicCatalogueConfig`, `Tracer`, `Meter`, `Logger`.
   - Provides:
     - `<R> R execute(Statement<R>, Connection, Span parentSpan)` for single statements.
     - `<R> List<R> executeBatch(Iterable<? extends Statement<R>>, Connection, Span parentSpan)` for batches.
   - Emits statement spans (CLIENT, child of `parentSpan` if non-null), records duration histogram with `db.query.text` and `pgenie.statement.name`, logs slow queries, records all throwables.

2. **Refactor `MusicCatalogueSession.execute(Statement)`**
   - Delegate statement execution to `StatementExecutor` with no parent span (root CLIENT span).

3. **Create `ObservableTransactionContext`**
   - Implements `TransactionContext`.
   - Wraps `TransactionContext.of(connection)`.
   - Tracks `commitCalled` and `rollbackCount` (no-arg `rollback` only).
   - `execute(Statement)` delegates to `StatementExecutor` with the transaction span as parent.
   - `executeBatch(...)` delegates similarly with batch-size attribute.
   - All other `TransactionContext` methods delegate straight through.

4. **Add transaction entry points to `MusicCatalogueSession`**
   - `public <R> R executeTransaction(Transaction<R> transaction) throws SQLException`
   - `public <R> R executeTransaction(Transaction<R> transaction, TransactionSettings settings) throws SQLException`
   - Both:
     - Create a transaction INTERNAL span.
     - Borrow a connection.
     - Build default settings from config when none provided (`SERIALIZABLE`, `readOnly=false`, `maxAttempts = max(1, config.transactionRetryAttempts())`).
     - Call `transaction.executeOn(observableContext, settings)`.
     - After return, compute retries from the context and emit `transactionRetryCounter`.
     - Close the connection in `finally`.

5. **Remove old transaction code**
   - Delete `TransactionWork` interface.
   - Delete `transaction(TransactionWork)` and `transaction(int, TransactionWork)` methods.
   - Delete helper methods only used by the old transaction path: `isRetryable`, `retryDelayMillis`, `sleepWithInterruptHandling`, `SingleConnectionDataSource`.
   - Keep `rollbackQuietly`, `restoreAndCloseQuietly` only if still needed elsewhere (likely not; remove if unused).

6. **Update tests**
   - Rewrite `TransactionIT`:
     - Commit path via `session.executeTransaction(Transaction.of(new InsertAlbum(...)))`.
     - Rollback path via a transaction that throws.
     - Custom isolation via `TransactionSettings.withIsolationLevel(IsolationLevel.SERIALIZABLE)` (or another level).
   - Rewrite `TransactionRetryIT`:
     - Concurrent counter test using `session.executeTransaction(...)` with `IsolationLevel.SERIALIZABLE`.
     - Add assertion that `transactionRetryCounter` is non-zero after a conflict.
   - Update `AbstractDatabaseIT` if it exposes any transaction helpers.

7. **Build and verify**
   - `mvn -pl music-catalogue verify`.
   - Fix any compilation or test failures.

## Files to modify

- `music-catalogue/src/main/java/io/pgenie/artifacts/myspace/musiccatalogue/MusicCatalogueSession.java`
- `music-catalogue/src/test/java/io/pgenie/artifacts/myspace/musiccatalogue/TransactionIT.java`
- `music-catalogue/src/test/java/io/pgenie/artifacts/myspace/musiccatalogue/TransactionRetryIT.java`

## Files to create

- `music-catalogue/src/main/java/io/pgenie/artifacts/myspace/musiccatalogue/StatementExecutor.java`
- `music-catalogue/src/main/java/io/pgenie/artifacts/myspace/musiccatalogue/ObservableTransactionContext.java`

## Risks / notes

- The `executeTransaction` name was chosen because `execute(Transaction<R>)` collides with `execute(Statement<R>)` after Java type erasure.
- Retry counting assumes no-arg `rollback()` is only called by the vendor retry loop. This is safe because transaction bodies only receive `ExecutionContext`, which exposes only `rollback(Savepoint)`.
- Vendor retries on `23505` (unique violation) in addition to `40001`/`40P01`; this is a behavioral change from the old code, which only retried `40001`/`40P01`.
- The old exponential-backoff/jitter delay is removed; retries happen immediately.
