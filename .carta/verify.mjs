#!/usr/bin/env node
// verify.mjs — carta affordance-inventory verification harness
//
// Walks .carta/ for *.md docs declaring a `verify` frontmatter field and runs
// the appropriate verifier per entry. One verifier today: screen-inventory,
// which cross-checks a surface's *.inventory.json sidecar against the
// navigation statechart.
//
// CLI: node .carta/verify.mjs [path]
//   no arg  — scan all of .carta/
//   path    — restrict discovery to that subtree
//
// Exit 0 if every entry passes, 1 if any has missing/phantom/mismatch.

import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, dirname, basename, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const CARTA_ROOT = __dirname;

// ── Discovery ─────────────────────────────────────────────────────────────────

function walkMd(dir) {
  const results = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      results.push(...walkMd(full));
    } else if (entry.endsWith('.md')) {
      results.push(full);
    }
  }
  return results;
}

function parseFrontmatter(text) {
  const lines = text.split('\n');
  if (lines[0].trim() !== '---') return null;
  const end = lines.indexOf('---', 1);
  if (end === -1) return null;
  return lines.slice(1, end).join('\n');
}

function extractVerify(frontmatter) {
  if (!frontmatter) return null;
  const match = frontmatter.match(/^verify:\s*(.+)$/m);
  if (!match) return null;
  try {
    return JSON.parse(match[1]);
  } catch {
    return null;
  }
}

// ── Doc-ref resolution ────────────────────────────────────────────────────────
//
// doc02.02.01 → segments ["02","02","01"]
// Walk from CARTA_ROOT matching NN-* directories for each non-final segment,
// then match NN-*.md for the final segment.

function resolveDocRef(ref) {
  const segments = ref.replace(/^doc/, '').split('.');
  let dir = CARTA_ROOT;
  for (let i = 0; i < segments.length - 1; i++) {
    const prefix = segments[i];
    const match = readdirSync(dir).find(e => e.startsWith(prefix + '-') && statSync(join(dir, e)).isDirectory());
    if (!match) throw new Error(`Cannot resolve segment "${prefix}" in ${dir}`);
    dir = join(dir, match);
  }
  const last = segments[segments.length - 1];
  const file = readdirSync(dir).find(e => e.startsWith(last + '-') && e.endsWith('.md'));
  if (!file) throw new Error(`Cannot find .md for segment "${last}" in ${dir}`);
  return join(dir, file);
}

function sidecarPath(mdPath, extension) {
  return join(dirname(mdPath), basename(mdPath, '.md') + extension);
}

// ── Verifier: screen-inventory ────────────────────────────────────────────────

function verifyScreenInventory(inventoryPath, statechartPath, key) {
  const inventory = JSON.parse(readFileSync(inventoryPath, 'utf8'));
  const statechart = JSON.parse(readFileSync(statechartPath, 'utf8'));

  // Find the matching state (by meta.surface or state id)
  const states = statechart.states || {};
  let state = null;
  for (const [id, s] of Object.entries(states)) {
    if ((s.meta && s.meta.surface === key) || id === key) {
      state = s;
      break;
    }
  }
  if (!state) {
    return { pass: false, error: `Surface "${key}" not found in statechart` };
  }

  // Expected set: transition event keys + meta.actions
  const expected = new Set([
    ...Object.keys(state.on || {}),
    ...((state.meta && state.meta.actions) || []),
  ]);

  // Covered set: affordance events/actions (top-level + list items + altAffordances)
  const covered = new Set();
  for (const aff of inventory.affordances || []) {
    if (aff.event) covered.add(aff.event);
    if (aff.action) covered.add(aff.action);
    for (const alt of aff.altAffordances || []) {
      if (alt.event) covered.add(alt.event);
      if (alt.action) covered.add(alt.action);
    }
  }
  for (const list of inventory.lists || []) {
    const ia = list.item && list.item.affordance;
    if (ia) {
      if (ia.event) covered.add(ia.event);
      if (ia.action) covered.add(ia.action);
    }
    for (const alt of (list.item && list.item.altAffordances) || []) {
      if (alt.event) covered.add(alt.event);
      if (alt.action) covered.add(alt.action);
    }
  }

  const deferred = new Set(inventory.deferred || []);

  // Missing = expected − covered − deferred
  const missing = [...expected].filter(e => !covered.has(e) && !deferred.has(e));

  // Phantom = covered − expected (deferred does not suppress phantoms)
  const phantom = [...covered].filter(c => !expected.has(c));

  // Target mismatch: inventory affordance target must match statechart transition target
  const mismatches = [];
  const allAffordances = [
    ...(inventory.affordances || []),
    ...(inventory.affordances || []).flatMap(a => a.altAffordances || []),
    ...(inventory.lists || []).flatMap(l => (l.item && l.item.affordance) ? [l.item.affordance] : []),
    ...(inventory.lists || []).flatMap(l => (l.item && l.item.altAffordances) || []),
  ];
  for (const aff of allAffordances) {
    if (!aff.event || aff.target === undefined) continue;
    const transition = (state.on || {})[aff.event];
    if (!transition) continue;
    const scTarget = transition.target;
    if (scTarget !== aff.target) {
      mismatches.push({ event: aff.event, inventoryTarget: aff.target, statechartTarget: scTarget });
    }
  }

  const pass = missing.length === 0 && phantom.length === 0 && mismatches.length === 0;
  return { pass, missing, phantom, mismatches, deferredCount: deferred.size };
}

