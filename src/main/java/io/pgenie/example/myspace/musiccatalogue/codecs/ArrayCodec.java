package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PGobject;

/**
 * Codec for PostgreSQL array types.
 *
 * <p>Renders arrays in the textual format {@code {elem1,elem2,...}} with proper
 * quoting and escaping. Parses the same format back, handling quoted elements,
 * NULL, and nested arrays/composites.
 *
 * @param <A> the element type
 */
public final class ArrayCodec<A> implements Codec<List<A>> {

    private final String pgTypeName;
    private final Codec<A> elementCodec;

    /**
     * Creates an array codec.
     *
     * @param pgTypeName   the PostgreSQL array type name (e.g. "_int4", "_text")
     * @param elementCodec the codec for individual elements
     */
    public ArrayCodec(String pgTypeName, Codec<A> elementCodec) {
        this.pgTypeName = pgTypeName;
        this.elementCodec = elementCodec;
    }

    @Override
    public String name() {
        return pgTypeName;
    }

    @Override
    public void bind(PreparedStatement ps, int index, List<A> value) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType(pgTypeName);
        if (value != null) {
            var sb = new StringBuilder();
            write(sb, value);
            obj.setValue(sb.toString());
        }
        ps.setObject(index, obj);
    }

    @Override
    public void write(StringBuilder sb, List<A> value) {
        sb.append('{');
        for (int i = 0; i < value.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            A elem = value.get(i);
            if (elem == null) {
                sb.append("NULL");
            } else {
                var elemSb = new StringBuilder();
                elementCodec.write(elemSb, elem);
                writeQuotedElement(sb, elemSb);
            }
        }
        sb.append('}');
    }

    @Override
    public Codec.ParsingResult<List<A>> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len || input.charAt(offset) != '{') {
            throw new Codec.ParseException(input, offset, "Expected '{' to open array");
        }
        int i = offset + 1; // skip '{'
        List<A> result = new ArrayList<>();

        // Handle empty array
        if (i < len && input.charAt(i) == '}') {
            return new Codec.ParsingResult<>(result, i + 1);
        }

        while (i < len) {
            // Check for NULL
            if (i + 4 <= len
                    && input.charAt(i) == 'N'
                    && input.charAt(i + 1) == 'U'
                    && input.charAt(i + 2) == 'L'
                    && input.charAt(i + 3) == 'L'
                    && (i + 4 >= len || input.charAt(i + 4) == ',' || input.charAt(i + 4) == '}')) {
                result.add(null);
                i += 4;
            } else if (i < len && input.charAt(i) == '"') {
                // Quoted element — unescape and parse
                i++; // skip opening '"'
                var sb = new StringBuilder();
                while (i < len) {
                    char c = input.charAt(i);
                    if (c == '"') {
                        i++; // skip closing '"'
                        break;
                    } else if (c == '\\') {
                        if (i + 1 < len) {
                            sb.append(input.charAt(i + 1));
                            i += 2;
                        } else {
                            sb.append(c);
                            i++;
                        }
                    } else {
                        sb.append(c);
                        i++;
                    }
                }
                var parsed = elementCodec.parse(sb, 0);
                result.add(parsed.value);
            } else if (i < len && input.charAt(i) == '{') {
                // Nested array — find matching closing brace
                int depth = 0;
                int start = i;
                boolean inQuote = false;
                while (i < len) {
                    char c = input.charAt(i);
                    if (inQuote) {
                        if (c == '\\' && i + 1 < len) {
                            i += 2;
                        } else if (c == '"') {
                            inQuote = false;
                            i++;
                        } else {
                            i++;
                        }
                    } else {
                        if (c == '"') {
                            inQuote = true;
                            i++;
                        } else if (c == '{') {
                            depth++;
                            i++;
                        } else if (c == '}') {
                            depth--;
                            i++;
                            if (depth == 0) break;
                        } else {
                            i++;
                        }
                    }
                }
                var parsed = elementCodec.parse(input.subSequence(start, i), 0);
                result.add(parsed.value);
            } else {
                // Unquoted element
                int elemStart = i;
                while (i < len && input.charAt(i) != ',' && input.charAt(i) != '}') {
                    i++;
                }
                var parsed = elementCodec.parse(input.subSequence(elemStart, i), 0);
                result.add(parsed.value);
            }

            // Expect ',' or '}'
            if (i < len && input.charAt(i) == ',') {
                i++; // skip ','
            } else if (i < len && input.charAt(i) == '}') {
                // will be handled below
            }

            // Check for end
            if (i < len && input.charAt(i) == '}') {
                return new Codec.ParsingResult<>(result, i + 1);
            }
        }
        throw new Codec.ParseException(input, offset, "Expected '}' to close array");
    }

    // -----------------------------------------------------------------------
    // PostgreSQL array text format helpers
    // -----------------------------------------------------------------------

    /**
     * Writes an element value with quoting if necessary.
     * Similar to composite quoting but with array-specific rules:
     * - NULL is handled by the caller
     * - Empty strings must be quoted as ""
     * - Strings containing special chars ({},"\) or whitespace must be quoted
     */
    private static void writeQuotedElement(StringBuilder sb, StringBuilder elemText) {
        int len = elemText.length();
        if (len == 0) {
            sb.append("\"\"");
            return;
        }
        // Check if it looks like NULL (must be quoted to distinguish from SQL NULL)
        boolean looksLikeNull = len == 4
                && (elemText.charAt(0) == 'N' || elemText.charAt(0) == 'n')
                && (elemText.charAt(1) == 'U' || elemText.charAt(1) == 'u')
                && (elemText.charAt(2) == 'L' || elemText.charAt(2) == 'l')
                && (elemText.charAt(3) == 'L' || elemText.charAt(3) == 'l');
        boolean needsQuoting = looksLikeNull;
        if (!needsQuoting) {
            for (int i = 0; i < len; i++) {
                char c = elemText.charAt(i);
                if (c == ',' || c == '{' || c == '}' || c == '"' || c == '\\'
                        || c == '(' || c == ')'
                        || c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    needsQuoting = true;
                    break;
                }
            }
        }
        if (!needsQuoting) {
            sb.append(elemText);
            return;
        }
        sb.append('"');
        for (int i = 0; i < len; i++) {
            char c = elemText.charAt(i);
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }

}
