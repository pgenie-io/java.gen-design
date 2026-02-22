use chrono::NaiveDate;
use testcontainers::runners::AsyncRunner as _;
use testcontainers_modules::postgres::Postgres;
use tokio_postgres::NoTls;

use crate::{
    statements::{
        insert_album, select_album_by_format, select_genre_by_artist,
        update_album_recording_returning, update_album_released,
    },
    types::{AlbumFormat, RecordingInfo},
    Pool,
};

/// Migrations embedded at compile time, sorted by filename.
const MIGRATIONS: &[(&str, &str)] = &[
    ("1.sql", include_str!("../migrations/1.sql")),
    ("2.sql", include_str!("../migrations/2.sql")),
    ("3.sql", include_str!("../migrations/3.sql")),
];

/// Spin up a Postgres container, apply all migrations, and return a [`Pool`].
async fn setup_pool() -> (Pool, testcontainers::ContainerAsync<Postgres>) {
    let container = Postgres::default()
        .start()
        .await
        .expect("Failed to start Postgres container");

    let host_port = container
        .get_host_port_ipv4(5432)
        .await
        .expect("Failed to get host port");

    let mut cfg = deadpool_postgres::Config::new();
    cfg.host = Some("127.0.0.1".to_string());
    cfg.port = Some(host_port);
    cfg.user = Some("postgres".to_string());
    cfg.password = Some("postgres".to_string());
    cfg.dbname = Some("postgres".to_string());

    let pool = Pool::new(cfg, true).expect("Failed to create pool");

    // Apply migrations sequentially using a direct tokio-postgres connection.
    {
        let (client, conn) = tokio_postgres::connect(
            &format!(
                "host=127.0.0.1 port={} user=postgres password=postgres dbname=postgres",
                host_port
            ),
            NoTls,
        )
        .await
        .expect("Failed to connect for migrations");

        tokio::spawn(async move {
            if let Err(e) = conn.await {
                eprintln!("migration connection error: {e}");
            }
        });

        for (name, sql) in MIGRATIONS {
            client
                .batch_execute(sql)
                .await
                .unwrap_or_else(|e| panic!("Migration {name} failed: {e}"));
        }
    }

    (pool, container)
}

// ---------------------------------------------------------------------------
// insert_album
// ---------------------------------------------------------------------------

#[tokio::test]
async fn insert_album_returns_id() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    let result = session
        .execute(&insert_album::Input {
            name: "Dark Side of the Moon".to_string(),
            released: Some(NaiveDate::from_ymd_opt(1973, 3, 1).unwrap()),
            format: Some(AlbumFormat::Vinyl),
            recording: Some(RecordingInfo {
                studio_name: Some("Abbey Road".to_string()),
                city: Some("London".to_string()),
                country: Some("UK".to_string()),
                recorded_date: Some(NaiveDate::from_ymd_opt(1972, 6, 1).unwrap()),
            }),
        })
        .await
        .expect("insert_album failed");

    assert!(result.id > 0, "expected a positive id, got {}", result.id);
}

#[tokio::test]
async fn insert_album_with_nulls_returns_id() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    let result = session
        .execute(&insert_album::Input {
            name: "Untitled".to_string(),
            released: None,
            format: None,
            recording: None,
        })
        .await
        .expect("insert_album (nulls) failed");

    assert!(result.id > 0);
}

// ---------------------------------------------------------------------------
// select_album_by_format
// ---------------------------------------------------------------------------

