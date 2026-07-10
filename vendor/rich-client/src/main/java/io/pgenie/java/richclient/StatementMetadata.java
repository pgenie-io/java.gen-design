package io.pgenie.java.richclient;

/**
 * Static metadata for a generated {@link io.codemine.java.postgresql.jdbc.Statement}.
 *
 * <p>Implementations are expected to be generated alongside the statement class and to
 * return values derived from the SQL template at codegen time, e.g. {@code "INSERT"} for
 * {@link #operationName()} and {@code "albums"} for {@link #collectionName()}.</p>
 *
 * <p>Statement classes that do not implement this interface are still executed by
 * {@link StatementExecutor}; the executor simply omits the {@code db.operation.name} and
 * {@code db.collection.name} attributes for those statements.</p>
 */
public interface StatementMetadata {

    /**
     * Returns the database operation name, e.g. {@code "INSERT"}, {@code "UPDATE"},
     * {@code "SELECT"} or {@code "DELETE"}.
     *
     * @return the operation name
     */
    String operationName();

    /**
     * Returns the collection (table) name the operation targets, e.g. {@code "albums"}.
     *
     * @return the collection name
     */
    String collectionName();
}
