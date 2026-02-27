package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

final class ByteaCodec implements Codec<byte[]> {

    static final ByteaCodec instance = new ByteaCodec();

    private ByteaCodec() {
    }

    public String name() {
        return "bytea";
    }

    @Override
    public void bind(PreparedStatement ps, int index, byte[] value) throws SQLException {
        if (value != null) {
            ps.setBytes(index, value);
        } else {
            ps.setNull(index, Types.BINARY);
        }
    }

    public void write(StringBuilder sb, byte[] value) {
        sb.append("\\x");
        for (byte b : value) {
            sb.append(String.format("%02x", b & 0xff));
        }
    }

    @Override
    public Codec.ParsingResult<byte[]> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len) {
            throw new Codec.ParseException(input, offset, "Expected bytea, reached end of input");
        }
        int i = offset;
        // Accept both \\x and \x prefix
        if (i < len && input.charAt(i) == '\\') {
            i++;
            if (i < len && input.charAt(i) == '\\') {
                i++;
            }
            if (i >= len || input.charAt(i) != 'x') {
                throw new Codec.ParseException(input, offset, "Expected hex format (\\x...)");
            }
            i++;
        } else {
            throw new Codec.ParseException(input, offset, "Expected hex format (\\x...)");
        }
        // Count hex digits
        int hexStart = i;
        while (i < len && isHexDigit(input.charAt(i))) {
            i++;
        }
        int hexLen = i - hexStart;
        if (hexLen % 2 != 0) {
            throw new Codec.ParseException(input, offset, "Odd number of hex digits");
        }
        byte[] result = new byte[hexLen / 2];
        for (int j = 0; j < result.length; j++) {
            int hi = Character.digit(input.charAt(hexStart + j * 2), 16);
            int lo = Character.digit(input.charAt(hexStart + j * 2 + 1), 16);
            result[j] = (byte) ((hi << 4) | lo);
        }
        return new Codec.ParsingResult<>(result, i);
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

}
