package io.pgenie.artifacts.myspace.musiccatalogue.statements;

import static org.junit.jupiter.api.Assertions.*;

import io.pgenie.artifacts.myspace.musiccatalogue.AbstractDatabaseIT;
import io.pgenie.artifacts.myspace.musiccatalogue.types.*;
import java.util.List;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SelectAlbumWithFiltersIT extends AbstractDatabaseIT {
    @Test
    void executesWithDefaultValues() {
        var result = execute(new SelectAlbumWithFilters(false, false, false, false, false, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), false, false));
        assertNotNull(result);
    }
}
