package io.pgenie.artifacts.myspace.musiccatalogue.statements;

import static org.junit.jupiter.api.Assertions.*;

import io.pgenie.artifacts.myspace.musiccatalogue.AbstractDatabaseIT;
import io.pgenie.artifacts.myspace.musiccatalogue.types.*;
import java.util.List;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InsertAlbumIT extends AbstractDatabaseIT {
    @Test
    void executesWithDefaultValues() {
        var result = execute(new InsertAlbum("", LocalDate.of(2000, 1, 1), AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0), RecordingInfo.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertNotNull(result);
    }
}
