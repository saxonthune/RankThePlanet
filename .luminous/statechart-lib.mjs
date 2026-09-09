// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  statechart-lib — shared stages for statechart-derived pipelines         ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
// Pack-agnostic helpers shared by `statechart-canvas.pipeline.mjs` (which
// renders the surface graph) and `journeys-canvas.pipeline.mjs` (which
// renders user-intent paths through it). Both need: discovery of sidecars
// under .rhidoc/, file read, contract-checked parsing of a flat XState v5
// machine, and the (surface, event) → label map sourced from per-screen
// inventories.
//
// Anything Luminous-shaped (kinds, render specs, palettes, NavModel /
// JourneyModel boundaries) is NOT here — those are per-pipeline concerns.

import { readFile, readdir } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
export const REPO_ROOT = resolve(__dirname, '..');
export const RHIDOC_ROOT = join(REPO_ROOT, '.rhidoc');
export const GENERATED_ROOT = join(REPO_ROOT, '.luminous', 'generated');

// ── walk / discover ──────────────────────────────────────────────────────────
export async function* walk(dir) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) yield* walk(full);
    else yield full;
  }
}

export async function discoverBySuffix(suffix) {
  if (!existsSync(RHIDOC_ROOT)) return [];
  const out = [];
  for await (const f of walk(RHIDOC_ROOT)) {
    if (f.endsWith(suffix)) out.push({ path: f });
  }
  return out.sort((a, b) => a.path.localeCompare(b.path));
}

// ── read ─────────────────────────────────────────────────────────────────────
export async function readSidecar(ref) {
  return {
    path: ref.path,
    sourceRel: relative(REPO_ROOT, ref.path),
    text: await readFile(ref.path, 'utf8'),
  };
}

// ── parse — RawSidecar → flat Statechart, contract-checked ───────────────────
// Enforces sidecar conventions from doc02.02.01: flat machine, every
// transition object-form with a `target` naming a known state. NOT XState's
// full generality — parallel regions, substates, string-shorthand
// transitions, and `#machine.region` paths are rejected.
export function parseStatechart(raw) {
  let chart;
  try { chart = JSON.parse(raw.text); }
  catch (e) { throw new Error(`${raw.sourceRel}: invalid JSON — ${e.message}`); }
  if (typeof chart !== 'object' || chart === null || typeof chart.states !== 'object') {
    throw new Error(`${raw.sourceRel}: not a statechart — missing 'states' object`);
  }
  assertStatechart(chart, raw.sourceRel);
  return chart;
}

export function assertStatechart(chart, ctx) {
  if (chart.type === 'parallel') {
    throw new Error(`${ctx}: parallel machine — sidecar must be a flat (single-region) machine.`);
  }
  const stateNames = new Set(Object.keys(chart.states));
  for (const [state, def] of Object.entries(chart.states)) {
    if (def.states) {
      throw new Error(`${ctx}: state '${state}' has substates — sidecar must be a flat machine.`);
    }
    for (const [event, t] of Object.entries(def.on ?? {})) {
      if (typeof t !== 'object' || t === null) {
        throw new Error(`${ctx}: transition '${state}.${event}' must be object-form {target, description}.`);
      }
      if (t.target === undefined) continue; // self-transition — in-place, valid
      if (typeof t.target !== 'string' || !stateNames.has(t.target)) {
        throw new Error(`${ctx}: transition '${state}.${event}' targets '${t.target}' — not a known state.`);
      }
    }
  }
}

// ── loadInventoryLabels — (surface, event) → human label ─────────────────────
// Sibling `02-screens/*.inventory.json` files declare per-surface affordances.
// Each affordance with an `event` contributes (surface, event) → label. First
// label wins on conflict; divergence is warned via `onWarn`.
export async function loadInventoryLabels(ref, onWarn = () => {}) {
  const screensDir = join(dirname(ref.path), '02-screens');
  if (!existsSync(screensDir)) return new Map();
  const labels = new Map(); // Map<surface, Map<event, label>>
  const files = (await readdir(screensDir)).filter((f) => f.endsWith('.inventory.json')).sort();
  for (const f of files) {
    const text = await readFile(join(screensDir, f), 'utf8');
    let inv;
    try { inv = JSON.parse(text); }
    catch (e) { onWarn(`${f}: invalid JSON — ${e.message}`); continue; }
    const surface = inv.surface;
    if (!surface) continue;
    if (!labels.has(surface)) labels.set(surface, new Map());
    const byEvent = labels.get(surface);
    const consider = (aff) => {
      if (!aff?.event || !aff?.label) return;
      if (byEvent.has(aff.event)) {
        const existing = byEvent.get(aff.event);
        if (existing !== aff.label) {
          onWarn(`${f}: ${surface}.${aff.event} has divergent affordance labels ('${existing}' vs '${aff.label}') — keeping first.`);
        }
        return;
      }
      byEvent.set(aff.event, aff.label);
    };
    for (const aff of inv.affordances ?? []) consider(aff);
    for (const aff of inv.altAffordances ?? []) consider(aff);
    for (const list of inv.lists ?? []) {
      consider(list.item?.affordance);
      for (const alt of list.item?.altAffordances ?? []) consider(alt);
    }
  }
  return labels;
}
