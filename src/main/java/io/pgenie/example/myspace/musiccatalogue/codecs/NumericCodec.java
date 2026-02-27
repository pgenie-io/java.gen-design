package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class NumericCodec implements Codec<BigDecimal> {

    static final NumericCodec instance = new NumericCodec();

    private NumericCodec() {
    }

    public String name() {
        return "numeric";
    }

    @Override
    public void bind(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value != null) {
            ps.setBigDecimal(index, value);
        } else {
            ps.setNull(index, Types.NUMERIC);
        }
    }

    public void write(StringBuilder sb, BigDecimal value) {
        sb.append(value.toPlainString());
    }

    @Override
    public Codec.ParsingResult<BigDecimal> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected numeric, reached end of input");
        }
        // Check for NaN
        if (offset + 3 <= len && input.subSequence(offset, offset + 3).toString().equals("NaN")) {
            throw new Codec.ParseException(input, offset, "NaN is not supported for numeric");
        }
        int i = offset;
        while (i < len && isNumericChar(input.charAt(i))) {
            i++;
        }
        if (i == offset) {
            throw new Codec.ParseException(input, offset, "Expected numeric value");
        }
        String token = input.subSequence(offset, i).toString();
        try {
            BigDecimal value = new BigDecimal(token);
            return new Codec.ParsingResult<>(value, i);
        } catch (NumberFormatException e) {
            throw new Codec.ParseException(input, offset, "Invalid numeric: " + token);
        }
    }

    private static boolean isNumericChar(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == '+' || c == '-' || c == 'e' || c == 'E';
    }

}
