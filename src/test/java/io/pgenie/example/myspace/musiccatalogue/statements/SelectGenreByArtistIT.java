package io.pgenie.example.myspace.musiccatalogue.statements;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.pgenie.example.myspace.musiccatalogue.AbstractDatabaseIT;

class SelectGenreByArtistIT extends AbstractDatabaseIT {

    @Test
    void selectGenreByArtistReturnsEmptyForUnknownArtist() throws SQLException {
        var rows = pool.execute(new SelectGenreByArtist(9999));
        assertTrue(rows.isEmpty());
    }

    @Test
    void selectGenreByArtistSeeded() throws SQLException {
        // Seed the database with data using a direct JDBC connection.
        // (The package only exposes generated statements; DDL/seed queries are
        // executed via raw JDBC — mirroring the Rust test pattern.)
        int artistId;
        try (var conn = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                var stmt = conn.createStatement()) {
            stmt.execute("""
                    INSERT INTO genre (name) VALUES ('Jazz')
                        ON CONFLICT (name) DO NOTHING;
                    INSERT INTO artist (name) VALUES ('Miles Davis');
                    INSERT INTO album (name) VALUES ('Kind of Blue');
                    INSERT INTO album_genre (album, genre)
                        SELECT a.id, g.id FROM album a, genre g
                        WHERE a.name = 'Kind of Blue' AND g.name = 'Jazz';
                    INSERT INTO album_artist (album, artist, "primary")
                        SELECT a.id, ar.id, true FROM album a, artist ar
                        WHERE a.name = 'Kind of Blue' AND ar.name = 'Miles Davis';
                    """);
            try (var rs = stmt.executeQuery(
                    "SELECT id FROM artist WHERE name = 'Miles Davis'")) {
                rs.next();
                artistId = rs.getInt(1);
            }
        }

        var rows = pool.execute(new SelectGenreByArtist(artistId));
        assertEquals(1, rows.size());
        assertEquals("Jazz", rows.get(0).name());
    }
}
