package io.pgenie.artifacts.myspace.musiccatalogue.exceptions;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/**
 * Base unchecked exception for the MusicCatalogue database layer.
 *
 * <p>Every subclass wraps the original {@link SQLException} as its cause so that
 * callers still have access to vendor-specific details while enjoying a type-safe,
 * SQLSTATE-driven exception hierarchy.</p>
 */
public class MusicCatalogueException extends RuntimeException {

    /**
     * Creates a new exception with a custom message and the original SQL failure.
     *
     * @param message the detail message
     * @param cause   the original {@code SQLException}; must not be {@code null}
     */
    public MusicCatalogueException(String message, SQLException cause) {
        super(message, Objects.requireNonNull(cause, "cause"));
    }

    /**
     * Creates a new exception using the original SQL failure's message.
     *
     * @param cause the original {@code SQLException}; must not be {@code null}
     */
    public MusicCatalogueException(SQLException cause) {
        super(Objects.requireNonNull(cause, "cause").getMessage(), cause);
    }

    /**
     * Returns the SQLSTATE of the wrapped {@code SQLException}, if any.
     *
     * @return an {@link Optional} containing the SQLSTATE, or empty if the cause
     *         did not report one
     */
    public Optional<String> sqlState() {
        return Optional.ofNullable(((SQLException) getCause()).getSQLState());
    }
}
