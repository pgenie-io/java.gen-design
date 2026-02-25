package io.pgenie.example.myspace.musiccatalogue.codecs;

final class Int8Scalar implements Scalar<Long> {

  public static final Int8Scalar instance = new Int8Scalar();

  public String name() {
    return "int8";
  }

  public void write(StringBuilder sb, Long value) {
    sb.append(value);
  }

  public Long parse(CharSequence text) {
    return Long.parseLong(text, 0, text.length(), 10);
  }

}
