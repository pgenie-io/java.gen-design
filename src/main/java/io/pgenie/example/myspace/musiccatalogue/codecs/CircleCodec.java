package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.geometric.PGcircle;

final class CircleCodec implements Codec<PGcircle> {

    static final CircleCodec instance = new CircleCodec();

    private CircleCodec() {
    }

    public String name() {
        return "circle";
    }

    @Override
    public void bind(PreparedStatement ps, int index, PGcircle value) throws SQLException {
        if (value != null) {
            ps.setObject(index, value);
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }

    public void write(StringBuilder sb, PGcircle value) {
        sb.append(value.getValue());
    }

    @Override
    public Codec.ParsingResult<PGcircle> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected circle, reached end of input");
        }
        // Circle format: <(x,y),r>
        int end = offset;
        if (input.charAt(end) == '<') {
            end++;
            while (end < len && input.charAt(end) != '>') {
                end++;
            }
            if (end < len) end++; // consume '>'
        } else {
            while (end < len) end++;
        }
        String token = input.subSequence(offset, end).toString();
        try {
            PGcircle value = new PGcircle(token);
            return new Codec.ParsingResult<>(value, end);
        } catch (java.sql.SQLException e) {
            throw new Codec.ParseException(input, offset, "Invalid circle: " + e.getMessage());
        }
    }

}
