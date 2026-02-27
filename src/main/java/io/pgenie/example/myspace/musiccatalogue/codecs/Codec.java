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

    /**
     * Parses a PostgreSQL text-format literal of type A from {@code input}
     * starting at {@code offset}.
     *
     * <p>The input must be a non-null {@link CharSequence} holding the raw text
     * as returned by the PostgreSQL server (e.g. the string value of a column
     * obtained via {@link java.sql.ResultSet#getString}). Passing the
     * {@code String} directly avoids an extra copy compared to converting to a
     * {@code char[]} first. NULL column values must be handled by the caller
     * before invoking this method.
     *
     * <p>Returns the parsed value together with the offset of the first
     * character that was <em>not</em> consumed, allowing callers to continue
     * parsing subsequent fields without copying the input. Throws
     * {@link ParseException} if the input cannot be interpreted as a valid
     * literal of type A.
     */
    ParsingResult<A> parse(CharSequence input, int offset) throws ParseException;

    final class ParsingResult<A> {

        public final A value;
        public final int nextOffset;

        public ParsingResult(A value, int nextOffset) {
            this.value = value;
            this.nextOffset = nextOffset;
        }

    }

    final class ParseException extends Exception {

        public ParseException(CharSequence input, int offset, String message) {
            this(input.subSequence(offset, input.length()), message);
        }

        public ParseException(CharSequence input, String message) {
            super(String.format("Parse error: %s (input: \"%s\")", message, input));
        }

    }

}
