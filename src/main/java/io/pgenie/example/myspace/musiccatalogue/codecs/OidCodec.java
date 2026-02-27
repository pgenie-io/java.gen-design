package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class OidCodec implements Codec<Long> {

    static final OidCodec instance = new OidCodec();

    private OidCodec() {
    }

    public String name() {
        return "oid";
    }

    @Override
    public void bind(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(index, value);
        } else {
            ps.setNull(index, Types.BIGINT);
        }
    }

    public void write(StringBuilder sb, Long value) {
        sb.append(value);
    }

    @Override
    public Codec.ParsingResult<Long> parse(CharSequence input, int offset) throws Codec.ParseException {
        int i = offset;
        int len = input.length();
        if (i >= len) {
            throw new Codec.ParseException(input, offset, "Expected oid, reached end of input");
        }
        if (input.charAt(i) < '0' || input.charAt(i) > '9') {
            throw new Codec.ParseException(input, offset, "Expected oid digits");
        }
        long value = 0;
        while (i < len && input.charAt(i) >= '0' && input.charAt(i) <= '9') {
            value = value * 10 + (input.charAt(i++) - '0');
        }
        return new Codec.ParsingResult<>(value, i);
    }

}
