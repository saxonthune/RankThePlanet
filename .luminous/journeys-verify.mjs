#!/usr/bin/env node
// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  journeys-verify — intent-vs-reality diff (journeys vs statechart)      ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
// Walks .rhidoc/ for *.journeys.json sidecars. For each declared step,
// classifies against the sibling statechart and reports observations:
//
//   match            journey and chart agree
//   chart-missing    journey says this transition exists; chart doesn't have it
//   target-mismatch  journey and chart both have the event, but disagree on target
//   unknown-surface  journey names a screen the chart doesn't define
//
// Any observation other than `match` is a divergence between intent and
// implementation, and the script exits non-zero so this can gate CI.
//
//   node .luminous/journeys-verify.mjs
//   node .luminous/journeys-verify.mjs --json
//   node .luminous/journeys-verify.mjs --only=target-mismatch

import { discoverBySuffix } from './statechart-lib.mjs';
import { readJourneys, parseJourneys, walkJourneys, compareToChart, loadStatechartFor } from './journeys-lib.mjs';

const SEVERITY = {
  match: 0,
  'chart-missing': 1,
  'target-mismatch': 1,
  'unknown-surface': 1,
};

async function verifyOne(ref) {
  const raw = await readJourneys(ref);
  const sidecar = parseJourneys(raw);
  const chart = await loadStatechartFor(ref, sidecar);
  const { steps } = walkJourneys(sidecar);
  const observations = compareToChart(chart, steps);
  return { sourceRel: raw.sourceRel, sidecar, observations };
}

function formatHuman(result, filterKinds) {
  const lines = [`\n${result.sourceRel}`];
  const groups = {};
  for (const o of result.observations) {
    if (filterKinds && !filterKinds.has(o.kind)) continue;
    (groups[o.kind] ??= []).push(o);
  }
  const matches = (groups.match ?? []).length;
  const total = result.observations.length;
  lines.push(`  ${matches}/${total} steps match the statechart across ${result.sidecar.journeys.length} journey(s).`);
  for (const kind of Object.keys(groups).sort()) {
    if (kind === 'match') continue;
    lines.push(`  ✗ ${groups[kind].length} ${kind}:`);
    for (const o of groups[kind]) {
      lines.push(`      [${o.journey}] step ${o.order}: ${o.from}.${o.event} → ${o.to}`);
      lines.push(`         ${o.detail}`);
      if (o.note) lines.push(`         note: ${o.note}`);
    }
  }
  return lines.join('\n');
}

async function main() {
  const args = process.argv.slice(2);
  const asJson = args.includes('--json');
  const onlyArg = args.find((a) => a.startsWith('--only='));
  const filterKinds = onlyArg ? new Set(onlyArg.slice('--only='.length).split(',')) : null;

  const sidecars = await discoverBySuffix('.journeys.json');
  if (sidecars.length === 0) {
    if (asJson) console.log(JSON.stringify({ results: [], failed: false }, null, 2));
    else console.log('No *.journeys.json sidecars found under .rhidoc/.');
    return;
  }

  const results = [];
  let failed = false;
  for (const ref of sidecars) {
    try {
      const r = await verifyOne(ref);
      results.push(r);
      for (const o of r.observations) if (SEVERITY[o.kind] > 0) failed = true;
    } catch (e) {
      results.push({ sourceRel: ref.path, fatal: e.message });
      failed = true;
    }
  }

  if (asJson) {
    console.log(JSON.stringify({ results, failed }, null, 2));
  } else {
    for (const r of results) {
      if (r.fatal) console.error(`\n${r.sourceRel}\n  ✗ ${r.fatal}`);
      else console.log(formatHuman(r, filterKinds));
    }
    console.log(failed ? '\n✗ journeys-verify: intent and statechart diverge.' : '\n✓ journeys-verify: all journeys match the statechart.');
  }
  if (failed) process.exit(1);
}

main().catch((e) => { console.error('\n✗ journeys-verify crashed:\n  ' + e.message); process.exit(2); });
