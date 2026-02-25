package io.pgenie.example.myspace.musiccatalogue.statements;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import io.pgenie.example.myspace.musiccatalogue.AbstractDatabaseIT;

class UpdateAlbumReleasedIT extends AbstractDatabaseIT {

    @Test
    void updateAlbumReleasedUpdatesRow() throws SQLException {
        var inserted = pool.execute(new InsertAlbum(
                "The Wall",
                null,
                null,
                null));

        LocalDate releaseDate = LocalDate.of(1979, 11, 30);

        long affected = pool.execute(new UpdateAlbumReleased(releaseDate, inserted.id()));
        assertEquals(1L, affected, "expected 1 row to be updated");

        // Verify via update_album_recording_returning (returns full row).
        var rows = pool.execute(
                new UpdateAlbumRecordingReturning(null, inserted.id()));

        assertEquals(1, rows.size());
        assertEquals(releaseDate, rows.get(0).released());
    }

    @Test
    void updateAlbumReleasedNoMatchIsNoop() throws SQLException {
        long affected = pool.execute(
                new UpdateAlbumReleased(LocalDate.of(2000, 1, 1), 99999L));
        assertEquals(0L, affected);
    }
}
