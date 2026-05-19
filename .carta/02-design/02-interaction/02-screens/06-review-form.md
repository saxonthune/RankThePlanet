---
title: Review Form
summary: Affordance inventory for the ReviewForm surface — the template field set as editable inputs, save and cancel
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"06-review-form.inventory.json","against":{"doc":"doc02.02.01","key":"ReviewForm"}}]
---

# Review Form

The affordance inventory for the `ReviewForm` surface — where a Review instance is authored or edited against its Collection's template. **The source of truth is the carta sidecar `06-review-form.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface has three regions. The **topBar** names the Review being authored and carries *Cancel* (`CANCEL`), which discards the draft and returns to `CollectionDetail`. The **fields** region iterates the Collection template's ordered fields, each rendered as an editable input for its type — score, text, enum, boolean, date, power-ranking ([[03-concepts]], doc01.03 §3). Editing a field's value is `Review.edit`; *Clear a field* is `Review.clear`, for fields where "not set" is meaningful and distinct from a false or zero value. The **submitBar** holds *Save the Review* (`SUBMIT`), which commits the draft (`Review.submit`) and advances to the new Collection Entry's `CollectionEntryDetail`.

A Review is valid with any subset of its fields filled — a sparse Review is a finished Review ([[03-concepts]], doc01.03 §3). The form never blocks *Save*; a field's `required` flag shows as a marker that nudges, never a gate. The fields-list empty state — a template with no fields — is not a normal state: it points the user back to `CollectionEditor` to author the template.

`Review.start` is **deferred**: it produces the pre-filled draft when the surface opens (today's date and other sensible defaults) and has no user affordance — the form is simply already started on entry. It is listed in the sidecar's `deferred` array so the `screen-inventory` verifier ([[05-verification-system]], doc01.04.02) counts it as an acknowledged gap, not a missing affordance. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
