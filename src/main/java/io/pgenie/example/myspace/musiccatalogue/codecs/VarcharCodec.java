package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class VarcharCodec implements Codec<String> {

    static final VarcharCodec instance = new VarcharCodec();

    private VarcharCodec() {
    }

    public String name() {
        return "varchar";
    }

    @Override
    public void bind(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null) {
            ps.setString(index, value);
        } else {
            ps.setNull(index, Types.VARCHAR);
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
