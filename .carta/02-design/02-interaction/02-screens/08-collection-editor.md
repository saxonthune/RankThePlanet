---
title: Collection Editor
summary: Affordance inventory for the CollectionEditor surface — Collection metadata and the Review template field set
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"08-collection-editor.inventory.json","against":{"doc":"doc02.02.01","key":"CollectionEditor"}}]
---

# Collection Editor

The affordance inventory for the `CollectionEditor` surface — where a Collection's identity and its Review template are authored or edited. **The source of truth is the carta sidecar `08-collection-editor.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface has three regions. The **topBar** carries *Cancel* (`CLOSE`), which discards uncommitted changes, and *Finish editing* (`DONE`), which commits and continues into `CollectionDetail`. The **metadata** region edits the Collection's `name`, `description`, and `appearance` (`Collection.edit`) — this is the only place that metadata is editable; `CollectionDetail` shows it read-only ([[02-collection-detail]], doc02.02.02.02). The **template** region authors the Review template: *Adopt a built-in template* (`Review.useBuiltIn`) seeds the field set, *Add a template field* and the per-field *Edit, reorder, or remove* affordance shape it (`Review.editTemplate`).

The template region iterates the template's ordered fields ([[03-concepts]], doc01.03 §3). Each field shows its name, its `type` — `score`, `text`, `enum`, `boolean`, `date`, `power-ranking` — its per-type configuration, and its required marker. A `score` field's configuration is a numeric range, a `step` granularity down to one decimal, and a `render` style (`number`, `stars`, `icon`, `slider`, `bar`) that governs presentation without changing the stored number — a star rating and a 0–10 numeric rating are one `score` type with different config. The empty state — a template with no fields — is a valid end state, though a Collection that keeps it leaves `ReviewForm` nothing to fill ([[06-review-form]], doc02.02.02.06).

`Collection.create` and `Review.defineTemplate` are **deferred**: they are the create-mode counterparts of `Collection.edit` and `Review.editTemplate`, reached through the same metadata and template affordances when the surface opens for a new Collection rather than an existing one. They carry no distinct affordance, so the sidecar lists them in `deferred` and the `screen-inventory` verifier ([[05-verification-system]], doc01.04.02) counts them as acknowledged gaps. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
