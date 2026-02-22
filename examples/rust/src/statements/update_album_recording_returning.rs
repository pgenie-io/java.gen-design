use chrono::NaiveDate;
use tokio_postgres::types::Type;

use crate::types::{AlbumFormat, RecordingInfo};

/// Parameters for the `update_album_recording_returning` query.
///
/// # SQL Template
///
/// ```sql
/// -- Update album recording information
/// update album
/// set recording = $recording
/// where id = $id
/// returning *
/// ```
///
/// # Source Path
///
/// `./queries/update_album_recording_returning.sql`
#[derive(Debug, Clone, PartialEq)]
pub struct Input {
    /// Maps to `$recording` in the template.
    pub recording: Option<RecordingInfo>,
    /// Maps to `$id` in the template.
    pub id: Option<i64>,
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

    const SQL: &str = "-- Update album recording information\n\
                       update album\n\
                       set recording = $1::recording_info\n\
                       where id = $2\n\
                       returning *";

    const PARAM_TYPES: &'static [tokio_postgres::types::Type] = &[Type::UNKNOWN, Type::INT8];

    #[allow(refining_impl_trait)]
    fn encode_params(
        &self,
    ) -> [&(dyn tokio_postgres::types::ToSql + Sync); Self::PARAM_TYPES.len()] {
        [&self.recording, &self.id]
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