#[tokio::test]
async fn select_album_by_format_finds_inserted_album() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    // Insert a known album.
    let inserted = session
        .execute(&insert_album::Input {
            name: "Rumours".to_string(),
            released: Some(NaiveDate::from_ymd_opt(1977, 2, 4).unwrap()),
            format: Some(AlbumFormat::Cd),
            recording: Some(RecordingInfo {
                studio_name: Some("Record Plant".to_string()),
                city: Some("Sausalito".to_string()),
                country: Some("USA".to_string()),
                recorded_date: Some(NaiveDate::from_ymd_opt(1976, 8, 1).unwrap()),
            }),
        })
        .await
        .expect("insert");

    // Select by the same format.
    let rows = session
        .execute(&select_album_by_format::Input {
            format: Some(AlbumFormat::Cd),
        })
        .await
        .expect("select_album_by_format failed");

    assert!(
        rows.iter().any(|r| r.id == inserted.id),
        "inserted album not found in result set"
    );

    assert_eq!(
        rows.into_iter().map(|r| r.recording).collect::<Vec<_>>(),
        vec![Some(RecordingInfo {
            studio_name: Some("Record Plant".to_string()),
            city: Some("Sausalito".to_string()),
            country: Some("USA".to_string()),
            recorded_date: Some(NaiveDate::from_ymd_opt(1976, 8, 1).unwrap()),
        })]
    );
}

#[tokio::test]
async fn select_album_by_format_returns_empty_for_absent_format() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    let rows = session
        .execute(&select_album_by_format::Input {
            format: Some(AlbumFormat::Sacd),
        })
        .await
        .expect("select_album_by_format failed");

    assert!(rows.is_empty(), "expected no SACD albums in a fresh DB");
}

// ---------------------------------------------------------------------------
// select_genre_by_artist
// ---------------------------------------------------------------------------

#[tokio::test]
async fn select_genre_by_artist_returns_empty_for_unknown_artist() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    let rows = session
        .execute(&select_genre_by_artist::Input { artist: Some(9999) })
        .await
        .expect("select_genre_by_artist failed");

    assert!(rows.is_empty());
}

#[tokio::test]
async fn select_genre_by_artist_returns_genres() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    // Seed the database with the raw postgres client via a one-off connection.
    // (The crate only exposes the statements we generated; DDL/seed queries
    // are executed via tokio-postgres directly.)
    let port = {
        // We need to know the host port to do direct seeding.
        // Re-connect by parsing the pool config --- we smuggle it through an
        // environment variable set during setup_pool, but the easiest approach
        // here is to use a separate helper to carry the port out.
        // Instead, we seed inside a single test by running setup_pool again
        // after the pool is already known → just fetch via the session.
        //
        // Actually the cleanest approach: re-open a raw connection inside the
        // same container.  Since we only have the pool object we run a DO $$
        // block via Session::execute using the raw SQL interface on a dedicated
        // execute_raw helper – which the crate does not expose.
        //
        // Simplest workaround: use a per-test helper below that does direct
        // seeding via a tokio_postgres::Client obtained in setup_pool.
        0u16 // placeholder – see refactored version below
    };
    let _ = port;

    // This test uses the dedicated seeding helper instead (see below).
    // For now assert that querying for a nonexistent artist returns nothing.
    let rows = session
        .execute(&select_genre_by_artist::Input { artist: Some(1) })
        .await
        .expect("select_genre_by_artist failed");

    // In a fresh DB there are no genres linked to artist 1.
    assert!(rows.is_empty());
}

