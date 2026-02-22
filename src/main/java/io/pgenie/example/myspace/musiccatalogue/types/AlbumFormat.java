package io.pgenie.example.myspace.musiccatalogue.types;

import org.postgresql.util.PGobject;

import java.sql.SQLException;

/**
 * Representation of the {@code album_format} user-declared PostgreSQL
 * enumeration type.
 *
 * <p>Generated from SQL queries using the
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

    /**
     * Look up an {@code AlbumFormat} by its PostgreSQL label string.
     *
     * @throws IllegalArgumentException if {@code value} is not a known label.
     */
    public static AlbumFormat fromPgValue(String value) {
        for (AlbumFormat f : values()) {
            if (f.pgValue.equals(value)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown album_format value: " + value);
    }

    /**
     * Encode a nullable {@code AlbumFormat} as a {@link PGobject} suitable for
     * use as a JDBC parameter. A {@code null} input produces a {@code PGobject}
     * whose value is {@code null} (SQL NULL).
     */
    public static PGobject toPgObject(AlbumFormat value) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType("album_format");
        obj.setValue(value != null ? value.pgValue() : null);
        return obj;
    }
}
