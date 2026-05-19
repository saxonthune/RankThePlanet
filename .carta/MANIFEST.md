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
| doc00.03 | `03-conventions.md` | Cross-reference syntax, frontmatter schema, file naming, writing style | docs, conventions | — | doc01.05 | — |
| doc00.04 | `04-ai-retrieval.md` | How AI agents navigate this workspace — hierarchical retrieval, MANIFEST usage, token budgets | docs, ai, retrieval | — | — | — |

## 01-product — Product

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc01.00 | `00-index.md` | Product specs — what we're building and why | product, index | — | — | — |
| doc01.01 | `01-background-context.md` | Condensed research session: My Maps API, map tech, sync, location abstraction, competitive landscape, cold start | product, research, background | — | doc01.02, doc02.01 | — |
| doc01.02 | `02-use-cases.md` | User-mental-model walkthroughs: Drip Coffee ranking, NYT Top 100 import, Geo Diary | product, use-cases, ux | doc01.01 | doc01.03 | — |
| doc01.03 | `03-concepts.md` | Concept-driven design (Jackson): Collection, Location, Review, Map Overview, Location Provider | product, concepts, design | doc01.02 | doc01.04, doc02.02.00, doc02.02.01, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc03.01, doc03.02 | — |
| doc01.04 | `04-development-philosophy.md` | How we work on RTP: two sources of truth, artifact chain, unfolding, concept-driven design, spec-before-code | product, philosophy, method, process | doc01.03 | — | — |
| doc01.05 | `05-verification-system.md` | How carta docs declare machine-checkable verifications; the verify.mjs harness and the screen-inventory check against the statechart | product, verification, coverage, process, tooling | doc02.02.01, doc02.02.02.00, doc00.03 | — | — |

## 02-design — Design

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc02.00 | `00-index.md` |  |  | — | — | — |
| doc02.01 | `01-architecture.md` | Tech stack decisions: Compose Multiplatform + maplibre-compose for the map-first cross-platform app | design, architecture, stack, cmp, maplibre | doc01.01 | doc02.03, doc03.01 | — |
| doc02.03 | `03-theme-tokens.md` | Three-tier design token system (primitive, semantic, provider) for the CMP theme: RtpColors, RtpSpacing, RtpTypography and the RtpTheme accessor | design, theme, tokens, styling, cmp | doc02.01 | — | — |

### Interaction

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc02.02.00 | `02-interaction/00-index.md` | Platform-agnostic UI design — surfaces, navigation graph, action coverage | design, interaction, index | doc01.03 | — | — |
| doc02.02.01 | `02-interaction/01-navigation.md` | Platform-agnostic surface graph as XState statechart; verifies every concept action has a UI affordance | design, interaction, navigation, statechart | doc01.03 | doc01.05, doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04 | statechart.json |
| doc02.02.02.00 | `02-interaction/02-screens/00-index.md` | Per-surface affordance inventories — regions, affordances, lists | design, interaction, screens, index | doc02.02.01 | doc01.05 | — |
| doc02.02.02.01 | `02-interaction/02-screens/01-collection-list.md` | Affordance inventory for the CollectionList surface — regions, affordances, lists | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.02 | `02-interaction/02-screens/02-collection-detail.md` | Affordance inventory for the CollectionDetail surface — Details section, sortable entry list | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.03 | `02-interaction/02-screens/03-settings.md` | Affordance inventory for the Settings surface — entry to provider config, sync and BYOK as stubs | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |
| doc02.02.02.04 | `02-interaction/02-screens/04-collection-entry-detail.md` | Affordance inventory for the CollectionEntryDetail surface — the (Location, Review) pair, reviewed/unreviewed | design, interaction, screens | doc02.02.01, doc01.03 | — | inventory.json |

## 03-system — System

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc03.00 | `00-index.md` | How RTP stores, reads, syncs, and structures its data — the bridge from concepts to code | system, index | — | — | — |
| doc03.01 | `01-store-model.md` | Two-store persistence: local SQLCipher DB + sync replica; table schema, local-first write path, memoized overview projection, op-log | system, storage, sqlcipher, sync, schema | doc01.03, doc02.01 | doc03.02 | schema.sql |
| doc03.02 | `02-data-interfaces.md` | Domain models and the data-layer interface inventory: typed repositories, op-log, overview projection, sync engine — the contract UI sessions build against | system, interfaces, repository, data, contract | doc01.03, doc03.01 | — | — |

## Tag Index

Quick lookup for file-path→doc mapping:

| Tag | Relevant Docs |
|-----|---------------|
| `ai` | doc00.04 |
| `architecture` | doc02.01 |
| `background` | doc01.01 |
| `cmp` | doc02.01, doc02.03 |
| `concepts` | doc01.03 |
| `contract` | doc03.02 |
| `conventions` | doc00.03 |
| `coverage` | doc01.05 |
| `data` | doc03.02 |
| `design` | doc01.03, doc02.01, doc02.02.00, doc02.02.01, doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04, doc02.03 |
| `docs` | doc00.01, doc00.02, doc00.03, doc00.04 |
| `index` | doc00.00, doc01.00, doc02.02.00, doc02.02.02.00, doc03.00 |
| `interaction` | doc02.02.00, doc02.02.01, doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04 |
| `interfaces` | doc03.02 |
| `maintenance` | doc00.02 |
| `maplibre` | doc02.01 |
| `meta` | doc00.00, doc00.01 |
| `method` | doc01.04 |
| `navigation` | doc02.02.01 |
| `philosophy` | doc00.02, doc01.04 |
| `process` | doc01.04, doc01.05 |
| `product` | doc01.00, doc01.01, doc01.02, doc01.03, doc01.04, doc01.05 |
| `repository` | doc03.02 |
| `research` | doc01.01 |
| `retrieval` | doc00.04 |
| `schema` | doc03.01 |
| `screens` | doc02.02.02.00, doc02.02.02.01, doc02.02.02.02, doc02.02.02.03, doc02.02.02.04 |
| `sqlcipher` | doc03.01 |
| `stack` | doc02.01 |
| `statechart` | doc02.02.01 |
| `storage` | doc03.01 |
| `styling` | doc02.03 |
| `sync` | doc03.01 |
| `system` | doc03.00, doc03.01, doc03.02 |
| `theme` | doc02.03 |
| `theory` | doc00.01 |
| `tokens` | doc02.03 |
| `tooling` | doc01.05 |
| `use-cases` | doc01.02 |
| `ux` | doc01.02 |
| `verification` | doc01.05 |
