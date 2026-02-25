package io.pgenie.example.myspace.musiccatalogue.statements;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import io.pgenie.example.myspace.musiccatalogue.Statement;

/**
 * Type-safe binding for the {@code select_genre_by_artist} query.
 *
 * <h2>SQL Template</h2>
 *
 * <pre>{@code
 * select id, genre.name
 * from genre
 * left join album_genre on album_genre.genre = genre.id
 * left join album_artist on album_artist.album = album_genre.album
 * where album_artist.artist = $artist
 * }</pre>
 *
 * <h2>Source Path</h2> {@code ./queries/select_genre_by_artist.sql}
 *
 * <p>
 * Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 *
 * @param artist Maps to {@code $artist} in the template. Nullable.
 */
public record SelectGenreByArtist(Integer artist)
        implements Statement<SelectGenreByArtist.Output> {

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------
    /**
     * Result of the statement parameterised by {@link SelectGenreByArtist}.
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
            int id,
            /**
             * Maps to the {@code name} result-set column.
             */
            String name) {

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
    public boolean returnsRows() {
        return true;
    }

    @Override
    public Output decodeResultSet(ResultSet rs) throws SQLException {
        Output output = new Output();
        while (rs.next()) {
            output.add(new OutputRow(rs.getInt(1), rs.getString(2)));
        }
        return output;
    }

    @Override
    public Output decodeAffectedRows(long affectedRows) {
        throw new UnsupportedOperationException();
    }
}
