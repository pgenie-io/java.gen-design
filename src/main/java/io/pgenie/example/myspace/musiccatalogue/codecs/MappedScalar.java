package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.util.function.Function;

public final class MappedScalar<A, B> implements Scalar<B> {

  private final Scalar<A> codec;
  private final Function<A, B> toMapped;
  private final Function<B, A> fromMapped;

  public MappedScalar(Scalar<A> codec, Function<A, B> toMapped, Function<B, A> fromMapped) {
    this.codec = codec;
    this.toMapped = toMapped;
    this.fromMapped = fromMapped;
  }

  public String name() {
    return codec.name();
  }

  public void write(StringBuilder sb, B value) {
    codec.write(sb, fromMapped.apply(value));
  }

  public B parse(CharSequence text) {
    return toMapped.apply(codec.parse(text));
  }

}
