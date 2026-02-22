//! Type-safe bindings for the `music_catalogue` database.
//!
//! Generated from SQL queries using the [pGenie](https://pgenie.io) code generator.
//!
//! - [`statements`] – ready-to-use statement definitions for all queries with
//!   associated parameter and result types.
//! - [`types`] – PostgreSQL enum and composite type mappings.

pub mod statements;
pub mod types;

#[cfg(test)]
mod tests;

/// Implemented by each query's parameter struct. Provides a uniform way to
/// prepare and execute statements against a [`tokio_postgres::Client`].
pub trait Statement {
    /// The type returned when the statement is successfully executed.
    type Result;

    const SQL: &str;

    const PARAM_TYPES: &'static [tokio_postgres::types::Type];

    /// Encode `self` as a list of type-erased parameter references.
    ///
    /// Implementations return a stack-allocated, fixed-size array (no heap
    /// allocation).
    fn encode_params(&self) -> impl AsRef<[&(dyn tokio_postgres::types::ToSql + Sync)]>;

    /// Whether the statement returns rows (i.e. is a `SELECT` or contains a
    /// `RETURNING` clause).  When `true` the execution functions use
    /// [`tokio_postgres::GenericClient::query`] and forward the rows together
    /// with `rows.len() as u64` as the affected-rows count.  When `false` they
    /// use [`tokio_postgres::GenericClient::execute`] instead, which discards
    /// any rows but returns the actual number of rows affected by the statement.
    const RETURNS_ROWS: bool;

    fn decode_result(
        rows: Vec<tokio_postgres::Row>,
        affected_rows: u64,
    ) -> Result<Self::Result, tokio_postgres::Error>;
}

pub struct Session {
    base: deadpool_postgres::Client,
    no_preparing: bool,
}

impl Session {
    pub async fn execute<P: Statement>(
        &self,
        params: &P,
    ) -> Result<P::Result, tokio_postgres::Error> {
        let params = params.encode_params();
        let (rows, affected_rows) = if P::RETURNS_ROWS {
            let rows = if self.no_preparing {
                self.base.query(P::SQL, params.as_ref()).await?
            } else {
                let prepared = self
                    .base
                    .prepare_typed_cached(P::SQL, P::PARAM_TYPES)
                    .await?;
                self.base.query(&prepared, params.as_ref()).await?
            };
            let affected = rows.len() as u64;
            (rows, affected)
        } else {
            let affected = if self.no_preparing {
                self.base.execute(P::SQL, params.as_ref()).await?
            } else {
                let prepared = self
                    .base
                    .prepare_typed_cached(P::SQL, P::PARAM_TYPES)
                    .await?;
                self.base.execute(&prepared, params.as_ref()).await?
            };
            (vec![], affected)
        };
        P::decode_result(rows, affected_rows)
    }
}

pub struct Pool {
    base: deadpool_postgres::Pool,
    no_preparing: bool,
}

impl Pool {
    pub fn new(
        deadpool_postgres: deadpool_postgres::Config,
        no_preparing: bool,
    ) -> Result<Self, deadpool_postgres::CreatePoolError> {
        let base = deadpool_postgres.create_pool(
            Some(deadpool_postgres::Runtime::Tokio1),
            tokio_postgres::NoTls,
        )?;

        Ok(Self { base, no_preparing })
    }
    pub async fn session(&self) -> Result<Session, deadpool_postgres::PoolError> {
        Ok(Session {
            base: self.base.get().await?,
            no_preparing: self.no_preparing,
        })
    }

