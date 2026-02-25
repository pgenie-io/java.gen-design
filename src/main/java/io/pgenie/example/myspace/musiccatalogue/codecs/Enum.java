package io.pgenie.example.myspace.musiccatalogue.codecs;

import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class Enum<E> implements Scalar<E> {

  private final String schema;
  private final String pgName;
  private final Function<E, String> pgLabel;
  private final Map<String, E> byPgLabel;

  public Enum(String schema, String name, E[] constants, Function<E, String> pgLabel) {
    this.schema = schema;
    this.pgName = name;
    this.pgLabel = pgLabel;
    this.byPgLabel = new HashMap<>(constants.length * 2);
    for (E constant : constants) {
      byPgLabel.put(pgLabel.apply(constant), constant);
    }
  }

  @Override
  public String name() {
    return pgName;
  }

  @Override
  public void write(StringBuilder sb, E value) {
    sb.append(pgLabel.apply(value));
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

  public PGobject toPgObject(E value) throws SQLException {
    var obj = new PGobject();
    obj.setType(pgName);
    if (value != null) {
      obj.setValue(pgLabel.apply(value));
    }
    return obj;
  }

}
