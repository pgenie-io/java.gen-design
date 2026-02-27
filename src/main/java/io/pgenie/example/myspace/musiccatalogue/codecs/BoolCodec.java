package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class BoolCodec implements Codec<Boolean> {

    static final BoolCodec instance = new BoolCodec();

    private BoolCodec() {
    }

    public String name() {
        return "bool";
    }

    @Override
    public void bind(PreparedStatement ps, int index, Boolean value) throws SQLException {
        if (value != null) {
            ps.setBoolean(index, value);
        } else {
            ps.setNull(index, Types.BOOLEAN);
        }
    }

    public void write(StringBuilder sb, Boolean value) {
        sb.append(value ? "t" : "f");
    }

    @Override
    public Codec.ParsingResult<Boolean> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected bool, reached end of input");
        }
        int remaining = len - offset;
        String sub = input.subSequence(offset, len).toString().toLowerCase();
        for (String t : new String[]{"true", "on", "yes"}) {
            if (sub.startsWith(t)) {
                return new Codec.ParsingResult<>(true, offset + t.length());
            }
        }
        for (String f : new String[]{"false", "off"}) {
            if (sub.startsWith(f)) {
                return new Codec.ParsingResult<>(false, offset + f.length());
            }
        }
        if (sub.startsWith("no")) {
            return new Codec.ParsingResult<>(false, offset + 2);
        }
        char c = sub.charAt(0);
        if (c == 't' || c == '1' || c == 'y') {
            return new Codec.ParsingResult<>(true, offset + 1);
        }
        if (c == 'f' || c == '0' || c == 'n') {
            return new Codec.ParsingResult<>(false, offset + 1);
        }
        throw new Codec.ParseException(input, offset, "Expected bool value");
    }

}
