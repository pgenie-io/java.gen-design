package io.pgenie.artifacts.myspace.musiccatalogue.exceptions;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/**
 * Thrown when a query is cancelled or exceeds its execution timeout.
 *
 * <p>This corresponds to PostgreSQL SQLSTATE {@code 57014} (query cancelled). It is
 * also used for {@link SQLTimeoutException}s when no SQLSTATE is reported. The
 * original {@link SQLException} is preserved as the cause.</p>
 */
public class QueryTimeoutException extends MusicCatalogueException {

    /**
     * Creates a new exception with a custom message and the original SQL failure.
     *
     * @param message the detail message
     * @param cause   the original {@code SQLException}
     */
    public QueryTimeoutException(String message, SQLException cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception using the original SQL failure's message.
     *
     * @param cause the original {@code SQLException}
     */
    public QueryTimeoutException(SQLException cause) {
        super(cause);
    }
}
