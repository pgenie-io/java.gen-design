package io.pgenie.java.richclient;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.codemine.java.postgresql.jdbc.Statement;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.Marker;

class StatementExecutorTest {

    private static final AttributeKey<String> DB_SYSTEM_NAME_KEY = AttributeKey.stringKey("db.system.name");
    private static final AttributeKey<String> DB_QUERY_TEXT_KEY = AttributeKey.stringKey("db.query.text");
    private static final AttributeKey<String> DB_OPERATION_NAME_KEY = AttributeKey.stringKey("db.operation.name");
    private static final AttributeKey<String> DB_COLLECTION_NAME_KEY = AttributeKey.stringKey("db.collection.name");
    private static final AttributeKey<String> STATEMENT_NAME_KEY = AttributeKey.stringKey("pgenie.statement.name");
    private static final AttributeKey<String> DB_USER_KEY = AttributeKey.stringKey("pgenie.db.user");
    private static final AttributeKey<Long> BATCH_SIZE_KEY = AttributeKey.longKey("db.operation.batch.size");

    private InMemorySpanExporter spanExporter;
    private InMemoryMetricReader metricReader;
    private SdkTracerProvider tracerProvider;
    private SdkMeterProvider meterProvider;
    private OpenTelemetrySdk openTelemetry;
    private CollectingLogger logger;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        metricReader = InMemoryMetricReader.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .build();
        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();
        logger = new CollectingLogger();
    }

    private StatementExecutor executor(Duration slowQueryThreshold) {
        return new StatementExecutor(
                openTelemetry.getTracer("test"),
                openTelemetry.getMeter("test"),
                logger,
                "test-user",
                slowQueryThreshold);
    }

    @Test
    void singleStatementSpanAttributesAndMetricPoint() throws SQLException {
        StatementExecutor executor = executor(Duration.ofSeconds(1));
        var statement = new MetadataStatement(
                "INSERT INTO albums (title) VALUES (?)", "INSERT", "albums", "ok");

        String result = executor.execute(statement, dummyConnection(), null);

        assertEquals("ok", result);
        flush();

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("MetadataStatement", span.getName());
        assertEquals(SpanKind.CLIENT, span.getKind());
        assertEquals(StatusCode.OK, span.getStatus().getStatusCode());
        assertEquals("postgresql", span.getAttributes().get(DB_SYSTEM_NAME_KEY));
        assertEquals("INSERT INTO albums (title) VALUES (?)", span.getAttributes().get(DB_QUERY_TEXT_KEY));
        assertEquals("INSERT", span.getAttributes().get(DB_OPERATION_NAME_KEY));
        assertEquals("albums", span.getAttributes().get(DB_COLLECTION_NAME_KEY));
        assertEquals("MetadataStatement", span.getAttributes().get(STATEMENT_NAME_KEY));
        assertEquals("test-user", span.getAttributes().get(DB_USER_KEY));

        MetricData metric = singleDurationMetric();
        assertEquals("db.client.operation.duration", metric.getName());
        assertEquals("s", metric.getUnit());
        HistogramPointData point = singlePoint(metric);
        assertEquals("postgresql", point.getAttributes().get(DB_SYSTEM_NAME_KEY));
        assertEquals("INSERT", point.getAttributes().get(DB_OPERATION_NAME_KEY));
        assertEquals("albums", point.getAttributes().get(DB_COLLECTION_NAME_KEY));
        assertEquals("MetadataStatement", point.getAttributes().get(STATEMENT_NAME_KEY));
        assertEquals("INSERT INTO albums (title) VALUES (?)", point.getAttributes().get(DB_QUERY_TEXT_KEY));
        assertNull(point.getAttributes().get(BATCH_SIZE_KEY));
    }

    @Test
    void batchSpanAttributesAndMetricPoint() throws SQLException {
        StatementExecutor executor = executor(Duration.ofSeconds(1));
        int[] batchResults = {1, 1};
        PreparedStatement preparedStatement = stubPreparedStatement(batchResults);
        Connection connection = stubConnection(preparedStatement);
        var statement1 = new BatchStatement("UPDATE albums SET title = ?", "UPDATE", "albums", "r1");
        var statement2 = new BatchStatement("UPDATE albums SET title = ?", "UPDATE", "albums", "r2");

        List<String> results = executor.executeBatch(List.of(statement1, statement2), connection, null);

        assertEquals(List.of("r1", "r2"), results);
        flush();

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        SpanData span = spans.get(0);
        assertEquals("batch", span.getName());
        assertEquals(SpanKind.CLIENT, span.getKind());
        assertEquals(StatusCode.OK, span.getStatus().getStatusCode());
        assertEquals("postgresql", span.getAttributes().get(DB_SYSTEM_NAME_KEY));
        assertEquals("UPDATE albums SET title = ?", span.getAttributes().get(DB_QUERY_TEXT_KEY));
        assertEquals("UPDATE", span.getAttributes().get(DB_OPERATION_NAME_KEY));
        assertEquals("albums", span.getAttributes().get(DB_COLLECTION_NAME_KEY));
        assertEquals("batch", span.getAttributes().get(STATEMENT_NAME_KEY));
        assertEquals("test-user", span.getAttributes().get(DB_USER_KEY));
        assertEquals(2L, span.getAttributes().get(BATCH_SIZE_KEY));

        MetricData metric = singleDurationMetric();
        HistogramPointData point = singlePoint(metric);
        assertEquals("postgresql", point.getAttributes().get(DB_SYSTEM_NAME_KEY));
        assertEquals("UPDATE", point.getAttributes().get(DB_OPERATION_NAME_KEY));
        assertEquals("albums", point.getAttributes().get(DB_COLLECTION_NAME_KEY));
        assertEquals("batch", point.getAttributes().get(STATEMENT_NAME_KEY));
        assertEquals("UPDATE albums SET title = ?", point.getAttributes().get(DB_QUERY_TEXT_KEY));
        assertNull(point.getAttributes().get(BATCH_SIZE_KEY));
    }

    @Test
    void statementWithoutMetadataOmitsOperationAndCollectionAttributes() throws SQLException {
        StatementExecutor executor = executor(Duration.ofSeconds(1));
        var statement = new SimpleStatement("SELECT 1", "one");

        executor.execute(statement, dummyConnection(), null);
        flush();

        SpanData span = spanExporter.getFinishedSpanItems().get(0);
        assertEquals("SimpleStatement", span.getName());
        assertNull(span.getAttributes().get(DB_OPERATION_NAME_KEY));
        assertNull(span.getAttributes().get(DB_COLLECTION_NAME_KEY));

        HistogramPointData point = singlePoint(singleDurationMetric());
        assertEquals("SimpleStatement", point.getAttributes().get(STATEMENT_NAME_KEY));
        assertNull(point.getAttributes().get(DB_OPERATION_NAME_KEY));
        assertNull(point.getAttributes().get(DB_COLLECTION_NAME_KEY));
    }

    @Test
    void exceptionPathSetsErrorStatusAndRecordsException() {
        StatementExecutor executor = executor(Duration.ofSeconds(1));
        var statement = new FailingStatement("INSERT INTO albums VALUES (1)");

        SQLException thrown = assertThrows(
                SQLException.class,
                () -> executor.execute(statement, dummyConnection(), null));

        assertEquals("boom", thrown.getMessage());
        flush();

        SpanData span = spanExporter.getFinishedSpanItems().get(0);
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("boom", span.getStatus().getDescription());
        assertFalse(span.getEvents().isEmpty());
        assertEquals("exception", span.getEvents().get(0).getName());
    }

    @Test
    void slowQueryLogThresholdTriggersWarnLog() throws SQLException {
        StatementExecutor executor = executor(Duration.ofNanos(1));
        var statement = new SlowStatement("SELECT pg_sleep(0)", 50L);

        executor.execute(statement, dummyConnection(), null);
        flush();

        assertEquals(1, logger.warnings().size());
        String message = logger.warnings().get(0);
        assertTrue(message.contains("SlowStatement"), message);
        assertTrue(message.contains("seconds"), message);
    }

    @Test
    void emptyBatchReturnsEmptyListWithNoSpanOrMetric() throws SQLException {
        StatementExecutor executor = executor(Duration.ofSeconds(1));

        List<String> results = executor.executeBatch(List.of(), dummyConnection(), null);

        assertEquals(List.of(), results);
        flush();

        assertEquals(0, spanExporter.getFinishedSpanItems().size());
        assertEquals(0, metricReader.collectAllMetrics().size());
    }

    private void flush() {
        tracerProvider.forceFlush().join(5, SECONDS);
        meterProvider.forceFlush().join(5, SECONDS);
    }

    private MetricData singleDurationMetric() {
        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertEquals(1, metrics.size(), metrics::toString);
        return metrics.iterator().next();
    }

    private static HistogramPointData singlePoint(MetricData metric) {
        Collection<HistogramPointData> points = metric.getHistogramData().getPoints();
        assertEquals(1, points.size());
        return points.iterator().next();
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> iface, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                StatementExecutorTest.class.getClassLoader(),
                new Class<?>[]{iface},
                handler);
    }

    private static Connection dummyConnection() {
        return stub(Connection.class, (proxy, method, args) -> {
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static Connection stubConnection(PreparedStatement preparedStatement) {
        return stub(Connection.class, (proxy, method, args) -> {
            if (method.getName().equals("prepareStatement")) {
                return preparedStatement;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static PreparedStatement stubPreparedStatement(int[] results) {
        return stub(PreparedStatement.class, (proxy, method, args) -> {
            String name = method.getName();
            return switch (name) {
                case "addBatch", "clearParameters", "close" -> null;
                case "executeBatch" -> results;
                default -> throw new UnsupportedOperationException(name);
            };
        });
    }

    record SimpleStatement(String sql, String result) implements Statement<String> {
        @Override
        public void bindParams(PreparedStatement preparedStatement) {}

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public String decodeResultSet(ResultSet resultSet) {
            return result;
        }

        @Override
        public String decodeAffectedRows(long affectedRows) {
            return result;
        }

        @Override
        public String executeOn(Connection connection) {
            return result;
        }
    }

    record MetadataStatement(
            String sql,
            String operationName,
            String collectionName,
            String result)
            implements Statement<String>, StatementMetadata {
        @Override
        public void bindParams(PreparedStatement preparedStatement) {}

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public String decodeResultSet(ResultSet resultSet) {
            return result;
        }

        @Override
        public String decodeAffectedRows(long affectedRows) {
            return result;
        }

        @Override
        public String executeOn(Connection connection) {
            return result;
        }
    }

    record FailingStatement(String sql) implements Statement<String> {
        @Override
        public void bindParams(PreparedStatement preparedStatement) {}

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public String decodeResultSet(ResultSet resultSet) {
            return null;
        }

        @Override
        public String decodeAffectedRows(long affectedRows) {
            return null;
        }

        @Override
        public String executeOn(Connection connection) throws SQLException {
            throw new SQLException("boom");
        }
    }

    record SlowStatement(String sql, long sleepMillis) implements Statement<String> {
        @Override
        public void bindParams(PreparedStatement preparedStatement) {}

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public String decodeResultSet(ResultSet resultSet) {
            return "ok";
        }

        @Override
        public String decodeAffectedRows(long affectedRows) {
            return "ok";
        }

        @Override
        public String executeOn(Connection connection) {
            try {
                TimeUnit.MILLISECONDS.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        }
    }

    record BatchStatement(
            String sql,
            String operationName,
            String collectionName,
            String result)
            implements Statement<String>, StatementMetadata {
        @Override
        public void bindParams(PreparedStatement preparedStatement) {}

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public String decodeResultSet(ResultSet resultSet) {
            return result;
        }

        @Override
        public String decodeAffectedRows(long affectedRows) {
            return result;
        }
    }

    static final class CollectingLogger implements Logger {
        private final List<String> warnings = new ArrayList<>();

        List<String> warnings() {
            return warnings;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void warn(String msg) {
            warnings.add(msg);
        }

        @Override
        public void warn(String format, Object arg) {
            warnings.add(substitute(format, arg));
        }

        @Override
        public void warn(String format, Object arg1, Object arg2) {
            warnings.add(substitute(format, arg1, arg2));
        }

        @Override
        public void warn(String format, Object... arguments) {
            warnings.add(substitute(format, arguments));
        }

        @Override
        public void warn(String msg, Throwable t) {
            warnings.add(msg);
        }

        private static String substitute(String template, Object... args) {
            StringBuilder result = new StringBuilder();
            int argIndex = 0;
            int i = 0;
            while (i < template.length()) {
                int placeholder = template.indexOf("{}", i);
                if (placeholder == -1) {
                    result.append(template, i, template.length());
                    break;
                }
                result.append(template, i, placeholder);
                if (argIndex < args.length) {
                    result.append(args[argIndex++]);
                } else {
                    result.append("{}");
                }
                i = placeholder + 2;
            }
            return result.toString();
        }

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public void trace(String msg) {}

        @Override
        public void trace(String format, Object arg) {}

        @Override
        public void trace(String format, Object arg1, Object arg2) {}

        @Override
        public void trace(String format, Object... arguments) {}

        @Override
        public void trace(String msg, Throwable t) {}

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return false;
        }

        @Override
        public void trace(Marker marker, String msg) {}

        @Override
        public void trace(Marker marker, String format, Object arg) {}

        @Override
        public void trace(Marker marker, String format, Object arg1, Object arg2) {}

        @Override
        public void trace(Marker marker, String format, Object... argArray) {}

        @Override
        public void trace(Marker marker, String msg, Throwable t) {}

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(String msg) {}

        @Override
        public void debug(String format, Object arg) {}

        @Override
        public void debug(String format, Object arg1, Object arg2) {}

        @Override
        public void debug(String format, Object... arguments) {}

        @Override
        public void debug(String msg, Throwable t) {}

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return false;
        }

        @Override
        public void debug(Marker marker, String msg) {}

        @Override
        public void debug(Marker marker, String format, Object arg) {}

        @Override
        public void debug(Marker marker, String format, Object arg1, Object arg2) {}

        @Override
        public void debug(Marker marker, String format, Object... arguments) {}

        @Override
        public void debug(Marker marker, String msg, Throwable t) {}

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public void info(String msg) {}

        @Override
        public void info(String format, Object arg) {}

        @Override
        public void info(String format, Object arg1, Object arg2) {}

        @Override
        public void info(String format, Object... arguments) {}

        @Override
        public void info(String msg, Throwable t) {}

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return false;
        }

        @Override
        public void info(Marker marker, String msg) {}

        @Override
        public void info(Marker marker, String format, Object arg) {}

        @Override
        public void info(Marker marker, String format, Object arg1, Object arg2) {}

        @Override
        public void info(Marker marker, String format, Object... arguments) {}

        @Override
        public void info(Marker marker, String msg, Throwable t) {}

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return true;
        }

        @Override
        public void warn(Marker marker, String msg) {
            warnings.add(msg);
        }

        @Override
        public void warn(Marker marker, String format, Object arg) {
            warnings.add(substitute(format, arg));
        }

        @Override
        public void warn(Marker marker, String format, Object arg1, Object arg2) {
            warnings.add(substitute(format, arg1, arg2));
        }

        @Override
        public void warn(Marker marker, String format, Object... arguments) {
            warnings.add(substitute(format, arguments));
        }

        @Override
        public void warn(Marker marker, String msg, Throwable t) {
            warnings.add(msg);
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public void error(String msg) {}

        @Override
        public void error(String format, Object arg) {}

        @Override
        public void error(String format, Object arg1, Object arg2) {}

        @Override
        public void error(String format, Object... arguments) {}

        @Override
        public void error(String msg, Throwable t) {}

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return false;
        }

        @Override
        public void error(Marker marker, String msg) {}

        @Override
        public void error(Marker marker, String format, Object arg) {}

        @Override
        public void error(Marker marker, String format, Object arg1, Object arg2) {}

        @Override
        public void error(Marker marker, String format, Object... arguments) {}

        @Override
        public void error(Marker marker, String msg, Throwable t) {}
    }
}
