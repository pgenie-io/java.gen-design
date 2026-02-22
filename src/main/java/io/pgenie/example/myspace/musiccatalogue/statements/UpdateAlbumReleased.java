package io.pgenie.example.myspace.musiccatalogue.statements;

import io.pgenie.example.myspace.musiccatalogue.Statement;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

/**
 * Type-safe binding for the {@code update_album_released} query.
 *
 * <h2>SQL Template</h2>
 * <pre>{@code
 * update album
 * set released = $released
 * where id = $id
 * }</pre>
 *
 * <h2>Source Path</h2>
 * {@code ./queries/update_album_released.sql}
 *
 * <p>Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 *
 * @param released Maps to {@code $released} in the template. Nullable.
 * @param id       Maps to {@code $id} in the template. Nullable.
 */
public record UpdateAlbumReleased(LocalDate released, Long id)
        implements Statement<Long> {

    // -------------------------------------------------------------------------
    // Statement implementation
    //
    // Result type is Long — the number of rows affected by the update.
    // -------------------------------------------------------------------------

    @Override
    public String sql() {
        return """
                update album
                set released = ?
                where id = ?""";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
        if (this.released() != null) {
            ps.setDate(1, Date.valueOf(this.released()));
        } else {
            ps.setNull(1, Types.DATE);
        }
        if (this.id() != null) {
            ps.setLong(2, this.id());
        } else {
            ps.setNull(2, Types.BIGINT);
        }
    }

    /**
     * Returns the number of rows affected by the update.
     *
     * <p>Reads {@link PreparedStatement#getUpdateCount()} after
     * {@link PreparedStatement#execute()} has been called.
     */
    @Override
    public Long decodeResult(PreparedStatement ps) throws SQLException {
        return (long) ps.getUpdateCount();
    }
}
