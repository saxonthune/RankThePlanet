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

// ── Guard predicate parser ────────────────────────────────────────────────────
//
// Grammar (recursive-descent, no operator precedence — composition is explicit):
//
//   predicate := atom | "not(" predicate ")"
//              | "and(" predicate ("," predicate)+ ")"
//              | "or("  predicate ("," predicate)+ ")"
//   atom      := "has(" context-key ")"
//              | "mode(" mode-name ")"
//              | "eq("  context-key "." field "," literal ")"
//   identifier := [a-zA-Z][a-zA-Z0-9-]*
//   literal    := "..." string | integer | "true" | "false"
//
// Returns { ok: true, ast } or { ok: false, error: { message, offset } }.
// The verifier (not the parser) validates that identifiers reference real
// context keys / mode names declared on the surface.

function parseGuard(source) {
  let i = 0;
  const src = source;

  function fail(message, at = i) {
    throw { __parseError: true, message, offset: at };
  }
  function ws() {
    while (i < src.length && /\s/.test(src[i])) i++;
  }
  function eat(ch) {
    ws();
    if (src[i] !== ch) fail(`expected '${ch}'`);
    i++;
  }
  function peekKeyword(kw) {
    ws();
    return src.slice(i, i + kw.length) === kw &&
      (i + kw.length === src.length || !/[a-zA-Z0-9-]/.test(src[i + kw.length]));
  }
  function consumeKeyword(kw) {
    if (!peekKeyword(kw)) fail(`expected '${kw}'`);
    i += kw.length;
  }
  function parseIdent() {
    ws();
    const start = i;
    if (!/[a-zA-Z]/.test(src[i] || '')) fail('expected identifier');
    while (i < src.length && /[a-zA-Z0-9-]/.test(src[i])) i++;
    return src.slice(start, i);
  }
  function parseLiteral() {
    ws();
    if (src[i] === '"') {
      i++;
      const start = i;
      while (i < src.length && src[i] !== '"') {
        if (src[i] === '\\') i++;
        i++;
      }
      if (src[i] !== '"') fail('unterminated string literal');
      const value = src.slice(start, i);
      i++;
      return { type: 'string', value };
    }
    if (peekKeyword('true')) { consumeKeyword('true'); return { type: 'bool', value: true }; }
    if (peekKeyword('false')) { consumeKeyword('false'); return { type: 'bool', value: false }; }
    if (/-|[0-9]/.test(src[i] || '')) {
      const start = i;
      if (src[i] === '-') i++;
      while (i < src.length && /[0-9]/.test(src[i])) i++;
      return { type: 'int', value: parseInt(src.slice(start, i), 10) };
    }
    fail('expected literal');
  }
  function parsePredicate() {
    ws();
    if (peekKeyword('not')) {
      consumeKeyword('not');
      eat('(');
      const child = parsePredicate();
      ws(); eat(')');
      return { op: 'not', child };
    }
    if (peekKeyword('and') || peekKeyword('or')) {
      const op = peekKeyword('and') ? 'and' : 'or';
      consumeKeyword(op);
      eat('(');
      const children = [parsePredicate()];
      ws();
      while (src[i] === ',') {
        i++;
        children.push(parsePredicate());
        ws();
      }
      eat(')');
      if (children.length < 2) fail(`'${op}' requires at least two arguments`);
      return { op, children };
    }
    if (peekKeyword('has')) {
      consumeKeyword('has');
      eat('(');
      const key = parseIdent();
      ws(); eat(')');
      return { op: 'has', key };
    }
    if (peekKeyword('mode')) {
      consumeKeyword('mode');
      eat('(');
      const name = parseIdent();
      ws(); eat(')');
      return { op: 'mode', name };
    }
    if (peekKeyword('eq')) {
      consumeKeyword('eq');
      eat('(');
      const key = parseIdent();
      eat('.');
      const field = parseIdent();
      ws(); eat(',');
      const lit = parseLiteral();
      ws(); eat(')');
      return { op: 'eq', key, field, value: lit.value, valueType: lit.type };
    }
    fail('expected predicate (has, mode, eq, not, and, or)');
  }

  try {
    const ast = parsePredicate();
    ws();
    if (i !== src.length) fail('trailing characters');
    return { ok: true, ast };
  } catch (e) {
    if (e && e.__parseError) return { ok: false, error: { message: e.message, offset: e.offset } };
    throw e;
  }
}

