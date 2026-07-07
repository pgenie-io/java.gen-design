package io.pgenie.artifacts.myspace.musiccatalogue.exceptions;

import java.sql.SQLException;

/**
 * Thrown when the database detects a deadlock and aborts the current transaction.
 *
 * <p>This corresponds to PostgreSQL SQLSTATE {@code 40P01} (deadlock detected). The
 * original {@link SQLException} is preserved as the cause.</p>
 */
public class DeadlockException extends MusicCatalogueException {

    /**
     * Creates a new exception with a custom message and the original SQL failure.
     *
     * @param message the detail message
     * @param cause   the original {@code SQLException}
     */
    public DeadlockException(String message, SQLException cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception using the original SQL failure's message.
     *
     * @param cause the original {@code SQLException}
     */
    public DeadlockException(SQLException cause) {
        super(cause);
    }
}
