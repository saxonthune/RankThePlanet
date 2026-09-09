---
title: Verification System
summary: How rhidoc docs declare machine-checkable verifications; the verify.mjs harness and the screen-inventory check against the statechart
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
- `against` — where the comparison data lives: a stable rhidoc doc ref (`against.doc`) and a join `key`. The ref survives moves, so the link does not rot.

Declaring the check beside the artifact keeps the two together: the inventory says "I implement the CollectionDetail surface, and here is the doc that defines what that surface may do."

## The harness — `verify.mjs`

`.rhidoc/verify.mjs` is run on bare `node`, no dependencies. It walks `.rhidoc/` (or a narrower path passed as an argument), collects every doc whose frontmatter carries a `verify` field, resolves each entry's `against` ref to a sibling sidecar, and dispatches on `kind` to the matching verifier. It exits non-zero if any check fails, so it doubles as a gate — a todotask verification block, a pre-merge step.

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
- **Forward check** — for an event-bearing transition whose event has a matching inventory affordance: every positive-polarity `has(k)` whose `k` is absent from the affordance's `reactsToContext` is a `key-disagreement` (strict — a missing or empty `reactsToContext` does not exempt the affordance); every positive-polarity `mode(m)` whose `m` is absent from the affordance's `appearsInModes` is a `mode-disagreement`. The `mode(m)` side stays permissive — an affordance with no `appearsInModes` still satisfies a `mode(m)` guard — because formally bridging modes back to context keys is deferred to a future kind that models `meta.modes` directly.
- **Inverse check** — an unguarded transition whose inventory affordance carries any `appearsInModes` or `reactsToContext` raises `missing-guard`. This is the drift this verifier exists to catch: the chart forgot to declare what the inventory already asserts.

Negative-polarity atoms (under an odd number of `not`s) participate in identifier validity but not in the forward inventory cross-check — `not(has(collection-context))` and `appearsInModes: ["browse"]` express the same fact via different vocabularies, and reconciling them requires the formal mode-model deferred above.

Transitions whose event has no inventory affordance (system-driven gestures, journey stubs, items in inventory `deferred` arrays) are skipped — the `screen-inventory` verifier owns the coverage check.

## The `invariant-resolution` verifier

Load-bearing assertions about a surface — *"typing in the search field while `search-context` is set fires `CLEAR_SEARCH`"*, *"`commit-search-to-map` renders only when at least one unadopted provider candidate is in the dropdown"* — sit in screen-description prose where nothing can reach them. A typo in a referenced event name, a renamed mode, or a deleted context key silently rots the prose. The `invariant-resolution` verifier lifts these assertions into a structured `invariants: []` array on inventory sidecars and resolves their references against the surface's local address space.

Each entry is `{id?, text, predicate?}`. `text` is free-form prose; symbol references are marked with backticks. The verifier scans every backtick-quoted token in each `text` field and resolves it against the union of: chart events (`state.on` keys), modes (`meta.modes` keys), context keys (`meta.context.owns` ∪ `meta.context.receives`), inventory region ids, affordance ids, list ids, and chart state ids (target surfaces). Dotted tokens like `` `search-context.viewportAtQuery` `` resolve the left side; the right side is unchecked at this layer — field-level resolution lands when context keys gain typed payloads. Tokens containing whitespace or starting with a quote are treated as prose and skipped — the cheap line between *symbol* and *prose phrase*. Issues: `missing-text`, `duplicate-id`, `unknown-reference`.

The optional `predicate` field carries a string in the Kind B guard grammar (`has(k)` / `mode(m)` / `eq(k.f, v)` with `not` / `and` / `or` — the same `parseGuard` consumed by `guard-coverage`). It states a *necessary precondition* for the invariant's positive case; the prose `text` refines with the consequence or the runtime-only part of the claim that the static grammar cannot express. The verifier parses the predicate and walks every leaf atom, validating `has`/`eq` keys against the surface's owns + receives keys and `mode` atoms against its declared modes. New issue kinds: `predicate-parse-error`, `predicate-unknown-key`, `predicate-unknown-mode`. The check is static-only — the predicate is not evaluated against generated transition sequences; that work is a future trace-runner kind under `:jvmTest`. Invariants whose claim resists the current atom vocabulary stay `text`-only and carry an optional `note` field documenting the gap (a candidate extension like `enabled(affordance-id)` or `fires(EVENT)`, or a multi-step temporal claim that waits for the runner). Reusing one parser across `guard-coverage`, `invariant-resolution`, and the eventual trace runner is the economy.

