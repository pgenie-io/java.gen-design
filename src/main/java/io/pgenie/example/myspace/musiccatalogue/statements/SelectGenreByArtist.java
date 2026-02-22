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
 */
public final class SelectGenreByArtist
        implements Statement<SelectGenreByArtist.Input, SelectGenreByArtist.Output> {

    /** Singleton — stateless; safe to share across threads. */
    public static final SelectGenreByArtist INSTANCE = new SelectGenreByArtist();

    private SelectGenreByArtist() {}

    // -------------------------------------------------------------------------
    // Parameter type
    // -------------------------------------------------------------------------

    /**
     * Parameters for the {@code select_genre_by_artist} query.
     *
     * @param artist Maps to {@code $artist} in the template. Nullable.
     */
    public record Input(Integer artist) {}

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    /** Result of the statement parameterised by {@link Input}. */
    public static final class Output extends ArrayList<OutputRow> {
        Output(List<OutputRow> rows) {
            super(rows);
        }
    }

    /** Row of {@link Output}. */
    public record OutputRow(
            /** Maps to the {@code id} result-set column. */
            int id,
            /** Maps to the {@code name} result-set column. */
            String name
    ) {}

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
    public void bindParams(PreparedStatement ps, Input p) throws SQLException {
        if (p.artist() != null) {
            ps.setInt(1, p.artist());
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
