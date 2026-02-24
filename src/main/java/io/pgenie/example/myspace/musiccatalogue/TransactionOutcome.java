package io.pgenie.example.myspace.musiccatalogue;

/**
 * The outcome of a {@link Transaction#run} call: a result value paired with a
 * flag indicating whether the transaction should be committed or rolled back.
 *
 * <p>
 * Use the factory methods {@link #commit(Object)} and
 * {@link #rollback(Object)} to construct instances.
 *
 * @param <R> the result type
 */
public final class TransactionOutcome<R> {

    private final R result;
    private final boolean commit;

    private TransactionOutcome(R result, boolean commit) {
        this.result = result;
        this.commit = commit;
    }

    /** Commit the transaction and return {@code result}. */
    public static <R> TransactionOutcome<R> commit(R result) {
        return new TransactionOutcome<>(result, true);
    }

    /** Roll back the transaction and return {@code result}. */
    public static <R> TransactionOutcome<R> rollback(R result) {
        return new TransactionOutcome<>(result, false);
    }

    /** The result value produced by the transaction body. */
    public R result() {
        return result;
    }

    /**
     * {@code true} if the transaction should be committed, {@code false} to roll
     * back.
     */
    public boolean shouldCommit() {
        return commit;
    }

}
