package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.geometric.PGpath;

final class PathCodec implements Codec<PGpath> {

    static final PathCodec instance = new PathCodec();

    private PathCodec() {
    }

    public String name() {
        return "path";
    }

    @Override
    public void bind(PreparedStatement ps, int index, PGpath value) throws SQLException {
        if (value != null) {
            ps.setObject(index, value);
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }

    public void write(StringBuilder sb, PGpath value) {
        sb.append(value.getValue());
    }

    @Override
    public Codec.ParsingResult<PGpath> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected path, reached end of input");
        }
        // Path is enclosed in [] (open) or () (closed) — consume to matching bracket
        int end = offset;
        char open = input.charAt(end);
        if (open == '[' || open == '(') {
            char close = (open == '[') ? ']' : ')';
            int depth = 1;
            end++;
            while (end < len && depth > 0) {
                char c = input.charAt(end);
                if (c == open) depth++;
                else if (c == close) depth--;
                end++;
            }
        } else {
            while (end < len) end++;
        }
        String token = input.subSequence(offset, end).toString();
        try {
            PGpath value = new PGpath(token);
            return new Codec.ParsingResult<>(value, end);
        } catch (java.sql.SQLException e) {
            throw new Codec.ParseException(input, offset, "Invalid path: " + e.getMessage());
        }
    }

}
