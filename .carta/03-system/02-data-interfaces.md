---
title: Data Interfaces
summary: Domain models and the data-layer interface inventory: typed repositories, op-log, overview projection, sync engine — the contract UI sessions build against
tags: [system, interfaces, repository, data, contract]
deps: [doc01.03, doc03.01]
---

# Data Interfaces

The contract between RTP's UI and its data. UI-mockup sessions build against the **domain models** and **repository interfaces** here — never against persistence (doc03.01) or providers directly.

Layering and state-holder patterns (ViewModel, one `UiState` per screen, `StateFlow`) follow the `kotlin-cmp` skill and are assumed, not restated.

## Domain models

Pure Kotlin in `commonMain` — no Compose, no platform, no I/O. One type per concept (doc01.03):

- `Collection` — id, name, appearance, current `templateVersion`, `isVisible`.
- `Location` — id, coordinates, displayName, `sourceType`, `sourceId`, cachedMetadata, refreshable.
- `Entry` — id, collectionId, the `Location`, and the `Review` instance (data map + `recordedTemplateVersion`). An Entry *is* the `(Location, Review)` pair.
- `ReviewTemplate` — ordered list of `TemplateField`; each field has name, `FieldType`, per-type config, required flag.
- `MapOverviewState` — viewport + `visiblePins` + collectionFilter; what `OverviewProjection` returns.

Models are immutable `data class`es. Persistence shapes (rows, op-log) never cross into this layer.

## Write API: typed repositories

Writes go through **concept-shaped, typed repository methods** — not a generic op funnel. Each method names a concept action (doc01.03), takes domain types, returns `Result`. Sketch:

```
CollectionRepository
    observeAll(): Flow<List<Collection>>
    create(name, appearance): Result<Collection>
    addEntry(collectionId, location, review): Result<Entry>
    removeEntry(entryId): Result<Unit>

EntryRepository
    observeByCollection(collectionId): Flow<List<Entry>>
    observe(entryId): Flow<Entry>
    editReview(entryId, data): Result<Entry>

LocationRepository
    findByIdentity(sourceType, sourceId): Location?
    upsert(location): Result<Location>
    merge(keep, drop): Result<Location>

TemplateRepository
    observe(collectionId): Flow<ReviewTemplate>
    define(collectionId, fields): Result<ReviewTemplate>
    edit(collectionId, fields): Result<ReviewTemplate>
```

Method names track concept actions so the future `coverage.mjs` (doc02.02.01) keeps verifying every action has both a UI affordance and a repository method. A repository implementation performs the doc03.01 write path internally: one transaction covering the materialized rows *and* the op-log append.

## Data-layer interface inventory

| Interface | Responsibility | First implementation |
|---|---|---|
| `CollectionRepository` / `EntryRepository` / `LocationRepository` / `TemplateRepository` | typed, concept-shaped read + write API | SQLCipher-backed |
| `OverviewProjection` | read the memoized overview cache: `loadOverview()`, `observeOverview()` | `PassthroughOverviewProjection` — queries the local store on demand |
| `ProjectionMaintainer` | keep the overview cache fresh from the mutation stream | no-op (passthrough needs none) |
| `OpLog` | append-only op store; query `since(hash)`, `all()` | table in the local store |
| `StoreSerializer` | local store ⇄ portable bytes; WAL checkpoint before serialize | `SqlCipherSerializer` |
| `OpLogMerger` | merge two op-logs; flag same-entry conflicts | `TimestampMerger` |
| `SyncTarget` | bytes in/out of the sync replica: `pull()`, `push()`, `watch()` | document-picker / WebDAV |
| `SyncEngine` | orchestrate the debounced read-check-merge-write | `OpLogSyncEngine` |
| `StoreKeyProvider` | supply the SQLCipher key | OS-keystore-backed |

The `Op` sealed type is internal to the data layer — repositories construct ops; nothing above the repository sees them. Repository implementations expose a `SharedFlow` of applied mutations that `ProjectionMaintainer` and `SyncEngine` subscribe to; that flow is the only coupling between writing and its downstream consumers.

The swappable *external* seams — `LocationProvider`, `ImportSource`, `ExportFormat`, `TileSource`, `FieldType` — will be catalogued in a future providers doc. They play no part in a review submission.

## What UI-mockup sessions consume

A mockup session builds **one surface** from the doc02.02.01 inventory. To do so:

1. Depend on the domain models and the repository *read* interfaces above — nothing lower.
2. Wire an in-memory fake repository (`FakeCollectionRepository`, …) seeded from a **shared fixtures set** — the "Drip Coffee" / "Blue Bottle" / NYT-100 world from doc01.02. One shared fixtures source keeps mockups visually consistent and comparable across sessions.
3. Read the surface's `meta.reads` and `invokes` (doc02.02.01) to know which repository methods the screen needs.

No mockup imports SQLCipher, a `SyncTarget`, or a real provider. When the real data layer lands behind these interfaces, no composable changes — that is the test of the seam.
