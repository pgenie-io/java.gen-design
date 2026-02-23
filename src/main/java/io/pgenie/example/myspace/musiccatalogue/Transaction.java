package io.pgenie.example.myspace.musiccatalogue;

import java.sql.SQLException;

/**
 * User-defined transaction logic executed by {@link Pool#transact}.
 *
 * <p>Implement this interface to define a unit of work that runs inside a
 * database transaction.  The {@link #run} method receives a
 * {@link TransactionContext} for executing statements and returns a
 * {@link TransactionOutcome} that carries both the result and a
 * commit-or-rollback decision.
 *
 * <p>Override {@link #isolationLevel()}, {@link #readOnly()}, or
 * {@link #deferrable()} to customise transaction characteristics.  The
 * defaults mirror PostgreSQL's session defaults ({@code READ COMMITTED},
 * read-write, non-deferrable).
 *
 * @param <R> the result type produced by the transaction
 */
public interface Transaction<R> {

    /** Isolation level for the transaction. Defaults to {@link IsolationLevel#READ_COMMITTED}. */
    default IsolationLevel isolationLevel() {
        return IsolationLevel.READ_COMMITTED;
    }

    /** Whether the transaction is read-only. Defaults to {@code false}. */
    default boolean readOnly() {
        return false;
    }

    /**
     * Whether the transaction is deferrable (only meaningful for
     * {@code SERIALIZABLE} read-only transactions). Defaults to {@code false}.
     */
    default boolean deferrable() {
        return false;
    }

    /**
     * Execute the transaction body using {@code ctx} and return a
     * {@link TransactionOutcome} indicating the result and whether to commit.
     *
     * <p>Throw {@link SQLException} to signal a failure; {@link Pool#transact}
     * will roll back and, if the error is a serialisation failure (SQLState
     * {@code 40001}) or deadlock (SQLState {@code 40P01}), automatically retry.
     */
    TransactionOutcome<R> run(TransactionContext ctx) throws SQLException;
}
