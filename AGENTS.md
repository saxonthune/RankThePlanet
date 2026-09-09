# AGENTS.md — RankThePlanet

Free, fast, open-source geo diary. User-curated lists of places with per-list review schemas.

**RTP** = RankThePlanet. Use the abbreviation freely in conversation and docs.

## First action, every session

**Before answering any product, design, or scaffolding question, read `.rhidoc/MANIFEST.md` and any docs whose summary/tags match the task.** Prior decisions and research live there. Do not propose technology, architecture, or behavior without first checking what the workspace has already converged on. This rule outranks brevity — a fast answer that contradicts existing docs is worse than a slow one that aligns with them.

## Project status: pre-alpha

RTP is pre-alpha. **No user data exists in the wild.** Don't reason about backwards compatibility, data migration, version history, or "what happens to existing reviews/templates when X changes." Old template versions, op-log compaction, schema migrations, sync conflict resolution across app versions — all deferred. When the code encounters state that would require a migration path (e.g. a Review instance whose `recorded_template_version` is older than the Collection's current `template_version`), it throws. Surface the error; don't silently coerce. Treat every storage path as if it was just created.

This rule outranks "be defensive" / "handle edge cases." It is load-bearing precisely because production-codebase instincts will push the opposite way.

## Working philosophy: unfolding design

Docs (and code, when it arrives) **unfold**. Start sparse. Grow only what the next concrete piece of work demands. A one-line doc is a finished doc until someone needs more from it.

Inspirations:

- `.rhidoc/00-handbook/02-maintenance.md` — the local statement of unfolding for this workspace.
- The artifact chain: each spec reduces residual entropy for the one after it. By the time you write code, the question "what should this do?" should already be answered upstream.
- Two sources of truth (product expectations vs. source code). Everything else is an artifact bridging them; there is no third source. Avoid simulation-style ("dollhouse") design — describe artifacts, actions, and side effects, not a model of the real world.

What this means day-to-day:

- Don't enumerate edge cases before the happy path exists.
- Don't scaffold empty groups for "future architecture."
- Don't invent content to fill thin docs.
- When behavior changes, the spec change comes first; the code follows mechanically.
- Sidecar JSONs (state machines, schemas) live next to their host `.md` in the same bundle, not in a separate directory.

## No temporal language in docs

This rule is load-bearing and easy to violate. Every rhidoc doc is written in the **present tense of intent** — describe how things *should be*, never when they were built, what they replace, what is "currently"/"now"/"today"/"pending"/"not yet"/"will soon"/"in the future" true, or how the doc changed. Temporal framing rots: the doc that says "will soon" is wrong the moment "soon" passes, and the reader has no way to tell.

For genuinely unbuilt ideas, use **conditional phrasing** and flag them as not-yet-built: "a candidate verifier kind", "the surface could declare X", "one approach would be". Conditional voice stays correct whether the idea ships or not.

For historical context (why this exists, what it replaces, an incident that motivated it), put it in the **commit message or PR description**, not the doc.

Detection: agents finding any of the words above in a doc they're editing should rewrite the surrounding sentence in intent voice. If the resulting sentence has no content, the original sentence was decoration — delete it.

See also the saved memory `feedback_carta_no_temporal_language.md` and doc00.03's writing-style section.

## Agentive prose, not zombie nouns

Experimental rule, applied to docs, code comments, PR descriptions, and the prose agents write in conversation. Background and citations in doc01.06.05.

**Name a concrete actor as the grammatical subject, and let verbs be verbs.** Where a sentence has no agent, find the person or system doing the work and put them in the subject slot. Where a verb has been turned into a noun (*-tion*, *-ment*, *-ity*, *-ance*, *-ism*) and paired with a weak verb (*is, has, makes, performs, conducts, undertakes*), promote the buried verb and drop the scaffolding.

Examples — the bad form is on the left:

- *"the ask is to verify X"* → *"verify X"*
- *"perform a verification of"* → *"verify"*
- *"there is a need for the addition of"* → *"add"*
- *"the change introduces a regression"* → *"the change breaks X"*
- *"an unblock is required"* → *"unblock X"*
- *"backlog this"* → *"defer this"* (the noun-as-verb is the same shape inverted)

The rule targets the heavy-noun-phrase pattern Pinker/Williams/Sword document, not a vocabulary list. Banning specific HN/Slack words while leaving the agent-less subjects around them in place is cosmetic. Pruning the agent-less subjects is the move that matters.

Detection: scan for a subject of the form *"the {noun}"* coupled with *is/has/makes/performs*, or for nominalized verbs (*-tion*, *-ment*, *-ance*) load-bearing in a sentence's main clause. Rewrite so a concrete actor does a concrete verb. If the rewrite drops content the original sentence claimed to carry, the original was decoration — delete it.

This is an experiment, not yet a load-bearing rule. The evidence for human readability is strong (jargon disrupts processing fluency even with inline definitions; see doc01.06.05 §1). The evidence for LLM output quality is thin and untested directly. Apply the rule and notice whether the prose reads better.

## Working with the rhidoc workspace

All design lives under `.rhidoc/`. Rhidoc is a CLI for managing numbered Markdown docs with stable cross-references and an auto-generated MANIFEST.

**Always read `.rhidoc/MANIFEST.md` first.** It is the machine-readable index — fetch only the docs whose summary/tags match your task. Target: <10% of docs read per operation.

Cross-references use `docXX.YY.ZZ` (two digits per segment). They survive moves automatically.

