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
| doc01.03 | `03-concepts.md` | Concept-driven design (Jackson): Collection, Location, Review, Map Overview, Location Provider | product, concepts, design | doc01.02 | doc01.04.01, doc02.02.00, doc02.02.01, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13, doc03.01, doc03.02.00, doc03.02.01, doc03.02.02 | — |
| doc01.05 | `05-cmp-composition-research.md` | Research session: how to design shared CMP surfaces that bend to context (mode parameter, sheet-not-route), with a Tier-1..4 audit of reference apps to compare against | product, research, cmp, patterns, references | doc02.01, doc02.02.01 | doc02.04 | — |

### Development Philosophy

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc01.04.00 | `04-development-philosophy/00-index.md` | How we work on RTP and the artifacts that support it — method, verification, derived code maps | product, philosophy, method, process | — | — | — |
| doc01.04.01 | `04-development-philosophy/01-development-philosophy.md` | How we work on RTP: two sources of truth, artifact chain, unfolding, concept-driven design, spec-before-code | product, philosophy, method, process | doc01.03 | doc01.04.03 | — |
| doc01.04.02 | `04-development-philosophy/02-verification-system.md` | How carta docs declare machine-checkable verifications; the verify.mjs harness and the screen-inventory check against the statechart | product, verification, coverage, process, tooling | doc02.02.01, doc02.02.02.00, doc00.03 | — | — |
| doc01.04.03 | `04-development-philosophy/03-code-map-pipeline.md` | Pipelines that derive agent-consumable artifacts from Kotlin source: a compressed code map and a Luminous graph of the interface seams | method, tooling, pipeline, code-map, luminous | doc01.04.01, doc03.02 | — | — |

## 02-design — Design

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc02.00 | `00-index.md` |  |  | — | — | — |
| doc02.01 | `01-architecture.md` | Tech stack decisions: Compose Multiplatform + maplibre-compose for the map-first cross-platform app | design, architecture, stack, cmp, maplibre | doc01.01 | doc01.05, doc02.03, doc03.01 | — |
| doc02.03 | `03-theme-tokens.md` | Three-tier design token system (primitive, semantic, provider) for the CMP theme: RtpColors, RtpSpacing, RtpTypography and the RtpTheme accessor | design, theme, tokens, styling, cmp | doc02.01 | — | — |
| doc02.04 | `04-surface-composition-rules.md` | Two design rules for how surfaces compose — same-surface-different-mode, overlay-surfaces-are-not-routes | design, interaction, composition, rules | doc02.02.01, doc01.05 | — | — |

### Interaction

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc02.02.00 | `02-interaction/00-index.md` | Platform-agnostic UI design — surfaces, navigation graph, action coverage | design, interaction, index | doc01.03 | — | — |
| doc02.02.01 | `02-interaction/01-navigation.md` | Platform-agnostic surface graph as XState statechart; verifies every concept action has a UI affordance | design, interaction, navigation, statechart | doc01.03 | doc01.04.02, doc01.05, doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13, doc02.02.03, doc02.04, doc03.03 | statechart.json |
| doc02.02.02.00 | `02-interaction/02-screens/00-index.md` | Per-surface affordance inventories — regions, affordances, lists | design, interaction, screens, index | doc02.02.01 | doc01.04.02 | — |
| doc02.02.02.01 | `02-interaction/02-screens/01-collection-list.md` | Affordance inventory for the CollectionList surface — regions, affordances, lists | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
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
| doc02.02.03 | `02-interaction/03-navigation-journeys.md` | User-intent navigation paths declared as event/target pairs; diffed against the statechart to surface chart-missing transitions, target mismatches, and undeclared screens | design, interaction, navigation, journeys, verification | doc02.02.01 | — | navigation.journeys.json |

## 03-system — System

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc03.00 | `00-index.md` | How RTP stores, reads, syncs, and wires itself together below and around the UI — the bridge from concepts to code | system, index | — | — | — |
| doc03.01 | `01-store-model.md` | Two-store persistence: local SQLCipher DB + sync replica; table schema, local-first write path, memoized overview projection, op-log | system, storage, sqlcipher, sync, schema | doc01.03, doc02.01 | doc03.02.00, doc03.02.01 | schema.sql |
| doc03.03 | `03-navigation-wiring.md` | How the platform-agnostic navigation statechart becomes a Compose Multiplatform NavHost — type-safe routes, back stack, per-route ViewModel scoping | system, navigation, cmp, wiring | doc02.02.01, doc03.02 | — | — |

