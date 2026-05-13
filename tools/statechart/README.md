# tools/statechart

Generates inline-literal `createMachine({...})` TypeScript files from carta `*.statechart.json` sidecars so the Stately VS Code extension can visualize them.

## One-time setup

```
cd tools/statechart
npm install
```

Install the **Stately VS Code extension** (`statelyai.stately-vscode`) — `.vscode/extensions.json` recommends it.

## Build

```
npm run build       # one-shot
npm run watch       # rebuild on sidecar changes
```

Both commands:
1. Walk `.carta/` for any file matching `*.statechart.json`.
2. Validate by calling `createMachine(json)`. Exits non-zero on invalid configs.
3. Emit `generated/<slug>.machine.ts` with an inline `createMachine({...})` literal.
4. Emit `generated/index.ts` re-exporting every machine.

Generated files **are committed** so the graph is viewable without running the build first. CI should fail if `git diff generated/` is non-empty after `npm run build` — that catches stale generated output.

## View the graph

Open any file in `generated/` in VS Code with the Stately extension installed. Above the `createMachine(...)` call, click the **"Open Visual Editor"** code lens.

If the extension fails to render, the same JSON sidecar can be dragged onto [stately.ai](https://stately.ai) — same data, different viewer.

## Authoring conventions

The carta sidecar is the source of truth. Edit it, run the build, commit both files together. Conventions for the sidecar live alongside its host doc — see `.carta/02-design/02-interaction/01-navigation.md` for the navigation machine.
