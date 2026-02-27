package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class Float4Codec implements Codec<Float> {

    static final Float4Codec instance = new Float4Codec();

    private Float4Codec() {
    }

    public String name() {
        return "float4";
    }

    @Override
    public void bind(PreparedStatement ps, int index, Float value) throws SQLException {
        if (value != null) {
            ps.setFloat(index, value);
        } else {
            ps.setNull(index, Types.REAL);
        }
    }

    public void write(StringBuilder sb, Float value) {
        sb.append(value);
    }

    @Override
    public Codec.ParsingResult<Float> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected float4, reached end of input");
        }
        int i = offset;
        while (i < len && isFloatChar(input.charAt(i))) {
            i++;
        }
        if (i == offset) {
            throw new Codec.ParseException(input, offset, "Expected float4 value");
        }
        String token = input.subSequence(offset, i).toString();
        try {
            float value = Float.parseFloat(token);
            return new Codec.ParsingResult<>(value, i);
        } catch (NumberFormatException e) {
            throw new Codec.ParseException(input, offset, "Invalid float4: " + token);
        }
    }

    private static boolean isFloatChar(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E'
                || c == '+' || c == '-'
                || c == 'N' || c == 'a' || c == 'I' || c == 'n'
                || c == 'f' || c == 'i' || c == 't' || c == 'y';
    }

}
