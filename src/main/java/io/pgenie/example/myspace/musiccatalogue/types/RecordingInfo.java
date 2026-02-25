package io.pgenie.example.myspace.musiccatalogue.types;

import io.pgenie.example.myspace.musiccatalogue.codecs.Composite;
import io.pgenie.example.myspace.musiccatalogue.codecs.CompositeField;
import io.pgenie.example.myspace.musiccatalogue.codecs.Scalar;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Representation of the {@code recording_info} user-declared PostgreSQL
 * composite (record) type.
 *
 * <p>
 * Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 *
 * <p>
 * All fields are nullable, matching the PostgreSQL column definitions.
 *
 * @param studioName   Maps to {@code studio_name}.
 * @param city         Maps to {@code city}.
 * @param country      Maps to {@code country}.
 * @param recordedDate Maps to {@code recorded_date}.
 */
public record RecordingInfo(
        String studioName,
        String city,
        String country,
        LocalDate recordedDate) {

    public static final Composite<RecordingInfo> codec = new Composite<>(
            "public", "recording_info",
            (String studioName) -> (String city) -> (String country) -> (LocalDate recordedDate) -> new RecordingInfo(
                    studioName, city, country, recordedDate),
            new CompositeField<>("studio_name", RecordingInfo::studioName, Scalar.text),
            new CompositeField<>("city", RecordingInfo::city, Scalar.text),
            new CompositeField<>("country", RecordingInfo::country, Scalar.text),
            new CompositeField<>("recorded_date", RecordingInfo::recordedDate, Scalar.localDate));

}
