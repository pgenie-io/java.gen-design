package io.pgenie.example.myspace.musiccatalogue.codecs;

final class TextScalar implements Scalar<String> {

  static final TextScalar instance = new TextScalar();

  private TextScalar() {}

  public String name() {
    return "text";
  }

  public void write(StringBuilder sb, String value) {
    sb.append(value);
  }

  public String parse(CharSequence text) {
    return text.toString();
  }

}
