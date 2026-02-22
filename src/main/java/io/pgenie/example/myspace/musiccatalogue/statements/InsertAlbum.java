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
public final class InsertAlbum implements Statement<InsertAlbum.Output> {

    private final String name;
    private final LocalDate released;
    private final AlbumFormat format;
    private final RecordingInfo recording;

    public InsertAlbum(String name, LocalDate released, AlbumFormat format, RecordingInfo recording) {
        this.name = name;
        this.released = released;
        this.format = format;
        this.recording = recording;
    }

    public String name() {
        return name;
    }

    public LocalDate released() {
        return released;
    }

    public AlbumFormat format() {
        return format;
    }

    public RecordingInfo recording() {
        return recording;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsertAlbum)) return false;
        InsertAlbum that = (InsertAlbum) o;
        return java.util.Objects.equals(name, that.name) &&
                java.util.Objects.equals(released, that.released) &&
                java.util.Objects.equals(format, that.format) &&
                java.util.Objects.equals(recording, that.recording);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, released, format, recording);
    }

    @Override
    public String toString() {
        return "InsertAlbum[name=" + name +
                ", released=" + released +
                ", format=" + format +
                ", recording=" + recording + "]";
    }

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    /** Result of the statement parameterised by {@link Input}. */
    public static final class Output {

        private final long id;

        Output(long id) {
            this.id = id;
        }

        /** Maps to the {@code id} result-set column. */
        public long id() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Output)) return false;
            Output that = (Output) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Output[id=" + id + "]";
        }
    }

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
    public Output decodeResult(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getResultSet()) {
            rs.next();
            return new Output(rs.getLong(1));
        }
    }
}
