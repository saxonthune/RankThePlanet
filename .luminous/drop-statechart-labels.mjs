#!/usr/bin/env node
// One-shot: drop `label` from statechart transitions where a matching
// inventory affordance covers the event. Transitions with no covering
// affordance keep their label (the pipeline reads it as a fallback).

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(here, '..');
const chartPath = path.join(repo, '.carta/02-design/02-interaction/01-navigation.statechart.json');
const screensDir = path.join(repo, '.carta/02-design/02-interaction/02-screens');

// Build (surface, event) → covered set from inventories.
const covered = new Set();
for (const f of fs.readdirSync(screensDir).sort()) {
  if (!f.endsWith('.inventory.json')) continue;
  const inv = JSON.parse(fs.readFileSync(path.join(screensDir, f), 'utf8'));
  const surface = inv.surface;
  const collect = (aff) => { if (aff?.event) covered.add(`${surface}::${aff.event}`); };
  for (const aff of inv.affordances ?? []) collect(aff);
  for (const list of inv.lists ?? []) collect(list.item?.affordance);
}

// Re-parse the chart, mutate in place. To preserve formatting, do a string-
// level edit guided by parsing — but since we only ever delete one line per
// hit, scan the file line-by-line with per-surface and per-event tracking.
const text = fs.readFileSync(chartPath, 'utf8');
const lines = text.split('\n');

// Find current state by looking for `"X": {` at indent 4, current event by
// looking for `"E": {` at indent 8.
const stateRe = /^    "([A-Za-z][A-Za-z0-9]*)": \{$/;
const eventRe = /^        "([A-Z_]+)": \{$/;
const labelRe = /^          "label": ".+",?$/;

let curState = null;
let curEvent = null;
const out = [];
let dropped = 0;
for (let i = 0; i < lines.length; i++) {
  const line = lines[i];
  const sm = line.match(stateRe);
  if (sm) { curState = sm[1]; curEvent = null; out.push(line); continue; }
  const em = line.match(eventRe);
  if (em) { curEvent = em[1]; out.push(line); continue; }
  if (labelRe.test(line) && curState && curEvent) {
    const key = `${curState}::${curEvent}`;
    if (covered.has(key)) {
      dropped++;
      continue; // skip — drop this line
    }
  }
  out.push(line);
}

fs.writeFileSync(chartPath, out.join('\n'), 'utf8');

// Validate the result parses.
JSON.parse(fs.readFileSync(chartPath, 'utf8'));
console.log(`Dropped ${dropped} transition labels from ${path.relative(repo, chartPath)}.`);
