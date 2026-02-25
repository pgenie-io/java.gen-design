package io.pgenie.example.myspace.musiccatalogue.statements;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.pgenie.example.myspace.musiccatalogue.AbstractDatabaseIT;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;

class InsertAlbumIT extends AbstractDatabaseIT {

    @Test
    void insertAlbumReturnsId() throws SQLException {
        var result = pool.execute(new InsertAlbum(
                "Dark Side of the Moon",
                LocalDate.of(1973, 3, 1),
                AlbumFormat.VINYL,
                new RecordingInfo(
                        "Abbey Road",
                        "London",
                        "UK",
                        LocalDate.of(1972, 6, 1))));
        assertTrue(result.id() > 0, "expected a positive id, got " + result.id());
    }

    @Test
    void insertAlbumWithNullsReturnsId() throws SQLException {
        var result = pool.execute(new InsertAlbum(
                "Untitled",
                null,
                null,
                null));
        assertTrue(result.id() > 0);
    }
}
