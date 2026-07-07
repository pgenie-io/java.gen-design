package io.pgenie.artifacts.myspace.musiccatalogue.exceptions;

import java.sql.SQLException;

/**
 * Thrown when the database cannot be reached or the connection fails.
 *
 * <p>This corresponds to SQLSTATE class {@code 08} (connection exception), such as
 * {@code 08001}, {@code 08003}, {@code 08006} and {@code 08008}. The original
 * {@link SQLException} is preserved as the cause.</p>
 */
public class ConnectionFailureException extends MusicCatalogueException {

    /**
     * Creates a new exception with a custom message and the original SQL failure.
     *
     * @param message the detail message
     * @param cause   the original {@code SQLException}
     */
    public ConnectionFailureException(String message, SQLException cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception using the original SQL failure's message.
     *
     * @param cause the original {@code SQLException}
     */
    public ConnectionFailureException(SQLException cause) {
        super(cause);
    }
}
