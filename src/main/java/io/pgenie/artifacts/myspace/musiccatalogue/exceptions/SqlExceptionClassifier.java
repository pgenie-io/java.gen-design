package io.pgenie.artifacts.myspace.musiccatalogue.exceptions;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/**
 * Translates {@link SQLException}s into typed, unchecked {@link MusicCatalogueException}s.
 *
 * <p>The classification is driven by {@link SQLException#getSQLState()} and follows
 * PostgreSQL conventions. SQLSTATEs that are {@code null}, empty or unrecognised are
 * mapped to {@link DatabaseException}, except for {@link SQLTimeoutException} which is
 * mapped to {@link QueryTimeoutException}.</p>
 */
public final class SqlExceptionClassifier {

    private SqlExceptionClassifier() {
        // utility class
    }

    /**
     * Classifies the given {@code SQLException} into the most specific unchecked
     * exception subtype.
     *
     * @param e the original {@code SQLException}; must not be {@code null}
     * @return a {@link MusicCatalogueException} wrapping {@code e}
     */
    public static RuntimeException classify(SQLException e) {
        String state = e.getSQLState();

        if (state != null && !state.isEmpty()) {
            if (state.length() >= 2) {
                String sqlClass = state.substring(0, 2);
                if ("08".equals(sqlClass)) {
                    return new ConnectionFailureException(e);
                }
                if ("23".equals(sqlClass)) {
                    if ("23503".equals(state)) {
                        return new ForeignKeyViolationException(e);
                    }
                    return new UniqueViolationException(e);
                }
            }

            switch (state) {
                case "40001" -> {
                    return new SerializationFailureException(e);
                }
                case "40P01" -> {
                    return new DeadlockException(e);
                }
                case "57014" -> {
                    return new QueryTimeoutException(e);
                }
                default -> {
                    // fall through to catch-all
                }
            }
        }

        if (e instanceof SQLTimeoutException) {
            return new QueryTimeoutException(e);
        }

        return new DatabaseException(e);
    }
}
