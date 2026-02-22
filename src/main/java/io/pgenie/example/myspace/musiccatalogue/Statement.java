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
     * Whether this statement returns rows (i.e. is a {@code SELECT} or
     * contains a {@code RETURNING} clause).
     *
     * <p>When {@code true} the execution functions use
     * {@link PreparedStatement#execute()} so that the result set is available
     * via {@link PreparedStatement#getResultSet()}.  When {@code false} they
     * use {@link PreparedStatement#executeUpdate()} instead, which returns the
     * actual number of rows affected by the statement.
     */
    boolean returnsRows();

    /**
     * Decode the result of the statement after execution.
     *
     * <ul>
     *   <li>Query statements (SELECT / UPDATE … RETURNING) read
     *       {@link PreparedStatement#getResultSet()}.</li>
     *   <li>DML statements without a returning clause use
     *       {@code affectedRows} directly.</li>
     * </ul>
     *
     * @param affectedRows the number of rows affected; for row-returning
     *                     statements this is {@code 0} and may be ignored.
     */
    R decodeResult(PreparedStatement ps, long affectedRows) throws SQLException;
}
