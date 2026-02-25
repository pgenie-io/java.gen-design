package io.pgenie.example.myspace.musiccatalogue.types;

import java.util.Map;

import io.pgenie.example.myspace.musiccatalogue.codecs.EnumScalar;

/**
 * Representation of the {@code album_format} user-declared PostgreSQL
 * enumeration type.
 *
 * <p>
 * Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 */
public enum AlbumFormat {

    /**
     * Corresponds to the PostgreSQL enum variant {@code Vinyl}.
     */
    VINYL,
    /**
     * Corresponds to the PostgreSQL enum variant {@code CD}.
     */
    CD,
    /**
     * Corresponds to the PostgreSQL enum variant {@code Cassette}.
     */
    CASSETTE,
    /**
     * Corresponds to the PostgreSQL enum variant {@code Digital}.
     */
    DIGITAL,
    /**
     * Corresponds to the PostgreSQL enum variant {@code DVD-Audio}.
     */
    DVD_AUDIO,
    /**
     * Corresponds to the PostgreSQL enum variant {@code SACD}.
     */
    SACD;

    public static final EnumScalar<AlbumFormat> CODEC = new EnumScalar<>(
            "public", "album_format",
            Map.ofEntries(
                    Map.entry(VINYL, "Vinyl"),
                    Map.entry(CD, "CD"),
                    Map.entry(CASSETTE, "Cassette"),
                    Map.entry(DIGITAL, "Digital"),
                    Map.entry(DVD_AUDIO, "DVD-Audio"),
                    Map.entry(SACD, "SACD")));

}
