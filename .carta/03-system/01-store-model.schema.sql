-- RTP local store schema. Host doc: doc03.01 (Store Model).
-- Encrypted at rest via SQLCipher. Concept source: doc01.03.

-- A Location. Identity is (source_type, source_id), not coordinates.
CREATE TABLE location (
    id              TEXT PRIMARY KEY,            -- surrogate UUID for FKs
    source_type     TEXT NOT NULL,               -- google | osm | apple | manual ...
    source_id       TEXT NOT NULL,               -- place_id, OSM node id, UUID for manual
    lat             REAL NOT NULL,
    lng             REAL NOT NULL,
    display_name    TEXT NOT NULL,
    cached_metadata TEXT,                         -- opaque JSON snapshot at import time
    refreshable     INTEGER NOT NULL DEFAULT 0,
    UNIQUE (source_type, source_id)
);

-- A Collection: the binding ring for entries.
CREATE TABLE collection (
    id                   TEXT PRIMARY KEY,
    name                 TEXT NOT NULL,
    description          TEXT,                    -- optional, user-authored
    appearance_color     TEXT NOT NULL,           -- drives pin fill on the map
    appearance_pin_style TEXT NOT NULL,
    template_version     INTEGER NOT NULL DEFAULT 1,
    is_visible           INTEGER NOT NULL DEFAULT 1,  -- Map Overview collectionFilter
    power_ranking        INTEGER NOT NULL DEFAULT 0,  -- 1 when entries are user-ordered (doc01.03 §1)
    created              TEXT NOT NULL,
    last_modified        TEXT NOT NULL
);

-- Review template fields, kept per version. editTemplate bumps
-- collection.template_version; removed fields stay under their old version.
CREATE TABLE template_field (
    collection_id   TEXT NOT NULL REFERENCES collection(id),
    version         INTEGER NOT NULL,
    ordinal         INTEGER NOT NULL,             -- template is an ordered list
    name            TEXT NOT NULL,                -- stable machine key, derived from label at create time; immutable thereafter
    label           TEXT NOT NULL,                -- user-facing display string (the "section" in the editor); freely renamable
    type            TEXT NOT NULL,                -- score|text|enum|boolean|date
    config          TEXT,                         -- per-type JSON or NULL; shapes per doc01.03 §3
    PRIMARY KEY (collection_id, version, name)
);

-- An entry = a Location within a Collection, optionally carrying a Review.
-- The Review instance folds in: no orphan Reviews exist. data is NULL until
-- the user submits a Review; a NULL-data entry is unreviewed (doc01.03 §3).
CREATE TABLE entry (
    id                        TEXT PRIMARY KEY,
    collection_id             TEXT NOT NULL REFERENCES collection(id),
    location_id               TEXT NOT NULL REFERENCES location(id),
    data                      TEXT,               -- JSON map: field-name -> value; NULL = unreviewed
    recorded_template_version INTEGER,             -- version `data` conforms to; NULL when unreviewed
    created                   TEXT NOT NULL,
    last_modified             TEXT NOT NULL,
    UNIQUE (collection_id, location_id)
);

-- Append-only mutation log. Merge artifact for multi-device sync.
CREATE TABLE op_log (
    op_id      TEXT PRIMARY KEY,                  -- globally unique; dedup key on merge
    ts         TEXT NOT NULL,                     -- timestamp; merge sort key
    device_id  TEXT NOT NULL,
    kind       TEXT NOT NULL,                     -- entry.submit, collection.created ...
    payload    TEXT NOT NULL                      -- JSON describing the mutation
);
