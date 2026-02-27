package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.geometric.PGpolygon;

final class PolygonCodec implements Codec<PGpolygon> {

    static final PolygonCodec instance = new PolygonCodec();

    private PolygonCodec() {
    }

    public String name() {
        return "polygon";
    }

    @Override
    public void bind(PreparedStatement ps, int index, PGpolygon value) throws SQLException {
        if (value != null) {
            ps.setObject(index, value);
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }

    public void write(StringBuilder sb, PGpolygon value) {
        sb.append(value.getValue());
    }

    @Override
    public Codec.ParsingResult<PGpolygon> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected polygon, reached end of input");
        }
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
            while (end < len) end++;
        }
        String token = input.subSequence(offset, end).toString();
        try {
            PGpolygon value = new PGpolygon(token);
            return new Codec.ParsingResult<>(value, end);
        } catch (java.sql.SQLException e) {
            throw new Codec.ParseException(input, offset, "Invalid polygon: " + e.getMessage());
        }
    }

}
