package io.pgenie.example.myspace.musiccatalogue.statements;

import io.pgenie.example.myspace.musiccatalogue.AbstractDatabaseIT;
import io.pgenie.example.myspace.musiccatalogue.statements.InsertAlbum;
import io.pgenie.example.myspace.musiccatalogue.statements.SelectAlbumByFormat;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectAlbumByFormatIT extends AbstractDatabaseIT {

    @Test
    void selectAlbumByFormatFindsInsertedAlbum() throws SQLException {
        var recording = new RecordingInfo(
                "Record Plant",
                "Sausalito",
                "USA",
                LocalDate.of(1976, 8, 1));
        var inserted = pool.execute(new InsertAlbum(
                "Rumours",
                LocalDate.of(1977, 2, 4),
                AlbumFormat.CD,
                recording));

        var rows = pool.execute(new SelectAlbumByFormat(AlbumFormat.CD));

        assertTrue(
                rows.stream().anyMatch(r -> r.id() == inserted.id()),
                "inserted album not found in result set");
        // Verify the recording round-trips correctly.
        var recordingsFromDb = rows.stream()
                .filter(r -> r.id() == inserted.id())
                .map(SelectAlbumByFormat.OutputRow::recording)
                .toList();
        assertEquals(1, recordingsFromDb.size());
        assertEquals(recording, recordingsFromDb.get(0));
    }

    @Test
    void selectAlbumByFormatReturnsEmptyForAbsentFormat() throws SQLException {
        var rows = pool.execute(new SelectAlbumByFormat(AlbumFormat.SACD));
        assertTrue(rows.isEmpty(), "expected no SACD albums in a fresh DB");
    }
}
