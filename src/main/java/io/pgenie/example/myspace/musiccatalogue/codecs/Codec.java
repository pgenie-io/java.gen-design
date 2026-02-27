package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;

/**
 * A codec for a single scalar value.
 *
 * @param <A> the type of the value
 */
public interface Codec<A> {

    // Codecs named by their PostgreSQL type name.
    public static final Codec<Long> INT8 = Int8Codec.instance;
    public static final Codec<String> TEXT = TextCodec.instance;
    public static final Codec<java.time.LocalDate> DATE = DateCodec.instance;

    String name();

    /**
     * Binds the given value to the specified index in the prepared statement.
     */
    void bind(PreparedStatement ps, int index, A value) throws java.sql.SQLException;

    /**
     * Writes the given value to the string builder in textual literal form.
     *
     * This is primarily used for encoding fields in composite types.
     * Unfortunately, the PostgreSQL JDBC driver does not support natively
     * encoding composite types or the binary format for them.
     */
    void write(StringBuilder sb, A value);

    A parse(CharSequence text);

}
