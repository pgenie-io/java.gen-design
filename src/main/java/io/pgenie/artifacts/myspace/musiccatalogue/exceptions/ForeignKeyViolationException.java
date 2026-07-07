package io.pgenie.artifacts.myspace.musiccatalogue.exceptions;

import java.sql.SQLException;

/**
 * Thrown when an insert or update violates a foreign-key constraint.
 *
 * <p>This corresponds to PostgreSQL SQLSTATE {@code 23503}. The original
 * {@link SQLException} is preserved as the cause.</p>
 */
public class ForeignKeyViolationException extends MusicCatalogueException {

    /**
     * Creates a new exception with a custom message and the original SQL failure.
     *
     * @param message the detail message
     * @param cause   the original {@code SQLException}
     */
    public ForeignKeyViolationException(String message, SQLException cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception using the original SQL failure's message.
     *
     * @param cause the original {@code SQLException}
     */
    public ForeignKeyViolationException(SQLException cause) {
        super(cause);
    }
}
