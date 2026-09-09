---
title: Store Model
summary: Two-store persistence: local SQLCipher DB + sync replica; table schema, local-first write path, memoized overview projection, op-log
tags: [system, storage, sqlcipher, sync, schema]
deps: [doc01.03, doc02.01]
---

# Store Model

How RTP persists concept state (doc01.03) and reconciles it across devices. Persistence format — SQLCipher — is decided in doc02.01.

## Two stores

There is no single store. RTP keeps two, with distinct authority:

- **Local store** — an encrypted SQLCipher database in the app's private sandbox. Authoritative on-device. The app reads and writes only here.
- **Sync replica** — a serialized copy of the local store at a user-chosen location (the sync target — a cloud-backed folder, WebDAV server, etc.). Eventually reconciled; never written by app logic directly.

The principle is a **single write path, local-first**: the app writes one place — the local store; the sync replica is downstream.

A third artifact, the **overview projection**, is a derived read cache — not a store of truth. See below.

## Table schema

Concept → table mapping. The DDL is the sidecar `store-model.schema.sql`; this section states behavior.

- `location` — a Location (doc01.03 §2). Surrogate `id` for foreign keys; `UNIQUE (source_type, source_id)` carries the real identity. `cached_metadata` is opaque JSON.
- `collection` — a Collection (§1). Holds `name`, optional `description`, `appearance`, the current `template_version`, and `is_visible` (absorbs Map Overview's persistent `collectionFilter`, §4).
- `template_field` — Review template fields (§3), keyed `(collection_id, version, name)`. Carries `label` (user-facing string, distinct from `name` which is the immutable machine key) and `config` (per-type JSON; shape per doc01.03 §3, `NULL` for types whose config defaults). `editTemplate` bumps `collection.template_version`; old fields stay under their old version rather than being deleted — this is how "removed fields archived but not destroyed" works.
- `entry` — an entry = a Location within a Collection, optionally carrying a Review. The Review *instance* folds in here (`data` JSON, `recorded_template_version`); there is no separate review table, because a Review never exists outside an entry (§3). `data` is `NULL` until the user submits a Review — a NULL-data entry is **unreviewed** (§3), which is what drives the Map Overview's unreviewed pin styling. Once non-NULL, `data` is a JSON object whose keys match `template_field.name` at the recorded version; **absent key = unset**, empty-string is a distinct deliberate value (doc01.03 §3). On `Review.edit` the row is rewritten with the current `collection.template_version` stamped as `recorded_template_version`; keys whose `name` no longer exists at the current version are dropped (per-key reconciliation, doc01.03 §3 Notes). `UNIQUE (collection_id, location_id)`: a Location appears at most once per Collection.
- `op_log` — append-only mutation log; see below.

`viewport` and `selectedPin` (§4) are device-local session state, not in either store — they belong in a tiny file restored synchronously at launch (doc01.01 cold-start playbook).

## Write path

Every concept action that mutates state follows the same order. Example: `Review.submit`.

1. **Validate** in memory against the template.
2. **Write** — one SQLCipher transaction: upsert the `entry` row, bump `collection.last_modified`, append one `op_log` row. Commit. The user's data is now durable on-device, with no network dependency.
3. **Refresh** the overview projection.
4. **UI transitions** (doc02.02.01). Everything below is background.
5. **Push** (debounced) — see Sync.

Durability is step 2. Steps 3–5 may lag or fail without risking data.

## Collection import

An import parses and validates its complete portable payload before it creates a Collection. The importer reports entry progress and checks coroutine cancellation between entries. If any template, Location, or Entry write fails, the importer removes the partial Collection, its template fields, its Entries, and Locations left orphaned by that attempt. The UI therefore observes either one complete imported Collection or no imported Collection.

## Overview projection (memoized secondary store)

A derived projection holding exactly what `MapOverview.open()` needs to paint the first frame: `viewport`, `collectionFilter`, and `visiblePins` (entry id, lat/lng, collection color, reviewed flag). No review `data`, no templates.

It is a **pure function of the local store** — rebuildable, never authoritative. If missing, stale, or corrupt, it is discarded and rebuilt; no data loss is possible.

Startup: paint from the projection first (fast), open and decrypt the local store off the main thread (50–200 ms), then reconcile. The projection's storage form is an implementation choice behind the `OverviewProjection` interface (doc03.02) — the first implementation simply queries the local store on demand.

## Sync

Three separate problems; do not conflate them (doc01.01).

**Sync** (one user, many devices). A debounced push:

1. `pull()` the sync replica → bytes + remote hash.
2. Remote hash == `last_synced_hash`? If yes, serialize and `push()`.
3. If no — the replica changed since our last sync: decrypt it, merge op-logs (sort by timestamp, dedupe by op-id), re-materialize the local store, re-derive the projection, then serialize and push.
4. On success, store the new hash as `last_synced_hash`.

Never blind-overwrite — step 3 is the conflict guard. Two different entries merge cleanly; two edits to the same entry's `data` are a genuine conflict (last-writer-wins per field, or prompt). Before serializing, the SQLCipher WAL must be checkpointed/truncated so the replica is a genuine single file.

**Sharing** (different users) and **backup** are deferred — see doc01.01.

## Op-log

Every write appends one `op_log` row in the same transaction as the materialized change. The log is the merge artifact: divergent replicas reconcile by replaying ops in causal order.

The op-log exists from day one as a **data-layer artifact**. It is *not* the app's write API — writes go through typed repositories (doc03.02), and `Op` is internal to the data layer.

**Deferred:** whether the op-log becomes the source of truth (event sourcing — materialized tables as a projection) or remains an audit log alongside authoritative tables. v1 may treat it as an audit trail; the merge machinery works either way. This decision is left open deliberately (doc01.01).
