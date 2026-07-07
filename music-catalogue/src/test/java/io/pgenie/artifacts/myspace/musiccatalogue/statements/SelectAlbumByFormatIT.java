package io.pgenie.artifacts.myspace.musiccatalogue.statements;

import static org.junit.jupiter.api.Assertions.*;

import io.pgenie.artifacts.myspace.musiccatalogue.AbstractDatabaseIT;
import io.pgenie.artifacts.myspace.musiccatalogue.types.*;
import java.util.List;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SelectAlbumByFormatIT extends AbstractDatabaseIT {
    @Test

    void executesWithDefaultValues() throws Exception {
        var result = execute(new SelectAlbumByFormat(AlbumFormat.CODEC.toAgnostic().random(new java.util.Random(0L), 0)));
        assertNotNull(result);
    }
}
