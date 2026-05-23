#!/usr/bin/env node
// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  journeys-verify — spec-to-spec checker (journeys vs statechart)        ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
// Walks .carta/ for *.journeys.json sidecars. For each, loads the declared
// sibling statechart, resolves every journey step, and reports unresolved
// steps and sidecar shape errors. Exits non-zero on any failure so this
// can gate CI.
//
// No Luminous output, no canvas — the visualization lives in
// `journeys-canvas.pipeline.mjs`. Both share the resolver in journeys-lib.
//
//   node .luminous/journeys-verify.mjs
//   node .luminous/journeys-verify.mjs --json   machine-readable report

import { discoverBySuffix } from './statechart-lib.mjs';
import { readJourneys, parseJourneys, resolveJourneys, loadStatechartFor } from './journeys-lib.mjs';

async function verifyOne(ref) {
  const raw = await readJourneys(ref);
  const sidecar = parseJourneys(raw);
  const chart = await loadStatechartFor(ref, sidecar);
  const { steps, issues } = resolveJourneys(chart, sidecar);
  const unresolved = steps.filter((s) => s.status === 'unresolved');
  return { sourceRel: raw.sourceRel, sidecar, steps, issues, unresolved };
}

function formatHuman(result) {
  const lines = [];
  lines.push(`\n${result.sourceRel}`);
  const total = result.steps.length;
  const ok = total - result.unresolved.length;
  lines.push(`  ${ok}/${total} steps resolved across ${result.sidecar.journeys.length} journey(s).`);
  if (result.unresolved.length) {
    lines.push(`  ✗ ${result.unresolved.length} unresolved step(s):`);
    for (const s of result.unresolved) {
      const where = s.from ? `${s.from}.${s.event}` : `(start) ${s.event}`;
      lines.push(`      [${s.journey}] step ${s.order}: ${where} — ${s.reason}`);
      if (s.note) lines.push(`         note: ${s.note}`);
    }
  }
  if (result.issues.length) {
    lines.push(`  ${result.issues.length} sidecar issue(s):`);
    for (const i of result.issues) lines.push(`      ${i}`);
  }
  return lines.join('\n');
}

async function main() {
  const asJson = process.argv.includes('--json');
  const sidecars = await discoverBySuffix('.journeys.json');
  if (sidecars.length === 0) {
    if (asJson) console.log(JSON.stringify({ results: [], failed: false }, null, 2));
    else console.log('No *.journeys.json sidecars found under .carta/.');
    return;
  }

  const results = [];
  let failed = false;
  for (const ref of sidecars) {
    try {
      const r = await verifyOne(ref);
      results.push(r);
      if (r.unresolved.length > 0) failed = true;
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
      else console.log(formatHuman(r));
    }
    console.log(failed ? '\n✗ journeys-verify: spec-to-spec gaps detected.' : '\n✓ journeys-verify: all journeys resolve.');
  }
  if (failed) process.exit(1);
}

main().catch((e) => { console.error('\n✗ journeys-verify crashed:\n  ' + e.message); process.exit(2); });
