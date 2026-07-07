package io.pgenie.artifacts.myspace.musiccatalogue.exceptions;

import java.sql.SQLException;

/**
 * Thrown when an insert or update violates a uniqueness constraint.
 *
 * <p>This corresponds to SQLSTATE class {@code 23} (integrity constraint violation),
 * such as PostgreSQL's {@code 23505} unique violation. The original
 * {@link SQLException} is preserved as the cause.</p>
 */
public class UniqueViolationException extends MusicCatalogueException {

    /**
     * Creates a new exception with a custom message and the original SQL failure.
     *
     * @param message the detail message
     * @param cause   the original {@code SQLException}
     */
    public UniqueViolationException(String message, SQLException cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception using the original SQL failure's message.
     *
     * @param cause the original {@code SQLException}
     */
    public UniqueViolationException(SQLException cause) {
        super(cause);
    }
}
