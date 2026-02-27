package io.pgenie.example.myspace.musiccatalogue.codecs;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for all codecs. Each test sends a value to PostgreSQL
 * and reads it back, verifying the round-trip through encode → server → decode.
 */
public class CodecIT {

    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:18");

    @BeforeAll
    static void startContainer() {
        pg.start();
    }

    @AfterAll
    static void stopContainer() {
        pg.stop();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    }

    /**
     * Generic helper: binds a value using the codec, sends it through PostgreSQL
     * via a cast expression, reads back the text representation, and parses it.
     */
    private <A> A roundTrip(Codec<A> codec, String castType, A value) throws Exception {
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::" + castType)) {
            codec.bind(ps, 1, value);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Expected a result row");
                String text = rs.getString(1);
                if (text == null) return null;
                var result = codec.parse(text, 0);
                return result.value;
            }
        }
    }

    /**
     * Helper for types where we just check string equality of results
     * (useful when the Java type doesn't have a natural equals, like byte[]).
     */
    private <A> String roundTripText(Codec<A> codec, String castType, A value) throws Exception {
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::" + castType)) {
            codec.bind(ps, 1, value);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Expected a result row");
                return rs.getString(1);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Bool
    // -----------------------------------------------------------------------

    @Test
    void boolTrue() throws Exception {
        assertEquals(true, roundTrip(Codec.BOOL, "bool", true));
    }

    @Test
    void boolFalse() throws Exception {
        assertEquals(false, roundTrip(Codec.BOOL, "bool", false));
    }

    @Test
    void boolNull() throws Exception {
        assertNull(roundTrip(Codec.BOOL, "bool", null));
    }

    // -----------------------------------------------------------------------
    // Int2
    // -----------------------------------------------------------------------

    @Test
    void int2RoundTrip() throws Exception {
        assertEquals((short) 12345, roundTrip(Codec.INT2, "int2", (short) 12345));
    }

    @Test
    void int2Negative() throws Exception {
        assertEquals((short) -32000, roundTrip(Codec.INT2, "int2", (short) -32000));
    }

    @Test
    void int2Null() throws Exception {
        assertNull(roundTrip(Codec.INT2, "int2", null));
    }

    // -----------------------------------------------------------------------
    // Int4
    // -----------------------------------------------------------------------

    @Test
    void int4RoundTrip() throws Exception {
        assertEquals(42, roundTrip(Codec.INT4, "int4", 42));
    }

    @Test
    void int4Negative() throws Exception {
        assertEquals(-100000, roundTrip(Codec.INT4, "int4", -100000));
    }

    @Test
    void int4Null() throws Exception {
        assertNull(roundTrip(Codec.INT4, "int4", null));
    }

    // -----------------------------------------------------------------------
    // Int8
    // -----------------------------------------------------------------------

    @Test
    void int8RoundTrip() throws Exception {
        assertEquals(9876543210L, roundTrip(Codec.INT8, "int8", 9876543210L));
    }

    @Test
    void int8Negative() throws Exception {
        assertEquals(-9876543210L, roundTrip(Codec.INT8, "int8", -9876543210L));
    }

    @Test
    void int8Null() throws Exception {
        assertNull(roundTrip(Codec.INT8, "int8", null));
    }

    // -----------------------------------------------------------------------
    // Float4
    // -----------------------------------------------------------------------

    @Test
    void float4RoundTrip() throws Exception {
        assertEquals(3.14f, roundTrip(Codec.FLOAT4, "float4", 3.14f), 0.001f);
    }

    @Test
    void float4NaN() throws Exception {
        assertTrue(Float.isNaN(roundTrip(Codec.FLOAT4, "float4", Float.NaN)));
    }

    @Test
    void float4Infinity() throws Exception {
        assertEquals(Float.POSITIVE_INFINITY, roundTrip(Codec.FLOAT4, "float4", Float.POSITIVE_INFINITY));
    }

    @Test
    void float4NegInfinity() throws Exception {
        assertEquals(Float.NEGATIVE_INFINITY, roundTrip(Codec.FLOAT4, "float4", Float.NEGATIVE_INFINITY));
    }

    @Test
    void float4Null() throws Exception {
        assertNull(roundTrip(Codec.FLOAT4, "float4", null));
    }

    // -----------------------------------------------------------------------
    // Float8
    // -----------------------------------------------------------------------

    @Test
    void float8RoundTrip() throws Exception {
        assertEquals(3.141592653589793, roundTrip(Codec.FLOAT8, "float8", 3.141592653589793));
    }

    @Test
    void float8NaN() throws Exception {
        assertTrue(Double.isNaN(roundTrip(Codec.FLOAT8, "float8", Double.NaN)));
    }

    @Test
    void float8Infinity() throws Exception {
        assertEquals(Double.POSITIVE_INFINITY, roundTrip(Codec.FLOAT8, "float8", Double.POSITIVE_INFINITY));
    }

    @Test
    void float8Null() throws Exception {
        assertNull(roundTrip(Codec.FLOAT8, "float8", null));
    }

    // -----------------------------------------------------------------------
    // Numeric
    // -----------------------------------------------------------------------

    @Test
    void numericRoundTrip() throws Exception {
        assertEquals(0, new BigDecimal("123456.789012").compareTo(
                roundTrip(Codec.NUMERIC, "numeric", new BigDecimal("123456.789012"))));
    }

    @Test
    void numericZero() throws Exception {
        assertEquals(0, BigDecimal.ZERO.compareTo(
                roundTrip(Codec.NUMERIC, "numeric", BigDecimal.ZERO)));
    }

    @Test
    void numericNegative() throws Exception {
        assertEquals(0, new BigDecimal("-99999.99").compareTo(
                roundTrip(Codec.NUMERIC, "numeric", new BigDecimal("-99999.99"))));
    }

    @Test
    void numericNull() throws Exception {
        assertNull(roundTrip(Codec.NUMERIC, "numeric", null));
    }

    // -----------------------------------------------------------------------
    // Text
    // -----------------------------------------------------------------------

    @Test
    void textRoundTrip() throws Exception {
        assertEquals("Hello, World!", roundTrip(Codec.TEXT, "text", "Hello, World!"));
    }

    @Test
    void textEmpty() throws Exception {
        assertEquals("", roundTrip(Codec.TEXT, "text", ""));
    }

    @Test
    void textSpecialChars() throws Exception {
        assertEquals("It's a \"test\" with \\backslash",
                roundTrip(Codec.TEXT, "text", "It's a \"test\" with \\backslash"));
    }

    @Test
    void textNull() throws Exception {
        assertNull(roundTrip(Codec.TEXT, "text", null));
    }

    // -----------------------------------------------------------------------
    // Varchar
    // -----------------------------------------------------------------------

    @Test
    void varcharRoundTrip() throws Exception {
        assertEquals("hello", roundTrip(Codec.VARCHAR, "varchar", "hello"));
    }

    // -----------------------------------------------------------------------
    // Char (bpchar - blank-padded)
    // -----------------------------------------------------------------------

    @Test
    void charRoundTrip() throws Exception {
        // char(5) pads with spaces
        String result = roundTrip(Codec.CHAR, "char(5)", "ab");
        assertEquals("ab   ", result);
    }

    // -----------------------------------------------------------------------
    // Bytea
    // -----------------------------------------------------------------------

    @Test
    void byteaRoundTrip() throws Exception {
        byte[] input = new byte[]{0x01, 0x02, (byte) 0xFF, 0x00, 0x7F};
        String text = roundTripText(Codec.BYTEA, "bytea", input);
        assertNotNull(text);
        var parsed = Codec.BYTEA.parse(text, 0);
        assertArrayEquals(input, parsed.value);
    }

    @Test
    void byteaEmpty() throws Exception {
        byte[] input = new byte[0];
        String text = roundTripText(Codec.BYTEA, "bytea", input);
        assertNotNull(text);
        var parsed = Codec.BYTEA.parse(text, 0);
        assertArrayEquals(input, parsed.value);
    }

    @Test
    void byteaNull() throws Exception {
        assertNull(roundTripText(Codec.BYTEA, "bytea", null));
    }

    // -----------------------------------------------------------------------
    // Date
    // -----------------------------------------------------------------------

    @Test
    void dateRoundTrip() throws Exception {
        assertEquals(LocalDate.of(2024, 6, 15),
                roundTrip(Codec.DATE, "date", LocalDate.of(2024, 6, 15)));
    }

    @Test
    void dateNull() throws Exception {
        assertNull(roundTrip(Codec.DATE, "date", null));
    }

    // -----------------------------------------------------------------------
    // Time
    // -----------------------------------------------------------------------

    @Test
    void timeRoundTrip() throws Exception {
        assertEquals(LocalTime.of(14, 30, 45),
                roundTrip(Codec.TIME, "time", LocalTime.of(14, 30, 45)));
    }

    @Test
    void timeWithMicros() throws Exception {
        var t = LocalTime.of(14, 30, 45, 123456000);
        assertEquals(t, roundTrip(Codec.TIME, "time", t));
    }

    @Test
    void timeNull() throws Exception {
        assertNull(roundTrip(Codec.TIME, "time", null));
    }

    // -----------------------------------------------------------------------
    // Timetz
    // -----------------------------------------------------------------------

    @Test
    void timetzRoundTrip() throws Exception {
        var t = OffsetTime.of(14, 30, 45, 0, ZoneOffset.ofHours(3));
        assertEquals(t, roundTrip(Codec.TIMETZ, "timetz", t));
    }

    @Test
    void timetzNull() throws Exception {
        assertNull(roundTrip(Codec.TIMETZ, "timetz", null));
    }

    // -----------------------------------------------------------------------
    // Timestamp
    // -----------------------------------------------------------------------

    @Test
    void timestampRoundTrip() throws Exception {
        var ts = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
        assertEquals(ts, roundTrip(Codec.TIMESTAMP, "timestamp", ts));
    }

    @Test
    void timestampWithMicros() throws Exception {
        var ts = LocalDateTime.of(2024, 6, 15, 14, 30, 45, 123456000);
        assertEquals(ts, roundTrip(Codec.TIMESTAMP, "timestamp", ts));
    }

    @Test
    void timestampNull() throws Exception {
        assertNull(roundTrip(Codec.TIMESTAMP, "timestamp", null));
    }

    // -----------------------------------------------------------------------
    // Timestamptz
    // -----------------------------------------------------------------------

    @Test
    void timestamptzRoundTrip() throws Exception {
        var ts = OffsetDateTime.of(2024, 6, 15, 14, 30, 45, 0, ZoneOffset.UTC);
        var result = roundTrip(Codec.TIMESTAMPTZ, "timestamptz", ts);
        assertEquals(ts.toInstant(), result.toInstant());
    }

    @Test
    void timestamptzNull() throws Exception {
        assertNull(roundTrip(Codec.TIMESTAMPTZ, "timestamptz", null));
    }

    // -----------------------------------------------------------------------
    // Interval
    // -----------------------------------------------------------------------

    @Test
    void intervalRoundTrip() throws Exception {
        // PostgreSQL normalizes intervals; "1 year 2 mons 3 days" is canonical
        String text = roundTripText(Codec.INTERVAL, "interval", "1 year 2 mons 3 days");
        assertNotNull(text);
        assertTrue(text.contains("1 year"));
    }

    @Test
    void intervalNull() throws Exception {
        assertNull(roundTripText(Codec.INTERVAL, "interval", null));
    }

    // -----------------------------------------------------------------------
    // UUID
    // -----------------------------------------------------------------------

    @Test
    void uuidRoundTrip() throws Exception {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(id, roundTrip(Codec.UUID, "uuid", id));
    }

    @Test
    void uuidRandom() throws Exception {
        UUID id = UUID.randomUUID();
        assertEquals(id, roundTrip(Codec.UUID, "uuid", id));
    }

    @Test
    void uuidNull() throws Exception {
        assertNull(roundTrip(Codec.UUID, "uuid", null));
    }

    // -----------------------------------------------------------------------
    // JSON
    // -----------------------------------------------------------------------

    @Test
    void jsonRoundTrip() throws Exception {
        String json = "{\"key\":\"value\",\"num\":42}";
        String result = roundTrip(Codec.JSON, "json", json);
        // JSON preserves exact format
        assertEquals(json, result);
    }

    @Test
    void jsonNull() throws Exception {
        assertNull(roundTrip(Codec.JSON, "json", null));
    }

    // -----------------------------------------------------------------------
    // JSONB
    // -----------------------------------------------------------------------

    @Test
    void jsonbRoundTrip() throws Exception {
        String json = "{\"key\": \"value\"}";
        String result = roundTrip(Codec.JSONB, "jsonb", json);
        // JSONB may reformat
        assertNotNull(result);
        assertTrue(result.contains("key"));
        assertTrue(result.contains("value"));
    }

    @Test
    void jsonbNull() throws Exception {
        assertNull(roundTrip(Codec.JSONB, "jsonb", null));
    }

    // -----------------------------------------------------------------------
    // OID
    // -----------------------------------------------------------------------

    @Test
    void oidRoundTrip() throws Exception {
        assertEquals(12345L, roundTrip(Codec.OID, "oid", 12345L));
    }

    @Test
    void oidNull() throws Exception {
        assertNull(roundTrip(Codec.OID, "oid", null));
    }

    // -----------------------------------------------------------------------
    // Money
    // -----------------------------------------------------------------------

    @Test
    void moneyRoundTrip() throws Exception {
        String text = roundTripText(Codec.MONEY, "money", "$100.50");
        assertNotNull(text);
        assertTrue(text.contains("100.50"));
    }

    @Test
    void moneyNull() throws Exception {
        assertNull(roundTripText(Codec.MONEY, "money", null));
    }

    // -----------------------------------------------------------------------
    // Inet
    // -----------------------------------------------------------------------

    @Test
    void inetIPv4() throws Exception {
        assertEquals("192.168.1.1", roundTrip(Codec.INET, "inet", "192.168.1.1"));
    }

    @Test
    void inetIPv6() throws Exception {
        String result = roundTrip(Codec.INET, "inet", "::1");
        assertNotNull(result);
        assertTrue(result.equals("::1") || result.contains("::1"));
    }

    @Test
    void inetCIDR() throws Exception {
        assertEquals("192.168.1.0/24", roundTrip(Codec.INET, "inet", "192.168.1.0/24"));
    }

    @Test
    void inetNull() throws Exception {
        assertNull(roundTrip(Codec.INET, "inet", null));
    }

    // -----------------------------------------------------------------------
    // CIDR
    // -----------------------------------------------------------------------

    @Test
    void cidrRoundTrip() throws Exception {
        assertEquals("192.168.1.0/24", roundTrip(Codec.CIDR, "cidr", "192.168.1.0/24"));
    }

    @Test
    void cidrNull() throws Exception {
        assertNull(roundTrip(Codec.CIDR, "cidr", null));
    }

    // -----------------------------------------------------------------------
    // Macaddr
    // -----------------------------------------------------------------------

    @Test
    void macaddrRoundTrip() throws Exception {
        assertEquals("08:00:2b:01:02:03", roundTrip(Codec.MACADDR, "macaddr", "08:00:2b:01:02:03"));
    }

    @Test
    void macaddrNull() throws Exception {
        assertNull(roundTrip(Codec.MACADDR, "macaddr", null));
    }

    // -----------------------------------------------------------------------
    // Macaddr8
    // -----------------------------------------------------------------------

    @Test
    void macaddr8RoundTrip() throws Exception {
        assertEquals("08:00:2b:01:02:03:04:05",
                roundTrip(Codec.MACADDR8, "macaddr8", "08:00:2b:01:02:03:04:05"));
    }

    @Test
    void macaddr8Null() throws Exception {
        assertNull(roundTrip(Codec.MACADDR8, "macaddr8", null));
    }

    // -----------------------------------------------------------------------
    // Geometric types
    // -----------------------------------------------------------------------

    @Test
    void pointRoundTrip() throws Exception {
        var pt = new org.postgresql.geometric.PGpoint(1.5, 2.5);
        var result = roundTrip(Codec.POINT, "point", pt);
        assertEquals(pt.x, result.x, 0.0001);
        assertEquals(pt.y, result.y, 0.0001);
    }

    @Test
    void pointNull() throws Exception {
        assertNull(roundTrip(Codec.POINT, "point", null));
    }

    @Test
    void boxRoundTrip() throws Exception {
        var box = new org.postgresql.geometric.PGbox(3.0, 4.0, 1.0, 2.0);
        var result = roundTrip(Codec.BOX, "box", box);
        assertNotNull(result);
    }

    @Test
    void circleRoundTrip() throws Exception {
        var circle = new org.postgresql.geometric.PGcircle(1.0, 2.0, 3.0);
        var result = roundTrip(Codec.CIRCLE, "circle", circle);
        assertNotNull(result);
        assertEquals(circle.center.x, result.center.x, 0.0001);
        assertEquals(circle.center.y, result.center.y, 0.0001);
        assertEquals(circle.radius, result.radius, 0.0001);
    }

    @Test
    void lsegRoundTrip() throws Exception {
        var lseg = new org.postgresql.geometric.PGlseg(1.0, 2.0, 3.0, 4.0);
        var result = roundTrip(Codec.LSEG, "lseg", lseg);
        assertNotNull(result);
    }

    // -----------------------------------------------------------------------
    // Bit types
    // -----------------------------------------------------------------------

    @Test
    void bitRoundTrip() throws Exception {
        assertEquals("10110", roundTrip(Codec.BIT, "bit(5)", "10110"));
    }

    @Test
    void bitNull() throws Exception {
        assertNull(roundTrip(Codec.BIT, "bit(5)", null));
    }

    @Test
    void varbitRoundTrip() throws Exception {
        assertEquals("1011010", roundTrip(Codec.VARBIT, "varbit", "1011010"));
    }

    @Test
    void varbitNull() throws Exception {
        assertNull(roundTrip(Codec.VARBIT, "varbit", null));
    }

    // -----------------------------------------------------------------------
    // Tsvector
    // -----------------------------------------------------------------------

    @Test
    void tsvectorRoundTrip() throws Exception {
        // Note: PostgreSQL normalizes tsvectors
        String text = roundTripText(Codec.TSVECTOR, "tsvector", "'hello' 'world'");
        assertNotNull(text);
        assertTrue(text.contains("hello") && text.contains("world"));
    }

    @Test
    void tsvectorNull() throws Exception {
        assertNull(roundTripText(Codec.TSVECTOR, "tsvector", null));
    }

    // -----------------------------------------------------------------------
    // Array of Int4
    // -----------------------------------------------------------------------

    @Test
    void int4ArrayRoundTrip() throws Exception {
        var codec = new ArrayCodec<>("_int4", Codec.INT4);
        var input = List.of(1, 2, 3);
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::int4[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input, result.value);
            }
        }
    }

    @Test
    void int4ArrayEmpty() throws Exception {
        var codec = new ArrayCodec<>("_int4", Codec.INT4);
        List<Integer> input = List.of();
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::int4[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input, result.value);
            }
        }
    }

    @Test
    void int4ArrayWithNulls() throws Exception {
        var codec = new ArrayCodec<>("_int4", Codec.INT4);
        var input = new java.util.ArrayList<Integer>();
        input.add(1);
        input.add(null);
        input.add(3);
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::int4[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(3, result.value.size());
                assertEquals(1, result.value.get(0));
                assertNull(result.value.get(1));
                assertEquals(3, result.value.get(2));
            }
        }
    }

    @Test
    void int4ArrayNull() throws Exception {
        var codec = new ArrayCodec<>("_int4", Codec.INT4);
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::int4[]")) {
            codec.bind(ps, 1, null);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertNull(rs.getString(1));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Array of Text
    // -----------------------------------------------------------------------

    @Test
    void textArrayRoundTrip() throws Exception {
        var codec = new ArrayCodec<>("_text", Codec.TEXT);
        var input = List.of("hello", "world", "foo bar");
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::text[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input, result.value);
            }
        }
    }

    @Test
    void textArrayWithSpecialChars() throws Exception {
        var codec = new ArrayCodec<>("_text", Codec.TEXT);
        var input = List.of("a,b", "c\"d", "e\\f", "");
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::text[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input, result.value);
            }
        }
    }

    @Test
    void textArrayWithNulls() throws Exception {
        var codec = new ArrayCodec<>("_text", Codec.TEXT);
        var input = new java.util.ArrayList<String>();
        input.add("hello");
        input.add(null);
        input.add("world");
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::text[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(3, result.value.size());
                assertEquals("hello", result.value.get(0));
                assertNull(result.value.get(1));
                assertEquals("world", result.value.get(2));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Array of Bool
    // -----------------------------------------------------------------------

    @Test
    void boolArrayRoundTrip() throws Exception {
        var codec = new ArrayCodec<>("_bool", Codec.BOOL);
        var input = List.of(true, false, true);
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::bool[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input, result.value);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Array of Float8
    // -----------------------------------------------------------------------

    @Test
    void float8ArrayRoundTrip() throws Exception {
        var codec = new ArrayCodec<>("_float8", Codec.FLOAT8);
        var input = List.of(1.1, 2.2, 3.3);
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::float8[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(3, result.value.size());
                assertEquals(1.1, result.value.get(0), 0.0001);
                assertEquals(2.2, result.value.get(1), 0.0001);
                assertEquals(3.3, result.value.get(2), 0.0001);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Array of UUID
    // -----------------------------------------------------------------------

    @Test
    void uuidArrayRoundTrip() throws Exception {
        var codec = new ArrayCodec<>("_uuid", Codec.UUID);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        var input = List.of(id1, id2);
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::uuid[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input, result.value);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Array of Date
    // -----------------------------------------------------------------------

    @Test
    void dateArrayRoundTrip() throws Exception {
        var codec = new ArrayCodec<>("_date", Codec.DATE);
        var input = List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::date[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input, result.value);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Array of Timestamp
    // -----------------------------------------------------------------------

    @Test
    void timestampArrayRoundTrip() throws Exception {
        var codec = new ArrayCodec<>("_timestamp", Codec.TIMESTAMP);
        var ts1 = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
        var ts2 = LocalDateTime.of(2024, 12, 25, 23, 59, 59);
        var input = List.of(ts1, ts2);
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::timestamp[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input, result.value);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Composite type (via table)
    // -----------------------------------------------------------------------

    @Test
    void compositeRoundTrip() throws Exception {
        try (var conn = connect();
             var stmt = conn.createStatement()) {
            stmt.execute("""
                    DO $$ BEGIN
                        CREATE TYPE test_comp AS (a int4, b text, c bool);
                    EXCEPTION WHEN duplicate_object THEN null;
                    END $$;
                    """);
        }
        record TestComp(Integer a, String b, Boolean c) {}
        var codec = new CompositeCodec<TestComp>(
                "public", "test_comp",
                (Integer a) -> (String b) -> (Boolean c) -> new TestComp(a, b, c),
                new CompositeCodec.Field<>("a", TestComp::a, Codec.INT4),
                new CompositeCodec.Field<>("b", TestComp::b, Codec.TEXT),
                new CompositeCodec.Field<>("c", TestComp::c, Codec.BOOL));

        var input = new TestComp(42, "hello world", true);
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::test_comp")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input.a(), result.value.a());
                assertEquals(input.b(), result.value.b());
                assertEquals(input.c(), result.value.c());
            }
        }
    }

    @Test
    void compositeWithNullFields() throws Exception {
        try (var conn = connect();
             var stmt = conn.createStatement()) {
            stmt.execute("""
                    DO $$ BEGIN
                        CREATE TYPE test_comp2 AS (a int4, b text);
                    EXCEPTION WHEN duplicate_object THEN null;
                    END $$;
                    """);
        }
        record TestComp2(Integer a, String b) {}
        var codec = new CompositeCodec<TestComp2>(
                "public", "test_comp2",
                (Integer a) -> (String b) -> new TestComp2(a, b),
                new CompositeCodec.Field<>("a", TestComp2::a, Codec.INT4),
                new CompositeCodec.Field<>("b", TestComp2::b, Codec.TEXT));

        var input = new TestComp2(null, "hello");
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::test_comp2")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertNull(result.value.a());
                assertEquals("hello", result.value.b());
            }
        }
    }

    @Test
    void compositeWithSpecialChars() throws Exception {
        try (var conn = connect();
             var stmt = conn.createStatement()) {
            stmt.execute("""
                    DO $$ BEGIN
                        CREATE TYPE test_comp3 AS (a text, b text);
                    EXCEPTION WHEN duplicate_object THEN null;
                    END $$;
                    """);
        }
        record TestComp3(String a, String b) {}
        var codec = new CompositeCodec<TestComp3>(
                "public", "test_comp3",
                (String a) -> (String b) -> new TestComp3(a, b),
                new CompositeCodec.Field<>("a", TestComp3::a, Codec.TEXT),
                new CompositeCodec.Field<>("b", TestComp3::b, Codec.TEXT));

        var input = new TestComp3("hello, world", "she said \"hi\"");
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::test_comp3")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input.a(), result.value.a());
                assertEquals(input.b(), result.value.b());
            }
        }
    }

    @Test
    void compositeNull() throws Exception {
        try (var conn = connect();
             var stmt = conn.createStatement()) {
            stmt.execute("""
                    DO $$ BEGIN
                        CREATE TYPE test_comp4 AS (a int4, b text);
                    EXCEPTION WHEN duplicate_object THEN null;
                    END $$;
                    """);
        }
        record TestComp4(Integer a, String b) {}
        var codec = new CompositeCodec<TestComp4>(
                "public", "test_comp4",
                (Integer a) -> (String b) -> new TestComp4(a, b),
                new CompositeCodec.Field<>("a", TestComp4::a, Codec.INT4),
                new CompositeCodec.Field<>("b", TestComp4::b, Codec.TEXT));

        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::test_comp4")) {
            codec.bind(ps, 1, null);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertNull(rs.getString(1));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Composite with row() syntax (writeAsRow)
    // -----------------------------------------------------------------------

    @Test
    void compositeWriteAsRow() throws Exception {
        try (var conn = connect();
             var stmt = conn.createStatement()) {
            stmt.execute("""
                    DO $$ BEGIN
                        CREATE TYPE test_row_comp AS (x int4, y text);
                    EXCEPTION WHEN duplicate_object THEN null;
                    END $$;
                    """);
        }
        record RowComp(Integer x, String y) {}
        var codec = new CompositeCodec<RowComp>(
                "public", "test_row_comp",
                (Integer x) -> (String y) -> new RowComp(x, y),
                new CompositeCodec.Field<>("x", RowComp::x, Codec.INT4),
                new CompositeCodec.Field<>("y", RowComp::y, Codec.TEXT));

        var input = new RowComp(42, "hello");
        var sb = new StringBuilder();
        codec.writeAsRow(sb, input);
        String rowExpr = sb.toString();
        assertTrue(rowExpr.startsWith("row("));
        assertTrue(rowExpr.endsWith(")"));

        // Verify it works as SQL by executing it
        try (var conn = connect();
             var stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT (" + rowExpr + "::test_row_comp).*")) {
                assertTrue(rs.next());
                assertEquals(42, rs.getInt(1));
                assertEquals("hello", rs.getString(2));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Array of composites
    // -----------------------------------------------------------------------

    @Test
    void arrayOfCompositesRoundTrip() throws Exception {
        try (var conn = connect();
             var stmt = conn.createStatement()) {
            stmt.execute("""
                    DO $$ BEGIN
                        CREATE TYPE test_arr_comp AS (id int4, name text);
                    EXCEPTION WHEN duplicate_object THEN null;
                    END $$;
                    """);
        }
        record ArrComp(Integer id, String name) {}
        var elemCodec = new CompositeCodec<ArrComp>(
                "public", "test_arr_comp",
                (Integer id) -> (String name) -> new ArrComp(id, name),
                new CompositeCodec.Field<>("id", ArrComp::id, Codec.INT4),
                new CompositeCodec.Field<>("name", ArrComp::name, Codec.TEXT));
        var arrayCodec = new ArrayCodec<>("_test_arr_comp", elemCodec);

        var input = List.of(new ArrComp(1, "Alice"), new ArrComp(2, "Bob"));
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::test_arr_comp[]")) {
            arrayCodec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = arrayCodec.parse(text, 0);
                assertEquals(2, result.value.size());
                assertEquals(1, result.value.get(0).id());
                assertEquals("Alice", result.value.get(0).name());
                assertEquals(2, result.value.get(1).id());
                assertEquals("Bob", result.value.get(1).name());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Array of Inet (PGobject-based in arrays)
    // -----------------------------------------------------------------------

    @Test
    void inetArrayRoundTrip() throws Exception {
        var codec = new ArrayCodec<>("_inet", Codec.INET);
        var input = List.of("192.168.1.1", "10.0.0.1");
        try (var conn = connect();
             var ps = conn.prepareStatement("SELECT ?::inet[]")) {
            codec.bind(ps, 1, input);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String text = rs.getString(1);
                assertNotNull(text);
                var result = codec.parse(text, 0);
                assertEquals(input, result.value);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Write then parse unit tests (no database)
    // -----------------------------------------------------------------------

    @Test
    void arrayWriteParseRoundTripNoDB() throws Exception {
        var codec = new ArrayCodec<>("_int4", Codec.INT4);
        var input = List.of(10, 20, 30);
        var sb = new StringBuilder();
        codec.write(sb, input);
        var result = codec.parse(sb.toString(), 0);
        assertEquals(input, result.value);
    }

    @Test
    void arrayWriteParseWithNullsNoDB() throws Exception {
        var codec = new ArrayCodec<>("_text", Codec.TEXT);
        var input = new java.util.ArrayList<String>();
        input.add("a");
        input.add(null);
        input.add("c");
        var sb = new StringBuilder();
        codec.write(sb, input);
        var result = codec.parse(sb.toString(), 0);
        assertEquals(input, result.value);
    }

    @Test
    void compositeWriteParseRoundTripNoDB() throws Exception {
        record Pair(Integer x, String y) {}
        var codec = new CompositeCodec<Pair>(
                "public", "pair",
                (Integer x) -> (String y) -> new Pair(x, y),
                new CompositeCodec.Field<>("x", Pair::x, Codec.INT4),
                new CompositeCodec.Field<>("y", Pair::y, Codec.TEXT));
        var input = new Pair(42, "test value");
        var sb = new StringBuilder();
        codec.write(sb, input);
        var result = codec.parse(sb.toString(), 0);
        assertEquals(input.x(), result.value.x());
        assertEquals(input.y(), result.value.y());
    }

    @Test
    void compositeWriteParseWithNullFieldsNoDB() throws Exception {
        record Pair(Integer x, String y) {}
        var codec = new CompositeCodec<Pair>(
                "public", "pair",
                (Integer x) -> (String y) -> new Pair(x, y),
                new CompositeCodec.Field<>("x", Pair::x, Codec.INT4),
                new CompositeCodec.Field<>("y", Pair::y, Codec.TEXT));
        var input = new Pair(null, null);
        var sb = new StringBuilder();
        codec.write(sb, input);
        var result = codec.parse(sb.toString(), 0);
        assertNull(result.value.x());
        assertNull(result.value.y());
    }

}
