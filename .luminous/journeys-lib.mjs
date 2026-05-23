// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  journeys-lib — parse journey sidecars; resolve them against a chart    ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
// Shared by `journeys-verify.mjs` (spec-to-spec checker, exits non-zero on
// resolution failure) and `journeys-canvas.pipeline.mjs` (Luminous renderer
// that draws the current state of the world including its gaps). Both
// reuse the same resolver — the visible bug on the canvas and the CI
// failure are the same fact rendered two ways.
//
// Boundary types:
//   JourneySidecar : { statechart, journeys: [{id, description, start}],
//                      steps: [{journey, order, event, note?}] }
//   ResolvedStep   : { journey, order, event, from, to, status, reason?, note? }
//                    status ∈ "resolved" | "unresolved"
//                    when unresolved: to=null, reason names why
//   ResolveResult  : { steps: ResolvedStep[], issues: string[] }

import { readFile } from 'node:fs/promises';
import { dirname, join, relative } from 'node:path';
import { existsSync } from 'node:fs';
import { REPO_ROOT, readSidecar, parseStatechart } from './statechart-lib.mjs';

// ── parse ────────────────────────────────────────────────────────────────────
// Authoring shape: each journey carries its own `events: [string]` array, and
// an optional `notes: { "<1-based-index>": string }` map for per-step notes.
// Internally we normalize to flat (journey, order, event, note?) step facts
// so the resolver and the eventual Datalog story stay flat-shaped.
//
// Contract violations throw — they are spec bugs in the sidecar itself,
// not gaps the renderer can show.
export function parseJourneys(raw) {
  let sidecar;
  try { sidecar = JSON.parse(raw.text); }
  catch (e) { throw new Error(`${raw.sourceRel}: invalid JSON — ${e.message}`); }
  if (typeof sidecar !== 'object' || sidecar === null) {
    throw new Error(`${raw.sourceRel}: not a journeys sidecar — top-level must be an object.`);
  }
  if (typeof sidecar.statechart !== 'string' || !sidecar.statechart) {
    throw new Error(`${raw.sourceRel}: missing 'statechart' string — name the sibling .statechart.json to resolve against.`);
  }
  if (!Array.isArray(sidecar.journeys)) {
    throw new Error(`${raw.sourceRel}: missing 'journeys' array.`);
  }

  const seenIds = new Set();
  const steps = [];
  for (const j of sidecar.journeys) {
    if (typeof j?.id !== 'string' || !j.id) throw new Error(`${raw.sourceRel}: journey missing 'id'.`);
    if (seenIds.has(j.id)) throw new Error(`${raw.sourceRel}: duplicate journey id '${j.id}'.`);
    seenIds.add(j.id);
    if (typeof j.start !== 'string' || !j.start) throw new Error(`${raw.sourceRel}: journey '${j.id}' missing 'start'.`);
    if (!Array.isArray(j.events)) {
      throw new Error(`${raw.sourceRel}: journey '${j.id}' missing 'events' array.`);
    }
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
    j.events.forEach((event, i) => {
      if (typeof event !== 'string' || !event) {
        throw new Error(`${raw.sourceRel}: journey '${j.id}' event at index ${i} must be a non-empty string.`);
      }
      const order = i + 1;
      const note = notes[String(order)];
      steps.push(note ? { journey: j.id, order, event, note } : { journey: j.id, order, event });
    });
  }

  // Return the normalized internal shape: downstream resolver and canvas
  // pipeline still read `sidecar.steps`, unchanged.
  return { ...sidecar, steps };
}

// ── resolveJourneys — walk each journey through the chart ─────────────────────
// For each step: look up chart.states[current].on[event]. If the transition
// exists and has a target, advance; otherwise emit an unresolved step with
// a reason and stop walking that journey (no further steps make sense once
// the path has broken).
export function resolveJourneys(chart, sidecar) {
  const stateNames = new Set(Object.keys(chart.states));
  const stepsByJourney = new Map();
  for (const s of sidecar.steps) {
    if (!stepsByJourney.has(s.journey)) stepsByJourney.set(s.journey, []);
    stepsByJourney.get(s.journey).push(s);
  }
  for (const list of stepsByJourney.values()) list.sort((a, b) => a.order - b.order);

  const resolved = [];
  const issues = [];

  for (const j of sidecar.journeys) {
    if (!stateNames.has(j.start)) {
      issues.push(`journey '${j.id}': start '${j.start}' is not a state in the statechart.`);
      // record a sentinel step so the canvas can still draw the broken origin
      resolved.push({
        journey: j.id, order: 0, event: '(start)', from: null, to: null,
        status: 'unresolved', reason: `unknown start state '${j.start}'`,
      });
      continue;
    }

    let current = j.start;
    const steps = stepsByJourney.get(j.id) ?? [];
    if (steps.length === 0) {
      issues.push(`journey '${j.id}': no steps declared.`);
      continue;
    }

    let broken = false;
    for (const s of steps) {
      if (broken) {
        // Skip remaining steps once a path has broken — keeps the canvas honest.
        // Authoring-time the user sees the first break and fixes upstream first.
        resolved.push({
          journey: j.id, order: s.order, event: s.event, from: null, to: null,
          status: 'unresolved', reason: 'upstream step broke the path',
          ...(s.note ? { note: s.note } : {}),
        });
        continue;
      }
      const def = chart.states[current];
      const t = def?.on?.[s.event];
      if (!t || t.target === undefined) {
        issues.push(`journey '${j.id}' step ${s.order}: ${current}.${s.event} — event not defined on this surface.`);
        resolved.push({
          journey: j.id, order: s.order, event: s.event, from: current, to: null,
          status: 'unresolved', reason: `no transition '${s.event}' on '${current}'`,
          ...(s.note ? { note: s.note } : {}),
        });
        broken = true;
        continue;
      }
      resolved.push({
        journey: j.id, order: s.order, event: s.event, from: current, to: t.target,
        status: 'resolved',
        ...(s.note ? { note: s.note } : {}),
      });
      current = t.target;
    }
  }

  return { steps: resolved, issues };
}

// ── loadStatechartFor — resolve sidecar's `statechart` to a parsed chart ─────
// The journeys sidecar names its sibling statechart by relative path. Reads
// and contract-checks it via statechart-lib so both pipelines see the same
// strict shape.
export async function loadStatechartFor(journeysRef, sidecar) {
  const chartPath = join(dirname(journeysRef.path), sidecar.statechart);
  if (!existsSync(chartPath)) {
    throw new Error(`${relative(REPO_ROOT, journeysRef.path)}: declared statechart '${sidecar.statechart}' not found at ${relative(REPO_ROOT, chartPath)}.`);
  }
  const raw = await readSidecar({ path: chartPath });
  return parseStatechart(raw);
}

export async function readJourneys(ref) {
  // tiny wrapper for symmetry with readSidecar; kept here so callers only
  // import from journeys-lib for journey-shaped work.
  return readSidecar(ref);
}
