package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class Int8Codec implements Codec<Long> {

    public static final Int8Codec instance = new Int8Codec();

    public String name() {
        return "int8";
    }

    @Override
    public void bind(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.BIGINT);
        }
    }

    public void write(StringBuilder sb, Long value) {
        sb.append(value);
    }

    @Override
    public Codec.ParsingResult<Long> parse(char[] input, int offset) throws Codec.ParseException {
        int i = offset;
        int len = input.length;
        if (i >= len) {
            throw new Codec.ParseException(input, offset, "Expected int8, reached end of input");
        }
        boolean negative = input[i] == '-';
        if (negative || input[i] == '+') {
            i++;
        }
        if (i >= len || input[i] < '0' || input[i] > '9') {
            throw new Codec.ParseException(input, offset, "Expected int8 digits");
        }
        long value = 0;
        while (i < len && input[i] >= '0' && input[i] <= '9') {
            value = value * 10 + (input[i++] - '0');
        }
        return new Codec.ParsingResult<>(negative ? -value : value, i);
    }

}