// Walk an AST and yield every leaf atom with its polarity (true = positive,
// false = inside an odd number of not()). Used by the verifier to derive the
// inventory comparison without re-implementing evaluation.

function* walkGuardLeaves(ast, polarity = true) {
  if (ast.op === 'not') {
    yield* walkGuardLeaves(ast.child, !polarity);
  } else if (ast.op === 'and' || ast.op === 'or') {
    for (const c of ast.children) yield* walkGuardLeaves(c, polarity);
  } else {
    yield { atom: ast, polarity };
  }
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

// ── Verifier: guard-coverage ──────────────────────────────────────────────────
//
// Reads the statechart sidecar named by the verify entry, plus every
// *.inventory.json under CARTA_ROOT. For each transition in the chart:
//
//   1. Parse the `guard` predicate (if present). Bad parse → issue.
//   2. Validate every leaf identifier against the surface's `meta.context.owns`
//      ∪ `meta.context.receives` (for has/eq keys) and `meta.modes` (for mode
//      names). Unknown identifier → issue.
//   3. Locate the inventory affordance whose `event` matches; if found, do a
//      permissive cross-check:
//        - For each positive `has(k)` leaf: if the affordance has a
//          `reactsToContext` field AND k ∉ reactsToContext → key disagreement.
//        - For each positive `mode(m)` leaf: if the affordance has an
//          `appearsInModes` field AND m ∉ appearsInModes → mode disagreement.
//        - If the transition has NO guard but the affordance has either
//          restriction field → missing-guard.
//      Atom-level rigor (mode↔context bridging, negative-polarity coverage)
//      is deferred to a future phase that models meta.modes formally.

function walkInventoryAffordances(inventory) {
  const out = [];
  for (const aff of inventory.affordances || []) {
    out.push(aff);
    for (const alt of aff.altAffordances || []) out.push(alt);
  }
  for (const list of inventory.lists || []) {
    const item = list.item;
    if (!item) continue;
    if (item.affordance) out.push(item.affordance);
    for (const alt of item.altAffordances || []) out.push(alt);
  }
  return out;
}

function loadAllInventories(root) {
  const out = new Map(); // surface name → inventory object
  function walk(dir) {
    for (const entry of readdirSync(dir)) {
      const full = join(dir, entry);
      if (statSync(full).isDirectory()) walk(full);
      else if (entry.endsWith('.inventory.json')) {
        try {
          const inv = JSON.parse(readFileSync(full, 'utf8'));
          if (inv.surface) out.set(inv.surface, inv);
        } catch {
          // Malformed JSON surfaces elsewhere (carta verify, JSON parse on demand).
        }
      }
    }
  }
  walk(root);
  return out;
}

function verifyGuardCoverage(statechartPath) {
  const statechart = JSON.parse(readFileSync(statechartPath, 'utf8'));
  const states = statechart.states || {};
  const inventories = loadAllInventories(CARTA_ROOT);
  const issues = [];

  for (const [stateId, state] of Object.entries(states)) {
    const surface = (state.meta && state.meta.surface) || stateId;
    const ownKeys = new Set([
      ...Object.keys((state.meta && state.meta.context && state.meta.context.owns) || {}),
      ...Object.keys((state.meta && state.meta.context && state.meta.context.receives) || {}),
    ]);
    const modeNames = new Set(Object.keys((state.meta && state.meta.modes) || {}));
    const inventory = inventories.get(surface);
    const affordances = inventory ? walkInventoryAffordances(inventory) : [];
    const affByEvent = new Map();
    for (const aff of affordances) {
      if (aff.event && !affByEvent.has(aff.event)) affByEvent.set(aff.event, aff);
    }

    for (const [event, transition] of Object.entries(state.on || {})) {
      const guardSrc = transition.guard;
      let ast = null;

      if (guardSrc != null) {
        const parsed = parseGuard(String(guardSrc));
        if (!parsed.ok) {
          issues.push({ state: stateId, event, kind: 'parse-error', message: parsed.error.message, offset: parsed.error.offset, source: guardSrc });
          continue;
        }
        ast = parsed.ast;

        for (const { atom } of walkGuardLeaves(ast)) {
          if (atom.op === 'has' || atom.op === 'eq') {
            if (!ownKeys.has(atom.key)) {
              issues.push({ state: stateId, event, kind: 'unknown-key', key: atom.key });
            }
          } else if (atom.op === 'mode') {
            if (!modeNames.has(atom.name)) {
              issues.push({ state: stateId, event, kind: 'unknown-mode', name: atom.name });
            }
          }
        }
      }

      const aff = affByEvent.get(event);
      if (!aff) continue; // no inventory affordance → screen-inventory check owns it

      const reactsTo = aff.reactsToContext;
      const appearsIn = aff.appearsInModes;

      if (!ast) {
        if ((reactsTo && reactsTo.length > 0) || (appearsIn && appearsIn.length > 0)) {
          issues.push({
            state: stateId, event, kind: 'missing-guard',
            inventoryReactsTo: reactsTo || [], inventoryAppearsIn: appearsIn || [],
          });
        }
        continue;
      }

      // Permissive forward check: only flag outright key/mode disagreement.
      for (const { atom, polarity } of walkGuardLeaves(ast)) {
        if (!polarity) continue; // negative-polarity atoms deferred (see header comment)
        if (atom.op === 'has' && reactsTo && reactsTo.length > 0 && !reactsTo.includes(atom.key)) {
          issues.push({
            state: stateId, event, kind: 'key-disagreement',
            guardKey: atom.key, inventoryReactsTo: reactsTo,
          });
        }
        if (atom.op === 'mode' && appearsIn && appearsIn.length > 0 && !appearsIn.includes(atom.name)) {
          issues.push({
            state: stateId, event, kind: 'mode-disagreement',
            guardMode: atom.name, inventoryAppearsIn: appearsIn,
          });
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
  'guard-coverage': (entry, mdDir) => {
    const scPath = join(mdDir, entry.sidecar);
    return verifyGuardCoverage(scPath);
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
                console.log(`      ${i.event} — references unknown context key "${i.key}"`);
              } else if (i.kind === 'unknown-mode') {
                console.log(`      ${i.event} — references unknown mode "${i.name}"`);
              } else if (i.kind === 'parse-error') {
                console.log(`      ${i.event} — guard parse error at offset ${i.offset}: ${i.message} (source: ${JSON.stringify(i.source)})`);
              } else if (i.kind === 'missing-guard') {
                const parts = [];
                if (i.inventoryAppearsIn && i.inventoryAppearsIn.length > 0) parts.push(`appearsInModes [${i.inventoryAppearsIn.join(', ')}]`);
                if (i.inventoryReactsTo && i.inventoryReactsTo.length > 0) parts.push(`reactsToContext [${i.inventoryReactsTo.join(', ')}]`);
                console.log(`      ${i.event} — affordance has ${parts.join(' and ')} but transition has no guard`);
              } else if (i.kind === 'key-disagreement') {
                console.log(`      ${i.event} — guard has(${i.guardKey}) disagrees with affordance reactsToContext [${i.inventoryReactsTo.join(', ')}]`);
              } else if (i.kind === 'mode-disagreement') {
                console.log(`      ${i.event} — guard mode(${i.guardMode}) disagrees with affordance appearsInModes [${i.inventoryAppearsIn.join(', ')}]`);
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

if (import.meta.url === `file://${process.argv[1]}`) {
  main();
}

export { parseGuard, walkGuardLeaves };
