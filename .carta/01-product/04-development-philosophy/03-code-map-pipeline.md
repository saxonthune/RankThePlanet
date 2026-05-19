---
title: Code Map Pipeline
summary: Pipelines that derive agent-consumable artifacts from Kotlin source: a compressed code map and a Luminous graph of the interface seams
tags: [method, tooling, pipeline, code-map, luminous]
deps: [doc01.04.01, doc03.02]
---

# Code Map Pipeline

Source code is one of the two sources of truth ([[01-development-philosophy]]).
An agent — or a human — should be able to grasp the codebase without reading
every file. A **code map pipeline** derives that understanding as a build
artifact: never hand-written, regenerated from the code it describes.

This mirrors the statechart pipeline (`.luminous/`), which reads carta sidecars
and emits derived output into a gitignored `generated/` tree. The code map
pipeline reads Kotlin instead of sidecars.

## The compressed code map

`.luminous/code-map.pipeline.mjs` parses every `.kt` file under
`app/composeApp/src/` with tree-sitter-kotlin and emits a signature skeleton to
`.luminous/generated/code-map.md`. Run it with `make code-map` (or
`npm run code-map`).

The skeleton has bodies stripped:

- **Keep:** package, public/internal declarations (interfaces, classes,
  objects, `@Composable`s, ViewModels, data classes), their signatures,
  supertypes, member signatures, KDoc summaries.
- **Drop:** function bodies, imports, private declarations.

Because a declaration's signature slice includes its supertype list, the
interface seams read directly off the skeleton — `class OsmLocationProvider :
LocationProvider` next to `interface LocationProvider` — without a separate
analysis pass.

## The Luminous graph (not yet built)

A second emitter could share the same parse to render a `*.graph.json` +
`*.pack.json` pair for visual navigation: nodes for declarations, edges for
`contains`, `implements`, and `uses`. Its reason to exist is showing **usages
flowing through interfaces onto implementations** — a `uses` edge lands on an
interface, `implements` edges fan out to the realizations. A precise `uses`
graph wants type information, which the syntactic parse does not have; a
semantic tier (a KSP or compiler-based analyzer) would close that gap. None of
this is built — the code map already exposes the seams in skeleton form.
