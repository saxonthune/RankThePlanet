---
title: System
summary: How RTP stores, reads, syncs, and wires itself together below and around the UI — the bridge from concepts to code
tags: [system, index]
deps: []
---

# System

**Purpose.** How RTP behaves below and around the UI: where data is stored, how it is written, how it syncs, the interfaces that keep those concerns separable, and how the app composes its surfaces into a running whole.

**Audience.** Read this group before writing any data-layer code, before building a screen that reads or writes concept state, and before touching app composition or navigation. UI-mockup sessions need doc03.02 specifically — it is the contract they build against.

**What belongs here.** Storage format, write path, sync, repository and provider contracts, domain model shapes, and app-level structural wiring — how the navigation statechart becomes a concrete `NavHost`. Per-screen CMP state-holder patterns (one ViewModel, one UiState per surface) are *not* here — those live in the `kotlin-cmp` skill and are assumed; this group covers only the framework-level scaffolding the screens plug into.

**What doesn't.** Concept definitions (doc01.03), UI surfaces (doc02.02), tech-stack rationale (doc02.01).

## Contents

- **doc03.01 Store Model** — the two stores (local SQLCipher DB + sync replica), the table schema, the local-first write path, the memoized overview projection, and the op-log.
- **doc03.02 Data Interfaces** — domain models and the data-layer interface inventory: typed repositories, op-log, overview projection, sync engine.
- **doc03.03 Navigation Wiring** — how the platform-agnostic navigation statechart (doc02.02.01) becomes a Compose Multiplatform `NavHost`: type-safe routes, back stack, per-route ViewModel scoping.

A providers doc (the swappable external seams — `LocationProvider`, `SyncTarget`, `TileSource`, …) will be added when a provider-focused work item demands it.
