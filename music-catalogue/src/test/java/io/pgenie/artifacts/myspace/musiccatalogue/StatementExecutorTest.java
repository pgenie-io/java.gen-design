package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.codemine.java.postgresql.jdbc.Statement;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link StatementExecutor}'s pure span/attribute-building logic and the batch
 * validation it delegates to {@link io.codemine.java.postgresql.jdbc.StatementBatch}. None of
 * these tests touch a JDBC connection: {@link StatementExecutor}'s constructor performs no I/O,
 * which is what makes it cheap to exercise here without Testcontainers.
 */
class StatementExecutorTest {

    private static final AttributeKey<String> DB_SYSTEM_KEY = AttributeKey.stringKey("db.system");
    private static final AttributeKey<String> DB_QUERY_TEXT_KEY = AttributeKey.stringKey("db.query.text");
    private static final AttributeKey<String> DB_USER_KEY = AttributeKey.stringKey("db.user");
    private static final AttributeKey<String> STATEMENT_NAME_KEY = AttributeKey.stringKey("pgenie.statement.name");
    private static final AttributeKey<Long> BATCH_SIZE_KEY = AttributeKey.longKey("pgenie.statement.batch_size");

    private StatementExecutor newExecutor(SdkTracerProvider tracerProvider) {
        var config = MusicCatalogueConfig.defaults("jdbc:postgresql://localhost/test", "user", "pw");
        var openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(SdkMeterProvider.builder().build())
                .build();
        var meter = openTelemetry.getMeter("test");
        var histogram = meter.histogramBuilder("test.duration").build();
        return new StatementExecutor(config, openTelemetry.getTracer("test"), meter, histogram, LoggerFactory.getLogger(StatementExecutorTest.class));
    }

    private SdkTracerProvider newTracerProvider(InMemorySpanExporter spanExporter) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
    }

    /**
     * A {@link Connection} that throws if any method is invoked. Batch validation rejects
     * malformed batches before touching the connection, so this proves those rejections do no
     * I/O without requiring a real database.
     */
    private static Connection unusableConnection() {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    throw new AssertionError("Unexpected connection use: " + method.getName());
                });
    }

    @Test
    void startStatementSpanSetsExpectedAttributes() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = newTracerProvider(spanExporter);
        StatementExecutor executor = newExecutor(tracerProvider);

        Span span = executor.startStatementSpan("InsertAlbum", "insert into album ...", null);
        span.end();
        tracerProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);

        SpanData spanData = spanExporter.getFinishedSpanItems().get(0);
        assertEquals("InsertAlbum", spanData.getName());
        assertEquals(SpanKind.CLIENT, spanData.getKind());
        assertEquals("postgresql", spanData.getAttributes().get(DB_SYSTEM_KEY));
        assertEquals("insert into album ...", spanData.getAttributes().get(DB_QUERY_TEXT_KEY));
        assertEquals("InsertAlbum", spanData.getAttributes().get(STATEMENT_NAME_KEY));
        assertEquals("user", spanData.getAttributes().get(DB_USER_KEY));
        assertNull(spanData.getAttributes().get(BATCH_SIZE_KEY));
    }

    @Test
    void startStatementSpanWithParentIsAChild() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = newTracerProvider(spanExporter);
        StatementExecutor executor = newExecutor(tracerProvider);

        Span parent = tracerProvider.get("test").spanBuilder("parent").startSpan();
        Span child = executor.startStatementSpan("InsertAlbum", "insert into album ...", parent);
        child.end();
        parent.end();
        tracerProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);

        SpanData parentData = spanExporter.getFinishedSpanItems().stream()
                .filter(s -> s.getName().equals("parent"))
                .findFirst()
                .orElseThrow();
        SpanData childData = spanExporter.getFinishedSpanItems().stream()
                .filter(s -> s.getName().equals("InsertAlbum"))
                .findFirst()
                .orElseThrow();
        assertEquals(parentData.getSpanId(), childData.getParentSpanId());
    }

    @Test
    void startBatchSpanSetsExpectedAttributesIncludingBatchSize() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = newTracerProvider(spanExporter);
        StatementExecutor executor = newExecutor(tracerProvider);

        Span span = executor.startBatchSpan("batch", "update album set x = ?", 3, null);
        span.end();
        tracerProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);

        SpanData spanData = spanExporter.getFinishedSpanItems().get(0);
        assertEquals("batch", spanData.getName());
        assertEquals(SpanKind.CLIENT, spanData.getKind());
        assertEquals("update album set x = ?", spanData.getAttributes().get(DB_QUERY_TEXT_KEY));
        assertEquals(3L, spanData.getAttributes().get(BATCH_SIZE_KEY));
        assertEquals("user", spanData.getAttributes().get(DB_USER_KEY));
    }

    @Test
    void executeBatchOnEmptyIterableReturnsEmptyListWithoutTouchingConnection() throws Exception {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = newTracerProvider(spanExporter);
        StatementExecutor executor = newExecutor(tracerProvider);

        List<Void> result = executor.executeBatch(List.of(), unusableConnection(), null);

        assertTrue(result.isEmpty());
    }

    @Test
    void executeBatchRejectsRowsReturningStatement() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = newTracerProvider(spanExporter);
        StatementExecutor executor = newExecutor(tracerProvider);

        var thrown = assertThrows(
                IllegalArgumentException.class,
                () -> executor.executeBatch(List.of(new FakeRowsReturningStatement()), unusableConnection(), null));
        assertEquals("Batch execution is only supported for update statements", thrown.getMessage());
    }

    @Test
    void executeBatchRejectsMismatchedSql() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = newTracerProvider(spanExporter);
        StatementExecutor executor = newExecutor(tracerProvider);

        var thrown = assertThrows(
                IllegalArgumentException.class,
                () -> executor.executeBatch(
                        List.of(new FakeUpdateStatement("UPDATE a SET x = ?"), new FakeUpdateStatement("UPDATE b SET y = ?")),
                        unusableConnection(),
                        null));
        assertEquals("All batch statements must use the same SQL text", thrown.getMessage());
    }

    private static class FakeUpdateStatement implements Statement<Void> {
        private final String sql;

        FakeUpdateStatement(String sql) {
            this.sql = sql;
        }

        @Override
        public String sql() {
            return sql;
        }

        @Override
        public void bindParams(PreparedStatement ps) {
            throw new UnsupportedOperationException("Not used");
        }

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public Void decodeResultSet(ResultSet rs) {
            throw new UnsupportedOperationException("Not used");
        }

        @Override
        public Void decodeAffectedRows(long affectedRows) {
            return null;
        }
    }

    private static final class FakeRowsReturningStatement extends FakeUpdateStatement {
        FakeRowsReturningStatement() {
            super("SELECT id FROM table WHERE id = ?");
        }

        @Override
        public boolean returnsRows() {
            return true;
        }
    }
}
