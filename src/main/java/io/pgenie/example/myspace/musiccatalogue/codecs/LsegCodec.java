package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.geometric.PGlseg;

final class LsegCodec implements Codec<PGlseg> {

    static final LsegCodec instance = new LsegCodec();

    private LsegCodec() {
    }

    public String name() {
        return "lseg";
    }

    @Override
    public void bind(PreparedStatement ps, int index, PGlseg value) throws SQLException {
        if (value != null) {
            ps.setObject(index, value);
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }

    public void write(StringBuilder sb, PGlseg value) {
        sb.append(value.getValue());
    }

    @Override
    public Codec.ParsingResult<PGlseg> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected lseg, reached end of input");
        }
        int end = offset;
        if (input.charAt(end) == '[') {
            end++;
            while (end < len && input.charAt(end) != ']') {
                end++;
            }
            if (end < len) end++; // consume ']'
        } else {
            while (end < len) end++;
        }
        String token = input.subSequence(offset, end).toString();
        try {
            PGlseg value = new PGlseg(token);
            return new Codec.ParsingResult<>(value, end);
        } catch (java.sql.SQLException e) {
            throw new Codec.ParseException(input, offset, "Invalid lseg: " + e.getMessage());
        }
    }

}