A doc opts in with `verify: [{"kind":"invariant-resolution","sidecar":"<inventory>","against":{"doc":"<chart-doc-ref>","key":"<surface-id>"}}]` — same shape as `screen-inventory` and `slot-coverage`.

## The `journeys-verify` verifier

The journey corpus ([[../../02-design/02-interaction/03-navigation-journeys]], doc02.02.03) declares user-intent paths as parallel `events[]` / `targets[]` arrays. `journeys-verify` diffs each step against the chart: for every `(from, event, claimedTarget)` triple along a journey, it reports `chart-missing` when `chart.states[from].on[event]` is absent, `target-mismatch` when the chart's transition target differs from `claimedTarget` (self-transitions fall back to `from`), and `unknown-surface` when the chart has no state for a named surface (including the journey's `start`). The verifier keeps walking after each divergence so a journey's full diff lands in one pass.

Divergences print as **warnings**: the run logs them and exits 0. Several existing journeys carry intentional `chart-missing` / `target-mismatch` entries as documentation of pending work (the chart-vs-journey reconciliation is editorial, not mechanical). Hardening to failing-mode waits on the chart catching up to those journeys — at which point the warnings should drop to zero and the threshold flips.

Alongside the diff, `journeys-verify` emits a **2-switch coverage** signal: every `(state, eventIn, eventOut)` triple in the chart is enumerated (every event reaching `state` paired with every event leaving it), the same shape is harvested from the journey corpus, and the difference is reported as a count plus a sample of up to twenty `uncovered-triple` warnings. Coverage is not a failing check — it is a backlog signal whose corpus today is far from saturation by design.

A doc opts in with `verify: [{"kind":"journeys-verify","sidecar":"<journeys-sidecar>"}]`. The sidecar's top-level `statechart` field names the sibling statechart sidecar; both verifiers below resolve it the same way.

## The `journey-trace` verifier

Where `journeys-verify` checks shape, `journey-trace` checks *context lifetime*. Each journey may carry an optional `expects: [{ afterEvent, active?, inactive?, note? }]` array, with `active` and `inactive` listing context-key names whose state is asserted at the step whose event slug matches `afterEvent`. The verifier walks the journey under a **host-stack derivation** and diffs the derived active set against each `expects` entry.

The derivation maintains a surface stack starting at `journey.start`, and at each step:

- Resolves the chart transition for `(from, event)`. A missing transition halts the journey with `trace-broken-by-chart` and the rest of the journey's expects are skipped.
- Activates every key whose owning surface declares `meta.context.owns[k].set` includes `"<from>.<event>"`.
- Deactivates every key in the transition's `clears` array. (`retains` is structural documentation; the derivation infers it.)
- Reshapes the stack against the target's modality: a sheet pushes over its declared `host`; a fullScreen target lower in the stack pops down to it (BACK-style return); any other fullScreen target replaces the entire stack and deactivates every key owned by an unmounted surface. Static map `ownedBy[k] → surface` (one-shot from `meta.context.owns`) drives the unmount cleanup.

The host-stack rule is the load-bearing piece: it catches multi-step context bugs the intra-state `context-chain` verifier cannot see by construction — most notably context owned by a sheet's host surviving past a sheet → fullScreen jump that leaves the host stack.

Issue kinds: `unknown-after-event` (anchor not in `events[]`), `ambiguous-after-event` (anchor appears more than once — split the journey or rephrase), `expected-active-missing`, `expected-inactive-present`, `derivation-error` (impossible stack — should never fire if `modality-host` is green), `trace-broken-by-chart` (chart cannot tell the derivation what comes next). Failing on any.

The verifier is static-only — predicates over `active` / `inactive` (e.g. `has(k)`, `and(…)`) and liveness assertions are deliberately deferred. Trace evaluation against generated sequences is the future Kind H runner under `:jvmTest`.

A doc opts in with `verify: [{"kind":"journey-trace","sidecar":"<journeys-sidecar>"}]`. The same sidecar typically opts into `journeys-verify` as well.

## The `generated-traces` verifier

Where `journey-trace` checks hand-authored intent paths, `generated-traces` is the adversarial reachability layer (doc01.06.03 §4). It walks **randomly generated** transition sequences from the chart's `initial` state, asserts safety invariants after every step, and shrinks failing sequences to a minimal counterexample. The shrink is what makes a 20-step crash debuggable.

