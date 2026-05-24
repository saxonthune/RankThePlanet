---
title: Visual Language
summary: How RTP looks and feels on a screen — principles derived from a running grievance log, anchored to the broader UX canon
tags: [design, visual-language, index]
deps: [doc02.03, doc02.04]
---

# Visual Language

The principles RTP's screens are designed against. Sister group to [[03-theme-tokens]] (doc02.03), which fixes the token contract, and [[04-surface-composition-rules]] (doc02.04), which fixes how surfaces compose. This group fixes what those surfaces *look like* once composed — hierarchy, weight, spacing, restraint, the rules a reviewer can point at.

## How this group grows

Principles here are **earned, not invented**. Each one starts as a complaint in [[01-grievance-log]], gets clustered with related complaints, and only graduates to a named rule once three or more grievances point at the same underlying issue. Principle without supporting grievances is premature abstraction — the kind of design rule that reads well and bends nothing.

The loop:

1. A screen looks wrong. The observation goes into the grievance log as raw voice: *what* is off, on *which* surface, with the date. No vocabulary required.
2. Periodically the log is read for clusters. A cluster gets a candidate name in the canon's idiom (information hierarchy, surface elevation, affordance signifier, restraint, …).
3. When a cluster is stable and named, a sibling doc in this group captures the rule, with each grievance ID it resolves cited as evidence.

The grievance log is the source of truth for *why* a rule exists. Rules without grievance citations have no warrant and can be challenged.

## Boundary with concept-model bugs

Some grievances look visual but are actually concept ambiguity — for instance, a status row whose label and affordance disagree reads as a style problem until the concept doc decides what state it represents. Those grievances are logged here, but the fix lives in [[03-concepts]] (doc01.03) or the relevant screen inventory under doc02.02.02 first. The visual rule cannot lead the concept.

## Canon — the sources principles in this group draw from

These are the references this group leans on when naming and justifying a rule. Cite them in principle docs the same way concept docs cite Jackson.

### Platform guidelines (canonical)

- **Apple Human Interface Guidelines** — the primary reference for iOS expectations. Three foundational words: *Clarity, Deference, Depth.* "Deference" is the load-bearing one for RTP: chrome should serve the map, not compete with it. <https://developer.apple.com/design/human-interface-guidelines/> · Foundations: <https://developer.apple.com/design/human-interface-guidelines/foundations> · Materials: <https://developer.apple.com/design/human-interface-guidelines/materials>
- **Material 3** — the structural grammar (tokens, elevation, motion) RTP's Android side inherits. Its *visual defaults* (e.g., the seed-color brown) are not adopted; its *system* is. Cross-reference with [[03-theme-tokens]] (doc02.03).

### Operational books (the working references)

- **Refactoring UI** — Wathan & Schoger. The most operational reference for translating "this looks off" into a concrete move (hierarchy by weight not size, color via HSL, spacing scales, depth via shadow + saturation). <https://refactoringui.com/>
- **Don Norman — *The Design of Everyday Things*** (1988 / 2013 rev). Primary source for *affordance*, *signifier*, *mapping*, and the *gulf of execution*. When a grievance is "this looks like a button but isn't" or "this looks like a status but should be an action," the vocabulary is here.

### Heuristics and lookups

- **Nielsen Norman Group** — the field's working journal. The 10 heuristics are the checklist; the long-form articles are the evidence behind any rule adopted here. 10 heuristics: <https://www.nngroup.com/articles/ten-usability-heuristics/> · Articles index: <https://www.nngroup.com/articles/>
- **Tognazzini — First Principles of Interaction Design** (NN/G). Named, checklist-shaped principles (Anticipation, Consistency, Defaults, Discoverability, Fitts's Law, …). Use as a review checklist. <https://www.nngroup.com/articles/first-principles-interaction-design/>
- **Laws of UX** — Yablonski. 21 named psychology heuristics, each citing the original research. Vocabulary lookup when a grievance feels like a known pattern but the name is missing. <https://lawsofux.com/>
- **Interaction Design Foundation — Design Principles topic.** Broader, encyclopedic. <https://ixdf.org/literature/topics/design-principles>

### What is deliberately *not* canon

- Pattern-theft galleries (Dribbble, Mobbin, "beautiful app" screenshots). Useful for stealing layouts after the principles are clear; harmful as a substitute for them.
- Engagement-pattern literature (Hooked et al.). RTP is not built to be addictive; intuition is the target, not retention.
- Downstream component libraries (Shadcn, MUI) — they are applications of the canon above, not sources for it.

## Contents

- doc02.05.01 — Grievance log: raw observations awaiting clustering.
- Principles graduate here as sibling docs once a cluster is stable.
