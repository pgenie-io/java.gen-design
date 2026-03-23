package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.postgresql.util.PGobject;

public final class EnumCodec<E> implements Codec<E> {

    private final String schema;
    private final String pgName;
    private final Map<E, String> pgLabels;
    private final Map<String, E> byPgLabel;

    public EnumCodec(String schema, String name, Map<E, String> pgLabels) {
        this.schema = schema;
        this.pgName = name;
        this.pgLabels = pgLabels;
        this.byPgLabel = new HashMap<>(pgLabels.size());
        pgLabels.forEach((constant, label) -> byPgLabel.put(label, constant));
    }

    @Override
    public String name() {
        return pgName;
    }

    @Override
    public void write(StringBuilder sb, E value) {
        sb.append(pgLabels.get(value));
    }

    @Override
    public Codec.ParsingResult<E> parse(CharSequence input, int offset) throws Codec.ParseException {
        String label = input.subSequence(offset, input.length()).toString();
        E value = byPgLabel.get(label);
        if (value == null) {
            throw new Codec.ParseException(input, offset, "Unknown " + pgName + " value: " + label);
        }
        return new Codec.ParsingResult<>(value, input.length());
    }

    @Override
    public void bind(PreparedStatement ps, int index, E value) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType(pgName);
        if (value != null) {
            obj.setValue(pgLabels.get(value));
        }
        ps.setObject(index, obj);
    }

}
