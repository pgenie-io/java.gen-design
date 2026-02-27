package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.postgresql.util.PGobject;

final class Macaddr8Codec implements Codec<String> {

    static final Macaddr8Codec instance = new Macaddr8Codec();

    private Macaddr8Codec() {
    }

    public String name() {
        return "macaddr8";
    }

    @Override
    public void bind(PreparedStatement ps, int index, String value) throws SQLException {
        if (value != null) {
            PGobject obj = new PGobject();
            obj.setType("macaddr8");
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
