package io.pgenie.artifacts.myspace.musiccatalogue.exceptions;

import java.sql.SQLException;

/**
 * Thrown when a transaction cannot be serialized and must be retried.
 *
 * <p>This corresponds to PostgreSQL SQLSTATE {@code 40001} (serialization failure).
 * The original {@link SQLException} is preserved as the cause.</p>
 */
public class SerializationFailureException extends MusicCatalogueException {

    /**
     * Creates a new exception with a custom message and the original SQL failure.
     *
     * @param message the detail message
     * @param cause   the original {@code SQLException}
     */
    public SerializationFailureException(String message, SQLException cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception using the original SQL failure's message.
     *
     * @param cause the original {@code SQLException}
     */
    public SerializationFailureException(SQLException cause) {
        super(cause);
    }
}
