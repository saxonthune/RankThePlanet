---
title: System
summary: How RTP stores, reads, syncs, and structures its data — the bridge from concepts to code
tags: [system, index]
deps: []
---

# System

**Purpose.** How RTP's data behaves below the UI: where it is stored, how it is written, how it syncs, and the interfaces that keep those concerns separable.

**Audience.** Read this group before writing any data-layer code, and before building a screen that reads or writes concept state. UI-mockup sessions need doc03.02 specifically — it is the contract they build against.

**What belongs here.** Storage format, write path, sync, repository and provider contracts, domain model shapes. CMP-specific state-holder patterns (ViewModel, UiState) are *not* here — those live in the `kotlin-cmp` skill and are assumed.

**What doesn't.** Concept definitions (doc01.03), UI surfaces (doc02.02), tech-stack rationale (doc02.01).

## Contents

- **doc03.01 Store Model** — the two stores (local SQLCipher DB + sync replica), the table schema, the local-first write path, the memoized overview projection, and the op-log.
- **doc03.02 Data Interfaces** — domain models and the data-layer interface inventory: typed repositories, op-log, overview projection, sync engine.

A providers doc (the swappable external seams — `LocationProvider`, `SyncTarget`, `TileSource`, …) will be added when a provider-focused work item demands it.
