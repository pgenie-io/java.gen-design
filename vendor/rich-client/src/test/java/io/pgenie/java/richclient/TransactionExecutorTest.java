package io.pgenie.java.richclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.TransactionContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionExecutorTest {

    @Test
    void attemptCountForSuccessfulTransactionIncludesFinalAttempt() {
        var tracking = trackingWith(0, true);

        assertEquals(1, TransactionExecutor.computeAttemptCount(tracking, true));
    }

    @Test
    void attemptCountForSuccessfulRetriedTransactionAddsOneToRollbackCount() {
        var tracking = trackingWith(2, true);

        assertEquals(3, TransactionExecutor.computeAttemptCount(tracking, true));
    }

    @Test
    void attemptCountForFailedTransactionEqualsRollbackCount() {
        var tracking = trackingWith(3, false);

        assertEquals(3, TransactionExecutor.computeAttemptCount(tracking, false));
    }

    @Test
    void attemptCountForFailedTransactionWithNoRollbacksIsOne() {
        var tracking = trackingWith(0, false);

        assertEquals(1, TransactionExecutor.computeAttemptCount(tracking, false));
    }

    @Test
    void failedTransactionDoesNotOvercountWhenCommitWasAttempted() {
        // Simulates a transaction where each attempt failed during commit, so commitCalled
        // is true but the transaction ultimately failed. The rollback count alone must
        // determine the attempt count; adding one because commit was attempted would overcount.
        var tracking = trackingWith(2, true);

        assertEquals(2, TransactionExecutor.computeAttemptCount(tracking, false));
    }

    private static AttemptTrackingTransactionContext trackingWith(int rollbackCount, boolean commitCalled) {
        var delegate = new NoOpTransactionContext();
        var tracking = new AttemptTrackingTransactionContext(delegate);
        for (int i = 0; i < rollbackCount; i++) {
            try {
                tracking.rollback();
            } catch (SQLException ignored) {
                // stub never throws
            }
        }
        if (commitCalled) {
            try {
                tracking.commit();
            } catch (SQLException ignored) {
                // stub never throws
            }
        }
        return tracking;
    }

    private static final class NoOpTransactionContext implements TransactionContext {
        @Override
        public <R> R execute(Statement<R> statement) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> List<R> executeBatch(Iterable<? extends Statement<R>> statements) {
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
        public void commit() {}

        @Override
        public void rollback() {}

        @Override
        public void rollback(Savepoint savepoint) {}

        @Override
        public Savepoint setSavepoint() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) {}
    }
}
