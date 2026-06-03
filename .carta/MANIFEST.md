# .carta/ Manifest

Machine-readable index for AI navigation. Read this file first, then open only the docs relevant to your query.

**Retrieval strategy:** See doc00.04 for AI retrieval patterns.

## Column Definitions

- **Ref**: Cross-reference ID (`docXX.YY.ZZ`)
- **File**: Path relative to title directory
- **Summary**: One-line description for semantic matching
- **Tags**: Keywords for file-path→doc mapping
- **Deps**: Doc refs to check when this doc changes
- **Refs**: Reverse deps — docs that list this one in their Deps (computed automatically)
- **Attachments**: Non-md files sharing the doc's numeric prefix. Sidecar artifacts that travel with the doc during structural operations. Purely filesystem-derived; not a frontmatter field.

Orphaned attachments (non-md files with no corresponding root .md) are reported as warnings on stderr during regeneration and do not appear in this table.

## 00-codex — Codex

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc00.00 | `00-index.md` | Meta-documentation — how to read this workspace | index, meta | — | — | — |
| doc00.01 | `01-about.md` | Why this workspace exists, how to read it, two-sources-of-truth theory | docs, meta, theory | — | — | — |
| doc00.02 | `02-maintenance.md` | Doc lifecycle — unfolding philosophy, development loop, versioning, epochs | docs, maintenance, philosophy | — | — | — |
| doc00.03 | `03-conventions.md` | Cross-reference syntax, frontmatter schema, file naming, writing style | docs, conventions | — | doc01.04.02 | — |
| doc00.04 | `04-ai-retrieval.md` | How AI agents navigate this workspace — hierarchical retrieval, MANIFEST usage, token budgets | docs, ai, retrieval | — | — | — |

## 01-product — Product

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc01.00 | `00-index.md` | Product specs — what we're building and why | product, index | — | — | — |
| doc01.01 | `01-background-context.md` | Condensed research session: My Maps API, map tech, sync, location abstraction, competitive landscape, cold start | product, research, background | — | doc01.02, doc02.01 | — |
| doc01.02 | `02-use-cases.md` | User-mental-model walkthroughs: Drip Coffee ranking, NYT Top 100 import, Geo Diary | product, use-cases, ux | doc01.01 | doc01.03 | — |
| doc01.03 | `03-concepts.md` | Concept-driven design (Jackson): Collection, Location, Review, Map Overview, Location Provider | product, concepts, design | doc01.02 | doc01.04.01, doc02.02.00, doc02.02.01, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13, doc02.02.02.14, doc02.02.02.15, doc03.01, doc03.02.00, doc03.02.01, doc03.02.02 | json |
| doc01.05 | `05-cmp-composition-research.md` | Research session: how to design shared CMP surfaces that bend to context (mode parameter, sheet-not-route), with a Tier-1..4 audit of reference apps to compare against | product, research, cmp, patterns, references | doc02.01, doc02.02.01 | doc02.04 | — |

### Development Philosophy

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc01.04.00 | `04-development-philosophy/00-index.md` | How we work on RTP and the artifacts that support it — method, verification, derived code maps | product, philosophy, method, process | — | — | — |
| doc01.04.01 | `04-development-philosophy/01-development-philosophy.md` | How we work on RTP: two sources of truth, artifact chain, unfolding, concept-driven design, spec-before-code | product, philosophy, method, process | doc01.03 | doc01.04.03 | — |
| doc01.04.02 | `04-development-philosophy/02-verification-system.md` | How carta docs declare machine-checkable verifications; the verify.mjs harness and the screen-inventory check against the statechart | product, verification, coverage, process, tooling | doc02.02.01, doc02.02.02.00, doc00.03 | doc01.06.02, doc01.06.03 | — |
| doc01.04.03 | `04-development-philosophy/03-code-map-pipeline.md` | Pipelines that derive agent-consumable artifacts from Kotlin source: a compressed code map and a Luminous graph of the interface seams | method, tooling, pipeline, code-map, luminous | doc01.04.01, doc03.02 | — | — |

