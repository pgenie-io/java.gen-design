package io.pgenie.example.myspace.musiccatalogue.types;

import java.time.LocalDate;

import io.pgenie.example.myspace.musiccatalogue.codecs.Composite;
import io.pgenie.example.myspace.musiccatalogue.codecs.CompositeField;
import io.pgenie.example.myspace.musiccatalogue.codecs.Scalar;

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

    public static final Composite<RecordingInfo> CODEC = new Composite<>(
            "public", "recording_info",
            (String studioName) -> (String city) -> (String country) -> (LocalDate recordedDate) -> new RecordingInfo(
                    studioName, city, country, recordedDate),
            new CompositeField<>("studio_name", RecordingInfo::studioName, Scalar.TEXT),
            new CompositeField<>("city", RecordingInfo::city, Scalar.TEXT),
            new CompositeField<>("country", RecordingInfo::country, Scalar.TEXT),
            new CompositeField<>("recorded_date", RecordingInfo::recordedDate, Scalar.DATE));

}
