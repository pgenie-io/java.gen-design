package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.util.function.Function;

public final class CompositeField<Z, A> {

    public final String name;
    public final Function<Z, A> accessor;
    public final Scalar<A> codec;

    public CompositeField(String name, Function<Z, A> accessor, Scalar<A> codec) {
        this.name = name;
        this.accessor = accessor;
        this.codec = codec;
    }

}
