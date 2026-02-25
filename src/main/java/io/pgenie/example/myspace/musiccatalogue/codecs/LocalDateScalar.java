package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

final class LocalDateScalar implements Scalar<LocalDate> {

  static final LocalDateScalar instance = new LocalDateScalar();

  private LocalDateScalar() {}

  public String name() {
    return "date";
  }

  @Override
  public void bind(PreparedStatement ps, int index, LocalDate value) throws SQLException {
    if (value != null) {
      ps.setDate(index, Date.valueOf(value));
    } else {
      ps.setNull(index, Types.DATE);
    }
  }

  public void write(StringBuilder sb, LocalDate value) {
    sb.append(value);
  }

  public LocalDate parse(CharSequence text) {
    return LocalDate.parse(text);
  }

}
