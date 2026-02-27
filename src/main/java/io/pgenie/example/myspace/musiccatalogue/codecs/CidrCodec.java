package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.util.PGobject;

final class CidrCodec implements Codec<String> {

    static final CidrCodec instance = new CidrCodec();

    private CidrCodec() {
    }

    public String name() {
        return "cidr";
    }

    @Override
    public void bind(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null) {
            PGobject obj = new PGobject();
            obj.setType("cidr");
            obj.setValue(value);
            ps.setObject(index, obj);
        } else {
            ps.setNull(index, Types.OTHER);
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
