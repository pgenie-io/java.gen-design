package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

final class UuidCodec implements Codec<UUID> {

    static final UuidCodec instance = new UuidCodec();

    private UuidCodec() {
    }

    public String name() {
        return "uuid";
    }

    @Override
    public void bind(PreparedStatement ps, int index, UUID value) throws SQLException {
        if (value != null) {
            ps.setObject(index, value);
        } else {
            ps.setNull(index, Types.OTHER);
        }
    }

    public void write(StringBuilder sb, UUID value) {
        sb.append(value.toString());
    }

    @Override
    public Codec.ParsingResult<UUID> parse(CharSequence input, int offset) throws Codec.ParseException {
        int end = offset + 36;
        if (end > input.length()) {
            throw new Codec.ParseException(input, offset, "Expected UUID (36 characters)");
        }
        String token = input.subSequence(offset, end).toString();
        try {
            java.util.UUID value = java.util.UUID.fromString(token);
            return new Codec.ParsingResult<>(value, end);
        } catch (IllegalArgumentException e) {
            throw new Codec.ParseException(input, offset, "Invalid UUID: " + token);
        }
    }

}
