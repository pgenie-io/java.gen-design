package io.pgenie.example.myspace.musiccatalogue;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implemented by each query's parameter+result class. Provides a uniform way
 * to prepare and execute statements against a JDBC {@link java.sql.Connection}.
 *
 * <p>Generated from SQL queries using the <a href="https://pgenie.io">pGenie</a>
 * code generator.
 *
 * @param <P> the parameter type passed to {@link #bindParams}
 * @param <R> the result type returned by {@link #decodeResult}
 */
public interface Statement<R> {

    /**
     * The SQL text for this statement. Parameter placeholders use JDBC
     * {@code ?} syntax; custom PostgreSQL types are cast explicitly, e.g.
     * {@code ?::album_format}.
     */
    String sql();

    /**
     * Bind to the prepared statement's parameter slots.
     *
     * <p>Implementations set positional parameters starting at index 1.
     * Custom types ({@code AlbumFormat}, {@code RecordingInfo}) are bound as
     * {@link org.postgresql.util.PGobject} instances so that the PostgreSQL
     * driver sends the correct type OID.
     */
    void bindParams(PreparedStatement ps) throws SQLException;

    /**
     * Decode the result of the statement after {@link PreparedStatement#execute()}
     * has been called.
     *
     * <ul>
     *   <li>Query statements (SELECT / UPDATE … RETURNING) read
     *       {@link PreparedStatement#getResultSet()}.</li>
     *   <li>DML statements without a returning clause read
     *       {@link PreparedStatement#getUpdateCount()}.</li>
     * </ul>
     */
    R decodeResult(PreparedStatement ps) throws SQLException;
}
