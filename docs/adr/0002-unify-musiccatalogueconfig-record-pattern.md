# Unify `MusicCatalogueConfig` onto the `TransactionSettings` record pattern

**Context:** `MusicCatalogueConfig` in `music-catalogue` was a hand-rolled builder class
with nine fields, no validation of numeric or duration bounds, and a `toString()` that
would emit the raw database password. The vendored `postgresql-jdbc.java` module already
had `TransactionSettings`, a Java record with a compact constructor validator, a
`defaults(...)` static factory, per-field `withX(...)` methods, and a redacted `toString()`.
The two modules cannot share code, but the `music-catalogue` config was solving the same
problem with a different, less-safe API shape.

**Decision:** Convert `MusicCatalogueConfig` from a builder class into a Java record with
the same structure as `TransactionSettings`:

- A compact canonical constructor that `Objects.requireNonNull(...)` every reference-typed
  component and throws `IllegalArgumentException` for out-of-bounds numeric or duration
  values (`maximumPoolSize` and `transactionRetryAttempts` must be at least 1; the three
  `Duration` fields must be non-negative, with `0` remaining a legal, meaningful value).
- A `defaults(jdbcUrl, user, password)` static factory that supplies the documented default
  values for all optional fields and the global `OpenTelemetry` instance.
- A `withX(...)` method for every one of the nine record components, so callers can override
  defaults without reconstructing the whole object.
- An overridden `toString()` that redacts `password` to `***` so credentials cannot leak into
  logs or exception messages.

`MusicCatalogueSession.defaultTransactionSettings(...)` previously clamped
`transactionRetryAttempts` to at least 1 with `Math.max(1, ...)`; that clamp was removed
because the record's constructor now rejects invalid values before the session ever sees them.

**Why:** A builder class was unnecessary boilerplate for an immutable bag of validated
values. Records give us immutability, `equals`/`hashCode`, and accessor methods for free,
while the compact constructor is the natural place to centralize validation. Copying the
existing `TransactionSettings` idiom keeps the two independent modules visually consistent
without introducing any shared dependency or base type between them.

**Consequences:** `MusicCatalogueConfig.Builder` and `MusicCatalogueConfig.builder()` no
longer exist; every call site in `music-catalogue` was migrated to `defaults(...)` plus
`withX(...)` chaining. The change closes three real behavior gaps: invalid numeric/duration
values are now hard construction errors, `toString()` no longer leaks the password, and
required fields remain impossible to omit even without a builder.
