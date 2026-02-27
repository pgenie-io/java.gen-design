package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class Float8Codec implements Codec<Double> {

    static final Float8Codec instance = new Float8Codec();

    private Float8Codec() {
    }

    public String name() {
        return "float8";
    }

    @Override
    public void bind(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value != null) {
            ps.setDouble(index, value);
        } else {
            ps.setNull(index, Types.DOUBLE);
        }
    }

    public void write(StringBuilder sb, Double value) {
        sb.append(value);
    }

    @Override
    public Codec.ParsingResult<Double> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected float8, reached end of input");
        }
        int i = offset;
        while (i < len && isFloatChar(input.charAt(i))) {
            i++;
        }
        if (i == offset) {
            throw new Codec.ParseException(input, offset, "Expected float8 value");
        }
        String token = input.subSequence(offset, i).toString();
        try {
            double value = Double.parseDouble(token);
            return new Codec.ParsingResult<>(value, i);
        } catch (NumberFormatException e) {
            throw new Codec.ParseException(input, offset, "Invalid float8: " + token);
        }
    }

    private static boolean isFloatChar(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E'
                || c == '+' || c == '-'
                || c == 'N' || c == 'a' || c == 'I' || c == 'n'
                || c == 'f' || c == 'i' || c == 't' || c == 'y';
    }

}