// ── Verifier: context-chain ───────────────────────────────────────────────────
//
// Reads the statechart sidecar named by the verify entry. For every state with
// meta.context.owns, walks every outbound transition and demands that each
// owned key be either cleared or retained — and never both. Batch-reports per
// state. Presence-only (per doc01.06.03 §1): the verifier reasons about whether
// a key is set, not what payload it carries.

function verifyContextChain(statechartPath) {
  const statechart = JSON.parse(readFileSync(statechartPath, 'utf8'));
  const states = statechart.states || {};
  const issues = [];

  for (const [stateId, state] of Object.entries(states)) {
    const owns = state.meta && state.meta.context && state.meta.context.owns;
    if (!owns) continue;
    const ownedKeys = Object.keys(owns);
    if (ownedKeys.length === 0) continue;

    for (const [event, transition] of Object.entries(state.on || {})) {
      const clears = new Set(transition.clears || []);
      const retains = new Set(transition.retains || []);

      // Unknown keys named in clears/retains are spec errors too.
      for (const k of [...clears, ...retains]) {
        if (!ownedKeys.includes(k)) {
          issues.push({ state: stateId, event, kind: 'unknown-key', key: k });
        }
      }

      for (const key of ownedKeys) {
        const inClears = clears.has(key);
        const inRetains = retains.has(key);
        if (inClears && inRetains) {
          issues.push({ state: stateId, event, kind: 'both', key });
        } else if (!inClears && !inRetains) {
          issues.push({ state: stateId, event, kind: 'unclassified', key });
        }
      }
    }
  }

  return { pass: issues.length === 0, issues };
}

// ── Dispatch ──────────────────────────────────────────────────────────────────

const VERIFIERS = {
  'screen-inventory': (entry, mdDir) => {
    const invPath = join(mdDir, entry.sidecar);
    const refMd = resolveDocRef(entry.against.doc);
    const scPath = sidecarPath(refMd, '.statechart.json');
    return verifyScreenInventory(invPath, scPath, entry.against.key);
  },
  'context-chain': (entry, mdDir) => {
    const scPath = join(mdDir, entry.sidecar);
    return verifyContextChain(scPath);
  },
};

// ── Main ──────────────────────────────────────────────────────────────────────

function main() {
  const arg = process.argv[2];
  const root = arg ? resolve(arg) : CARTA_ROOT;

  const mdFiles = walkMd(root);
  let anyFail = false;

  for (const mdPath of mdFiles) {
    const text = readFileSync(mdPath, 'utf8');
    const fm = parseFrontmatter(text);
    const entries = extractVerify(fm);
    if (!entries || entries.length === 0) continue;

    const rel = relative(CARTA_ROOT, mdPath);
    console.log(`\n${rel}`);

    for (const entry of entries) {
      const verifier = VERIFIERS[entry.kind];
      const label = `[${entry.kind}/${entry.against?.key ?? entry.sidecar}]`;

      if (!verifier) {
        console.log(`  ✗ ${label} unknown verifier kind`);
        anyFail = true;
        continue;
      }

      let result;
      try {
        result = verifier(entry, dirname(mdPath));
      } catch (err) {
        console.log(`  ✗ ${label} error: ${err.message}`);
        anyFail = true;
        continue;
      }

      if (result.error) {
        console.log(`  ✗ ${label} ${result.error}`);
        anyFail = true;
        continue;
      }

      if (result.pass) {
        const note = result.deferredCount > 0 ? ` (${result.deferredCount} deferred)` : '';
        console.log(`  ✓ ${label}${note}`);
      } else {
        console.log(`  ✗ ${label}`);
        if (result.missing && result.missing.length > 0)
          console.log(`    missing:  ${result.missing.join(', ')}`);
        if (result.phantom && result.phantom.length > 0)
          console.log(`    phantom:  ${result.phantom.join(', ')}`);
        for (const m of result.mismatches || []) {
          const sc = m.statechartTarget ?? '(self-transition)';
          console.log(`    mismatch: ${m.event} → inventory "${m.inventoryTarget}", statechart "${sc}"`);
        }
        if (result.issues) {
          const byState = new Map();
          for (const i of result.issues) {
            if (!byState.has(i.state)) byState.set(i.state, []);
            byState.get(i.state).push(i);
          }
          for (const [state, items] of byState) {
            console.log(`    ${state}:`);
            for (const i of items) {
              if (i.kind === 'unclassified') {
                console.log(`      ${i.event} — owned key "${i.key}" neither cleared nor retained`);
              } else if (i.kind === 'both') {
                console.log(`      ${i.event} — owned key "${i.key}" listed in both clears and retains`);
              } else if (i.kind === 'unknown-key') {
                console.log(`      ${i.event} — clears/retains names "${i.key}" which is not in meta.context.owns`);
              }
            }
          }
        }
        anyFail = true;
      }
    }
  }

  process.exit(anyFail ? 1 : 0);
}

main();
