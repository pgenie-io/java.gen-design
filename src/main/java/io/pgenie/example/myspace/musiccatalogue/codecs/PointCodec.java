package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.geometric.PGpoint;

final class PointCodec implements Codec<PGpoint> {

    static final PointCodec instance = new PointCodec();

    private PointCodec() {
    }

    public String name() {
        return "point";
    }

    @Override
    public void bind(PreparedStatement ps, int index, PGpoint value) throws SQLException {
        if (value != null) {
            ps.setObject(index, value);
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }

    public void write(StringBuilder sb, PGpoint value) {
        sb.append('(').append(value.x).append(',').append(value.y).append(')');
    }

    @Override
    public Codec.ParsingResult<PGpoint> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected point, reached end of input");
        }
        // Find the closing parenthesis
        int end = offset;
        if (input.charAt(end) == '(') {
            int depth = 1;
            end++;
            while (end < len && depth > 0) {
                if (input.charAt(end) == '(') depth++;
                else if (input.charAt(end) == ')') depth--;
                end++;
            }
        } else {
            throw new Codec.ParseException(input, offset, "Expected '(' for point");
        }
        String token = input.subSequence(offset, end).toString();
        try {
            PGpoint value = new PGpoint(token);
            return new Codec.ParsingResult<>(value, end);
        } catch (java.sql.SQLException e) {
            throw new Codec.ParseException(input, offset, "Invalid point: " + e.getMessage());
        }
    }

}