    /// Execute a statement directly on the pool, acquiring and releasing a
    /// connection automatically.
    pub async fn execute<P: Statement>(&self, params: &P) -> Result<P::Result, Error> {
        let session = self.session().await.map_err(Error::Deadpool)?;
        session.execute(params).await.map_err(Error::Postgres)
    }
    /// Execute a transaction, retrying if it aborts due to a serialization failure or deadlock.
    pub async fn transact_retrying<T: Transaction>(
        &self,
        transaction: T,
    ) -> Result<T::Result, Error> {
        let mut client = self.base.get().await.map_err(Error::Deadpool)?;
        let isolation_level = match T::ISOLATION_LEVEL {
            IsolationLevel::ReadCommitted => tokio_postgres::IsolationLevel::ReadCommitted,
            IsolationLevel::RepeatableRead => tokio_postgres::IsolationLevel::RepeatableRead,
            IsolationLevel::Serializable => tokio_postgres::IsolationLevel::Serializable,
        };
        loop {
            let base_transaction = client
                .build_transaction()
                .deferrable(T::DEFERRABLE)
                .read_only(T::READ_ONLY)
                .isolation_level(isolation_level)
                .start()
                .await
                .map_err(Error::Postgres)?;

            let context = TransactionContext {
                base: base_transaction,
                no_preparing: self.no_preparing,
            };

            let (result, commit) = match transaction.run(&context).await {
                Ok(ok) => ok,
                Err(err) => {
                    // Always rollback before deciding what to do next, even on an
                    // aborted transaction (Postgres accepts ROLLBACK in error state).
                    context.base.rollback().await.map_err(Error::Postgres)?;

                    let is_retryable = err.code().is_some_and(|c| {
                        *c == tokio_postgres::error::SqlState::T_R_SERIALIZATION_FAILURE
                            || *c == tokio_postgres::error::SqlState::T_R_DEADLOCK_DETECTED
                    });

                    if is_retryable {
                        // No delay here; callers that need backoff should wrap this.
                        continue;
                    } else {
                        return Err(Error::Postgres(err));
                    }
                }
            };

            if commit {
                context.base.commit().await.map_err(Error::Postgres)?;
            } else {
                context.base.rollback().await.map_err(Error::Postgres)?;
            }

            return Ok(result);
        }
    }
}

pub trait Transaction {
    const DEFERRABLE: bool = false;
    const READ_ONLY: bool = false;
    const ISOLATION_LEVEL: IsolationLevel = IsolationLevel::ReadCommitted;
    type Result;
    fn run(
        &self,
        context: &TransactionContext,
    ) -> impl std::future::Future<Output = Result<(Self::Result, bool), tokio_postgres::Error>> + Send;
}

#[derive(Debug, Copy, Clone, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub enum IsolationLevel {
    /// An individual statement in the transaction will see rows committed before it began.
    ReadCommitted,

    /// All statements in the transaction will see the same view of rows committed before the first query in the
    /// transaction.
    RepeatableRead,

    /// The reads and writes in this transaction must be able to be committed as an atomic "unit" with respect to reads
    /// and writes of all other concurrent serializable transactions without interleaving.
    Serializable,
}

pub struct TransactionContext<'a> {
    base: deadpool_postgres::Transaction<'a>,
    no_preparing: bool,
}

impl<'a> TransactionContext<'a> {
    pub async fn execute<P: Statement>(
        &self,
        params: &P,
    ) -> Result<P::Result, tokio_postgres::Error> {
        let params = params.encode_params();
        let (rows, affected_rows) = if P::RETURNS_ROWS {
            let rows = if self.no_preparing {
                self.base.query(P::SQL, params.as_ref()).await?
            } else {
                let prepared = self
                    .base
                    .prepare_typed_cached(P::SQL, P::PARAM_TYPES)
                    .await?;
                self.base.query(&prepared, params.as_ref()).await?
            };
            let affected = rows.len() as u64;
            (rows, affected)
        } else {
            let affected = if self.no_preparing {
                self.base.execute(P::SQL, params.as_ref()).await?
            } else {
                let prepared = self
                    .base
                    .prepare_typed_cached(P::SQL, P::PARAM_TYPES)
                    .await?;
                self.base.execute(&prepared, params.as_ref()).await?
            };
            (vec![], affected)
        };
        P::decode_result(rows, affected_rows)
    }
}

#[derive(Debug)]
pub enum Error {
    Deadpool(deadpool_postgres::PoolError),
    Postgres(tokio_postgres::Error),
}
