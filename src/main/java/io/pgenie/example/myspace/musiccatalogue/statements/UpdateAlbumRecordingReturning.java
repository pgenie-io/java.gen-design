package io.pgenie.example.myspace.musiccatalogue.statements;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;

import io.pgenie.example.myspace.musiccatalogue.Statement;
import io.pgenie.example.myspace.musiccatalogue.codecs.Codec;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;

/**
 * Type-safe binding for the {@code update_album_recording_returning} query.
 *
 * <h2>SQL Template</h2>
 *
 * <pre>{@code
 * -- Update album recording information
 * update album
 * set recording = $recording
 * where id = $id
 * returning *
 * }</pre>
 *
 * <h2>Source Path</h2> {@code ./queries/update_album_recording_returning.sql}
 *
 * <p>
 * Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 *
 * @param recording Maps to {@code $recording} in the template. Nullable.
 * @param id Maps to {@code $id} in the template. Nullable.
 */
public record UpdateAlbumRecordingReturning(RecordingInfo recording, Long id)
        implements Statement<UpdateAlbumRecordingReturning.Output> {

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------
    /**
     * Result of the statement parameterised by
     * {@link UpdateAlbumRecordingReturning}.
     */
    public static final class Output extends ArrayList<OutputRow> {

        Output() {
        }
    }

    /**
     * Row of {@link Output}.
     */
    public record OutputRow(
            /**
             * Maps to the {@code id} result-set column.
             */
            long id,
            /**
             * Maps to the {@code name} result-set column.
             */
            String name,
            /**
             * Maps to the {@code released} result-set column. Nullable.
             */
            LocalDate released,
            /**
             * Maps to the {@code format} result-set column. Nullable.
             */
            AlbumFormat format,
            /**
             * Maps to the {@code recording} result-set column. Nullable.
             */
            RecordingInfo recording) {

    }

    // -------------------------------------------------------------------------
    // Statement implementation
    // -------------------------------------------------------------------------
    @Override
    public String sql() {
        return """
                -- Update album recording information
                update album
                set recording = ?::recording_info
                where id = ?
                returning *""";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
        RecordingInfo.CODEC.bind(ps, 1, this.recording());
        if (this.id() != null) {
            ps.setLong(2, this.id());
        } else {
            ps.setNull(2, Types.BIGINT);
        }
    }

    @Override
    public boolean returnsRows() {
        return true;
    }

    @Override
    public Output decodeResultSet(ResultSet rs) throws SQLException {
        Output output = new Output();
        while (rs.next()) {
            long id = rs.getLong(1);
            String name = rs.getString(2);
            Date releasedSql = rs.getDate(3);
            LocalDate released = releasedSql != null ? releasedSql.toLocalDate() : null;
            String formatStr = rs.getString(4);
            String recordingStr = rs.getString(5);
            try {
                AlbumFormat format = formatStr != null ? AlbumFormat.CODEC.parse(formatStr, 0).value : null;
                RecordingInfo recording = recordingStr != null ? RecordingInfo.CODEC.parse(recordingStr, 0).value : null;
                output.add(new OutputRow(id, name, released, format, recording));
            } catch (Codec.ParseException e) {
                throw new IllegalStateException(e);
            }
        }
        return output;
    }

    @Override
    public Output decodeAffectedRows(long affectedRows) {
        throw new UnsupportedOperationException();
    }
}
