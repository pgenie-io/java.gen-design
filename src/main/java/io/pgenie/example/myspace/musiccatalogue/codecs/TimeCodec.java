package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalTime;

final class TimeCodec implements Codec<LocalTime> {

    static final TimeCodec instance = new TimeCodec();

    private TimeCodec() {
    }

    public String name() {
        return "time";
    }

    @Override
    public void bind(PreparedStatement ps, int index, LocalTime value) throws SQLException {
        if (value != null) {
            ps.setTime(index, Time.valueOf(value));
        } else {
            ps.setNull(index, Types.TIME);
        }
    }

    public void write(StringBuilder sb, LocalTime value) {
        sb.append(value.toString());
    }

    @Override
    public Codec.ParsingResult<LocalTime> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected time, reached end of input");
        }
        // Minimum: "HH:mm:ss" = 8 chars
        int i = offset + 8;
        if (i > len) {
            throw new Codec.ParseException(input, offset, "Expected time (HH:mm:ss)");
        }
        // Optional fractional seconds
        if (i < len && input.charAt(i) == '.') {
            i++;
            while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                i++;
            }
        }
        String token = input.subSequence(offset, i).toString();
        try {
            LocalTime value = LocalTime.parse(token);
            return new Codec.ParsingResult<>(value, i);
        } catch (java.time.format.DateTimeParseException e) {
            throw new Codec.ParseException(input, offset, "Invalid time: " + e.getMessage());
        }
    }

}
