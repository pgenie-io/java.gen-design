package io.pgenie.java.richclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.TransactionContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttemptTrackingTransactionContextTest {

    @Test
    void commitAndRollbackCountersUpdateOnSuccess() throws SQLException {
        var stub = new StubTransactionContext();
        var tracking = new AttemptTrackingTransactionContext(stub);

        tracking.commit();
        tracking.rollback();
        tracking.rollback();

        assertTrue(tracking.committed(), "committed flag should be set after commit");
        assertEquals(2, tracking.rollbackCount(), "rollback count should reflect two rollbacks");
        assertTrue(stub.commitCalled, "delegate commit should have been called");
        assertEquals(2, stub.rollbackCalledCount, "delegate rollback should have been called twice");
    }

    @Test
    void rollbackCounterIncrementsWhenDelegateThrows() {
        var stub = new StubTransactionContext();
        stub.rollbackException = new SQLException("rollback failed", "40000");
        var tracking = new AttemptTrackingTransactionContext(stub);

        var thrown = assertThrows(SQLException.class, tracking::rollback);

        assertEquals("rollback failed", thrown.getMessage());
        assertEquals(1, tracking.rollbackCount(), "rollback should be counted even when delegate throws");
        assertEquals(1, stub.rollbackCalledCount, "delegate rollback should have been called once");
        assertFalse(tracking.committed(), "commit was never attempted");
    }

    @Test
    void commitAttemptedFlagSetWhenDelegateThrows() {
        var stub = new StubTransactionContext();
        stub.commitException = new SQLException("commit failed", "40001");
        var tracking = new AttemptTrackingTransactionContext(stub);

        var thrown = assertThrows(SQLException.class, tracking::commit);

        assertEquals("commit failed", thrown.getMessage());
        assertTrue(tracking.committed(), "commit attempt should be recorded even when delegate throws");
        assertEquals(0, tracking.rollbackCount(), "rollback was never attempted");
        assertTrue(stub.commitCalled, "delegate commit should have been called once");
    }

    @Test
    void isRetryableFailureRecognizesRetryableSqlStates() {
        assertTrue(AttemptTrackingTransactionContext.isRetryableFailure(new SQLException("x", "40001")));
        assertTrue(AttemptTrackingTransactionContext.isRetryableFailure(new SQLException("x", "40P01")));
        assertTrue(AttemptTrackingTransactionContext.isRetryableFailure(new SQLException("x", "23505")));
    }

    @Test
    void isRetryableFailureRejectsNonRetryableFailures() {
        assertFalse(AttemptTrackingTransactionContext.isRetryableFailure(null));
        assertFalse(AttemptTrackingTransactionContext.isRetryableFailure(new SQLException("x")));
        assertFalse(AttemptTrackingTransactionContext.isRetryableFailure(new SQLException("x", (String) null)));
        assertFalse(AttemptTrackingTransactionContext.isRetryableFailure(new SQLException("x", "40000")));
        assertFalse(AttemptTrackingTransactionContext.isRetryableFailure(new SQLException("x", "23503")));
        assertFalse(AttemptTrackingTransactionContext.isRetryableFailure(new SQLException("x", "40P02")));
    }

    /**
     * Hand-written stub delegate so the unit tests have no mocking-library dependency.
     */
    private static final class StubTransactionContext implements TransactionContext {

        boolean commitCalled;
        int rollbackCalledCount;
        SQLException commitException;
        SQLException rollbackException;

        @Override
        public <R> R execute(Statement<R> statement) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> List<R> executeBatch(Iterable<? extends Statement<R>> statements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Savepoint setSavepoint() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void rollback(Savepoint savepoint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getAutoCommit() {
            return false;
        }

        @Override
        public void setAutoCommit(boolean autoCommit) {}

        @Override
        public int getTransactionIsolation() {
            return Connection.TRANSACTION_READ_COMMITTED;
        }

        @Override
        public void setTransactionIsolation(int level) {}

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public void setReadOnly(boolean readOnly) {}

        @Override
        public void commit() throws SQLException {
            commitCalled = true;
            if (commitException != null) {
                throw commitException;
            }
        }

        @Override
        public void rollback() throws SQLException {
            rollbackCalledCount++;
            if (rollbackException != null) {
                throw rollbackException;
            }
        }
    }
}
