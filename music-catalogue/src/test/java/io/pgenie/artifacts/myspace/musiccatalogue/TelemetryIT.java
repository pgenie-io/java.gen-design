package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.pgenie.artifacts.myspace.musiccatalogue.statements.InsertAlbum;
import io.pgenie.artifacts.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.artifacts.myspace.musiccatalogue.types.RecordingInfo;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TelemetryIT extends AbstractDatabaseIT {

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

        var config = MusicCatalogueConfig.builder()
                .jdbcUrl(PG.getJdbcUrl())
                .user(PG.getUsername())
                .password(PG.getPassword())
                .openTelemetry(openTelemetry)
                .build();

        try (var telemetrySession = new MusicCatalogueSession(config)) {
            assertDoesNotThrow(() -> telemetrySession.execute(
                    new InsertAlbum("Telemetry Album", LocalDate.of(2023, 1, 1), AlbumFormat.Cd, randomRecordingInfo())));
            assertDoesNotThrow(() -> telemetrySession.executeTransaction(
                    tx -> tx.execute(new InsertAlbum("Telemetry Tx Album", LocalDate.of(2023, 2, 1), AlbumFormat.Vinyl, randomRecordingInfo()))));
        }

        tracerProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);
        meterProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);

        var spans = spanExporter.getFinishedSpanItems();
        assertFalse(spans.isEmpty());
        var spanNames = spans.stream().map(s -> s.getName()).toList();
        assertTrue(spanNames.contains("InsertAlbum"), "Expected InsertAlbum span, got: " + spanNames);
        assertTrue(spanNames.contains("transaction"), "Expected transaction span, got: " + spanNames);

        var metrics = metricReader.collectAllMetrics();
        assertFalse(metrics.isEmpty(), "Expected metrics to be emitted");
        var metricNames = metrics.stream().map(MetricData::getName).toList();
        assertTrue(metricNames.contains("pgenie.musiccatalogue.statement.duration"), metricNames::toString);
        assertTrue(metricNames.contains("pgenie.musiccatalogue.pool.connections.active"), metricNames::toString);
        assertTrue(metricNames.contains("pgenie.musiccatalogue.pool.connections.idle"), metricNames::toString);
        assertTrue(metricNames.contains("pgenie.musiccatalogue.pool.connections.total"), metricNames::toString);
    }

    private static RecordingInfo randomRecordingInfo() {
        return RecordingInfo.CODEC.toAgnostic().random(new Random(0L), 0);
    }
}
