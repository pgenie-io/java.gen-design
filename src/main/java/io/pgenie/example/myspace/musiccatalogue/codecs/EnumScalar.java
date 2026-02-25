package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.postgresql.util.PGobject;

public final class EnumScalar<E> implements Scalar<E> {

    private final String schema;
    private final String pgName;
    private final Map<E, String> pgLabels;
    private final Map<String, E> byPgLabel;

    public EnumScalar(String schema, String name, Map<E, String> pgLabels) {
        this.schema = schema;
        this.pgName = name;
        this.pgLabels = pgLabels;
        this.byPgLabel = new HashMap<>(pgLabels.size() * 2);
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
    public E parse(CharSequence text) {
        if (text == null) {
            return null;
        }
        String label = text.toString();
        E value = byPgLabel.get(label);
        if (value == null) {
            throw new IllegalArgumentException("Unknown " + pgName + " value: " + label);
        }
        return value;
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
