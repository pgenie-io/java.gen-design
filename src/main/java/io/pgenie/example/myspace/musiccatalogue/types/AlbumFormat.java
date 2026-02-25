package io.pgenie.example.myspace.musiccatalogue.types;

import io.pgenie.example.myspace.musiccatalogue.codecs.Enum;

/**
 * Representation of the {@code album_format} user-declared PostgreSQL
 * enumeration type.
 *
 * <p>
 * Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 */
public enum AlbumFormat {

    /** Corresponds to the PostgreSQL enum variant {@code Vinyl}. */
    VINYL("Vinyl"),
    /** Corresponds to the PostgreSQL enum variant {@code CD}. */
    CD("CD"),
    /** Corresponds to the PostgreSQL enum variant {@code Cassette}. */
    CASSETTE("Cassette"),
    /** Corresponds to the PostgreSQL enum variant {@code Digital}. */
    DIGITAL("Digital"),
    /** Corresponds to the PostgreSQL enum variant {@code DVD-Audio}. */
    DVD_AUDIO("DVD-Audio"),
    /** Corresponds to the PostgreSQL enum variant {@code SACD}. */
    SACD("SACD");

    private final String pgValue;

    AlbumFormat(String pgValue) {
        this.pgValue = pgValue;
    }

    /** The PostgreSQL enum label string for this variant. */
    public String pgValue() {
        return pgValue;
    }

    public static final Enum<AlbumFormat> codec = new Enum<>(
            "public", "album_format",
            AlbumFormat.values(),
            AlbumFormat::pgValue);

}
