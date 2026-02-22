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
public final class SelectAlbumByFormat implements Statement<SelectAlbumByFormat.Output> {

    private final AlbumFormat format;

    public SelectAlbumByFormat(AlbumFormat format) {
        this.format = format;
    }

    public AlbumFormat format() {
        return format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectAlbumByFormat)) return false;
        SelectAlbumByFormat that = (SelectAlbumByFormat) o;
        return java.util.Objects.equals(format, that.format);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(format);
    }

    @Override
    public String toString() {
        return "SelectAlbumByFormat[format=" + format + "]";
    }


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
