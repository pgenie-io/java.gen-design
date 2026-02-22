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
import java.util.ArrayList;
import java.util.List;

/**
 * Type-safe binding for the {@code update_album_recording_returning} query.
 *
 * <h2>SQL Template</h2>
 * <pre>{@code
 * -- Update album recording information
 * update album
 * set recording = $recording
 * where id = $id
 * returning *
 * }</pre>
 *
 * <h2>Source Path</h2>
 * {@code ./queries/update_album_recording_returning.sql}
 *
 * <p>Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 *
 * @param recording Maps to {@code $recording} in the template. Nullable.
 * @param id        Maps to {@code $id} in the template. Nullable.
 */
public final class UpdateAlbumRecordingReturning
        implements Statement<UpdateAlbumRecordingReturning.Output> {

    private final RecordingInfo recording;
    private final Long id;

    public UpdateAlbumRecordingReturning(RecordingInfo recording, Long id) {
        this.recording = recording;
        this.id = id;
    }

    public RecordingInfo recording() {
        return recording;
    }

    public Long id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateAlbumRecordingReturning)) return false;
        UpdateAlbumRecordingReturning that = (UpdateAlbumRecordingReturning) o;
        return java.util.Objects.equals(recording, that.recording) &&
                java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(recording, id);
    }

    @Override
    public String toString() {
        return "UpdateAlbumRecordingReturning[recording=" + recording +
                ", id=" + id + "]";
    }


    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    /** Result of the statement parameterised by {@link UpdateAlbumRecordingReturning}. */
    public static final class Output extends ArrayList<OutputRow> {
        Output(List<OutputRow> rows) {
            super(rows);
        }
    }

    /** Row of {@link Output}. */
    public static final class OutputRow {

        private final long id;
        private final String name;
        private final LocalDate released;
        private final AlbumFormat format;
        private final RecordingInfo recording;

        OutputRow(long id, String name, LocalDate released, AlbumFormat format, RecordingInfo recording) {
            this.id = id;
            this.name = name;
            this.released = released;
            this.format = format;
            this.recording = recording;
        }

        /** Maps to the {@code id} result-set column. */
        public long id() { return id; }
        /** Maps to the {@code name} result-set column. */
        public String name() { return name; }
        /** Maps to the {@code released} result-set column. Nullable. */
        public LocalDate released() { return released; }
        /** Maps to the {@code format} result-set column. Nullable. */
        public AlbumFormat format() { return format; }
        /** Maps to the {@code recording} result-set column. Nullable. */
        public RecordingInfo recording() { return recording; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OutputRow)) return false;
            OutputRow that = (OutputRow) o;
            return id == that.id &&
                    java.util.Objects.equals(name, that.name) &&
                    java.util.Objects.equals(released, that.released) &&
                    java.util.Objects.equals(format, that.format) &&
                    java.util.Objects.equals(recording, that.recording);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name, released, format, recording);
        }

        @Override
        public String toString() {
            return "OutputRow[id=" + id +
                    ", name=" + name +
                    ", released=" + released +
                    ", format=" + format +
                    ", recording=" + recording + "]";
        }
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
        ps.setObject(1, RecordingInfo.toPgObject(this.recording()));
        if (this.id() != null) {
            ps.setLong(2, this.id());
        } else {
            ps.setNull(2, Types.BIGINT);
        }
    }

    @Override
    public Output decodeResult(PreparedStatement ps) throws SQLException {
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