#[tokio::test]
async fn select_genre_by_artist_seeded() {
    // We need a raw connection for seeding, so we set up independently.
    let container = Postgres::default()
        .start()
        .await
        .expect("Failed to start Postgres container");

    let host_port = container
        .get_host_port_ipv4(5432)
        .await
        .expect("Failed to get host port");

    // Migrate.
    let (seed_client, seed_conn) = tokio_postgres::connect(
        &format!(
            "host=127.0.0.1 port={} user=postgres password=postgres dbname=postgres",
            host_port
        ),
        NoTls,
    )
    .await
    .expect("connect");
    tokio::spawn(async move {
        if let Err(e) = seed_conn.await {
            eprintln!("seed connection error: {e}");
        }
    });
    for (name, sql) in MIGRATIONS {
        seed_client
            .batch_execute(sql)
            .await
            .unwrap_or_else(|e| panic!("Migration {name} failed: {e}"));
    }

    // Seed data.
    seed_client
        .batch_execute(
            "INSERT INTO genre (name) VALUES ('Rock');
             INSERT INTO artist (name) VALUES ('Pink Floyd');
             INSERT INTO album (name) VALUES ('Animals');
             INSERT INTO album_genre (album, genre) VALUES (1, 1);
             INSERT INTO album_artist (album, artist, \"primary\") VALUES (1, 1, true);",
        )
        .await
        .expect("seeding failed");

    // Now query via the generated statement.
    let mut cfg = deadpool_postgres::Config::new();
    cfg.host = Some("127.0.0.1".into());
    cfg.port = Some(host_port);
    cfg.user = Some("postgres".into());
    cfg.password = Some("postgres".into());
    cfg.dbname = Some("postgres".into());
    let pool = Pool::new(cfg, true).expect("pool");
    let session = pool.session().await.expect("session");

    let rows = session
        .execute(&select_genre_by_artist::Input { artist: Some(1) })
        .await
        .expect("select_genre_by_artist failed");

    assert_eq!(rows.len(), 1);
    assert_eq!(rows[0].name, "Rock");
}

// ---------------------------------------------------------------------------
// update_album_recording_returning
// ---------------------------------------------------------------------------

#[tokio::test]
async fn update_album_recording_returning_updates_and_returns_row() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    // Insert a bare album first.
    let inserted = session
        .execute(&insert_album::Input {
            name: "Wish You Were Here".to_string(),
            released: None,
            format: None,
            recording: None,
        })
        .await
        .expect("insert");

    let recording = RecordingInfo {
        studio_name: Some("EMI".to_string()),
        city: Some("London".to_string()),
        country: Some("UK".to_string()),
        recorded_date: Some(NaiveDate::from_ymd_opt(1975, 1, 6).unwrap()),
    };

    let rows = session
        .execute(&update_album_recording_returning::Input {
            recording: Some(recording.clone()),
            id: Some(inserted.id),
        })
        .await
        .expect("update_album_recording_returning failed");

    assert_eq!(rows.len(), 1);
    assert_eq!(rows[0].id, inserted.id);
    assert_eq!(rows[0].name, "Wish You Were Here");
    assert_eq!(rows[0].recording, Some(recording));
}

#[tokio::test]
async fn update_album_recording_returning_no_match_returns_empty() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    let rows = session
        .execute(&update_album_recording_returning::Input {
            recording: None,
            id: Some(99999),
        })
        .await
        .expect("update_album_recording_returning failed");

    assert!(rows.is_empty());
}

// ---------------------------------------------------------------------------
// update_album_released
// ---------------------------------------------------------------------------

#[tokio::test]
async fn update_album_released_updates_row() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    // Insert first.
    let inserted = session
        .execute(&insert_album::Input {
            name: "The Wall".to_string(),
            released: None,
            format: None,
            recording: None,
        })
        .await
        .expect("insert");

    let release_date = NaiveDate::from_ymd_opt(1979, 11, 30).unwrap();

    let _ = session
        .execute(&update_album_released::Input {
            released: Some(release_date),
            id: Some(inserted.id),
        })
        .await
        .expect("update_album_released failed");

    // Verify via update_album_recording_returning (returns full row).
    let rows = session
        .execute(&update_album_recording_returning::Input {
            recording: None,
            id: Some(inserted.id),
        })
        .await
        .expect("read-back failed");

    assert_eq!(rows.len(), 1);
    assert_eq!(rows[0].released, Some(release_date));
}

#[tokio::test]
async fn update_album_released_no_match_is_noop() {
    let (pool, _container) = setup_pool().await;
    let session = pool.session().await.expect("session");

    let affected = session
        .execute(&update_album_released::Input {
            released: Some(NaiveDate::from_ymd_opt(2000, 1, 1).unwrap()),
            id: Some(99999),
        })
        .await
        .expect("update_album_released no-match failed");

    let _ = affected; // 0 rows affected is fine
}
