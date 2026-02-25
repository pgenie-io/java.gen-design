package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;

final class TextScalar implements Scalar<String> {

  static final TextScalar instance = new TextScalar();

  private TextScalar() {}

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

  public String parse(CharSequence text) {
    return text.toString();
  }

}
