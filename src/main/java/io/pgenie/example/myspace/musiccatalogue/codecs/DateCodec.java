package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

final class DateCodec implements Codec<LocalDate> {

    static final DateCodec instance = new DateCodec();

    private DateCodec() {
    }

    public String name() {
        return "date";
    }

    @Override
    public void bind(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value != null) {
            ps.setDate(index, Date.valueOf(value));
        } else {
            ps.setNull(index, Types.DATE);
        }
    }

    public void write(StringBuilder sb, LocalDate value) {
        sb.append(value);
    }

    @Override
    public Codec.ParsingResult<LocalDate> parse(char[] input, int offset) throws Codec.ParseException {
        // ISO local date format: YYYY-MM-DD (10 characters)
        int end = offset + 10;
        if (end > input.length) {
            throw new Codec.ParseException(input, offset, "Expected ISO date (YYYY-MM-DD)");
        }
        try {
            LocalDate value = LocalDate.parse(new String(input, offset, 10));
            return new Codec.ParsingResult<>(value, end);
        } catch (java.time.format.DateTimeParseException e) {
            throw new Codec.ParseException(input, offset, e.getMessage());
        }
    }

}
