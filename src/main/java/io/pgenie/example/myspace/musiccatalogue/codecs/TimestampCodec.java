package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

final class TimestampCodec implements Codec<LocalDateTime> {

    static final TimestampCodec instance = new TimestampCodec();

    private TimestampCodec() {
    }

    public String name() {
        return "timestamp";
    }

    @Override
    public void bind(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value != null) {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        } else {
            ps.setNull(index, Types.TIMESTAMP);
        }
    }

    public void write(StringBuilder sb, LocalDateTime value) {
        sb.append(value.toString().replace('T', ' '));
    }

    @Override
    public Codec.ParsingResult<LocalDateTime> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected timestamp, reached end of input");
        }
        // Minimum: "yyyy-MM-dd HH:mm:ss" = 19 chars
        int i = offset + 19;
        if (i > len) {
            throw new Codec.ParseException(input, offset, "Expected timestamp (yyyy-MM-dd HH:mm:ss)");
        }
        // Optional fractional seconds
        if (i < len && input.charAt(i) == '.') {
            i++;
            while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
                i++;
            }
        }
        String token = input.subSequence(offset, i).toString().replace(' ', 'T');
        try {
            LocalDateTime value = LocalDateTime.parse(token);
            return new Codec.ParsingResult<>(value, i);
        } catch (java.time.format.DateTimeParseException e) {
            throw new Codec.ParseException(input, offset, "Invalid timestamp: " + e.getMessage());
        }
    }

}
