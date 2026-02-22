//! Type-safe bindings for the `music_catalogue` database.
//!
//! Generated from SQL queries using the [pGenie](https://pgenie.io) code generator.
//!
//! - [`statements`] – ready-to-use statement definitions for all queries with
//!   associated parameter and result types.
//! - [`types`] – PostgreSQL enum and composite type mappings.

pub mod statements;
#[cfg(test)]
mod tests;
pub mod types;

/// Implemented by each query's parameter struct. Provides a uniform way to
/// prepare and execute statements against a [`tokio_postgres::Client`].
pub trait StatementParams {
    /// The type returned when the statement is successfully executed.
    type Result;

    const SQL: &str;

    const PARAM_TYPES: &'static [tokio_postgres::types::Type];

    /// Encode `self` as a list of type-erased parameter references.
    ///
    /// Implementations return a stack-allocated, fixed-size array (no heap
    /// allocation).
    fn encode_params(&self) -> impl AsRef<[&(dyn tokio_postgres::types::ToSql + Sync)]>;

    fn decode_result(rows: Vec<tokio_postgres::Row>)
        -> Result<Self::Result, tokio_postgres::Error>;
}

pub struct Session {
    base: deadpool_postgres::Client,
    no_preparing: bool,
}

impl Session {
    pub async fn execute<P: StatementParams>(
        &self,
        params: &P,
    ) -> Result<P::Result, tokio_postgres::Error> {
        let params = params.encode_params();
        let rows = if self.no_preparing {
            self.base.query(P::SQL, params.as_ref()).await?
        } else {
            let prepared = self
                .base
                .prepare_typed_cached(P::SQL, P::PARAM_TYPES)
                .await?;
            self.base.query(&prepared, params.as_ref()).await?
        };
        P::decode_result(rows)
    }

    pub async fn transaction(&mut self) -> Result<Transaction<'_>, deadpool_postgres::PoolError> {
        Ok(Transaction {
            base: self.base.transaction().await?,
            no_preparing: self.no_preparing,
        })
    }

    pub fn build_transaction(
        &mut self,
    ) -> Result<TransactionBuilder<'_>, deadpool_postgres::PoolError> {
        Ok(TransactionBuilder {
            base: self.base.build_transaction(),
            no_preparing: self.no_preparing,
        })
    }
}

pub struct Transaction<'a> {
    base: deadpool_postgres::Transaction<'a>,
    no_preparing: bool,
}

impl<'a> Transaction<'a> {
    pub async fn execute<P: StatementParams>(
        &mut self,
        params: &P,
    ) -> Result<P::Result, tokio_postgres::Error> {
        let params = params.encode_params();
        let rows = if self.no_preparing {
            self.base.query(P::SQL, params.as_ref()).await?
        } else {
            let prepared = self
                .base
                .prepare_typed_cached(P::SQL, P::PARAM_TYPES)
                .await?;
            self.base.query(&prepared, params.as_ref()).await?
        };
        P::decode_result(rows)
    }
}

pub struct TransactionBuilder<'a> {
    base: deadpool_postgres::TransactionBuilder<'a>,
    no_preparing: bool,
}

impl<'a> TransactionBuilder<'a> {
    pub fn isolation_level(mut self, level: tokio_postgres::IsolationLevel) -> Self {
        self.base = self.base.isolation_level(level);
        self
    }

    pub fn read_only(mut self, read_only: bool) -> Self {
        self.base = self.base.read_only(read_only);
        self
    }

    pub fn deferrable(mut self, deferrable: bool) -> Self {
        self.base = self.base.deferrable(deferrable);
        self
    }

    pub async fn start(self) -> Result<Transaction<'a>, deadpool_postgres::PoolError> {
        Ok(Transaction {
            base: self.base.start().await?,
            no_preparing: self.no_preparing,
        })
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
}
