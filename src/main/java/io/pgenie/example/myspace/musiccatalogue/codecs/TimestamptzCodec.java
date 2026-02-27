package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

final class TimestamptzCodec implements Codec<OffsetDateTime> {

    static final TimestamptzCodec instance = new TimestamptzCodec();

    private TimestamptzCodec() {
    }

    public String name() {
        return "timestamptz";
    }

    @Override
    public void bind(PreparedStatement ps, int index, OffsetDateTime value) throws SQLException {
        if (value != null) {
            ps.setObject(index, value, Types.TIMESTAMP_WITH_TIMEZONE);
        } else {
            ps.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
        }
    }

    public void write(StringBuilder sb, OffsetDateTime value) {
        sb.append(value.toString().replace('T', ' '));
    }

    @Override
    public Codec.ParsingResult<OffsetDateTime> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected timestamptz, reached end of input");
        }
        // Minimum: "yyyy-MM-dd HH:mm:ss+00" = 22 chars
        int i = offset + 19;
        if (i > len) {
            throw new Codec.ParseException(input, offset, "Expected timestamptz");
        }
        // Optional fractional seconds
        if (i < len && input.charAt(i) == '.') {
            i++;
            while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                i++;
            }
        }
        // Timezone offset: +HH, -HH, +HH:mm, -HH:mm, +HH:mm:ss, Z
        if (i < len) {
            char c = input.charAt(i);
            if (c == '+' || c == '-') {
                i++;
                // HH
                while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                    i++;
                }
                // :mm
                if (i < len && input.charAt(i) == ':') {
                    i++;
                    while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                        i++;
                    }
                    // :ss
                    if (i < len && input.charAt(i) == ':') {
                        i++;
                        while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                            i++;
                        }
                    }
                }
            } else if (c == 'Z') {
                i++;
            }
        }
        String token = input.subSequence(offset, i).toString().replace(' ', 'T');
        try {
            OffsetDateTime value = OffsetDateTime.parse(token, PARSER);
            return new Codec.ParsingResult<>(value, i);
        } catch (java.time.format.DateTimeParseException e) {
            throw new Codec.ParseException(input, offset, "Invalid timestamptz: " + e.getMessage());
        }
    }

    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart().appendOffset("+HH:MM:ss", "Z").optionalEnd()
            .optionalStart().appendOffset("+HH:MM", "Z").optionalEnd()
            .optionalStart().appendOffset("+HH", "Z").optionalEnd()
            .toFormatter();

}
