---
title: Verification System
summary: How carta docs declare machine-checkable verifications; the verify.mjs harness and the screen-inventory check against the statechart
tags: [product, verification, coverage, process, tooling]
deps: [doc02.02.01, doc02.02.02.00, doc00.03]
---

# Verification System

The artifact chain ([[01-about]], doc00.01) only holds if each artifact stays consistent with the one upstream of it. Some of those consistencies are mechanical — a screen's affordance inventory ([[00-index]], doc02.02.02.00) should never name an action the navigation statechart ([[01-navigation]], doc02.02.01) does not grant that surface. The verification system makes such consistencies machine-checkable: a doc declares what should be true, and a script proves it still is.

## How a doc declares a verification

A doc opts in through a `verify` field in its frontmatter ([[03-conventions]], doc00.03). The value is an inline-JSON array — valid YAML, and parseable without a YAML library — with one entry per check:

```yaml
verify: [{"kind":"screen-inventory","sidecar":"02-collection-detail.inventory.json","against":{"doc":"doc02.02.01","key":"CollectionDetail"}}]
```

- `kind` — which verifier runs. The verifier set is closed and small; it grows as new kinds of consistency become worth enforcing.
- `sidecar` — the file in the doc's own bundle that holds the data being checked. A doc may have several sidecars, so the entry names one explicitly.
- `against` — where the comparison data lives: a stable carta doc ref (`against.doc`) and a join `key`. The ref survives moves, so the link does not rot.

Declaring the check beside the artifact keeps the two together: the inventory says "I implement the CollectionDetail surface, and here is the doc that defines what that surface may do."

## The harness — `verify.mjs`

`.carta/verify.mjs` is run on bare `node`, no dependencies. It walks `.carta/` (or a narrower path passed as an argument), collects every doc whose frontmatter carries a `verify` field, resolves each entry's `against` ref to a sibling sidecar, and dispatches on `kind` to the matching verifier. It exits non-zero if any check fails, so it doubles as a gate — a todotask verification block, a pre-merge step.

The harness is deliberately thin: discover, dispatch, report. Verifiers are hardcoded, one per `kind`. No plugin system until a second category of check earns one.

## The `screen-inventory` verifier

The first — and for now only — verifier checks a screen's affordance inventory against its surface in the navigation statechart. It locates the statechart state whose `meta.surface` equals the entry's `key`, then compares:

- **Missing** — a transition or `meta.actions` entry the statechart grants the surface that no inventory affordance covers. This is the gulf-of-execution catch: the design promises a gesture the screen never offers.
- **Phantom** — an inventory `event` or `action` the statechart has no record of. Catches typos and references left stale by a statechart edit.
- **Target mismatch** — an affordance whose `target` disagrees with the statechart transition's `target`.

### Acknowledged gaps — `deferred`

A surface is rarely inventoried all at once. An inventory may carry a `deferred` array — statechart events and actions it knowingly has not covered yet. Deferred items are not reported as *missing*; instead their count is surfaced as a backlog metric. The distinction matters: a gap nobody chose is a defect, a gap on the `deferred` list is a plan. Phantoms are never deferrable — a reference to something that does not exist is always wrong.

## How it grows

Each affordance in an inventory already pairs an action with a target, so the inventory is, in effect, an action inventory the verifier walks entry by entry. The system unfolds along two axes: new verifier `kind`s as other artifact pairs become worth checking, and stricter checks within `screen-inventory` as the inventory schema firms up. Neither is built before a concrete piece of work needs it.

### Candidate verifier kinds

The schema fields introduced for [[01-navigation]] (doc02.02.01) and [[00-index]] (doc02.02.02.00) — modality, host, modes, context, propagates, appearsInModes, reactsToContext — admit further mechanical consistency checks. Each is a one-pass walk of the statechart plus inventories.

- **`modality-host`** could check that every state with `modality != fullScreen` names a real `host`, and that every `host`'s `hostsSheets` matches its inbound sheet/drawer/overlay states.
- **`context-chain`** could check that every key in a state's `meta.context` is either consumed (named by an affordance's `appearsInModes` or `reactsToContext` in the inventory, or in `meta.actions`) or forwarded by an outgoing transition's `propagates`. Catches the silent-drop failure named in [[04-surface-composition-rules]] (doc02.04).
- **`mode-coverage`** could check that every mode named in a state's `meta.modes` is referenced by at least one region or affordance in the inventory's `appearsInModes`.
- **`action-concept`** could diff `Concept.action` strings against doc01.03's concept-action lists to catch orphan actions (gulf of execution) and phantom tags (stale concept reference).

A kind earns implementation when a concrete piece of work — typically the unfolding of a new surface — would benefit from the check.
