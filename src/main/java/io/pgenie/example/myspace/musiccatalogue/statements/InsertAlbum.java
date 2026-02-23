package io.pgenie.example.myspace.musiccatalogue.statements;

import io.pgenie.example.myspace.musiccatalogue.Statement;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

/**
 * Type-safe binding for the {@code insert_album} query.
 *
 * <h2>SQL Template</h2>
 * <pre>{@code
 * insert into album (name, released, format, recording)
 * values ($name, $released, $format, $recording)
 * returning id
 * }</pre>
 *
 * <h2>Source Path</h2>
 * {@code ./queries/insert_album.sql}
 *
 * <p>Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 * 
 * @param name      Maps to {@code $name} in the template. Non-null.
 * @param released  Maps to {@code $released} in the template. Nullable.
 * @param format    Maps to {@code $format} in the template. Nullable.
 * @param recording Maps to {@code $recording} in the template. Nullable.
 */
public record InsertAlbum(
            String name,
            LocalDate released,
            AlbumFormat format,
            RecordingInfo recording
    ) implements Statement<InsertAlbum.Output> {

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    /** Result of the statement parameterised by {@link Input}. */
    public record Output(
            /** Maps to the {@code id} result-set column. */
            long id
    ) {}

    // -------------------------------------------------------------------------
    // Statement implementation
    // -------------------------------------------------------------------------

    @Override
    public String sql() {
        return """
                insert into album (name, released, format, recording)
                values (?, ?, ?::album_format, ?::recording_info)
                returning id""";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
        ps.setString(1, this.name());
        if (this.released() != null) {
            ps.setDate(2, Date.valueOf(this.released()));
        } else {
            ps.setNull(2, Types.DATE);
        }
        ps.setObject(3, AlbumFormat.toPgObject(this.format()));
        ps.setObject(4, RecordingInfo.toPgObject(this.recording()));
    }

    @Override
    public boolean returnsRows() {
        return true;
    }

    @Override
    public Output decodeResultSet(ResultSet rs) throws SQLException {
        rs.next();
        return new Output(rs.getLong(1));
    }

    @Override
    public Output decodeAffectedRows(long affectedRows) {
        throw new UnsupportedOperationException();
    }
}
