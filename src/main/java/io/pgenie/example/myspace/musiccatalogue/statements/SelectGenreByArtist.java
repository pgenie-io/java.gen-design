package io.pgenie.example.myspace.musiccatalogue.statements;

import io.pgenie.example.myspace.musiccatalogue.Statement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Type-safe binding for the {@code select_genre_by_artist} query.
 *
 * <h2>SQL Template</h2>
 * <pre>{@code
 * select id, genre.name
 * from genre
 * left join album_genre on album_genre.genre = genre.id
 * left join album_artist on album_artist.album = album_genre.album
 * where album_artist.artist = $artist
 * }</pre>
 *
 * <h2>Source Path</h2>
 * {@code ./queries/select_genre_by_artist.sql}
 *
 * <p>Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 *
 * @param artist Maps to {@code $artist} in the template. Nullable.
 */
public final class SelectGenreByArtist implements Statement<SelectGenreByArtist.Output> {

    private final Integer artist;

    public SelectGenreByArtist(Integer artist) {
        this.artist = artist;
    }

    public Integer artist() {
        return artist;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectGenreByArtist)) return false;
        SelectGenreByArtist that = (SelectGenreByArtist) o;
        return java.util.Objects.equals(artist, that.artist);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(artist);
    }

    @Override
    public String toString() {
        return "SelectGenreByArtist[artist=" + artist + "]";
    }


    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    /** Result of the statement parameterised by {@link SelectGenreByArtist}. */
    public static final class Output extends ArrayList<OutputRow> {
        Output(List<OutputRow> rows) {
            super(rows);
        }
    }

    /** Row of {@link Output}. */
    public static final class OutputRow {

        private final int id;
        private final String name;

        OutputRow(int id, String name) {
            this.id = id;
            this.name = name;
        }

        /** Maps to the {@code id} result-set column. */
        public int id() { return id; }
        /** Maps to the {@code name} result-set column. */
        public String name() { return name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OutputRow)) return false;
            OutputRow that = (OutputRow) o;
            return id == that.id && java.util.Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "OutputRow[id=" + id + ", name=" + name + "]";
        }
    }


    // -------------------------------------------------------------------------
    // Statement implementation
    // -------------------------------------------------------------------------

    @Override
    public String sql() {
        return """
                select id, genre.name
                from genre
                left join album_genre on album_genre.genre = genre.id
                left join album_artist on album_artist.album = album_genre.album
                where album_artist.artist = ?""";
    }

    @Override
    public void bindParams(PreparedStatement ps) throws SQLException {
        if (this.artist() != null) {
            ps.setInt(1, this.artist());
        } else {
            ps.setNull(1, Types.INTEGER);
        }
    }

    @Override
    public Output decodeResult(PreparedStatement ps) throws SQLException {
        List<OutputRow> rows = new ArrayList<>();
        try (ResultSet rs = ps.getResultSet()) {
            while (rs.next()) {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                rows.add(new OutputRow(id, name));
            }
        }
        return new Output(rows);
    }
}
