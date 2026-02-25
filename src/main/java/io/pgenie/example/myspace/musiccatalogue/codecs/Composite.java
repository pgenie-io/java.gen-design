package io.pgenie.example.myspace.musiccatalogue.codecs;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.postgresql.util.PGobject;

import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;

import java.sql.PreparedStatement;

public final class Composite<Z> implements Scalar<Z> {

  private final String schema;
  private final String pgName;
  private final Object constructor;
  private final CompositeField<Z, ?>[] fields;

  @SuppressWarnings("unchecked")
  public <A, B> Composite(
      String schema, String name,
      Function<A, Function<B, Z>> construct,
      CompositeField<Z, A> fieldA,
      CompositeField<Z, B> fieldB) {
    this.schema = schema;
    this.pgName = name;
    this.constructor = construct;
    this.fields = new CompositeField[] { fieldA, fieldB };
  }

  @SuppressWarnings("unchecked")
  public <A, B, C> Composite(
      String schema, String name,
      Function<A, Function<B, Function<C, Z>>> construct,
      CompositeField<Z, A> fieldA,
      CompositeField<Z, B> fieldB,
      CompositeField<Z, C> fieldC) {
    this.schema = schema;
    this.pgName = name;
    this.constructor = construct;
    this.fields = new CompositeField[] { fieldA, fieldB, fieldC };
  }

  @SuppressWarnings("unchecked")
  public <A, B, C, D> Composite(
      String schema, String name,
      Function<A, Function<B, Function<C, Function<D, Z>>>> construct,
      CompositeField<Z, A> fieldA,
      CompositeField<Z, B> fieldB,
      CompositeField<Z, C> fieldC,
      CompositeField<Z, D> fieldD) {
    this.schema = schema;
    this.pgName = name;
    this.constructor = construct;
    this.fields = new CompositeField[] { fieldA, fieldB, fieldC, fieldD };
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
      if (i > 0)
        sb.append(',');
      var field = (CompositeField<Z, Object>) fields[i];
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
  public Z parse(CharSequence text) {
    if (text == null) {
      return null;
    }
    var rawFields = parseRawFields(text);
    if (rawFields.size() != fields.length) {
      throw new IllegalArgumentException(
          "Expected " + fields.length + " fields in " + pgName + " composite, got "
              + rawFields.size() + ": " + text);
    }
    Object fn = constructor;
    for (int i = 0; i < fields.length; i++) {
      CharSequence raw = rawFields.get(i);
      Object fieldValue = raw != null ? fields[i].codec.parse(raw) : null;
      fn = ((Function<Object, Object>) fn).apply(fieldValue);
    }
    return (Z) fn;
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
      if (c == '"')
        sb.append("\"\"");
      else if (c == '\\')
        sb.append("\\\\");
      else
        sb.append(c);
    }
    sb.append('"');
  }

  private static List<CharSequence> parseRawFields(CharSequence text) {
    int len = text.length();
    if (len < 2 || text.charAt(0) != '(' || text.charAt(len - 1) != ')') {
      throw new IllegalArgumentException(
          "Composite literal must be wrapped in parentheses: " + text);
    }
    var fields = new ArrayList<CharSequence>();
    int i = 1;
    int end = len - 1;
    if (i == end) {
      return fields;
    }
    while (true) {
      if (i >= end) {
        fields.add(null); // trailing comma → NULL field
        break;
      }
      char c = text.charAt(i);
      if (c == '"') {
        var sb = new StringBuilder();
        i++; // skip opening quote
        while (i < end) {
          char q = text.charAt(i);
          if (q == '"') {
            if (i + 1 < end && text.charAt(i + 1) == '"') {
              sb.append('"');
              i += 2;
            } else {
              i++;
              break;
            }
          } else if (q == '\\') {
            if (i + 1 < end) {
              sb.append(text.charAt(i + 1));
              i += 2;
            } else {
              sb.append(q);
              i++;
            }
          } else {
            sb.append(q);
            i++;
          }
        }
        fields.add(sb);
        if (i < end && text.charAt(i) == ',')
          i++;
        else
          break;
      } else if (c == ',') {
        fields.add(null); // unquoted empty token → NULL
        i++;
      } else {
        var sb = new StringBuilder();
        while (i < end && text.charAt(i) != ',')
          sb.append(text.charAt(i++));
        fields.add(sb.length() == 0 ? null : sb);
        if (i < end && text.charAt(i) == ',')
          i++;
        else
          break;
      }
    }
    return fields;
  }

}
