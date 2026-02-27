package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;

public final class MappedCodec<A, B> implements Codec<B> {

    private final Codec<A> codec;
    private final Function<A, B> toMapped;
    private final Function<B, A> fromMapped;

    public MappedCodec(Codec<A> codec, Function<A, B> toMapped, Function<B, A> fromMapped) {
        this.codec = codec;
        this.toMapped = toMapped;
        this.fromMapped = fromMapped;
    }

    public String name() {
        return codec.name();
    }

    @Override
    public void bind(PreparedStatement ps, int index, B value) throws SQLException {
        codec.bind(ps, index, fromMapped.apply(value));
    }

    public void write(StringBuilder sb, B value) {
        codec.write(sb, fromMapped.apply(value));
    }

    public B parse(CharSequence text) {
        return toMapped.apply(codec.parse(text));
    }

}
