#!/usr/bin/env node
// One-shot survey: for each statechart transition with a label,
// find the matching affordance(s) in the inventory and report label
// match / divergence / orphan.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(here, '..');
const statechartPath = path.join(repo, '.carta/02-design/02-interaction/01-navigation.statechart.json');
const screensDir = path.join(repo, '.carta/02-design/02-interaction/02-screens');

const chart = JSON.parse(fs.readFileSync(statechartPath, 'utf8'));

const invByEvent = new Map();
const invByAction = new Map();
for (const f of fs.readdirSync(screensDir)) {
  if (!f.endsWith('.inventory.json')) continue;
  const inv = JSON.parse(fs.readFileSync(path.join(screensDir, f), 'utf8'));
  const surface = inv.surface;
  const collect = (aff, listId) => {
    if (aff.event) {
      const k = `${surface}::${aff.event}`;
      if (!invByEvent.has(k)) invByEvent.set(k, []);
      invByEvent.get(k).push({ surface, id: aff.id, label: aff.label, source: f, list: listId });
    }
    if (aff.action) {
      const k = `${surface}::${aff.action}`;
      if (!invByAction.has(k)) invByAction.set(k, []);
      invByAction.get(k).push({ surface, id: aff.id, label: aff.label, source: f, list: listId });
    }
  };
  for (const aff of inv.affordances ?? []) collect(aff, null);
  for (const list of inv.lists ?? []) {
    if (list.item?.affordance) collect(list.item.affordance, list.id);
  }
}

const rows = [];
for (const [surface, state] of Object.entries(chart.states)) {
  for (const [event, t] of Object.entries(state.on ?? {})) {
    const key = `${surface}::${event}`;
    const aff = invByEvent.get(key) ?? [];
    const txLabel = t.label ?? null;
    rows.push({ surface, event, txLabel, aff, deferred: false });
  }
}

const fmt = r => {
  if (!r.aff.length) {
    return `  ORPHAN  ${r.surface}.${r.event}  txLabel=${JSON.stringify(r.txLabel)}`;
  }
  const lines = r.aff.map(a => `${a.id}=${JSON.stringify(a.label)}`).join(' | ');
  const allMatch = r.aff.every(a => a.label === r.txLabel);
  const noLabel = r.txLabel == null;
  const tag = noLabel ? 'NO_TX_LABEL' : (allMatch ? 'MATCH' : 'DIVERGE');
  return `  ${tag.padEnd(11)} ${r.surface}.${r.event}  tx=${JSON.stringify(r.txLabel)}  inv: ${lines}`;
};

const buckets = { MATCH: [], DIVERGE: [], ORPHAN: [], NO_TX_LABEL: [] };
for (const r of rows) {
  const line = fmt(r);
  if (line.includes(' MATCH ')) buckets.MATCH.push(line);
  else if (line.includes(' DIVERGE ')) buckets.DIVERGE.push(line);
  else if (line.includes(' ORPHAN ')) buckets.ORPHAN.push(line);
  else buckets.NO_TX_LABEL.push(line);
}

console.log(`# Label-drop survey\n`);
console.log(`MATCH       ${buckets.MATCH.length}  — safe to drop tx.label, affordance.label is identical`);
console.log(`DIVERGE     ${buckets.DIVERGE.length}  — needs reconciliation before drop`);
console.log(`ORPHAN      ${buckets.ORPHAN.length}  — tx has label, no inventory affordance fires the event (keep tx.label)`);
console.log(`NO_TX_LABEL ${buckets.NO_TX_LABEL.length}  — tx has no label already, nothing to drop\n`);

for (const k of ['DIVERGE', 'ORPHAN', 'NO_TX_LABEL', 'MATCH']) {
  console.log(`\n## ${k}`);
  for (const line of buckets[k]) console.log(line);
}
