package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.geometric.PGbox;

final class BoxCodec implements Codec<PGbox> {

    static final BoxCodec instance = new BoxCodec();

    private BoxCodec() {
    }

    public String name() {
        return "box";
    }

    @Override
    public void bind(PreparedStatement ps, int index, PGbox value) throws SQLException {
        if (value != null) {
            ps.setObject(index, value);
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }

    public void write(StringBuilder sb, PGbox value) {
        sb.append(value.getValue());
    }

    @Override
    public Codec.ParsingResult<PGbox> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected box, reached end of input");
        }
        // Box format: (x1,y1),(x2,y2) — consume all remaining
        String token = input.subSequence(offset, len).toString();
        try {
            PGbox value = new PGbox(token);
            return new Codec.ParsingResult<>(value, len);
        } catch (java.sql.SQLException e) {
            throw new Codec.ParseException(input, offset, "Invalid box: " + e.getMessage());
        }
    }

}
