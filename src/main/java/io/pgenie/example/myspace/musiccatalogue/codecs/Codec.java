package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;

/**
 * A codec for a single scalar value.
 *
 * @param <A> the type of the value
 */
public interface Codec<A> {

    // Codecs named by their PostgreSQL type name.
    public static final Codec<Boolean> BOOL = BoolCodec.instance;
    public static final Codec<Short> INT2 = Int2Codec.instance;
    public static final Codec<Integer> INT4 = Int4Codec.instance;
    public static final Codec<Long> INT8 = Int8Codec.instance;
    public static final Codec<Float> FLOAT4 = Float4Codec.instance;
    public static final Codec<Double> FLOAT8 = Float8Codec.instance;
    public static final Codec<java.math.BigDecimal> NUMERIC = NumericCodec.instance;
    public static final Codec<String> TEXT = TextCodec.instance;
    public static final Codec<String> CHAR = CharCodec.instance;
    public static final Codec<String> VARCHAR = VarcharCodec.instance;
    public static final Codec<byte[]> BYTEA = ByteaCodec.instance;
    public static final Codec<java.time.LocalDate> DATE = DateCodec.instance;
    public static final Codec<java.time.LocalTime> TIME = TimeCodec.instance;
    public static final Codec<java.time.OffsetTime> TIMETZ = TimetzCodec.instance;
    public static final Codec<java.time.LocalDateTime> TIMESTAMP = TimestampCodec.instance;
    public static final Codec<java.time.OffsetDateTime> TIMESTAMPTZ = TimestamptzCodec.instance;
    public static final Codec<String> INTERVAL = IntervalCodec.instance;
    public static final Codec<java.util.UUID> UUID = UuidCodec.instance;
    public static final Codec<String> JSON = JsonCodec.instance;
    public static final Codec<String> JSONB = JsonbCodec.instance;
    public static final Codec<Long> OID = OidCodec.instance;
    public static final Codec<String> MONEY = MoneyCodec.instance;
    public static final Codec<String> INET = InetCodec.instance;
    public static final Codec<String> CIDR = CidrCodec.instance;
    public static final Codec<String> MACADDR = MacaddrCodec.instance;
    public static final Codec<String> MACADDR8 = Macaddr8Codec.instance;
    public static final Codec<org.postgresql.geometric.PGpoint> POINT = PointCodec.instance;
    public static final Codec<org.postgresql.geometric.PGline> LINE = LineCodec.instance;
    public static final Codec<org.postgresql.geometric.PGlseg> LSEG = LsegCodec.instance;
    public static final Codec<org.postgresql.geometric.PGbox> BOX = BoxCodec.instance;
    public static final Codec<org.postgresql.geometric.PGpath> PATH = PathCodec.instance;
    public static final Codec<org.postgresql.geometric.PGpolygon> POLYGON = PolygonCodec.instance;
    public static final Codec<org.postgresql.geometric.PGcircle> CIRCLE = CircleCodec.instance;
    public static final Codec<String> BIT = BitCodec.instance;
    public static final Codec<String> VARBIT = VarbitCodec.instance;
    public static final Codec<String> TSVECTOR = TsvectorCodec.instance;

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
