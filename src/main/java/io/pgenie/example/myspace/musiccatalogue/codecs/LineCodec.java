package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.geometric.PGline;

final class LineCodec implements Codec<PGline> {

    static final LineCodec instance = new LineCodec();

    private LineCodec() {
    }

    public String name() {
        return "line";
    }

    @Override
    public void bind(PreparedStatement ps, int index, PGline value) throws SQLException {
        if (value != null) {
            ps.setObject(index, value);
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }

    public void write(StringBuilder sb, PGline value) {
        sb.append(value.getValue());
    }

    @Override
    public Codec.ParsingResult<PGline> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected line, reached end of input");
        }
        int end = offset;
        if (input.charAt(end) == '{') {
            end++;
            while (end < len && input.charAt(end) != '}') {
                end++;
            }
            if (end < len) end++; // consume '}'
        } else {
            // Consume until end or delimiter
            while (end < len) end++;
        }
        String token = input.subSequence(offset, end).toString();
        try {
            PGline value = new PGline(token);
            return new Codec.ParsingResult<>(value, end);
        } catch (java.sql.SQLException e) {
            throw new Codec.ParseException(input, offset, "Invalid line: " + e.getMessage());
        }
    }

}
