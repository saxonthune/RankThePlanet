---
title: Review Form
summary: Affordance inventory for the ReviewForm surface — the template field set as editable inputs, save and cancel
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"06-review-form.inventory.json","against":{"doc":"doc02.02.01","key":"ReviewForm"}}]
---

# Review Form

The affordance inventory for the `ReviewForm` surface — where a Review instance is authored or edited against its Collection's template. **The source of truth is the rhidoc sidecar `06-review-form.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface has three regions. The **topBar** names the Review being authored and carries *Cancel* (`CANCEL`), which discards the draft and returns to `CollectionDetail`. The **fields** region iterates the Collection template's ordered fields. A quiet bordered field surface separates each field from its neighbors; the label, optional clear action, and full-width input form one visual group. Score targets meet the platform minimum touch size, enum choices wrap, boolean fields make the whole labeled row pressable, and date fields render as explicit value controls ([[03-concepts]], doc01.03 §3). Editing a field's value is `Review.edit`; *Clear a field* is `Review.clear`, for fields where "not set" is meaningful and distinct from a false or zero value. The **submitBar** holds *Save the Review* (`SUBMIT`), which commits the draft (`Review.submit`) and advances to the new Collection Entry's `CollectionEntryDetail`.

A Review is valid with any subset of its fields filled — a sparse Review is a finished Review ([[03-concepts]], doc01.03 §3). The form never blocks *Save*; a field's `required` flag shows as a marker that nudges, never a gate. The fields-list empty state — a template with no fields — is not a normal state: it points the user back to `CollectionEditor` to author the template.

The form renders the Collection's **current** template version. When the instance's `recorded_template_version` differs, the form still opens: the draft is loaded by intersecting the instance's `data` keys with the current template's field `name`s, so orphaned keys (fields removed from the template) are invisible without being destroyed (see [[03-concepts]], doc01.03 §3 Notes for the per-key reconciliation rule). Submitting the form rewrites `recorded_template_version` to the current version and drops the orphans. Each field row shows the template's `label`; the form never displays the underlying `name` key.

`Review.start` is **deferred**: it produces the pre-filled draft when the surface opens (today's date and other sensible defaults) and has no user affordance — the form is simply already started on entry. It is listed in the sidecar's `deferred` array so the `screen-inventory` verifier ([[05-verification-system]], doc01.04.02) counts it as an acknowledged gap, not a missing affordance. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
