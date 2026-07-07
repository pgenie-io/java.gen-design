package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.*;

import io.pgenie.artifacts.myspace.musiccatalogue.statements.InsertAlbum;
import io.pgenie.artifacts.myspace.musiccatalogue.statements.SelectAlbumById;
import io.pgenie.artifacts.myspace.musiccatalogue.statements.SelectAlbumByName;
import io.pgenie.artifacts.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.artifacts.myspace.musiccatalogue.types.RecordingInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TransactionIT extends AbstractDatabaseIT {

    @Test
    void transactionCommitsInsertedRow() throws SQLException {
        String name = "Commit Test Album";
        LocalDate released = LocalDate.of(2024, 1, 15);

        long id = session.transaction(tx ->
            tx.execute(new InsertAlbum(name, released, AlbumFormat.Cd, randomRecordingInfo())).id()
        );

        Optional<SelectAlbumById.ResultRow> row = session.execute(new SelectAlbumById(Optional.of(id)));

        assertTrue(row.isPresent(), "inserted album should be selectable after commit");
        assertEquals(name, row.get().name());
        assertEquals(Optional.of(released), row.get().released());
    }

    @Test
    void transactionRollsBackOnException() throws SQLException {
        String name = "Rollback Test Album";
        LocalDate released = LocalDate.of(2024, 2, 20);

        assertThrows(RuntimeException.class, () ->
            session.transaction(tx -> {
                tx.execute(new InsertAlbum(name, released, AlbumFormat.Digital, randomRecordingInfo()));
                throw new RuntimeException("boom");
            })
        );

        var rows = session.execute(new SelectAlbumByName(name));
        assertTrue(rows.isEmpty(), "rolled-back album should not exist");
    }

    @Test

    void transactionIsolationLevelOverloadDoesNotFail() throws Exception {
        assertDoesNotThrow(() ->
            session.transaction(Connection.TRANSACTION_SERIALIZABLE, tx ->
                tx.execute(new InsertAlbum("Serializable Album", LocalDate.of(2024, 3, 10), AlbumFormat.Vinyl, randomRecordingInfo()))
            )
        );
    }

    private static RecordingInfo randomRecordingInfo() {
        return RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0);
    }
}
