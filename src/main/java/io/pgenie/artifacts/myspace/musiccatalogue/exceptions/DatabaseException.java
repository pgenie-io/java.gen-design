package io.pgenie.artifacts.myspace.musiccatalogue.exceptions;

import java.sql.SQLException;

/**
 * Catch-all unchecked exception for database failures that do not map to a more
 * specific subtype.
 *
 * <p>The original {@link SQLException} is preserved as the cause.</p>
 */
public class DatabaseException extends MusicCatalogueException {

    /**
     * Creates a new exception with a custom message and the original SQL failure.
     *
     * @param message the detail message
     * @param cause   the original {@code SQLException}
     */
    public DatabaseException(String message, SQLException cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception using the original SQL failure's message.
     *
     * @param cause the original {@code SQLException}
     */
    public DatabaseException(SQLException cause) {
        super(cause);
    }
}
