package io.pgenie.example.myspace.musiccatalogue;

import java.sql.Connection;

/**
 * Transaction isolation level.
 *
 * <p>
 * Passed to {@link Pool#transact} via {@link Transaction#isolationLevel()}.
 */
public enum IsolationLevel {

    /**
     * An individual statement sees rows committed before it began.
     */
    READ_COMMITTED,
    /**
     * All statements see the same snapshot of rows committed before the first
     * query in the transaction.
     */
    REPEATABLE_READ,
    /**
     * Reads and writes must be serialisable with respect to all other
     * concurrent serialisable transactions.
     */
    SERIALIZABLE;

    /**
     * Convert to the corresponding {@link Connection}{@code .TRANSACTION_*}
     * constant.
     */
    int toJdbc() {
        return switch (this) {
            case READ_COMMITTED ->
                Connection.TRANSACTION_READ_COMMITTED;
            case REPEATABLE_READ ->
                Connection.TRANSACTION_REPEATABLE_READ;
            case SERIALIZABLE ->
                Connection.TRANSACTION_SERIALIZABLE;
        };
    }
}
