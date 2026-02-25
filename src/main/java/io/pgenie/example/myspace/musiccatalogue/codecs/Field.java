package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.util.function.Function;

public final class Field<Z, A> {

    public final String name;
    public final Function<Z, A> accessor;
    public final Scalar<A> codec;

    public Field(String name, Function<Z, A> accessor, Scalar<A> codec) {
        this.name = name;
        this.accessor = accessor;
        this.codec = codec;
    }

}