### Research Sessions

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc01.06.00 | `06-research-sessions/00-index.md` | Investigation logs that turn into reusable context — symptoms, diagnostic techniques, root causes, and the resulting fixes | research, debugging, index | — | — | — |
| doc01.06.01 | `06-research-sessions/01-ios-map-tap-latency.md` | Why a pin tap on iOS lags ~300ms before the sheet animates, how the gesture-recognizer cascade is diagnosed, and the runtime patch on MLNMapView that removes the delay | research, ios, maplibre, gesture, latency, debugging | doc02.01, doc03.04 | — | — |
| doc01.06.02 | `06-research-sessions/02-ui-behavioral-spec-patterns.md` | Survey of how production teams specify component-level UI behavior — per-flow statecharts, trace expect-tests, LTL-over-DOM invariants, schema-driven screens, preview-test pairing — and which layer fits next to RTP's carta sidecars and navigation statechart | research, ui, spec, statechart, behavior, verification | doc02.02.01, doc01.04.02 | doc01.06.03 | — |
| doc01.06.03 | `06-research-sessions/03-fact-data-verification.md` | Survey of risks and prior art for moving carta prose into machine-checkable fact data — state explosion, spec drift, coverage criteria, property-based testing, liveness gap, two-sources-of-truth wedge | research, verification, statechart, datalog, model-checking, property-based-testing | doc02.02.01, doc01.06.02, doc01.04.02 | — | — |

## 02-design — Design

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc02.00 | `00-index.md` |  |  | — | — | — |
| doc02.01 | `01-architecture.md` | Tech stack decisions: Compose Multiplatform + maplibre-compose for the map-first cross-platform app | design, architecture, stack, cmp, maplibre | doc01.01 | doc01.05, doc01.06.01, doc02.03, doc03.01, doc03.04, doc03.05 | — |
| doc02.03 | `03-theme-tokens.md` | Three-tier design token system (primitive, semantic, provider) for the CMP theme: RtpColors, RtpSpacing, RtpTypography and the RtpTheme accessor | design, theme, tokens, styling, cmp | doc02.01 | doc02.05.00 | — |
| doc02.04 | `04-surface-composition-rules.md` | Two design rules for how surfaces compose — same-surface-different-mode, overlay-surfaces-are-not-routes | design, interaction, composition, rules | doc02.02.01, doc01.05 | doc02.05.00 | — |

### Interaction

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc02.02.00 | `02-interaction/00-index.md` | Platform-agnostic UI design — surfaces, navigation graph, action coverage | design, interaction, index | doc01.03 | — | — |
| doc02.02.01 | `02-interaction/01-navigation.md` | Platform-agnostic surface graph as XState statechart; verifies every concept action has a UI affordance | design, interaction, navigation, statechart | doc01.03 | doc01.04.02, doc01.05, doc01.06.02, doc01.06.03, doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13, doc02.02.02.14, doc02.02.02.15, doc02.02.02.16, doc02.02.02.17, doc02.02.03, doc02.04, doc03.03 | statechart.json |
| doc02.02.02.00 | `02-interaction/02-screens/00-index.md` | Per-surface affordance inventories — regions, affordances, lists | design, interaction, screens, index | doc02.02.01 | doc01.04.02 | — |
| doc02.02.02.01 | `02-interaction/02-screens/01-collection-list.md` | Affordance inventory for the CollectionList surface — a tall bottom-sheet over MapOverview that browses Collections and filters the map | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.02 | `02-interaction/02-screens/02-collection-detail.md` | Affordance inventory for the CollectionDetail surface — Details section, sortable entry list | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.03 | `02-interaction/02-screens/03-settings.md` | Affordance inventory for the Settings surface — entry to provider config, sync and BYOK as stubs | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.04 | `02-interaction/02-screens/04-collection-entry-detail.md` | Affordance inventory for the CollectionEntryDetail surface — the (Location, Review) pair, reviewed/unreviewed | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.05 | `02-interaction/02-screens/05-location-draft.md` | Affordance inventory for the LocationDraftSheet surface — coordinates, nearby resolution candidates, keep-or-adopt | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.06 | `02-interaction/02-screens/06-review-form.md` | Affordance inventory for the ReviewForm surface — the template field set as editable inputs, save and cancel | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.07 | `02-interaction/02-screens/07-map-overview.md` | Affordance inventory for the MapOverview surface — the everything view: pins, collection filter, search, drop-pin | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.08 | `02-interaction/02-screens/08-collection-editor.md` | Affordance inventory for the CollectionEditor surface — Collection metadata and the Review template field set | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.09 | `02-interaction/02-screens/09-entry-drawer.md` | Affordance inventory for the EntrySheet surface — a bottom-sheet peek of one Collection Entry over MapOverview, with three pressable regions that hand off to the owning Collection, the full detail, and the Review form | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.10 | `02-interaction/02-screens/10-location-detail.md` | Affordance inventory for the LocationSheet surface — the skinny bottom-sheet peek of a Location, the Collection Entries that reference it, and the add-another-Entry entry point | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.11 | `02-interaction/02-screens/11-add-location-to-collection.md` | Affordance inventory for the AddLocationToCollection sheet — pick a Collection (pre-selected in add mode) or create a new one, hosted over MapOverview | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.12 | `02-interaction/02-screens/12-manage-providers.md` | Affordance inventory for the ManageProviders surface — current default header, list of provider rows that route to per-provider config | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.13 | `02-interaction/02-screens/13-provider-config.md` | Affordance inventory for the ProviderConfig surface — per-provider setup parameterized by provider-context, key entry for BYOK providers | design, interaction, screens | doc02.02.01, doc01.03, doc03.02.02 | — | inventory.json |
| doc02.02.02.14 | `02-interaction/02-screens/14-debug-settings.md` | Affordance inventory for the DebugSettings surface — developer-only tools reachable from Settings, outside the production product spec | design, interaction, screens, debug | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.15 | `02-interaction/02-screens/15-about.md` | Affordance inventory for the About surface — app blurb, repository link, entries to in-app license viewer and open-source attributions | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.16 | `02-interaction/02-screens/16-license-viewer.md` | Affordance inventory for the LicenseViewer surface — in-app scrollable view of the AGPLv3 LICENSE bundled with the app | design, interaction, screens | doc02.02.01 | — | inventory.json |
| doc02.02.02.17 | `02-interaction/02-screens/17-attributions.md` | Affordance inventory for the Attributions surface — open-source library list generated from the build's dependency graph | design, interaction, screens | doc02.02.01 | — | inventory.json |
| doc02.02.03 | `02-interaction/03-navigation-journeys.md` | User-intent navigation paths declared as event/target pairs; diffed against the statechart to surface chart-missing transitions, target mismatches, and undeclared screens | design, interaction, navigation, journeys, verification | doc02.02.01 | — | navigation.journeys.json |

