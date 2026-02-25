package io.pgenie.example.myspace.musiccatalogue.types;

import java.time.LocalDate;

import io.pgenie.example.myspace.musiccatalogue.codecs.CompositeScalar;
import io.pgenie.example.myspace.musiccatalogue.codecs.Field;
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
 * @param studioName Maps to {@code studio_name}.
 * @param city Maps to {@code city}.
 * @param country Maps to {@code country}.
 * @param recordedDate Maps to {@code recorded_date}.
 */
public record RecordingInfo(
        String studioName,
        String city,
        String country,
        LocalDate recordedDate) {

    public static final CompositeScalar<RecordingInfo> CODEC = new CompositeScalar<>(
            "public", "recording_info",
            (String studioName) -> (String city) -> (String country) -> (LocalDate recordedDate) -> new RecordingInfo(
                    studioName, city, country, recordedDate),
            new Field<>("studio_name", RecordingInfo::studioName, Scalar.TEXT),
            new Field<>("city", RecordingInfo::city, Scalar.TEXT),
            new Field<>("country", RecordingInfo::country, Scalar.TEXT),
            new Field<>("recorded_date", RecordingInfo::recordedDate, Scalar.DATE));

}
