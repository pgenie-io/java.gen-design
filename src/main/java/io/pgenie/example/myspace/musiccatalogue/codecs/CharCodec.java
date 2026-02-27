package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class CharCodec implements Codec<String> {

    static final CharCodec instance = new CharCodec();

    private CharCodec() {
    }

    public String name() {
        return "char";
    }

    @Override
    public void bind(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null) {
            ps.setString(index, value);
        } else {
            ps.setNull(index, Types.CHAR);
        }
    }

    public void write(StringBuilder sb, String value) {
        sb.append(value);
    }

    @Override
    public Codec.ParsingResult<String> parse(CharSequence input, int offset) throws Codec.ParseException {
        return new Codec.ParsingResult<>(input.subSequence(offset, input.length()).toString(), input.length());
    }

}
