---
title: About
summary: Affordance inventory for the About surface — identity, privacy and support copy, repository link, license, and open-source attributions
tags: [design, interaction, screens]
deps: [doc02.02.01, doc01.03]
verify: [{"kind":"screen-inventory","sidecar":"15-about.inventory.json","against":{"doc":"doc02.02.01","key":"About"}}]
---

# About

The affordance inventory for the `About` surface — the *About* entry reachable from `Settings` ([[03-settings]], doc02.02.02.03). **The source of truth is the carta sidecar `15-about.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

About discharges two obligations and one piece of identity. The legal obligation is AGPLv3 §5(d): an interactive UI must display "Appropriate Legal Notices" — copyright, no-warranty, where to view the License — and §13 expects a path to source. The license-and-source pair is satisfied by *View license* ([[16-license-viewer]], doc02.02.02.16, an in-app scrollable viewer of the bundled `LICENSE`) and *View source* (a handoff to the public GitHub repository). The second obligation is upstream attribution for permissive dependencies (Apache 2.0 / BSD-style) — satisfied by *Open-source attributions* ([[17-attributions]], doc02.02.02.17), whose list is generated from the resolved build dependency graph rather than hand-curated. The identity piece — app name, version, a one-paragraph blurb — is a static `aboutBlock` region whose copy is content, not affordance.

The `aboutBlock` also states RTP's privacy posture: the app keeps Collections and Reviews on-device, fetches map tiles from OpenFreeMap, and sends search text or coordinates to the active Location Provider. The public repository supplies the support and full policy path.

The surface has three regions. The **topBar** identifies the surface and carries *Back* to `Settings`. The **aboutBlock** holds the static identity copy and the copyright line — the "Appropriate Legal Notices" text itself, alongside name, version, and the blurb. The **links** region holds the three transitions: *View license*, *Open-source attributions*, and *View source on GitHub*. The first two move the user to other surfaces (`TAP_VIEW_LICENSE`, `TAP_ATTRIBUTIONS`); the third hands off out of the app — a peripheral concept action `About.openRepository` modeled in `meta.actions` rather than as a transition. Nothing here names a Composable or a UiState; that binding is not part of the interaction layer (doc02.02.00).
