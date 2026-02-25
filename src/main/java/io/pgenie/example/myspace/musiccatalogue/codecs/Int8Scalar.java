package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class Int8Scalar implements Scalar<Long> {

  public static final Int8Scalar instance = new Int8Scalar();

  public String name() {
    return "int8";
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

  public Long parse(CharSequence text) {
    return Long.parseLong(text, 0, text.length(), 10);
  }

}
