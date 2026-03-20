package io.pgenie.example.myspace.musiccatalogue.transactions;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

import io.pgenie.example.myspace.musiccatalogue.IsolationLevel;
import io.pgenie.example.myspace.musiccatalogue.Transaction;
import io.pgenie.example.myspace.musiccatalogue.TransactionContext;
import io.pgenie.example.myspace.musiccatalogue.TransactionOutcome;
import io.pgenie.example.myspace.musiccatalogue.statements.InsertAlbum;
import io.pgenie.example.myspace.musiccatalogue.statements.SelectAlbumByName;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;

/**
 * Composite transaction example placed in the statements package: select an
 * album by name, or insert it if absent. Demonstrates using multiple statements
 * inside a single transaction body.
 */
public final class SelectOrInsertAlbumTransaction implements Transaction<Long> {

    private final String name;
    private final LocalDate released;
    private final AlbumFormat format;

    public SelectOrInsertAlbumTransaction(String name, LocalDate released, AlbumFormat format) {
        this.name = Objects.requireNonNull(name, "name");
        this.released = released;
        this.format = format;
    }

    /**
     * Use SERIALIZABLE to make the select-or-insert atomic under concurrent
     * runs.
     */
    @Override
    public IsolationLevel isolationLevel() {
        return IsolationLevel.SERIALIZABLE;
    }

    @Override
    public TransactionOutcome<Long> run(TransactionContext ctx) throws SQLException {
        Long existingId = ctx.execute(new SelectAlbumByName(name));
        if (existingId != null) {
            return TransactionOutcome.commit(existingId);
        }

        InsertAlbum.Output output = ctx.execute(new InsertAlbum(name, released, format, null));
        return TransactionOutcome.commit(output.id());
    }

}
