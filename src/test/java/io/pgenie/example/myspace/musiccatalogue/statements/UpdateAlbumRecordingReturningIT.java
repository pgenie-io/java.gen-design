package io.pgenie.example.myspace.musiccatalogue.statements;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import io.pgenie.example.myspace.musiccatalogue.AbstractDatabaseIT;
import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;

class UpdateAlbumRecordingReturningIT extends AbstractDatabaseIT {

    @Test
    void updateAlbumRecordingReturningUpdatesAndReturnsRow() throws SQLException {
        var inserted = pool.execute(new InsertAlbum(
                "Wish You Were Here",
                null,
                null,
                null));

        var recording = new RecordingInfo(
                "EMI",
                "London",
                "UK",
                LocalDate.of(1975, 1, 6));

        var rows = pool.execute(
                new UpdateAlbumRecordingReturning(recording, inserted.id()));

        assertEquals(1, rows.size());
        assertEquals(inserted.id(), rows.get(0).id());
        assertEquals("Wish You Were Here", rows.get(0).name());
        assertEquals(recording, rows.get(0).recording());
    }

    @Test
    void updateAlbumRecordingReturningNoMatchReturnsEmpty() throws SQLException {
        var rows = pool.execute(new UpdateAlbumRecordingReturning(null, 99999L));
        assertTrue(rows.isEmpty());
    }
}