### Visual Language

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc02.05.00 | `05-visual-language/00-index.md` | How RTP looks and feels on a screen — principles derived from a running grievance log, anchored to the broader UX canon | design, visual-language, index | doc02.03, doc02.04 | — | — |
| doc02.05.01 | `05-visual-language/01-grievance-log.md` | Raw observations of what is off in current RTP screens — the source material for visual-language principles | design, visual-language, grievances, log | doc02.05 | — | — |

## 03-system — System

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc03.00 | `00-index.md` | How RTP stores, reads, syncs, and wires itself together below and around the UI — the bridge from concepts to code | system, index | — | — | — |
| doc03.01 | `01-store-model.md` | Two-store persistence: local SQLCipher DB + sync replica; table schema, local-first write path, memoized overview projection, op-log | system, storage, sqlcipher, sync, schema | doc01.03, doc02.01 | doc03.02.00, doc03.02.01 | schema.sql |
| doc03.03 | `03-navigation-wiring.md` | How the platform-agnostic navigation statechart becomes a Compose Multiplatform NavHost — type-safe routes, back stack, per-route ViewModel scoping | system, navigation, cmp, wiring | doc02.02.01, doc03.02 | — | — |
| doc03.04 | `04-map-rendering.md` | The maplibre-compose source/layer split, the two source-kind shapes, and what crossing the native boundary implies for state updates | system, maplibre, rendering, source | doc02.01 | doc01.06.01, doc03.05 | — |
| doc03.05 | `05-pin-render-resilience.md` | The PinRenderController facade and the substrate choices (GeoJsonSource push, JsonString serialization, symbol-collision flags) that keep pins on screen when the native render path misbehaves | system, maplibre, rendering, resilience, controller | doc02.01, doc03.04 | — | — |

### Data Interfaces

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc03.02.00 | `02-data-interfaces/00-index.md` | Domain models and the data-layer interface inventory: typed repositories, op-log, overview projection, sync engine — the contract UI sessions build against | system, interfaces, repository, data, contract | doc01.03, doc03.01 | — | — |
| doc03.02.01 | `02-data-interfaces/01-location-repository.md` | The LocationRepository interface: identity lookup, upsert, merge — the persistence-shaped contract screens consume for Locations | system, interfaces, repository, data, contract, location | doc01.03, doc03.01, doc03.02 | — | — |
| doc03.02.02 | `02-data-interfaces/02-location-providers.md` | The LocationProvider seam: osm default backed by Photon (forward + reverse) endpoints, BYOK providers, per-provider caching rules, ODbL export obligations | system, providers, location, osm, odbl, licensing | doc01.03, doc03.02 | doc02.02.02.13 | — |

