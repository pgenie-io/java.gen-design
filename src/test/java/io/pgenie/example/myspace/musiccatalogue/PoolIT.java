package io.pgenie.example.myspace.musiccatalogue;

import io.pgenie.example.myspace.musiccatalogue.statements.InsertAlbum;
import io.pgenie.example.myspace.musiccatalogue.statements.UpdateAlbumRecordingReturning;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link Pool#execute} and {@link Pool#transact}. */
class PoolIT extends AbstractDatabaseIT {

    // -------------------------------------------------------------------------
    // Pool.execute shorthand
    // -------------------------------------------------------------------------

    @Test
    void poolExecuteInsertsAlbum() throws SQLException {
        var result = pool.execute(new InsertAlbum(
                "Animals",
                LocalDate.of(1977, 1, 23),
                AlbumFormat.VINYL,
                null));
        assertTrue(result.id() > 0, "expected a positive id, got " + result.id());
    }

    // -------------------------------------------------------------------------
    // Transaction: commit path
    // -------------------------------------------------------------------------

    @Test
    void transactCommitInsertsAlbum() throws SQLException {
        long id = pool.transact(ctx -> {
            var output = ctx.execute(new InsertAlbum("Committed Album", null, null, null));
            return TransactionOutcome.commit(output.id());
        });

        assertTrue(id > 0, "expected positive id, got " + id);

        // Verify the insertion was actually persisted.
        var rows = pool.execute(new UpdateAlbumRecordingReturning(null, id));
        assertEquals(1, rows.size(), "album should exist after commit");
        assertEquals("Committed Album", rows.get(0).name());
    }

    // -------------------------------------------------------------------------
    // Transaction: retry on serialisation failure (SQLState 40001)
    // -------------------------------------------------------------------------

    /** Transaction that fails on the first attempt with a serialisation error. */
    private final class RetryOnSerializationFailure implements Transaction<Long> {
        final AtomicInteger attempts = new AtomicInteger(0);

        @Override
        public TransactionOutcome<Long> run(TransactionContext ctx) throws SQLException {
            if (attempts.getAndIncrement() == 0) {
                ctx.execute(new RaiseSerializationFailure());
            }
            var output = ctx.execute(new InsertAlbum("Retried Album", null, null, null));
            return TransactionOutcome.commit(output.id());
        }
    }

    @Test
    void transactRetriesOnSerializationFailure() throws SQLException {
        var tx = new RetryOnSerializationFailure();
        long id = pool.transact(tx);

        assertTrue(id > 0);
        assertEquals(2, tx.attempts.get(), "expected exactly 2 attempts");
    }

    // -------------------------------------------------------------------------
    // Transaction: non-retryable error propagates
    // -------------------------------------------------------------------------

    @Test
    void transactNonRetryableErrorPropagates() {
        assertThrows(SQLException.class, () -> pool.<Void>transact(ctx -> {
            ctx.execute(new RaiseGenericError());
            return TransactionOutcome.commit(null);
        }));
    }

    // -------------------------------------------------------------------------
    // Statement helpers
    // -------------------------------------------------------------------------

    /**
     * Raises a serialisation failure (SQLState {@code 40001}) via a PL/pgSQL DO
     * block.
     */
    private static final class RaiseSerializationFailure implements Statement<Void> {
        @Override
        public String sql() {
            return "DO $$ BEGIN RAISE EXCEPTION 'simulated' USING ERRCODE = '40001'; END $$";
        }

        @Override
        public void bindParams(PreparedStatement ps) {
        }

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public Void decodeAffectedRows(long affectedRows) {
            return null;
        }

        @Override
        public Void decodeResultSet(ResultSet rs) {
            throw new UnsupportedOperationException();
        }
    }

    /** Raises a generic PL/pgSQL exception (non-retryable). */
    private static final class RaiseGenericError implements Statement<Void> {
        @Override
        public String sql() {
            return "DO $$ BEGIN RAISE EXCEPTION 'generic error'; END $$";
        }

        @Override
        public void bindParams(PreparedStatement ps) {
        }

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public Void decodeAffectedRows(long affectedRows) {
            return null;
        }

        @Override
        public Void decodeResultSet(ResultSet rs) {
            throw new UnsupportedOperationException();
        }
    }
}
