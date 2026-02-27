package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class Int4Codec implements Codec<Integer> {

    static final Int4Codec instance = new Int4Codec();

    private Int4Codec() {
    }

    public String name() {
        return "int4";
    }

    @Override
    public void bind(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    public void write(StringBuilder sb, Integer value) {
        sb.append(value);
    }

    @Override
    public Codec.ParsingResult<Integer> parse(CharSequence input, int offset) throws Codec.ParseException {
        int i = offset;
        int len = input.length();
        if (i >= len) {
            throw new Codec.ParseException(input, offset, "Expected int4, reached end of input");
        }
        boolean negative = input.charAt(i) == '-';
        if (negative || input.charAt(i) == '+') {
            i++;
        }
        if (i >= len || input.charAt(i) < '0' || input.charAt(i) > '9') {
            throw new Codec.ParseException(input, offset, "Expected int4 digits");
        }
        int value = 0;
        while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
            value = value * 10 + (input.charAt(i++) - '0');
        }
        return new Codec.ParsingResult<>(negative ? -value : value, i);
    }

}