The runner reuses `deriveActiveContext`'s host-stack semantics for the step function, plus a guard evaluator built over `parseGuard`/`walkGuardLeaves`. From the current state, the generator lists every `state.on` entry whose guard evaluates true against the current world (`active` set + per-surface mode) and picks one uniformly. Mode tracking uses event-name conventions — `ENTER_*_MODE` sets the top surface's mode, `EXIT_*_MODE` resets it to `browse`. `eq(k.f, v)` atoms are payload-shape and evaluate to false (under-generate rather than over-generate).

Sibling sheet → sibling sheet over the same host pops the source sheet before pushing the target — a small extension of `deriveActiveContext`'s strict push, matching doc02.04 Rule 2 (overlays are owned by the host's UiState, not stacked).

Two properties are asserted after every step:

- **owner-in-stack**: for every key `k` in `active`, the surface listed in `meta.context.owns[k]` is present in the current stack. Catches the search-context-survives-unmount class of bug.
- **derivation-error-free**: no overlay-with-absent-host or no-modality events accumulate.

On failure, the shrinker bisects the trace to the minimal failing prefix, then attempts to drop each remaining index. The output reports the shrunk event sequence plus the violation list.

A doc opts in with `verify: [{"kind":"generated-traces","sidecar":"<chart-sidecar>","traces":200,"length":20,"seed":1}]`. The runner is deterministic for a given seed.

## The `action-concept` verifier

A surface's affordances are tagged with `Concept.action` strings (`Collection.create`, `Location.resolve`, `MapOverview.pan`), and the chart's `meta.actions` carries the same vocabulary. The set of valid concept actions lives in prose on [[03-concepts]] (doc01.03). Without a join, typos in either tag (`Collecton.create`) and references to actions a concept never declared survive both review and the `screen-inventory` check — that verifier only cares whether the inventory covers the chart's transitions, not whether the action names them in agreement with the concept layer.

The `action-concept` verifier closes that gap. A flat sidecar attached to doc01.03 lists every concept action as data — `{"concept": "Collection", "action": "create"}` rows, plus a `skipNamespaces: ["Debug", "About"]` field declaring the closed set of non-concept namespaces (debug-only tools, surface-local affordances like `About.openRepository`) that may legitimately appear in tag strings without backing a concept.

For each `Concept.action` string in chart `meta.actions` and inventory `affordance.action`, the verifier splits on the first `.` and dispatches:

- **`namespace ∈ skipNamespaces`** — skip silently.
- **`namespace` is a concept and `action` is one of its rows** — record a reference.
- **`namespace` is a concept but `action` is not a row** — `phantom` issue (stale reference). Includes the source state-id or surface-id so reconciliation has a target.
- **`namespace` is neither a concept nor in the skip-list** — `unknown-namespace` issue. This is the typo-catcher.
- **string has no `.`** — `malformed` issue.

After the scan, every row in the sidecar that no chart or inventory referenced becomes an `orphan` issue: a concept action declared but unreachable from the UI — the gulf-of-execution catch named in doc01.06.03. Following the epic's permissive-first norm ([[fact-verification.epic]]), orphans surface as backlog signal but do not fail the run; only phantoms, unknown-namespace, and malformed entries are failing.

The verifier needs no `against` ref — it discovers the chart via `doc02.02.01` and walks every `*.inventory.json` under `.rhidoc/`. A doc opts in with `verify: [{"kind":"action-concept","sidecar":"03-concepts.json"}]`.

The sidecar does not capture action arity, parameter names, or types. Those are reserved for a future signature-verifier kind.

## How it grows

Each affordance in an inventory already pairs an action with a target, so the inventory is, in effect, an action inventory the verifier walks entry by entry. The system unfolds along two axes: new verifier `kind`s as other artifact pairs become worth checking, and stricter checks within `screen-inventory` as the inventory schema firms up. Neither is built before a concrete piece of work needs it.

### Candidate verifier kinds

The schema fields introduced for [[01-navigation]] (doc02.02.01) and [[00-index]] (doc02.02.02.00) — modality, host, modes, context, propagates, appearsInModes, reactsToContext — admit further mechanical consistency checks. Each is a one-pass walk of the statechart plus inventories.

- **`modality-host`** could check that every state with `modality != fullScreen` names a real `host`, and that every `host`'s `hostsSheets` matches its inbound sheet/drawer/overlay states.
- **`mode-coverage`** could check that every mode named in a state's `meta.modes` is referenced by at least one region or affordance in the inventory's `appearsInModes`.

A kind earns implementation when a concrete piece of work — typically the unfolding of a new surface — would benefit from the check.
