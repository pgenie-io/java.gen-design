package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import io.pgenie.artifacts.myspace.musiccatalogue.statements.InsertAlbum;
import io.pgenie.artifacts.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.artifacts.myspace.musiccatalogue.types.RecordingInfo;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TelemetryIT extends AbstractDatabaseIT {

    private static final AttributeKey<String> DB_SYSTEM_NAME_KEY = AttributeKey.stringKey("db.system.name");
    private static final AttributeKey<String> DB_QUERY_TEXT_KEY = AttributeKey.stringKey("db.query.text");
    private static final AttributeKey<String> DB_USER_KEY = AttributeKey.stringKey("pgenie.db.user");
    private static final AttributeKey<String> STATEMENT_NAME_KEY = AttributeKey.stringKey("pgenie.statement.name");
    private static final AttributeKey<Long> ATTEMPT_COUNT_KEY = AttributeKey.longKey("pgenie.transaction.attempt_count");
    private static final AttributeKey<String> OUTCOME_KEY = AttributeKey.stringKey("pgenie.transaction.outcome");

    @Test
    void statementAndTransactionSpansAndMetricsAreEmitted() throws Exception {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        InMemoryMetricReader metricReader = InMemoryMetricReader.create();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();

        var config = MusicCatalogueConfig
                .defaults(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .withOpenTelemetry(openTelemetry);

        var telemetrySession = new MusicCatalogueSession(config);
        try {
            assertDoesNotThrow(() -> telemetrySession.execute(
                    new InsertAlbum("Telemetry Album", LocalDate.of(2023, 1, 1), AlbumFormat.Cd, randomRecordingInfo())));
            assertDoesNotThrow(() -> telemetrySession.executeTransaction(
                    tx -> tx.execute(new InsertAlbum("Telemetry Tx Album", LocalDate.of(2023, 2, 1), AlbumFormat.Vinyl, randomRecordingInfo()))));

            tracerProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);
            meterProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);

            var spans = spanExporter.getFinishedSpanItems();
            assertFalse(spans.isEmpty());
            var spanNames = spans.stream().map(s -> s.getName()).toList();
            assertTrue(spanNames.contains("InsertAlbum"), "Expected InsertAlbum span, got: " + spanNames);
            assertTrue(spanNames.contains("transaction"), "Expected transaction span, got: " + spanNames);

            SpanData statementSpan = spans.stream()
                    .filter(s -> s.getName().equals("InsertAlbum"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("postgresql", statementSpan.getAttributes().get(DB_SYSTEM_NAME_KEY));
            assertNotNull(statementSpan.getAttributes().get(DB_QUERY_TEXT_KEY));
            assertEquals("InsertAlbum", statementSpan.getAttributes().get(STATEMENT_NAME_KEY));
            assertEquals(PG.getUsername(), statementSpan.getAttributes().get(DB_USER_KEY));
            assertEquals(
                    "io.pgenie.artifacts.myspace.musiccatalogue",
                    statementSpan.getInstrumentationScopeInfo().getName());

            SpanData transactionSpan = spans.stream()
                    .filter(s -> s.getName().equals("transaction"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("postgresql", transactionSpan.getAttributes().get(DB_SYSTEM_NAME_KEY));
            assertNotNull(transactionSpan.getAttributes().get(ATTEMPT_COUNT_KEY));
            assertNotNull(transactionSpan.getAttributes().get(OUTCOME_KEY));
            assertEquals(
                    "io.pgenie.artifacts.myspace.musiccatalogue",
                    transactionSpan.getInstrumentationScopeInfo().getName());

            // Metrics must be collected while the session (and its pool gauges) is still
            // open: session.close() unregisters the gauge callbacks, so collecting after
            // close would miss the pool.connections.* series.
            var metrics = metricReader.collectAllMetrics();
            assertFalse(metrics.isEmpty(), "Expected metrics to be emitted");
            var metricNames = metrics.stream().map(MetricData::getName).toList();
            assertTrue(metricNames.contains("db.client.operation.duration"), metricNames::toString);
            assertTrue(metricNames.contains("pgenie.pool.connections.active"), metricNames::toString);
            assertTrue(metricNames.contains("pgenie.pool.connections.idle"), metricNames::toString);
            assertTrue(metricNames.contains("pgenie.pool.connections.total"), metricNames::toString);
        } finally {
            telemetrySession.close();
        }
    }

    @Test
    void explicitParentSpanIsUsedForStatementAndTransactionTraces() throws Exception {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        var config = MusicCatalogueConfig
                .defaults(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .withOpenTelemetry(openTelemetry);

        io.opentelemetry.api.trace.Span parentSpan;
        try (var telemetrySession = new MusicCatalogueSession(config)) {
            parentSpan = openTelemetry.getTracer("test").spanBuilder("test-parent").startSpan();
            telemetrySession.execute(
                    new InsertAlbum("Parented Album", LocalDate.of(2023, 3, 1), AlbumFormat.Cd, randomRecordingInfo()),
                    parentSpan);
            telemetrySession.executeTransaction(
                    tx -> tx.execute(new InsertAlbum(
                            "Parented Tx Album", LocalDate.of(2023, 4, 1), AlbumFormat.Vinyl, randomRecordingInfo())),
                    parentSpan);
            parentSpan.end();
        }

        tracerProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertFalse(spans.isEmpty());

        SpanData parent = spans.stream()
                .filter(s -> s.getName().equals("test-parent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected parent span"));

        List<SpanData> childrenOfParent = spans.stream()
                .filter(s -> s.getParentSpanId().equals(parent.getSpanId()))
                .toList();
        var childNames = childrenOfParent.stream().map(SpanData::getName).toList();
        assertTrue(childNames.contains("InsertAlbum"), "Expected statement span under parent, got: " + childNames);
        assertTrue(childNames.contains("transaction"), "Expected transaction span under parent, got: " + childNames);

        var transactionSpan = childrenOfParent.stream()
                .filter(s -> s.getName().equals("transaction"))
                .findFirst()
                .orElseThrow();
        boolean statementUnderTransaction = spans.stream()
                .anyMatch(s -> s.getName().equals("InsertAlbum") && s.getParentSpanId().equals(transactionSpan.getSpanId()));
        assertTrue(
                statementUnderTransaction,
                "Expected statement executed inside transaction to be a child of the transaction span");
    }

    private static RecordingInfo randomRecordingInfo() {
        return RecordingInfo.CODEC.toAgnostic().random(new Random(0L), 0);
    }
}
