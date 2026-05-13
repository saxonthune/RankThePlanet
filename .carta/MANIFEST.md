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
| doc00.03 | `03-conventions.md` | Cross-reference syntax, frontmatter schema, file naming, writing style | docs, conventions | — | — | — |
| doc00.04 | `04-ai-retrieval.md` | How AI agents navigate this workspace — hierarchical retrieval, MANIFEST usage, token budgets | docs, ai, retrieval | — | — | — |

## 01-product — Product

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc01.00 | `00-index.md` | Product specs — what we're building and why | product, index | — | — | — |
| doc01.01 | `01-background-context.md` | Condensed research session: My Maps API, map tech, sync, location abstraction, competitive landscape, cold start | product, research, background | — | doc02.01 | — |

## 02-design — Design

| Ref | File | Summary | Tags | Deps | Refs | Attachments |
|-----|------|---------|------|------|------|-------------|

| doc02.00 | `00-index.md` |  |  | — | — | — |
| doc02.01 | `01-architecture.md` | Tech stack decisions: Compose Multiplatform + maplibre-compose for the map-first cross-platform app | design, architecture, stack, cmp, maplibre | doc01.01 | — | — |

## Tag Index

Quick lookup for file-path→doc mapping:

| Tag | Relevant Docs |
|-----|---------------|
| `ai` | doc00.04 |
| `architecture` | doc02.01 |
| `background` | doc01.01 |
| `cmp` | doc02.01 |
| `conventions` | doc00.03 |
| `design` | doc02.01 |
| `docs` | doc00.01, doc00.02, doc00.03, doc00.04 |
| `index` | doc00.00, doc01.00 |
| `maintenance` | doc00.02 |
| `maplibre` | doc02.01 |
| `meta` | doc00.00, doc00.01 |
| `philosophy` | doc00.02 |
| `product` | doc01.00, doc01.01 |
| `research` | doc01.01 |
| `retrieval` | doc00.04 |
| `stack` | doc02.01 |
| `theory` | doc00.01 |
