package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import org.postgresql.util.PGobject;

final class TimetzCodec implements Codec<OffsetTime> {

    static final TimetzCodec instance = new TimetzCodec();

    private TimetzCodec() {
    }

    public String name() {
        return "timetz";
    }

    @Override
    public void bind(PreparedStatement ps, int index, OffsetTime value) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType("timetz");
        if (value != null) {
            obj.setValue(value.toString());
        }
        ps.setObject(index, obj);
    }

    public void write(StringBuilder sb, OffsetTime value) {
        sb.append(value.toString());
    }

    @Override
    public Codec.ParsingResult<OffsetTime> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected timetz, reached end of input");
        }
        // Minimum: "HH:mm:ss+HH" = 11 chars
        int i = offset + 8;
        if (i > len) {
            throw new Codec.ParseException(input, offset, "Expected timetz (HH:mm:ss±HH[:mm])");
        }
        // Optional fractional seconds
        if (i < len && input.charAt(i) == '.') {
            i++;
            while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                i++;
            }
        }
        // Timezone offset
        if (i < len) {
            char c = input.charAt(i);
            if (c == '+' || c == '-') {
                i++;
                while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                    i++;
                }
                if (i < len && input.charAt(i) == ':') {
                    i++;
                    while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                        i++;
                    }
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
        String token = input.subSequence(offset, i).toString();
        try {
            OffsetTime value = OffsetTime.parse(token, PARSER);
            return new Codec.ParsingResult<>(value, i);
        } catch (java.time.format.DateTimeParseException e) {
            throw new Codec.ParseException(input, offset, "Invalid timetz: " + e.getMessage());
        }
    }

    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .optionalStart().appendOffset("+HH:MM:ss", "Z").optionalEnd()
            .optionalStart().appendOffset("+HH:MM", "Z").optionalEnd()
            .optionalStart().appendOffset("+HH", "Z").optionalEnd()
            .toFormatter();

}
