package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;

final class TextCodec implements Codec<String> {

    static final TextCodec instance = new TextCodec();

    private TextCodec() {
    }

    public String name() {
        return "text";
    }

    @Override
    public void bind(PreparedStatement ps, int index, String value) throws SQLException {
        ps.setString(index, value);
    }

    public void write(StringBuilder sb, String value) {
        sb.append(value);
    }

    @Override
    public Codec.ParsingResult<String> parse(CharSequence input, int offset) throws Codec.ParseException {
        return new Codec.ParsingResult<>(input.subSequence(offset, input.length()).toString(), input.length());
    }

}
