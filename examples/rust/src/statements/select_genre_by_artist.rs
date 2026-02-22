use tokio_postgres::types::Type;

/// Parameters for the `select_genre_by_artist` query.
///
/// # SQL Template
///
/// ```sql
/// select id, genre.name
/// from genre
/// left join album_genre on album_genre.genre = genre.id
/// left join album_artist on album_artist.album = album_genre.album
/// where album_artist.artist = $artist
/// ```
///
/// # Source Path
///
/// `./queries/select_genre_by_artist.sql`
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Input {
    /// Maps to `$artist` in the template.
    pub artist: Option<i32>,
}

/// Result of the statement parameterised by [`Input`].
pub type Output = Vec<OutputRow>;

/// Row of [`Output`].
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OutputRow {
    /// Maps to the `id` result set column.
    pub id: i32,
    /// Maps to the `name` result set column.
    pub name: String,
}

impl crate::StatementParams for Input {
    type Result = Output;

    const SQL: &str = "select id, genre.name\n\
                       from genre\n\
                       left join album_genre on album_genre.genre = genre.id\n\
                       left join album_artist on album_artist.album = album_genre.album\n\
                       where album_artist.artist = $1";

    const PARAM_TYPES: &'static [tokio_postgres::types::Type] = &[Type::INT4];

    #[allow(refining_impl_trait)]
    fn encode_params(
        &self,
    ) -> [&(dyn tokio_postgres::types::ToSql + Sync); Self::PARAM_TYPES.len()] {
        [&self.artist]
    }

    fn decode_result(
        rows: Vec<tokio_postgres::Row>,
    ) -> Result<Self::Result, tokio_postgres::Error> {
        rows.iter()
            .map(|row| {
                Ok(OutputRow {
                    id: row.try_get(0)?,
                    name: row.try_get(1)?,
                })
            })
            .collect()
    }
}
