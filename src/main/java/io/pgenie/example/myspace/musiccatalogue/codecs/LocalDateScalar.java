package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.time.LocalDate;

final class LocalDateScalar implements Scalar<LocalDate> {

  static final LocalDateScalar instance = new LocalDateScalar();

  private LocalDateScalar() {}

  public String name() {
    return "date";
  }

  public void write(StringBuilder sb, LocalDate value) {
    sb.append(value);
  }

  public LocalDate parse(CharSequence text) {
    return LocalDate.parse(text);
  }

}
