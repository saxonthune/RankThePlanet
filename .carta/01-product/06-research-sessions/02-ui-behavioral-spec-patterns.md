---
title: UI behavioral spec patterns
summary: Survey of how production teams specify component-level UI behavior — per-flow statecharts, trace expect-tests, LTL-over-DOM invariants, schema-driven screens, preview-test pairing — and which layer fits next to RTP's carta sidecars and navigation statechart
tags: [research, ui, spec, statechart, behavior, verification]
deps: [doc02.02.01, doc01.04.02]
---

# UI behavioral spec patterns

## What prompted this

RTP already maintains two spec layers under `.carta/`: per-screen affordance inventories ([[00-index]], doc02.02.02.00) and a surface-level XState navigation chart ([[01-navigation]], doc02.02.01) whose verifier ([[02-verification-system]], doc01.04.02) checks every concept action against a screen affordance. Neither layer reaches *inside* a screen. A field-editor bottom sheet was found to reuse its `remember`-backed local state across an "Add another" cycle — label, type dropdown choice, and dynamic options list all leaked from one virtual blank form to the next. The structural inventory listed every affordance correctly. The navigation chart treated the entire sheet as one node. Nothing in the workspace was positioned to catch a stale-state bug confined to a single composable.

The candidate is a behavioral spec layer between the inventory and the source — small enough to write per interactive screen, mechanical enough to verify. This doc surveys what production teams (not academic papers) use for that layer.

## Patterns observed in industry

### 1. Per-flow XState charts, scoped to one screen

Stately (the company that maintains XState) publishes case studies showing teams split their charts by scope: an app-level chart owns surfaces and long-lived actors, while each multi-step form, onboarding flow, or modal sheet gets its own statechart bound to one composable. The per-flow chart owns local field state, validation branches, and — crucially for the bug above — *explicit* final/reset states. "Add another" becomes a named transition whose target is a `reset` state with cleared context, not an implicit re-entry of `editing`.

What the artifact looks like: a `*.statechart.json` (or `.ts`) per interactive component, sibling to the component file. State node ids name UX-visible modes (`editing`, `submittingNew`, `reset`); transitions name user events (`ADD_ANOTHER`, `CONFIRM`, `CANCEL`); guards and actions reference symbolic operations that the runtime binds to real handlers.

### 2. Component-as-state-machine with trace expect-tests (Jane Street `bonsai` + `bonsai_test`)

Bonsai is a production OCaml UI library where every component is a pure state machine. `bonsai_test` drives the component programmatically and snapshots the resulting view tree; the committed snapshot file *is* the behavioral spec. A reset-leak bug shows up as a visible diff in the snapshot when a test scenario types into a field, fires `Add another`, and expects an empty render. The spec and the test are the same artifact, kept in source control next to the component.

What the artifact looks like: a `.ml` test file beside the component, plus a checked-in `.expected` snapshot of the rendered output trace per scenario.

### 3. LTL-over-DOM invariants (Quickstrom)

Quickstrom takes the opposite stance to a state-machine model: don't model the internal state at all, just write linear-temporal-logic propositions over what the DOM exposes. A spec for the field-editor bug would read approximately *"after the user activates the `Add another` affordance, every form input's observed value equals empty until the next `CONFIRM` or `CANCEL`."* The tool generates random user-action sequences and reports any sequence that falsifies the proposition.

What the artifact looks like: a `.spec.purs` file declaring `readyWhen` (a CSS selector for "page is loaded"), `actions` (clickable affordances), and `proposition` (an LTL formula over CSS queries). Implementation-agnostic by construction — the spec survives a rewrite of the component as long as the affordances keep their roles.

### 4. Flows-as-state-machines in internal tooling (Airbnb)

Airbnb's engineering blog on large-scale LLM test migration describes modeling each file's migration lifecycle as an explicit state machine with validated transitions (`draft → validating → applying → confirmed`, with named failure transitions). The pattern is not about UI per se, but the lesson transfers: when a user journey is a pipeline with multiple intermediate confirmations, encoding the transitions catches the missing-edge bugs that prose specs miss.

### 5. Schema-driven screens — structural, not behavioral (Salesforce Lightning, Retool, Forms.io)

These platforms ship a JSON schema per screen that declares components, data bindings, and validation rules. The schema is rich and runtime-interpreted, but it stops at structure: behavior — including "what resets when" — lives in imperative handlers attached to bindings. This is the layer RTP's carta inventory JSONs already occupy. The data point matters as a cautionary one: even a very detailed structural schema does not, on its own, prevent local-state leaks. The behavior layer is additive, not a refinement of the schema layer.

### 6. Preview + system-test pairing (GitHub Primer ViewComponents)

Each Primer component ships a *previews* file — named scenarios encoded as code (`empty_state`, `with_three_options`, `after_add_another`) — plus Capybara/Cuprite system tests that drive those previews in a headless browser. The named previews function as the de facto enumeration of the component's interesting states; the system tests are the executable behavioral contract. The discipline is bottom-up rather than top-down: states are discovered as scenarios are added.

## Best fit for RTP

The closest natural extension of the existing toolchain is **pattern 1 layered on the existing verifier**. Concretely: a `*.local.statechart.json` sidecar next to each interactive screen's inventory, scoped to that screen's *internal* state (e.g. for the field editor, `editing → committing → reset → editing`), parallel to but distinct from the surface-level chart in [[01-navigation]] (doc02.02.01). The verifier ([[02-verification-system]], doc01.04.02) extends to assert that every transition the inventory lists as an affordance has a corresponding event in the local chart, and that any "Add another"-style affordance targets a designated `reset` state — not an implicit re-entry. The bottom-sheet bug would have surfaced as a verifier failure: the inventory declared `addAnother` as an affordance, but the local chart would have had no `reset` target for it.

A lighter complement borrows pattern 3's style: a small `invariants: []` array inside the existing inventory JSON, each entry a short proposition (`"after addAnother, all field-config affordances observe empty value"`). No LTL engine required — these can be checked statically against the local chart by the same verifier, or eventually against a real runtime once Compose UI tests exist.

Pattern 2 (Bonsai-style trace tests) is the right *runtime* counterpart once Compose UI testing is wired, but it is downstream of the spec layer being designed here, not a substitute. Pattern 5 (purely structural schemas) is what the workspace already has and what failed to catch the bug — confirming the spec gap is at the behavioral layer, not the structural one.

## Sources

- Stately — TIDEFI case study (per-flow charts for multi-step forms): https://stately.ai/blog/2023-12-07-tidefi-and-stately-case-study
- Stately — Koordinates case study (scoping big problems into per-surface machines): https://stately.ai/blog/2023-11-28-koordinates-and-stately-case-study
- Jane Street Bonsai — components as composable state machines: https://opensource.janestreet.com/bonsai/
- `bonsai_test` — trace-driven expect tests as specs: https://github.com/janestreet/bonsai_test
- Quickstrom — *Specifying and Testing Web Applications* (LTL-over-DOM): https://owickstrom.github.io/specifying-and-testing-web-applications/
- Airbnb Tech Blog — Accelerating large-scale test migration with LLMs (flow-as-state-machine): https://medium.com/airbnb-engineering/accelerating-large-scale-test-migration-with-llms-9565c208023b
- Primer ViewComponents — previews guide: https://viewcomponent.org/guide/previews.html
- Salesforce Lightning Types UI Configuration — structural-only schema reference: https://developer.salesforce.com/docs/platform/lightning-types/guide/lightning-types-ui-config.html
