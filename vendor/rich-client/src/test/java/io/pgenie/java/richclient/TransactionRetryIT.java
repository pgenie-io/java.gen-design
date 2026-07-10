package io.pgenie.java.richclient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.Transaction;
import io.codemine.java.postgresql.jdbc.TransactionSettings;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class TransactionRetryIT {

    private static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("postgres:18").withCommand("postgres", "-c", "max_connections=200");

    @BeforeAll
    static void startContainer() {
        PG.start();
    }

    @BeforeEach
    void createCounterTable() throws SQLException {
        try (Connection conn = openConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "create table if not exists retry_counter (id int primary key, value int not null)")) {
            ps.execute();
        }
        try (Connection conn = openConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "insert into retry_counter (id, value) values (1, 0) on conflict (id) do update set value = 0")) {
            ps.executeUpdate();
        }
    }

    @AfterEach
    void dropCounterTable() throws SQLException {
        try (Connection conn = openConnection();
                PreparedStatement ps = conn.prepareStatement("drop table if exists retry_counter")) {
            ps.execute();
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
    }

    @Test
    void singleAttemptCommitSucceedsWithZeroRollbacks() throws SQLException {
        try (Connection conn = openConnection()) {
            var context = AttemptTrackingTransactionContext.of(conn);
            Transaction<Integer> select = ctx -> ctx.execute(new SelectCounterStatement());

            int value = select.executeOn(context, TransactionSettings.SERIALIZABLE_READ.withMaxAttempts(1));

            assertEquals(0, value);
            assertTrue(context.committed(), "transaction should have attempted a commit");
            assertEquals(0, context.rollbackCount(), "no rollbacks should have occurred");
        }
    }

    @Test
    void serializableConflictIsRetriedThenCommitted() throws Exception {
        List<AttemptTrackingTransactionContext> contexts = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Void> increment = () -> {
            try (Connection conn = openConnection()) {
                var context = AttemptTrackingTransactionContext.of(conn);
                contexts.add(context);
                Transaction<Void> tx = ctx -> {
                    int current = ctx.execute(new SelectCounterStatement());
                    ctx.execute(new UpdateCounterStatement(current + 1));
                    return null;
                };
                tx.executeOn(context, TransactionSettings.SERIALIZABLE_WRITE.withMaxAttempts(10));
                return null;
            }
        };

        Future<Void> first = executor.submit(increment);
        Future<Void> second = executor.submit(increment);

        assertDoesNotThrow(() -> first.get(30, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> second.get(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(2, contexts.size(), "both workers should have created a context");
        for (AttemptTrackingTransactionContext context : contexts) {
            assertTrue(context.committed(), "every successful worker should have attempted a commit");
        }
        int totalRollbacks = contexts.stream().mapToInt(AttemptTrackingTransactionContext::rollbackCount).sum();
        assertTrue(totalRollbacks > 0, "at least one retry should have rolled back, got " + totalRollbacks);

        try (Connection conn = openConnection()) {
            var context = AttemptTrackingTransactionContext.of(conn);
            Transaction<Integer> select = ctx -> ctx.execute(new SelectCounterStatement());
            int finalValue = select.executeOn(context, TransactionSettings.SERIALIZABLE_READ);
            assertEquals(2, finalValue);
        }
    }

    @Test
    void persistentConflictExhaustsRetries() throws SQLException {
        try (Connection conn = openConnection()) {
            var context = AttemptTrackingTransactionContext.of(conn);
            Transaction<Void> conflicting = ctx -> {
                ctx.execute(new InsertDuplicateCounterStatement());
                return null;
            };

            SQLException failure = assertThrows(
                    SQLException.class,
                    () -> conflicting.executeOn(context, TransactionSettings.SERIALIZABLE_WRITE.withMaxAttempts(2)));

            assertTrue(
                    AttemptTrackingTransactionContext.isRetryableFailure(failure),
                    "exhausted failure should be retryable: " + failure.getSQLState());
            assertFalse(context.committed(), "commit should never have been attempted");
            assertEquals(2, context.rollbackCount(), "each failed attempt should have rolled back");
        }
    }

    private record SelectCounterStatement() implements Statement<Integer> {
        @Override
        public String sql() {
            return "select value from retry_counter where id = 1";
        }

        @Override
        public void bindParams(PreparedStatement ps) {}

        @Override
        public boolean returnsRows() {
            return true;
        }

        @Override
        public Integer decodeResultSet(ResultSet rs) throws SQLException {
            rs.next();
            return rs.getInt(1);
        }

        @Override
        public Integer decodeAffectedRows(long affectedRows) {
            throw new UnsupportedOperationException();
        }
    }

    private record UpdateCounterStatement(int value) implements Statement<Long> {
        @Override
        public String sql() {
            return "update retry_counter set value = ? where id = 1";
        }

        @Override
        public void bindParams(PreparedStatement ps) throws SQLException {
            ps.setInt(1, value);
        }

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public Long decodeResultSet(ResultSet rs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long decodeAffectedRows(long affectedRows) {
            return affectedRows;
        }
    }

    private record InsertDuplicateCounterStatement() implements Statement<Long> {
        @Override
        public String sql() {
            return "insert into retry_counter (id, value) values (1, 0)";
        }

        @Override
        public void bindParams(PreparedStatement ps) {}

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public Long decodeResultSet(ResultSet rs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long decodeAffectedRows(long affectedRows) {
            return affectedRows;
        }
    }
}
