package io.pgenie.example.myspace.musiccatalogue.types;

import java.time.LocalDate;

import io.pgenie.example.myspace.musiccatalogue.codecs.CompositeCodec;
import io.pgenie.example.myspace.musiccatalogue.codecs.Codec;

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

    public static final CompositeCodec<RecordingInfo> CODEC = new CompositeCodec<>(
            "public", "recording_info",
            (String studioName) -> (String city) -> (String country) -> (LocalDate recordedDate) -> new RecordingInfo(
                    studioName, city, country, recordedDate),
            new CompositeCodec.Field<>("studio_name", RecordingInfo::studioName, Codec.TEXT),
            new CompositeCodec.Field<>("city", RecordingInfo::city, Codec.TEXT),
            new CompositeCodec.Field<>("country", RecordingInfo::country, Codec.TEXT),
            new CompositeCodec.Field<>("recorded_date", RecordingInfo::recordedDate, Codec.DATE));

}