### Data Interfaces

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc03.02.00 | `02-data-interfaces/00-index.md` | Domain models and the data-layer interface inventory: typed repositories, op-log, overview projection, sync engine — the contract UI sessions build against | system, interfaces, repository, data, contract | doc01.03, doc03.01 | — | — |
| doc03.02.01 | `02-data-interfaces/01-location-repository.md` | The LocationRepository interface: identity lookup, upsert, merge — the persistence-shaped contract screens consume for Locations | system, interfaces, repository, data, contract, location | doc01.03, doc03.01, doc03.02 | — | — |
| doc03.02.02 | `02-data-interfaces/02-location-providers.md` | The LocationProvider seam: osm default backed by Photon + Nominatim endpoints, BYOK providers, per-provider caching rules, ODbL export obligations | system, providers, location, osm, odbl, licensing | doc01.03, doc03.02 | doc02.02.02.13 | — |

## Tag Index

Quick lookup for file-path→doc mapping:

| Tag | Relevant Docs |
|-----|---------------|
| `ai` | doc00.04 |
| `architecture` | doc02.01 |
| `background` | doc01.01 |
| `cmp` | doc01.05, doc02.01, doc02.03, doc03.03 |
| `code-map` | doc01.04.03 |
| `composition` | doc02.04 |
| `concepts` | doc01.03 |
| `contract` | doc03.02.00, doc03.02.01 |
| `conventions` | doc00.03 |
| `coverage` | doc01.04.02 |
| `data` | doc03.02.00, doc03.02.01 |
| `design` | doc01.03, doc02.01, doc02.02.00, doc02.02.01, doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13, doc02.02.03, doc02.03, doc02.04 |
| `docs` | doc00.01, doc00.02, doc00.03, doc00.04 |
| `index` | doc00.00, doc01.00, doc02.02.00, doc02.02.02.00, doc03.00 |
| `interaction` | doc02.02.00, doc02.02.01, doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13, doc02.02.03, doc02.04 |
| `interfaces` | doc03.02.00, doc03.02.01 |
| `journeys` | doc02.02.03 |
| `licensing` | doc03.02.02 |
| `location` | doc03.02.01, doc03.02.02 |
| `luminous` | doc01.04.03 |
| `maintenance` | doc00.02 |
| `maplibre` | doc02.01 |
| `meta` | doc00.00, doc00.01 |
| `method` | doc01.04.00, doc01.04.01, doc01.04.03 |
| `navigation` | doc02.02.01, doc02.02.03, doc03.03 |
| `odbl` | doc03.02.02 |
| `osm` | doc03.02.02 |
| `patterns` | doc01.05 |
| `philosophy` | doc00.02, doc01.04.00, doc01.04.01 |
| `pipeline` | doc01.04.03 |
| `process` | doc01.04.00, doc01.04.01, doc01.04.02 |
| `product` | doc01.00, doc01.01, doc01.02, doc01.03, doc01.04.00, doc01.04.01, doc01.04.02, doc01.05 |
| `providers` | doc03.02.02 |
| `references` | doc01.05 |
| `repository` | doc03.02.00, doc03.02.01 |
| `research` | doc01.01, doc01.05 |
| `retrieval` | doc00.04 |
| `rules` | doc02.04 |
| `schema` | doc03.01 |
| `screens` | doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.02.02.05, doc02.02.02.06, doc02.02.02.07, doc02.02.02.08, doc02.02.02.09, doc02.02.02.10, doc02.02.02.11, doc02.02.02.12, doc02.02.02.13 |
| `sqlcipher` | doc03.01 |
| `stack` | doc02.01 |
| `statechart` | doc02.02.01 |
| `storage` | doc03.01 |
| `styling` | doc02.03 |
| `sync` | doc03.01 |
| `system` | doc03.00, doc03.01, doc03.02.00, doc03.02.01, doc03.02.02, doc03.03 |
| `theme` | doc02.03 |
| `theory` | doc00.01 |
| `tokens` | doc02.03 |
| `tooling` | doc01.04.02, doc01.04.03 |
| `use-cases` | doc01.02 |
| `ux` | doc01.02 |
| `verification` | doc01.04.02, doc02.02.03 |
| `wiring` | doc03.03 |
