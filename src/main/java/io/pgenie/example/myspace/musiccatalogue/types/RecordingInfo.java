package io.pgenie.example.myspace.musiccatalogue.types;

import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of the {@code recording_info} user-declared PostgreSQL
 * composite (record) type.
 *
 * <p>
 * Generated from SQL queries using the
 * <a href="https://pgenie.io">pGenie</a> code generator.
 *
 * <p>
 * All fields are nullable, matching the PostgreSQL column definitions.
 *
 * @param studioName   Maps to {@code studio_name}.
 * @param city         Maps to {@code city}.
 * @param country      Maps to {@code country}.
 * @param recordedDate Maps to {@code recorded_date}.
 */
public record RecordingInfo(
        String studioName,
        String city,
        String country,
        LocalDate recordedDate) {

    /**
     * Encode this record as a PostgreSQL composite literal string, e.g.
     * {@code ("Abbey Road",London,UK,1972-06-01)}.
     *
     * <p>
     * Fields that are {@code null} are represented as empty tokens.
     * Fields that contain commas, parentheses, double-quotes, backslashes, or
     * white-space are double-quoted with internal double-quotes escaped as
     * {@code ""} and backslashes escaped as {@code \\}.
     */
    public String toCompositeString() {
        return "(" +
                quoteField(studioName) + "," +
                quoteField(city) + "," +
                quoteField(country) + "," +
                quoteField(recordedDate != null ? recordedDate.toString() : null) +
                ")";
    }

    /**
     * Encode a nullable {@code RecordingInfo} as a {@link PGobject} suitable
     * for use as a JDBC parameter. A {@code null} input produces a
     * {@code PGobject} whose value is {@code null} (SQL NULL).
     */
    public static PGobject toPgObject(RecordingInfo value) throws SQLException {
        PGobject obj = new PGobject();
        obj.setType("recording_info");
        obj.setValue(value != null ? value.toCompositeString() : null);
        return obj;
    }

    /**
     * Parse a PostgreSQL composite literal string into a {@code RecordingInfo}.
     *
     * <p>
     * The expected format is {@code (field1,field2,field3,field4)} where
     * fields follow the PostgreSQL composite text-output rules:
     * <ul>
     * <li>NULL fields are empty tokens (no quotes).</li>
     * <li>Fields with special characters are double-quoted.</li>
     * <li>A literal double-quote inside a quoted field is written as
     * {@code ""}.</li>
     * <li>A literal backslash is written as {@code \\}.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if the string is not a valid composite
     *                                  literal or does not contain exactly four
     *                                  fields.
     */
    public static RecordingInfo parse(String text) {
        if (text == null) {
            return null;
        }
        List<String> fields = parseCompositeFields(text);
        if (fields.size() != 4) {
            throw new IllegalArgumentException(
                    "Expected 4 fields in recording_info composite, got " +
                            fields.size() + ": " + text);
        }
        String studioName = fields.get(0);
        String city = fields.get(1);
        String country = fields.get(2);
        String recordedDateStr = fields.get(3);
        LocalDate recordedDate = recordedDateStr != null
                ? LocalDate.parse(recordedDateStr)
                : null;
        return new RecordingInfo(studioName, city, country, recordedDate);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String quoteField(String value) {
        if (value == null) {
            return "";
        }
        // Empty string must be quoted to be distinguishable from NULL.
        if (value.isEmpty()) {
            return "\"\"";
        }
        boolean needsQuoting = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ',' || c == '(' || c == ')' || c == '"' ||
                    c == '\\' || c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                needsQuoting = true;
                break;
            }
        }
        if (!needsQuoting) {
            return value;
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                sb.append("\"\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Parse the fields out of a PostgreSQL composite text literal.
     *
     * @param text the full literal, e.g. {@code (field1,"field,2",,field4)}
     * @return a list of field values; {@code null} entries represent SQL NULL.
     */
    private static List<String> parseCompositeFields(String text) {
        if (text.length() < 2 || text.charAt(0) != '(' ||
                text.charAt(text.length() - 1) != ')') {
            throw new IllegalArgumentException(
                    "Composite literal must be wrapped in parentheses: " + text);
        }
        List<String> fields = new ArrayList<>();
        int i = 1;
        int end = text.length() - 1;

        // Handle the empty composite "()" – zero fields.
        if (i == end) {
            return fields;
        }

        while (true) {
            if (i >= end) {
                // Trailing comma → trailing NULL field.
                fields.add(null);
                break;
            }
            char c = text.charAt(i);
            if (c == '"') {
                // Quoted field.
                StringBuilder sb = new StringBuilder();
                i++; // skip opening quote
                while (i < end) {
                    char q = text.charAt(i);
                    if (q == '"') {
                        if (i + 1 < end && text.charAt(i + 1) == '"') {
                            // Escaped double-quote.
                            sb.append('"');
                            i += 2;
                        } else {
                            // End of quoted field.
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
                fields.add(sb.toString());
                // Skip separator comma or end.
                if (i < end && text.charAt(i) == ',') {
                    i++;
                } else {
                    break;
                }
            } else if (c == ',') {
                // Empty / NULL unquoted field.
                fields.add(null);
                i++; // skip comma
            } else {
                // Unquoted non-empty field.
                StringBuilder sb = new StringBuilder();
                while (i < end && text.charAt(i) != ',') {
                    sb.append(text.charAt(i++));
                }
                String val = sb.toString();
                fields.add(val.isEmpty() ? null : val);
                if (i < end && text.charAt(i) == ',') {
                    i++;
                } else {
                    break;
                }
            }
        }
        return fields;
    }
}
