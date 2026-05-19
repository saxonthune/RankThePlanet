---
title: Development Philosophy
summary: How we work on RTP: two sources of truth, artifact chain, unfolding, concept-driven design, spec-before-code
tags: [product, philosophy, method, process]
deps: [doc01.03]
---

# Development Philosophy

How we develop RTP. Workspace conventions live in [[03-conventions]] (doc00.03); doc lifecycle in [[02-maintenance]] (doc00.02); concept-design specifics in [[03-concepts]] (doc01.03). This doc is the connective tissue.

## Two sources of truth

There are exactly two sources of truth: **product expectations** (what we want, unreal) and **source code** (what runs, real). Everything else — this workspace included — is an *artifact* that bridges them. The team's purpose is to reconcile the two: change code to realize expectations, or change expectations to match what's been learned.

Without product expectations, there is no such thing as a bug. Without source code, there is no product.

## The artifact chain

We build a procession of artifacts where each reduces residual uncertainty for the next:

```
concepts → use cases → interaction surfaces → system specs → source code
```

By the time we write code, the question "what should this do?" should already be answered upstream. Code is a mechanical translation, not an act of invention. When code feels inventive, an upstream artifact is missing.

The current chain (numbering is for navigation, not dependency order — `deps:` carries the real graph):

- **`01-product/`** — concepts, use cases, this doc, background research. *What and why.*
- **`02-design/`** — framework and library decisions. *What tech.*
- **`03-interaction/`** *(planned)* — surfaces, navigation, action coverage. Platform-agnostic. *How the user reaches every action.*
- **`04-system/`** *(planned)* — state tiers, contracts, screens-as-Composables, component tree. CMP-bound. *How the code is partitioned.*
- **`src/`** *(planned)* — derived from the above.

## Unfolding

Docs (and code, when it arrives) **unfold**: start sparse, grow only what the next concrete piece of work demands. A one-line doc is a finished doc until someone needs more. Full statement in [[02-maintenance]].

This rules out:

- Enumerating edge cases before the happy path exists.
- Scaffolding empty groups for "future architecture."
- Inventing content to fill thin docs.
- Pre-elaborating concepts to seem complete.

It also rules in:

- Ship the minimum that lets the next decision be made, then stop.
- Trust that future-you will add detail when the work demands it.
- Treat brevity as a feature, not a defect.

## Concept-driven design

Following Jackson, RTP is a composition of **concepts** — each freestanding, with a single purpose, state, actions, and operational principle. Concepts compose by **synchronization**, never by inheritance or shared internals. Current concepts: Collection, Location, Review, Map Overview ([[03-concepts]]).

A new concept is justified only by a distinct user-facing purpose. Internal mechanisms are not concepts. The bar for adding one is the same as the bar for unfolding a doc: the work must demand it.

## Don't simulate the domain

Software is a composition of artifacts that authorize actions and produce side effects, not a model of the real world. We do not write `Kitchen.prepareBurger()`-style classes. Locations are Locations because the user refers to them — not because the real world has places. The Review concept exists because the user fills out evaluations — not because reviews are a thing in restaurants.

Dollhouse design begets dollhouse code, which begets more dollhouse design. AI agents in particular mimic surrounding code; clean artifact-driven specs are a defensive strategy that lengthens the time between human interventions.

## Spec-before-code

Behavior changes begin with the spec, not the code. The flow:

1. Edit the relevant concept doc and/or its sidecar.
2. Mirror the edit downstream — interaction graph, system contracts, UiState — until the change reaches code.
3. Add or update the verification artifact (action-coverage check, property test) in the same change set.

If you find yourself editing code without an upstream spec change, either the spec is wrong (fix it first) or the change is purely mechanical refactor (rare, and still worth a note).

## Sidecars live next to host docs

State machines, JSON Schemas, and other machine-readable artifacts live as **sidecars** in the same carta bundle as their host `.md` file (`02-navigation.md` + `02-navigation.json`). Carta's structural ops (move, rename, punch, flatten) treat the bundle as a unit. Never create a `.statemachines/` or `.schemas/` directory.

This matters because: an AI agent reading the host doc sees the sidecar in the same neighborhood and treats them as one artifact; structural refactors don't break references; humans editing the spec edit the JSON in the same PR.

## Verification artifacts are first-class

Each layer ships a verification artifact alongside its descriptive content:

- **Concepts:** operational principles serve as informal acceptance scenarios.
- **Interaction:** action-coverage matrix — every concept action must have at least one UI affordance. Statechart with `concept.action` tags makes this checkable by a script.
- **System:** repository interface contracts — refactorability test is "could I swap persistence without changing a composable?"
- **Code:** property tests with acceptance IDs (COLL-*, LOC-*, REV-*, MAP-*).

A change that breaks a verification artifact is a real change, not a regression in the artifact. Update both together.

## Tooling philosophy

- **Source of truth is plain text** — Markdown for prose, JSON for structured data. Both are AI-writeable, diffable, and version-controllable.
- **Viewers are interchangeable.** A statechart JSON sidecar can be rendered by Stately Studio, by a self-hosted react-flow viewer, or by Mermaid. The data outlives the viewer.
- **Verification scripts are small and standalone.** A coverage check or contract diff should be ~50 lines, runnable from CLI, suitable for CI.
- **Carta is the substrate.** Doc structure operations always go through `carta` — never hand-edit numbering or move files manually. See [[ai-retrieval]] (doc00.04) for the AI navigation patterns.

## When in doubt

- Read [[MANIFEST]] first, fetch only what's relevant.
- Ask: what's the smallest change that lets the next decision be made?
- If a concept gets harder to explain, it's probably overloaded — split it.
- If a layer feels like ceremony, it probably is — collapse it.
- Always cite an upstream artifact when proposing a downstream change.
