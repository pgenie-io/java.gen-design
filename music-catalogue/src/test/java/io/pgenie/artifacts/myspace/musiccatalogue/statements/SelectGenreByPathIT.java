package io.pgenie.artifacts.myspace.musiccatalogue.statements;

import static org.junit.jupiter.api.Assertions.*;

import io.pgenie.artifacts.myspace.musiccatalogue.AbstractDatabaseIT;
import io.pgenie.artifacts.myspace.musiccatalogue.types.*;
import io.codemine.java.postgresql.codecs.Ltree;
import java.util.List;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SelectGenreByPathIT extends AbstractDatabaseIT {
    @Test

    void executesWithDefaultValues() throws Exception {
        var result = execute(new SelectGenreByPath(new Ltree(List.of("root"))));
        assertNotNull(result);
    }
}
