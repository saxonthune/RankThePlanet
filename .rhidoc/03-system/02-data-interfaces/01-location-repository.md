---
title: Location Repository
summary: The LocationRepository interface: identity lookup, upsert, merge — the persistence-shaped contract screens consume for Locations
tags: [system, interfaces, repository, data, contract, location]
deps: [doc01.03, doc03.01, doc03.02]
---

# Location Repository

The persistence-shaped contract for the Location concept (doc01.03 §2). One of the repositories enumerated in doc03.02's data-layer interface inventory; this doc unfolds its surface in full.

Screens that touch Locations consume `LocationRepository`, not the store (doc03.01) and not `LocationProvider` (doc03.02.02). The repository is the seam UI mockups and ViewModels build against.

## The interface

Pure Kotlin in `commonMain`. Domain types from `domain/Model.kt` (doc03.02) — `Coordinates`, `SourceType`, `Location` — and nothing else.

```kotlin
interface LocationRepository {
    // Identity-shaped read. The dedupe seam: a candidate adopted from one
    // provider call must not create a second Location row for the same place.
    // Also the seam MapOverview's search-result merge uses to suppress
    // provider candidates whose identity already corresponds to a saved
    // Location (doc02.02.02.07).
    fun findByIdentity(sourceType: SourceType, sourceId: String): Location?

    // Idempotent write. Creates a row when no identity match exists; updates
    // the cached fields (displayName, coordinates, cachedMetadata) when one
    // does. Returns the resulting Location either way.
    suspend fun upsert(location: Location): Result<Location>

    // Collapse two Locations onto one identity. `keep` survives; every Entry
    // that referenced `drop` is rewritten to point at `keep`, and `drop` is
    // removed. Used when a Manual Location is later matched to a provider
    // identity, or when two provider identities turn out to name one place.
    suspend fun merge(keep: Location, drop: Location): Result<Location>
}
```

The three methods cover the Location concept's persistence actions exactly — provider-side search stays on `LocationProvider` (doc03.02.02), which produces `LocationCandidate`s the user adopts into Locations via `upsert`. The existing-entry half of MapOverview's search list (doc02.02.02.07) is not a repository concern either: it is an in-memory filter over the surface's already-collected entry stream, biased by the live viewport. A dedicated text-query method on the repository unfolds only when the entry stream grows too large to filter in memory.

## How it composes with the provider

A LocationDraftSheet session (doc02.02.02.05) reads candidates from `LocationProvider`, then — when the user adopts one — constructs a `Location` and calls `LocationRepository.upsert`. The repository owns persistence; the provider owns search. Neither knows about the other.

`SourceType.Manual` Locations (dropped pins, candidates without a `sourceId`) go through `upsert` the same way — `findByIdentity` is only consulted when a `sourceId` exists.

## Write path

A repository implementation performs the doc03.01 write path internally: one transaction covering the materialized `Location` row *and* the op-log append. Repositories construct ops; nothing above the repository sees them.

`merge` is a single op-shaped action — the rewrite of dependent Entry rows is part of the same transaction.