Use the rhidoc CLI for structural ops — never hand-edit numbering or move files manually:

- `rhidoc make <dir> <slug>` — new doc
- `rhidoc make -g <dir> <slug>` — new group with index
- `rhidoc move <src> <dst> [--order N]` — move/reorder; rewrites all refs
- `rhidoc delete <ref>` — gap-closing delete; rewrites refs
- `rhidoc punch <ref>` — leaf .md → directory
- `rhidoc hoist <ref>` — directory → siblings in parent
- `rhidoc attach <host> <file>` — add a sidecar (JSON, etc.) to a doc's bundle
- `rhidoc regenerate` — rebuild MANIFEST after batch ops
- `rhidoc cat <ref>` / `rhidoc tree` / `rhidoc ls <dir>` — read

Full reference: `rhidoc ai-skill`.

Conventions live in `doc00.03`. Maintenance / unfolding rules live in `doc00.02`. About / theory in `doc00.01`. Agent navigation rules live in `.rhidoc/AGENTS.md`.

## Where things live (so far)

- `00-handbook/` — Rhidoc's handbook about the workspace itself
- `01-product/` — what we're building and why; user-visible behavior; research; development philosophy
- `02-design/` — framework decisions; interaction (surfaces, navigation graph)
- `.luminous/` — Luminous pipelines that turn rhidoc sidecars into visual canvas graphs

CMP-bound system specs (state tiers, repository contracts, screens-as-Composables, component tree) will appear in a future group when the interaction layer stabilizes.

## Searching and reading files

Use the dedicated tools — they're allowlisted and don't trigger approval prompts. Shell pipelines that wrap file access (`cd`, `xargs`, `sh -c`, output redirection, `find … | cat`) do trigger prompts. This list grows as new anti-patterns surface.

- DON'T `find … -name '*.kt' | xargs cat` — DO use Glob to list, then Read each file (Read takes parallel calls).
- DON'T `cd some/dir && cmd` — DO pass absolute paths; for unavoidable multi-step shell use a single subshell `(cd dir && cmd)`.
- DON'T `find … | xargs -I{} sh -c '…'` — DO use Glob/Grep, or Read files individually.
- DON'T `grep -r pattern path/` — DO use the Grep tool.
- DON'T `cat file` to read — DO use the Read tool.
- DON'T loop the shell over files (`for f in …; do cat $f; done`) — variable expansion blocks auto-approval; DO issue parallel Read calls, one per file.
- DON'T `wc -l *.kt` or other glob-expanded shell over many files — DO use Glob to list paths, then Read each (Read reports line counts).
- DON'T `python3 -c '…'` / `node -e '…'` / `jq` to parse, query, or pretty-print a file (JSON sidecars included) — DO Read the file directly; Read renders JSON fine and these inline-interpreter invocations trigger approval prompts. To inspect one slice of a large JSON, Read with `offset`/`limit` or Grep for the key.
- DON'T filter a build/verify command's output inline (`make verify | grep …`, `make verify > /tmp/log; grep … /tmp/log`, `… | tail`, `echo "EXIT=$?"`) — the pipe/redirect-then-grep wrapper triggers an approval prompt every session. DO run the bare allowlisted command (`make verify`, `make compile-check`) and read its output directly; if it's long, redirect once to a file (`make verify > /tmp/rtp_verify.log 2>&1`) and then **Read** that file (with `offset`/`limit`), never `grep`/`tail` it.

## Statechart sidecar workflow

When editing a `*.statechart.json` rhidoc sidecar, regenerate its Luminous canvas:

```
node .luminous/statechart-canvas.pipeline.mjs
```

The pipeline walks `.rhidoc/` for `*.statechart.json` sidecars and emits a derived canvas pair (`*.canvas.graph.json` + `*.canvas.pack.json`) per sidecar under `.luminous/generated/`. That output tree is gitignored — edit the sidecar and re-run, never hand-edit the generated files.

## Verifying changes locally

Three gates, picked by what changed:

- `make verify` — the rhidoc doc verifier (`node .rhidoc/verify.mjs`). Walks every `.md` doc for a `verify:` frontmatter entry and runs the named kinds against their sidecars — `context-chain`, `guard-coverage`, `modality-host`, `screen-inventory`, `invariant-resolution`, `action-concept`, `journeys-verify`, `journey-trace`, `generated-traces`. Each kind is documented in doc01.04.02. Run after any `.rhidoc/` change — schema edits, transition changes, inventory additions, statechart edits all flow through this.
- `make compile-check` — compile Android + commonMain metadata. Off-macOS proxy for iOS; on macOS the iOS Kotlin/Native targets compile too. Run after Kotlin changes. For real iOS verification on macOS prefer `make ios-device-run`.
- `make test` — JVM unit suite (`:composeApp:jvmTest`). Pass `FILTER=...` to scope it, e.g. `make test FILTER=com.saxonthune.ranktheplanet.data.sql.*`. Prefer this over invoking `./gradlew` directly — the make wrapper is allowlisted, the direct gradlew invocation is not.

## Code map

`make code-map` regenerates `.luminous/generated/code-map.md` — a compressed signature skeleton of the Kotlin sources under `app/composeApp/src/` (bodies stripped, grouped by package and file). Read it for a fast whole-codebase overview without opening every `.kt` file. It is a gitignored build artifact — re-run after code changes, never hand-edit. Spec: `doc01.04.03`.
