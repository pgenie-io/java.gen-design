package io.pgenie.example.myspace.musiccatalogue.statements;

import io.pgenie.example.myspace.musiccatalogue.Statement;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Type-safe binding for the {@code select_album_by_format} query.
 *
 * <h2>SQL Template</h2>
 * <pre>{@code
 * select
 *   id,
 *   name,
 *   released,
 *   format,
 *   recording
 * from album
 * where format = $format
 * }</pre>
 *
 * <h2>Source Path</h2>
 * {@code ./queries/select_album_by_format.sql}
 *
 * <p>Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 *
 * @param format Maps to {@code $format} in the template. Nullable.
 */
public record SelectAlbumByFormat(AlbumFormat format)
        implements Statement<SelectAlbumByFormat.Output> {

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    /** Result of the statement parameterised by {@link SelectAlbumByFormat}. */
    public static final class Output extends ArrayList<OutputRow> {
        Output(List<OutputRow> rows) {
            super(rows);
        }
    }

    /** Row of {@link Output}. */
    public record OutputRow(
            /** Maps to the {@code id} result-set column. */
            long id,
            /** Maps to the {@code name} result-set column. */
            String name,
            /** Maps to the {@code released} result-set column. Nullable. */
            LocalDate released,
            /** Maps to the {@code format} result-set column. Nullable. */
            AlbumFormat format,
            /** Maps to the {@code recording} result-set column. Nullable. */
            RecordingInfo recording
    ) {}

    // -------------------------------------------------------------------------
    // Statement implementation
    // -------------------------------------------------------------------------

    @Override
    public String sql() {
        return """
                select
                  id,
                  name,
                  released,
                  format,
                  recording
                from album
                where format = ?::album_format""";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
        ps.setObject(1, AlbumFormat.toPgObject(this.format()));
    }

    @Override
    public boolean returnsRows() {
        return true;
    }

    @Override
    public Output decodeResult(PreparedStatement ps, long affectedRows) throws SQLException {
        List<OutputRow> rows = new ArrayList<>();
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                Date releasedSql = rs.getDate(3);
                LocalDate released = releasedSql != null ? releasedSql.toLocalDate() : null;
                String formatStr = rs.getString(4);
                AlbumFormat format = formatStr != null
                        ? AlbumFormat.fromPgValue(formatStr) : null;
                String recordingStr = rs.getString(5);
                RecordingInfo recording = recordingStr != null
                        ? RecordingInfo.parse(recordingStr) : null;
                rows.add(new OutputRow(id, name, released, format, recording));
            }
        }
        return new Output(rows);
    }
}
