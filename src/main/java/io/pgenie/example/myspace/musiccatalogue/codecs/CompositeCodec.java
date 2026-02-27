package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;

import org.postgresql.util.PGobject;

public final class CompositeCodec<Z> implements Codec<Z> {

    private final String schema;
    private final String pgName;
    private final Object constructor;
    private final Field<Z, ?>[] fields;

    @SuppressWarnings("unchecked")
    public <A, B> CompositeCodec(
            String schema, String name,
            Function<A, Function<B, Z>> construct,
            Field<Z, A> fieldA,
            Field<Z, B> fieldB) {
        this.schema = schema;
        this.pgName = name;
        this.constructor = construct;
        this.fields = new Field[]{fieldA, fieldB};
    }

    @SuppressWarnings("unchecked")
    public <A, B, C> CompositeCodec(
            String schema, String name,
            Function<A, Function<B, Function<C, Z>>> construct,
            Field<Z, A> fieldA,
            Field<Z, B> fieldB,
            Field<Z, C> fieldC) {
        this.schema = schema;
        this.pgName = name;
        this.constructor = construct;
        this.fields = new Field[]{fieldA, fieldB, fieldC};
    }

    @SuppressWarnings("unchecked")
    public <A, B, C, D> CompositeCodec(
            String schema, String name,
            Function<A, Function<B, Function<C, Function<D, Z>>>> construct,
            Field<Z, A> fieldA,
            Field<Z, B> fieldB,
            Field<Z, C> fieldC,
            Field<Z, D> fieldD) {
        this.schema = schema;
        this.pgName = name;
        this.constructor = construct;
        this.fields = new Field[]{fieldA, fieldB, fieldC, fieldD};
    }

    @SuppressWarnings("unchecked")
    public <A, B, C, D, E> CompositeCodec(
            String schema, String name,
            Function<A, Function<B, Function<C, Function<D, Function<E, Z>>>>> construct,
            Field<Z, A> fieldA,
            Field<Z, B> fieldB,
            Field<Z, C> fieldC,
            Field<Z, D> fieldD,
            Field<Z, E> fieldE) {
        this.schema = schema;
        this.pgName = name;
        this.constructor = construct;
        this.fields = new Field[]{fieldA, fieldB, fieldC, fieldD, fieldE};
    }

    @Override
    public String name() {
        return pgName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(StringBuilder sb, Z value) {
        sb.append('(');
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            var field = (Field<Z, Object>) fields[i];
            Object fieldValue = field.accessor.apply(value);
            if (fieldValue != null) {
                var fieldSb = new StringBuilder();
                field.codec.write(fieldSb, fieldValue);
                writeQuotedField(sb, fieldSb);
            }
        }
        sb.append(')');
    }

    @Override
    @SuppressWarnings("unchecked")
    public Codec.ParsingResult<Z> parse(CharSequence input, int offset) throws Codec.ParseException {
        int len = input.length();
        if (offset >= len || input.charAt(offset) != '(') {
            throw new Codec.ParseException(input, offset, "Expected '(' to open composite " + pgName);
        }
        int i = offset + 1; // skip '('
        Object fn = constructor;
        for (int fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
            if (fieldIdx > 0) {
                if (i >= len || input.charAt(i) != ',') {
                    throw new Codec.ParseException(input, i, "Expected ',' between fields in composite " + pgName);
                }
                i++; // skip ','
            }
            if (i >= len || input.charAt(i) == ',' || input.charAt(i) == ')') {
                // NULL field
                fn = ((Function<Object, Object>) fn).apply(null);
            } else if (input.charAt(i) == '"') {
                // Quoted field — unescape into a StringBuilder and parse it directly
                i++; // skip opening '"'
                var sb = new StringBuilder();
                while (i < len) {
                    char c = input.charAt(i);
                    if (c == '"') {
                        if (i + 1 < len && input.charAt(i + 1) == '"') {
                            sb.append('"');
                            i += 2;
                        } else {
                            i++; // skip closing '"'
                            break;
                        }
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
                var result = ((Codec<Object>) fields[fieldIdx].codec).parse(sb, 0);
                fn = ((Function<Object, Object>) fn).apply(result.value);
            } else {
                // Unquoted field — pass a subSequence bounded to this field
                int fieldStart = i;
                while (i < len && input.charAt(i) != ',' && input.charAt(i) != ')') {
                    i++;
                }
                var result = ((Codec<Object>) fields[fieldIdx].codec).parse(input.subSequence(fieldStart, i), 0);
                fn = ((Function<Object, Object>) fn).apply(result.value);
            }
        }
        if (i >= len || input.charAt(i) != ')') {
            throw new Codec.ParseException(input, i, "Expected ')' to close composite " + pgName);
        }
        return new Codec.ParsingResult<>((Z) fn, i + 1);
    }

    /**
     * Writes the value in {@code row(...)} syntax, which handles nested
     * composites better than the quoted-literal form.
     */
    @SuppressWarnings("unchecked")
    public void writeAsRow(StringBuilder sb, Z value) {
        sb.append("row(");
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            var field = (Field<Z, Object>) fields[i];
            Object fieldValue = field.accessor.apply(value);
            if (fieldValue == null) {
                sb.append("null");
            } else if (field.codec instanceof CompositeCodec<?> compositeCodec) {
                @SuppressWarnings("rawtypes")
                var cc = (CompositeCodec) compositeCodec;
                cc.writeAsRow(sb, fieldValue);
            } else {
                var fieldSb = new StringBuilder();
                field.codec.write(fieldSb, fieldValue);
                writeRowLiteral(sb, fieldSb);
            }
        }
        sb.append(')');
    }

    @Override
    public void bind(PreparedStatement ps, int index, Z value) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType(this.name());
        if (value != null) {
            var sb = new StringBuilder();
            this.write(sb, value);
            obj.setValue(sb.toString());
        }
        ps.setObject(index, obj);
    }

    // ---------------------------------------------------------------------------
    // PostgreSQL composite text format helpers
    // ---------------------------------------------------------------------------
    private static void writeQuotedField(StringBuilder sb, StringBuilder fieldText) {
        int len = fieldText.length();
        if (len == 0) {
            sb.append("\"\"");
            return;
        }
        boolean needsQuoting = false;
        for (int i = 0; i < len; i++) {
            char c = fieldText.charAt(i);
            if (c == ',' || c == '(' || c == ')' || c == '"' || c == '\\'
                    || c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                needsQuoting = true;
                break;
            }
        }
        if (!needsQuoting) {
            sb.append(fieldText);
            return;
        }
        sb.append('"');
        for (int i = 0; i < len; i++) {
            char c = fieldText.charAt(i);
            if (c == '"') {
                sb.append("\"\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }

    /**
     * Writes a scalar literal in the row(...) syntax context. Single-quotes
     * the value and escapes embedded single quotes by doubling them.
     */
    private static void writeRowLiteral(StringBuilder sb, StringBuilder fieldText) {
        sb.append('\'');
        int len = fieldText.length();
        for (int i = 0; i < len; i++) {
            char c = fieldText.charAt(i);
            if (c == '\'') {
                sb.append("''");
            } else {
                sb.append(c);
            }
        }
        sb.append('\'');
    }

    public static final class Field<Z, A> {

        public final String name;
        public final Function<Z, A> accessor;
        public final Codec<A> codec;

        public Field(String name, Function<Z, A> accessor, Codec<A> codec) {
            this.name = name;
            this.accessor = accessor;
            this.codec = codec;
        }

    }

}
