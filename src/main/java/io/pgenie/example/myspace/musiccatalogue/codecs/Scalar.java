package io.pgenie.example.myspace.musiccatalogue.codecs;

public interface Scalar<A> {

  public final Scalar<Long> int8 = Int8Scalar.instance;
  public final Scalar<String> text = TextScalar.instance;
  public final Scalar<java.time.LocalDate> localDate = LocalDateScalar.instance;

  String name();

  void write(StringBuilder sb, A value);

  A parse(CharSequence text);

}
