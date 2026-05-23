// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  journeys-lib — parse journey sidecars; walk them as declared paths     ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
// A journey fully declares its path: parallel `events[]` and `targets[]`
// arrays of the same length, walked from `start`. Each step's target is
// what the journey author says happens — no statechart lookup, no fallback,
// no "intent vs reality" status. Intent-vs-reality is a *derived*
// observation produced by the verifier, not encoded in journey state.
//
// Pipelines:
//   journeys-verify.mjs         spec-to-spec: declared transitions vs chart
//   journeys-canvas.pipeline.mjs full graph: one edge per step
//   journeys-tree.pipeline.mjs   trie: shared prefixes collapsed
//
// All three share `parseJourneys` and `walkJourneys` from this file.
//
// Boundary types:
//   JourneySidecar : { statechart, journeys: [{id, description, start, events, targets, notes?}] }
//   WalkedStep     : { journey, order, event, from, to, note? }
//   WalkResult     : { steps: WalkedStep[], issues: string[] }

import { readFile } from 'node:fs/promises';
import { dirname, join, relative } from 'node:path';
import { existsSync } from 'node:fs';
import { REPO_ROOT, readSidecar, parseStatechart } from './statechart-lib.mjs';

// ── parse ────────────────────────────────────────────────────────────────────
// Sidecar shape (see 03-navigation-journeys.md). Each journey carries:
//   start      — entry surface (must be a string)
//   events[]   — ordered events the user fires
//   targets[]  — ordered screens those events land on; events.length === targets.length
//   notes?     — { "<1-based-index>": string } commentary
//
// Shape violations throw — they are spec bugs in the sidecar itself.
export function parseJourneys(raw) {
  let sidecar;
  try { sidecar = JSON.parse(raw.text); }
  catch (e) { throw new Error(`${raw.sourceRel}: invalid JSON — ${e.message}`); }
  if (typeof sidecar !== 'object' || sidecar === null) {
    throw new Error(`${raw.sourceRel}: not a journeys sidecar — top-level must be an object.`);
  }
  if (typeof sidecar.statechart !== 'string' || !sidecar.statechart) {
    throw new Error(`${raw.sourceRel}: missing 'statechart' string — name the sibling .statechart.json to verify against.`);
  }
  if (!Array.isArray(sidecar.journeys)) {
    throw new Error(`${raw.sourceRel}: missing 'journeys' array.`);
  }

  const seenIds = new Set();
  for (const j of sidecar.journeys) {
    if (typeof j?.id !== 'string' || !j.id) throw new Error(`${raw.sourceRel}: journey missing 'id'.`);
    if (seenIds.has(j.id)) throw new Error(`${raw.sourceRel}: duplicate journey id '${j.id}'.`);
    seenIds.add(j.id);
    if (typeof j.start !== 'string' || !j.start) throw new Error(`${raw.sourceRel}: journey '${j.id}' missing 'start'.`);
    if (!Array.isArray(j.events)) throw new Error(`${raw.sourceRel}: journey '${j.id}' missing 'events' array.`);
    if (!Array.isArray(j.targets)) throw new Error(`${raw.sourceRel}: journey '${j.id}' missing 'targets' array.`);
    if (j.events.length !== j.targets.length) {
      throw new Error(`${raw.sourceRel}: journey '${j.id}' has ${j.events.length} events but ${j.targets.length} targets — must be equal length.`);
    }
    j.events.forEach((e, i) => {
      if (typeof e !== 'string' || !e) throw new Error(`${raw.sourceRel}: journey '${j.id}' event at index ${i} must be a non-empty string.`);
    });
    j.targets.forEach((t, i) => {
      if (typeof t !== 'string' || !t) throw new Error(`${raw.sourceRel}: journey '${j.id}' target at index ${i} must be a non-empty string.`);
    });
    const notes = j.notes ?? {};
    if (typeof notes !== 'object' || Array.isArray(notes)) {
      throw new Error(`${raw.sourceRel}: journey '${j.id}' 'notes' must be a {index: string} object.`);
    }
    for (const k of Object.keys(notes)) {
      const idx = Number(k);
      if (!Number.isInteger(idx) || idx < 1 || idx > j.events.length) {
        throw new Error(`${raw.sourceRel}: journey '${j.id}' note key '${k}' is out of range (events length ${j.events.length}).`);
      }
    }
  }
  return sidecar;
}

// ── walkJourneys — produce flat (journey, order, event, from, to) facts ──────
// Trivial: declared targets ARE the path. No statechart needed.
export function walkJourneys(sidecar) {
  const steps = [];
  for (const j of sidecar.journeys) {
    let current = j.start;
    const notes = j.notes ?? {};
    for (let i = 0; i < j.events.length; i++) {
      const order = i + 1;
      const event = j.events[i];
      const to = j.targets[i];
      const note = notes[String(order)];
      const s = { journey: j.id, order, event, from: current, to };
      if (note) s.note = note;
      steps.push(s);
      current = to;
    }
  }
  return { steps };
}

// ── compareToChart — derive intent-vs-reality observations ───────────────────
// For each declared step, classify against the statechart:
//   "match"           — chart has from.event → to, matches journey
//   "target-mismatch" — chart has from.event but → other-target
//   "chart-missing"   — chart has no from.event transition
//   "unknown-surface" — `from` or `to` is not a state in the chart
// Pure derivation — no fallback, no rewriting. The journey is the truth of
// intent; the chart is the truth of implementation; this is the diff.
export function compareToChart(chart, walkedSteps) {
  const stateNames = new Set(Object.keys(chart.states));
  const observations = [];
  for (const s of walkedSteps) {
    if (!stateNames.has(s.from)) {
      observations.push({ ...s, kind: 'unknown-surface', detail: `from '${s.from}' is not in the statechart` });
      continue;
    }
    if (!stateNames.has(s.to)) {
      observations.push({ ...s, kind: 'unknown-surface', detail: `to '${s.to}' is not in the statechart` });
      continue;
    }
    const t = chart.states[s.from].on?.[s.event];
    if (!t || t.target === undefined) {
      observations.push({ ...s, kind: 'chart-missing', detail: `statechart has no '${s.event}' transition on '${s.from}'` });
      continue;
    }
    if (t.target !== s.to) {
      observations.push({ ...s, kind: 'target-mismatch', detail: `statechart sends ${s.from}.${s.event} → ${t.target}, journey says → ${s.to}` });
      continue;
    }
    observations.push({ ...s, kind: 'match' });
  }
  return observations;
}

export async function loadStatechartFor(journeysRef, sidecar) {
  const chartPath = join(dirname(journeysRef.path), sidecar.statechart);
  if (!existsSync(chartPath)) {
    throw new Error(`${relative(REPO_ROOT, journeysRef.path)}: declared statechart '${sidecar.statechart}' not found at ${relative(REPO_ROOT, chartPath)}.`);
  }
  const raw = await readSidecar({ path: chartPath });
  return parseStatechart(raw);
}

export async function readJourneys(ref) {
  return readSidecar(ref);
}
