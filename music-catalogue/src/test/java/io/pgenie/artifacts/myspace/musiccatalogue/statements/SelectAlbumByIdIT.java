package io.pgenie.artifacts.myspace.musiccatalogue.statements;

import static org.junit.jupiter.api.Assertions.*;

import io.pgenie.artifacts.myspace.musiccatalogue.AbstractDatabaseIT;
import io.pgenie.artifacts.myspace.musiccatalogue.types.*;
import java.util.List;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SelectAlbumByIdIT extends AbstractDatabaseIT {
    @Test

    void executesWithDefaultValues() throws Exception {
        var result = execute(new SelectAlbumById(Optional.empty()));
        assertNotNull(result);
    }
}
