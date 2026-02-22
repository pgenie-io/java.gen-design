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
 */
public final class InsertAlbum implements Statement<InsertAlbum.Input, InsertAlbum.Output> {

    /** Singleton — stateless; safe to share across threads. */
    public static final InsertAlbum INSTANCE = new InsertAlbum();

    private InsertAlbum() {}

    // -------------------------------------------------------------------------
    // Parameter type
    // -------------------------------------------------------------------------

    /**
     * Parameters for the {@code insert_album} query.
     *
     * @param name      Maps to {@code $name} in the template. Non-null.
     * @param released  Maps to {@code $released} in the template. Nullable.
     * @param format    Maps to {@code $format} in the template. Nullable.
     * @param recording Maps to {@code $recording} in the template. Nullable.
     */
    public record Input(
            String name,
            LocalDate released,
            AlbumFormat format,
            RecordingInfo recording
    ) {}

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
    public void bindParams(PreparedStatement ps, Input p) throws SQLException {
        ps.setString(1, p.name());
        if (p.released() != null) {
            ps.setDate(2, Date.valueOf(p.released()));
        } else {
            ps.setNull(2, Types.DATE);
        }
        ps.setObject(3, AlbumFormat.toPgObject(p.format()));
        ps.setObject(4, RecordingInfo.toPgObject(p.recording()));
    }

    @Override
    public Output decodeResult(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getResultSet()) {
            rs.next();
            return new Output(rs.getLong(1));
        }
    }
}
