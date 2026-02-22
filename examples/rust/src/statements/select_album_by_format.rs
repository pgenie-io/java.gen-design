use chrono::NaiveDate;
use tokio_postgres::types::Type;

use crate::types::{AlbumFormat, RecordingInfo};

/// Parameters for the `select_album_by_format` query.
///
/// # SQL Template
///
/// ```sql
/// select
///   id,
///   name,
///   released,
///   format,
///   recording
/// from album
/// where format = $format
/// ```
///
/// # Source Path
///
/// `./queries/select_album_by_format.sql`
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Input {
    /// Maps to `$format` in the template.
    pub format: Option<AlbumFormat>,
}

/// Result of the statement parameterised by [`Input`].
pub type Output = Vec<OutputRow>;

/// Row of [`Output`].
#[derive(Debug, Clone, PartialEq)]
pub struct OutputRow {
    /// Maps to the `id` result set column.
    pub id: i64,
    /// Maps to the `name` result set column.
    pub name: String,
    /// Maps to the `released` result set column.
    pub released: Option<NaiveDate>,
    /// Maps to the `format` result set column.
    pub format: Option<AlbumFormat>,
    /// Maps to the `recording` result set column.
    pub recording: Option<RecordingInfo>,
}

impl crate::StatementParams for Input {
    type Result = Output;

    const SQL: &str = "select\n\
                         id,\n\
                         name,\n\
                         released,\n\
                         format,\n\
                         recording\n\
                       from album\n\
                       where format = $1::album_format";

    const PARAM_TYPES: &'static [tokio_postgres::types::Type] = &[Type::UNKNOWN];

    #[allow(refining_impl_trait)]
    fn encode_params(
        &self,
    ) -> [&(dyn tokio_postgres::types::ToSql + Sync); Self::PARAM_TYPES.len()] {
        [&self.format]
    }

    fn decode_result(
        rows: Vec<tokio_postgres::Row>,
    ) -> Result<Self::Result, tokio_postgres::Error> {
        rows.iter()
            .map(|row| {
                Ok(OutputRow {
                    id: row.try_get(0)?,
                    name: row.try_get(1)?,
                    released: row.try_get(2)?,
                    format: row.try_get(3)?,
                    recording: row.try_get(4)?,
                })
            })
            .collect()
    }
}
