---
title: License Viewer
summary: Affordance inventory for the LicenseViewer surface — in-app scrollable view of the AGPLv3 LICENSE bundled with the app
tags: [design, interaction, screens]
deps: [doc02.02.01]
verify: [{"kind":"screen-inventory","sidecar":"16-license-viewer.inventory.json","against":{"doc":"doc02.02.01","key":"LicenseViewer"}}]
---

# License Viewer

The affordance inventory for the `LicenseViewer` surface — the in-app view of the bundled `LICENSE` reachable from `About` ([[15-about]], doc02.02.02.15). **The source of truth is the rhidoc sidecar `16-license-viewer.inventory.json`** ([[00-index]], doc02.02.02.00 explains the shape); this `.md` is a lean companion.

The surface exists to satisfy AGPLv3 §5(d)'s "convenient and prominently visible" standard for displaying the License text. Bundling the License text as an app resource and rendering it in a dedicated surface keeps the obligation discharged offline, with no network round-trip and no dependence on an external host. The text is verbatim — the file shipped with the binary is the same file in the source tree's `LICENSE`.

The surface has two regions. The **topBar** identifies the surface and carries *Back* to `About`. The **content** region holds the scrollable License text. There are no in-view concept actions: the user reads the License, and `BACK` is the only transition.
