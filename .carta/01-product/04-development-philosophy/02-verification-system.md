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

## The `context-chain` verifier

The second verifier addresses a class of bug the screen-inventory check is blind to: a context key whose lifetime is described in prose but never declared as a fact, so a transition that ought to drop it instead silently retains it. Motivated by the *Search this area → switch Collection* case named in [[03-fact-data-verification]] (doc01.06.03).

`meta.context` on a state splits into two structural fields rather than a scope enum:

- **`owns`** — keys whose lifetime this state controls. Each owned key declares a `set: [<State>.<EVENT>, …]` array naming the transitions that establish it, and a one-line `description`.
- **`receives`** — keys forwarded into this state by an inbound transition's `propagates`. Read-only here; lifetime managed at the owner.

Every outbound transition on a state that owns context declares, per owned key, **`clears: [keys]`** (the key is dropped on this transition) or **`retains: [keys]`** (the key survives). Both is an error; neither is an error. Presence-only — the verifier reasons about whether a key is set, not what payload it carries (doc01.06.03 §1).

The verifier walks every owner state in the chart, evaluates each outbound transition against the owner's `owns` set, and batch-reports unclassified, both-listed, and unknown-key entries grouped by state. A `verify:` entry of kind `context-chain` on the chart doc activates it; no separate `against` ref because the verifier reads the chart sidecar directly.

Safety only by design — `clears` / `retains` cannot express liveness (something good eventually happens). Liveness invariants are a candidate future verifier kind, scoped separately in doc01.06.03 §5.

## The `guard-coverage` verifier

A transition's *liveness condition* — when the affordance is enabled — is asserted twice today: as prose on the transition (*"Live only when `filter-context` is set"*) and as structural fields on the inventory affordance (`appearsInModes`, `reactsToContext`). The two assertions can drift independently. The `guard-coverage` verifier replaces the prose with a typed `guard` field on the transition and cross-checks the chart side against the inventory side.

### The predicate grammar

A `guard` is a string in a small s-expression-shaped grammar:

```
predicate := atom | "not(" predicate ")"
           | "and(" predicate ("," predicate)+ ")"
           | "or("  predicate ("," predicate)+ ")"
atom      := "has(" context-key ")"
           | "mode(" mode-name ")"
           | "eq("  context-key "." field "," literal ")"
literal   := double-quoted string | integer | "true" | "false"
```

- `has(k)` — context key `k` is set on the surface. The primitive: `has(collection-context)` reads "the addToCollection mode is active" because that mode is *defined* as "collection-context is set."
- `mode(m)` — the surface is in named mode `m`. Used where a mode is not backed by a context key (e.g., `CollectionList`'s `selection` mode is an in-sheet toggle, not a carried value).
- `eq(k.field, v)` — payload field equality. Parses and identifier-validates in this phase; no caller in the chart today, but the grammar is shared with the invariants kind (Kind F).
- `not / and / or` — boolean composition, explicit (no operator precedence).

Preference: `has()` is canonical over `mode()` when both express the same fact, because mode definitions in `meta.modes` are themselves stated as "X-context is set" — `has()` is the primitive, `mode()` is sugar.

### What the verifier checks

For each transition in the chart sidecar:

- **Parse** — the `guard` string parses; otherwise a `parse-error` issue names the offending substring and offset.
- **Identifier validity** — every `has(k)` / `eq(k.f, v)` references a key in the surface's `meta.context.owns` ∪ `meta.context.receives`; every `mode(m)` references a name in the surface's `meta.modes`.
- **Forward check** (permissive) — for an event-bearing transition whose event has a matching inventory affordance: every positive-polarity `has(k)` whose `k` is absent from the affordance's `reactsToContext` is a `key-disagreement`; every positive-polarity `mode(m)` whose `m` is absent from the affordance's `appearsInModes` is a `mode-disagreement`. Permissive means an affordance that uses `appearsInModes` to encode a context-backed mode (and omits `reactsToContext`) still satisfies a `has(k)` guard — bridging the two vocabularies is deferred to a future kind that models `meta.modes` formally.
- **Inverse check** — an unguarded transition whose inventory affordance carries any `appearsInModes` or `reactsToContext` raises `missing-guard`. This is the drift this verifier exists to catch: the chart forgot to declare what the inventory already asserts.

Negative-polarity atoms (under an odd number of `not`s) participate in identifier validity but not in the forward inventory cross-check — `not(has(collection-context))` and `appearsInModes: ["browse"]` express the same fact via different vocabularies, and reconciling them requires the formal mode-model deferred above.

Transitions whose event has no inventory affordance (system-driven gestures, journey stubs, items in inventory `deferred` arrays) are skipped — the `screen-inventory` verifier owns the coverage check.

## How it grows

Each affordance in an inventory already pairs an action with a target, so the inventory is, in effect, an action inventory the verifier walks entry by entry. The system unfolds along two axes: new verifier `kind`s as other artifact pairs become worth checking, and stricter checks within `screen-inventory` as the inventory schema firms up. Neither is built before a concrete piece of work needs it.

### Candidate verifier kinds

The schema fields introduced for [[01-navigation]] (doc02.02.01) and [[00-index]] (doc02.02.02.00) — modality, host, modes, context, propagates, appearsInModes, reactsToContext — admit further mechanical consistency checks. Each is a one-pass walk of the statechart plus inventories.

- **`modality-host`** could check that every state with `modality != fullScreen` names a real `host`, and that every `host`'s `hostsSheets` matches its inbound sheet/drawer/overlay states.
- **`mode-coverage`** could check that every mode named in a state's `meta.modes` is referenced by at least one region or affordance in the inventory's `appearsInModes`.
- **`action-concept`** could diff `Concept.action` strings against doc01.03's concept-action lists to catch orphan actions (gulf of execution) and phantom tags (stale concept reference).

A kind earns implementation when a concrete piece of work — typically the unfolding of a new surface — would benefit from the check.