## Tag Index

Quick lookup for file-path→doc mapping:

| Tag | Relevant Docs |
|-----|---------------|
| `ai` | doc00.04 |
| `architecture` | doc02.01 |
| `background` | doc01.01 |
| `behavior` | doc01.06.02 |
| `cmp` | doc01.05, doc02.01, doc02.03, doc03.03 |
| `code-map` | doc01.04.03 |
| `composition` | doc02.04 |
| `concepts` | doc01.03 |
| `contract` | doc03.02.00, doc03.02.01 |
| `controller` | doc03.05 |
| `conventions` | doc00.03 |
| `coverage` | doc01.04.02 |
| `data` | doc03.02.00, doc03.02.01 |
| `datalog` | doc01.06.03 |
| `debug` | doc02.02.02.14 |
| `debugging` | doc01.06.00, doc01.06.01 |
| `design` | doc01.03, doc02.01, doc02.02.00, doc02.02.01, doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13, doc02.02.02.14, doc02.02.02.15, doc02.02.02.16, doc02.02.02.17, doc02.02.03, doc02.03, doc02.04, doc02.05.00, doc02.05.01 |
| `docs` | doc00.01, doc00.02, doc00.03, doc00.04 |
| `gesture` | doc01.06.01 |
| `grievances` | doc02.05.01 |
| `index` | doc00.00, doc01.00, doc01.06.00, doc02.02.00, doc02.02.02.00, doc02.05.00, doc03.00 |
| `interaction` | doc02.02.00, doc02.02.01, doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13, doc02.02.02.14, doc02.02.02.15, doc02.02.02.16, doc02.02.02.17, doc02.02.03, doc02.04 |
| `interfaces` | doc03.02.00, doc03.02.01 |
| `ios` | doc01.06.01 |
| `journeys` | doc02.02.03 |
| `latency` | doc01.06.01 |
| `licensing` | doc03.02.02 |
| `location` | doc03.02.01, doc03.02.02 |
| `log` | doc02.05.01 |
| `luminous` | doc01.04.03 |
| `maintenance` | doc00.02 |
| `maplibre` | doc01.06.01, doc02.01, doc03.04, doc03.05 |
| `meta` | doc00.00, doc00.01 |
| `method` | doc01.04.00, doc01.04.01, doc01.04.03 |
| `model-checking` | doc01.06.03 |
| `navigation` | doc02.02.01, doc02.02.03, doc03.03 |
| `odbl` | doc03.02.02 |
| `osm` | doc03.02.02 |
| `patterns` | doc01.05 |
| `philosophy` | doc00.02, doc01.04.00, doc01.04.01 |
| `pipeline` | doc01.04.03 |
| `process` | doc01.04.00, doc01.04.01, doc01.04.02 |
| `product` | doc01.00, doc01.01, doc01.02, doc01.03, doc01.04.00, doc01.04.01, doc01.04.02, doc01.05 |
| `property-based-testing` | doc01.06.03 |
| `providers` | doc03.02.02 |
| `references` | doc01.05 |
| `rendering` | doc03.04, doc03.05 |
| `repository` | doc03.02.00, doc03.02.01 |
| `research` | doc01.01, doc01.05, doc01.06.00, doc01.06.01, doc01.06.02, doc01.06.03 |
| `resilience` | doc03.05 |
| `retrieval` | doc00.04 |
| `rules` | doc02.04 |
| `schema` | doc03.01 |
| `screens` | doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13, doc02.02.02.14, doc02.02.02.15, doc02.02.02.16, doc02.02.02.17 |
| `source` | doc03.04 |
| `spec` | doc01.06.02 |
| `sqlcipher` | doc03.01 |
| `stack` | doc02.01 |
| `statechart` | doc01.06.02, doc01.06.03, doc02.02.01 |
| `storage` | doc03.01 |
| `styling` | doc02.03 |
| `sync` | doc03.01 |
| `system` | doc03.00, doc03.01, doc03.02.00, doc03.02.01, doc03.02.02, doc03.03, doc03.04, doc03.05 |
| `theme` | doc02.03 |
| `theory` | doc00.01 |
| `tokens` | doc02.03 |
| `tooling` | doc01.04.02, doc01.04.03 |
| `ui` | doc01.06.02 |
| `use-cases` | doc01.02 |
| `ux` | doc01.02 |
| `verification` | doc01.04.02, doc01.06.02, doc01.06.03, doc02.02.03 |
| `visual-language` | doc02.05.00, doc02.05.01 |
| `wiring` | doc03.03 |
