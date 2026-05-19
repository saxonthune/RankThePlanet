---
title: Product
summary: Product specs — what we're building and why
tags: [product, index]
deps: []
---

# Product

Specs that describe what RankThePlanet is for and how it behaves from a user's perspective. Architecture and implementation live in later groups, once the product surface stabilizes enough to design against.

## What belongs here

- The one-sentence purpose
- User-visible behaviors (lists, schemas, reviews, sharing)
- Background research that informs product decisions

## What does not

- Code structure, framework choice, file layout
- Database schema, sync protocol details
- Deployment / build / CI

These belong in a future system or operations group, created when the work demands them (see doc00.02).

## Contents

- doc01.01 — Background Context: condensed research session covering API constraints, map tech, sync model, location abstraction, competitive landscape, cold-start playbook.
- doc01.02 — Use Cases: user-mental-model walkthroughs (Drip Coffee ranking, NYT Top 100 import, Geo Diary).
- doc01.03 — Concepts: concept-driven design (Jackson) for Collection, Location, Review, Map Overview.
- doc01.04 — Development Philosophy: two sources of truth, artifact chain, unfolding, spec-before-code.
- doc01.05 — Verification System: how docs declare machine-checkable verifications; the verify.mjs harness and the screen-inventory check.
