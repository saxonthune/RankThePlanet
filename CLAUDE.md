# CLAUDE.md — RankThePlanet

Free, fast, open-source geo diary. User-curated lists of places with per-list review schemas.

**RTP** = RankThePlanet. Use the abbreviation freely in conversation and docs.

## First action, every session

**Before answering any product, design, or scaffolding question, read `.carta/MANIFEST.md` and any docs whose summary/tags match the task.** Prior decisions and research live there. Do not propose technology, architecture, or behavior without first checking what the workspace has already converged on. This rule outranks brevity — a fast answer that contradicts existing docs is worse than a slow one that aligns with them.

## Working philosophy: unfolding design

Docs (and code, when it arrives) **unfold**. Start sparse. Grow only what the next concrete piece of work demands. A one-line doc is a finished doc until someone needs more from it.

Inspirations:

- `.carta/00-codex/02-maintenance.md` — the local statement of unfolding for this workspace.
- The artifact chain: each spec reduces residual entropy for the one after it. By the time you write code, the question "what should this do?" should already be answered upstream.
- Two sources of truth (product expectations vs. source code). Everything else is an artifact bridging them; there is no third source. Avoid simulation-style ("dollhouse") design — describe artifacts, actions, and side effects, not a model of the real world.

What this means day-to-day:

- Don't enumerate edge cases before the happy path exists.
- Don't scaffold empty groups for "future architecture."
- Don't invent content to fill thin docs.
- When behavior changes, the spec change comes first; the code follows mechanically.
- Sidecar JSONs (state machines, schemas) live next to their host `.md` in the same bundle, not in a separate directory.

## Working with the carta workspace

All design lives under `.carta/`. Carta is a CLI for managing numbered Markdown docs with stable cross-references and an auto-generated MANIFEST.

**Always read `.carta/MANIFEST.md` first.** It is the machine-readable index — fetch only the docs whose summary/tags match your task. Target: <10% of docs read per operation.

Cross-references use `docXX.YY.ZZ` (two digits per segment). They survive moves automatically.

Use the carta CLI for structural ops — never hand-edit numbering or move files manually:

- `carta create <dir> <slug> --title ... --summary ... --tags csv --deps csv` — new doc
- `carta group <dir> --title ...` — new group with index
- `carta move <src> <dst> [--order N]` — move/reorder; rewrites all refs
- `carta delete <ref>` — gap-closing delete; rewrites refs
- `carta punch <ref>` — leaf .md → directory
- `carta flatten <ref>` — directory → siblings in parent
- `carta attach <host> <file>` — add a sidecar (JSON, etc.) to a doc's bundle
- `carta regenerate` — rebuild MANIFEST after batch ops
- `carta cat <ref>` / `carta tree` / `carta ls <dir>` — read

Full reference: `carta ai-skill`.

Conventions live in `doc00.03`. Maintenance / unfolding rules live in `doc00.02`. About / theory in `doc00.01`. AI retrieval patterns in `doc00.04`.

## Where things live (so far)

- `00-codex/` — meta-documentation about the workspace itself
- `01-product/` — what we're building and why; user-visible behavior; research that informs product decisions

System / architecture / operations groups will appear when the work demands them — not before.
