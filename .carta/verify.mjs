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
//      cross-check. The `has(k)` side is **strict**: every positive `has(k)`
//      leaf requires the affordance to declare `reactsToContext` including k —
//      no mode-bridge escape. The `mode(m)` side stays permissive (deferred to
//      Phase 6's mode-vs-context bridging):
//        - For each positive `has(k)` leaf: if the affordance's
//          `reactsToContext` is missing or does not contain k → key disagreement.
//        - For each positive `mode(m)` leaf: if the affordance has an
//          `appearsInModes` field AND m ∉ appearsInModes → mode disagreement.
//        - If the transition has NO guard but the affordance has either
//          restriction field → missing-guard.
//      Negative-polarity coverage remains deferred.

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

      // Forward check. Strict on has(k): the affordance must declare
      // reactsToContext including k. Permissive on mode(m): only flag when
      // appearsInModes is present and disagrees.
      for (const { atom, polarity } of walkGuardLeaves(ast)) {
        if (!polarity) continue; // negative-polarity atoms deferred (see header comment)
        if (atom.op === 'has' && !(reactsTo && reactsTo.includes(atom.key))) {
          issues.push({
            state: stateId, event, kind: 'key-disagreement',
            guardKey: atom.key, inventoryReactsTo: reactsTo || [],
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

// ── Verifier: modality-host ───────────────────────────────────────────────────
//
// Enforces doc02.04 Rule 2 ("overlay surface = state owned by host's UiState,
// not a separate route") mechanically. Reads only `meta.modality`, `meta.host`,
// and `meta.hostsSheets`. Bidirectional: every overlay state must name a
// fullScreen host that lists it in hostsSheets, and every hostsSheets entry
// must resolve to an overlay state whose `host` points back. Strict placement:
// `host` is illegal on fullScreen states, `hostsSheets` is illegal on overlays.

function verifyModalityHost(statechartPath) {
  const statechart = JSON.parse(readFileSync(statechartPath, 'utf8'));
  const states = statechart.states || {};
  const issues = [];

  for (const [stateId, state] of Object.entries(states)) {
    const meta = state.meta || {};
    const modality = meta.modality;
    const host = meta.host;
    const hostsSheets = meta.hostsSheets;

    if (modality === 'fullScreen') {
      if (host !== undefined) {
        issues.push({ state: stateId, kind: 'host-on-fullscreen', host });
      }
    } else if (modality !== undefined) {
      if (hostsSheets !== undefined) {
        issues.push({ state: stateId, kind: 'hostssheets-on-overlay', modality });
      }
      if (!host) {
        issues.push({ state: stateId, kind: 'missing-host', modality });
      } else {
        const hostState = states[host];
        if (!hostState) {
          issues.push({ state: stateId, kind: 'unknown-host', host });
        } else {
          const hostMeta = hostState.meta || {};
          if (hostMeta.modality !== 'fullScreen') {
            issues.push({ state: stateId, kind: 'host-not-fullscreen', host, hostModality: hostMeta.modality });
          }
          const hosted = hostMeta.hostsSheets || [];
          if (!hosted.includes(stateId)) {
            issues.push({ state: stateId, kind: 'host-missing-back-ref', host });
          }
        }
      }
    }
  }

  for (const [stateId, state] of Object.entries(states)) {
    const hosted = (state.meta && state.meta.hostsSheets) || [];
    for (const sheetId of hosted) {
      const sheetState = states[sheetId];
      if (!sheetState) {
        issues.push({ state: stateId, kind: 'phantom-sheet', sheet: sheetId });
        continue;
      }
      const sMeta = sheetState.meta || {};
      if (sMeta.modality === 'fullScreen' || sMeta.modality === undefined) {
        issues.push({ state: stateId, kind: 'sheet-not-overlay', sheet: sheetId, sheetModality: sMeta.modality });
      }
      if (sMeta.host !== stateId) {
        issues.push({ state: stateId, kind: 'sheet-host-mismatch', sheet: sheetId, sheetHost: sMeta.host });
      }
    }
  }

  return { pass: issues.length === 0, issues };
}

// ── Verifier: slot-coverage ───────────────────────────────────────────────────
//
// A `slot` is a named chrome anchor where two or more regions compete by mode
// (e.g. MapOverview's navigationIcon, filled by settingsButton in browse/
// searchResults and by closeButton in addToCollection). Slots are an optional
// string field on inventory regions, restricted to a closed enum so the
// vocabulary stays disciplined.
//
// Semantics:
//   - A region with `slot: X` and `appearsInModes: [m1, m2]` claims slot X
//     in modes m1 and m2.
//   - A region with `slot: X` and no `appearsInModes` claims slot X in every
//     mode declared on the surface's statechart state (or `_default` if the
//     surface has no `meta.modes`).
//   - A region with `appearsInModes` and no `slot` is visibility-gated but
//     not slot-tracked.
//   - A region with neither is present in all modes and not slot-tracked.
//
// Errors:
//   - `unknown-slot`: slot value not in ALLOWED_SLOTS.
//   - `unknown-mode`: claimed mode is not in the surface's meta.modes.
//   - `collision`: two or more regions claim the same (slot, mode).
//   - `orphan-slot`: a slot is named on the surface but ends up filled in
//     zero modes (every claimed mode was unknown).

const ALLOWED_SLOTS = new Set(['navigationIcon', 'fab', 'bottomBar']);

function verifySlotCoverage(inventoryPath, statechartPath, key) {
  const inventory = JSON.parse(readFileSync(inventoryPath, 'utf8'));
  const statechart = JSON.parse(readFileSync(statechartPath, 'utf8'));

  const states = statechart.states || {};
  let state = null;
  for (const [id, s] of Object.entries(states)) {
    if ((s.meta && s.meta.surface === key) || id === key) { state = s; break; }
  }
  if (!state) return { pass: false, error: `Surface "${key}" not found in statechart` };

  const modes = Object.keys((state.meta && state.meta.modes) || {});
  const modeUniverse = modes.length > 0 ? modes : ['_default'];

  const issues = [];
  const occupancy = new Map(); // "slot mode" → [regionId, ...]
  const namedSlots = new Set();

  for (const region of inventory.regions || []) {
    const slot = region.slot;
    if (slot === undefined) continue;
    if (!ALLOWED_SLOTS.has(slot)) {
      issues.push({ region: region.id, kind: 'unknown-slot', slot });
      continue;
    }
    namedSlots.add(slot);
    const claimedModes = region.appearsInModes && region.appearsInModes.length > 0
      ? region.appearsInModes
      : modeUniverse;
    for (const m of claimedModes) {
      if (!modeUniverse.includes(m)) {
        issues.push({ region: region.id, kind: 'unknown-mode', mode: m, slot });
        continue;
      }
      const k = `${slot} ${m}`;
      if (!occupancy.has(k)) occupancy.set(k, []);
      occupancy.get(k).push(region.id);
    }
  }

  for (const [k, regions] of occupancy) {
    if (regions.length > 1) {
      const [slot, mode] = k.split(' ');
      issues.push({ kind: 'collision', slot, mode, regions });
    }
  }

  for (const slot of namedSlots) {
    const anyFilled = [...occupancy.keys()].some(k => k.startsWith(slot + ' '));
    if (!anyFilled) issues.push({ kind: 'orphan-slot', slot });
  }

  return { pass: issues.length === 0, issues };
}

// ── Verifier: invariant-resolution ────────────────────────────────────────────
//
// Phase 6a of the fact-verification epic. Each inventory carries an optional
// `invariants: [{id?, text, predicate?}]` array. The verifier scans backtick-
// quoted tokens in every `text` field and resolves each against the surface's
// address space:
//
//   events  ∪ modes ∪ context-keys (owns + receives) ∪ region-ids
//           ∪ affordance-ids ∪ list-ids ∪ chart state-ids
//
// Dotted tokens like `search-context.viewportAtQuery` resolve on the left
// side; the right side is unchecked in 6a (field-level resolution lands when
// context keys gain typed payloads). Tokens containing whitespace or starting
// with a quote are treated as prose and skipped.
//
// Phase 6b adds the optional `predicate` field — a string parsed with the
// Phase 2 guard grammar (parseGuard). The predicate states a necessary
// precondition for the invariant's positive case; the prose `text` refines
// with the consequence or runtime-only part. The verifier statically checks
// the predicate by parsing it and validating every leaf-atom identifier
// against the surface's context keys (owns + receives) and modes. Trace
// evaluation against generated sequences is Kind H (Phase 8) and stays out
// of scope here.
//
// Issue kinds:
//   - missing-text           — entry has no `text` field, or it's empty.
//   - duplicate-id           — two entries share the same `id`.
//   - unknown-reference      — a backticked symbol token doesn't resolve.
//   - predicate-parse-error  — predicate string fails to parse.
//   - predicate-unknown-key  — has/eq atom names a key not on the surface.
//   - predicate-unknown-mode — mode atom names a mode not on the surface.

function verifyInvariantResolution(inventoryPath, statechartPath, key) {
  const inventory = JSON.parse(readFileSync(inventoryPath, 'utf8'));
  const statechart = JSON.parse(readFileSync(statechartPath, 'utf8'));

  const states = statechart.states || {};
  let state = null;
  for (const [id, s] of Object.entries(states)) {
    if ((s.meta && s.meta.surface === key) || id === key) { state = s; break; }
  }
  if (!state) return { pass: false, error: `Surface "${key}" not found in statechart` };

  const address = new Set();
  for (const e of Object.keys(state.on || {})) address.add(e);
  for (const m of Object.keys((state.meta && state.meta.modes) || {})) address.add(m);
  for (const k of Object.keys((state.meta && state.meta.context && state.meta.context.owns) || {})) address.add(k);
  for (const k of Object.keys((state.meta && state.meta.context && state.meta.context.receives) || {})) address.add(k);
  for (const r of inventory.regions || []) if (r.id) address.add(r.id);
  for (const aff of walkInventoryAffordances(inventory)) if (aff.id) address.add(aff.id);
  for (const l of inventory.lists || []) if (l.id) address.add(l.id);
  for (const stateId of Object.keys(states)) address.add(stateId);

  const issues = [];
  const seenIds = new Set();
  const invariants = inventory.invariants || [];

  for (let idx = 0; idx < invariants.length; idx++) {
    const entry = invariants[idx];
    const label = entry.id || `#${idx}`;

    if (entry.id) {
      if (seenIds.has(entry.id)) {
        issues.push({ kind: 'duplicate-id', id: entry.id });
      } else {
        seenIds.add(entry.id);
      }
    }

    if (!entry.text || typeof entry.text !== 'string' || entry.text.trim().length === 0) {
      issues.push({ kind: 'missing-text', label });
      continue;
    }

    for (const match of entry.text.matchAll(/`([^`]+)`/g)) {
      const raw = match[1];
      if (/\s/.test(raw)) continue;          // prose phrase
      if (raw[0] === '"' || raw[0] === "'") continue; // string literal

      const dot = raw.indexOf('.');
      const head = dot === -1 ? raw : raw.slice(0, dot);
      if (!address.has(head)) {
        issues.push({ kind: 'unknown-reference', label, token: raw });
      }
    }

    if (entry.predicate !== undefined) {
      const parsed = parseGuard(String(entry.predicate));
      if (!parsed.ok) {
        issues.push({
          kind: 'predicate-parse-error', label,
          message: parsed.error.message, offset: parsed.error.offset,
          source: entry.predicate,
        });
      } else {
        const ownKeys = new Set([
          ...Object.keys((state.meta && state.meta.context && state.meta.context.owns) || {}),
          ...Object.keys((state.meta && state.meta.context && state.meta.context.receives) || {}),
        ]);
        const modeNames = new Set(Object.keys((state.meta && state.meta.modes) || {}));
        for (const { atom } of walkGuardLeaves(parsed.ast)) {
          if (atom.op === 'has' || atom.op === 'eq') {
            if (!ownKeys.has(atom.key)) {
              issues.push({ kind: 'predicate-unknown-key', label, key: atom.key });
            }
          } else if (atom.op === 'mode') {
            if (!modeNames.has(atom.name)) {
              issues.push({ kind: 'predicate-unknown-mode', label, name: atom.name });
            }
          }
        }
      }
    }
  }

  return { pass: issues.length === 0, issues };
}

// ── Verifier: action-concept ──────────────────────────────────────────────────
//
// Joins every `Concept.action` string in chart `meta.actions` and inventory
// `affordance.action` back to a row in the concepts sidecar. Sidecar shape:
//
//   { "actions": [{"concept": "Collection", "action": "create"}, …],
//     "skipNamespaces": ["Debug", "About"] }
//
// Issue kinds:
//   - phantom         — string's namespace is a concept but the action is not
//                       a row in the sidecar (typo or stale reference).
//   - unknown-namespace — namespace is neither a concept nor in skipNamespaces
//                       (catches mis-spelled concept names).
//   - malformed       — action string has no '.' separator.
//   - orphan          — sidecar row never referenced by chart or inventory
//                       (gulf of execution; non-failing — backlog signal only).
//
// The verifier passes if phantoms / unknown-namespace / malformed are empty.
// Orphans surface in the issues array but do not fail the run.
//
// The chart is resolved via doc02.02.01 (the only navigation statechart today).
// Inventories are discovered the same way guard-coverage does — every
// *.inventory.json under CARTA_ROOT.

function verifyActionConcept(conceptsPath) {
  const concepts = JSON.parse(readFileSync(conceptsPath, 'utf8'));

  const conceptActions = new Map(); // concept → Set<action>
  for (const row of concepts.actions || []) {
    if (!conceptActions.has(row.concept)) conceptActions.set(row.concept, new Set());
    conceptActions.get(row.concept).add(row.action);
  }
  const skipNamespaces = new Set(concepts.skipNamespaces || []);
  const referenced = new Set(); // "concept.action" referenced by chart/inventory

  const issues = [];

  function classify(actionString, source) {
    if (typeof actionString !== 'string') return;
    const dot = actionString.indexOf('.');
    if (dot === -1) {
      issues.push({ kind: 'malformed', action: actionString, source });
      return;
    }
    const namespace = actionString.slice(0, dot);
    const action = actionString.slice(dot + 1);

    if (skipNamespaces.has(namespace)) return;

    const actions = conceptActions.get(namespace);
    if (!actions) {
      issues.push({ kind: 'unknown-namespace', action: actionString, namespace, source });
      return;
    }
    if (!actions.has(action)) {
      issues.push({ kind: 'phantom', action: actionString, concept: namespace, source });
      return;
    }
    referenced.add(`${namespace}.${action}`);
  }

  // Chart side
  const chartMdPath = resolveDocRef('doc02.02.01');
  const chartPath = sidecarPath(chartMdPath, '.statechart.json');
  const chart = JSON.parse(readFileSync(chartPath, 'utf8'));
  for (const [stateId, state] of Object.entries(chart.states || {})) {
    const actions = (state.meta && state.meta.actions) || [];
    for (const a of actions) classify(a, `chart:${stateId}`);
  }

  // Inventory side
  const inventories = loadAllInventories(CARTA_ROOT);
  for (const [surface, inv] of inventories) {
    for (const aff of walkInventoryAffordances(inv)) {
      if (aff.action) classify(aff.action, `inventory:${surface}${aff.event ? '.' + aff.event : ''}`);
    }
  }

  // Orphans
  for (const [concept, actions] of conceptActions) {
    for (const action of actions) {
      if (!referenced.has(`${concept}.${action}`)) {
        issues.push({ kind: 'orphan', concept, action });
      }
    }
  }

  const failing = issues.filter(i => i.kind !== 'orphan');
  const warnings = issues.filter(i => i.kind === 'orphan');
  return { pass: failing.length === 0, issues: failing, warnings };
}

// ── Verifiers: journeys-verify and journey-trace ──────────────────────────────
//
// Phase 7 of the fact-verification epic. Journeys (`03-navigation.journeys.json`,
// doc02.02.03) declare user-intent paths as parallel `events[]` / `targets[]`
// arrays. Two verifier kinds share the sidecar:
//
//   - `journeys-verify` diffs each step against the chart: chart-missing /
//     target-mismatch / unknown-surface. Issues print as WARNINGS in this
//     phase (existing journeys carry intentional documentation-of-pending-work
//     divergences); hardening to failing-mode is filed but not built. Also
//     emits a 2-switch coverage count + sample as a backlog signal.
//
//   - `journey-trace` runs a host-stack derivation over each journey and
//     diffs the derived active-context against optional `expects: []`
//     assertions per journey. Failing on any expects miss.
//
// Both share `deriveActiveContext`, the pure-JS host-stack walker.

function loadJourneysSidecar(sidecarPath, mdDir) {
  const sc = JSON.parse(readFileSync(sidecarPath, 'utf8'));
  if (!sc.statechart) {
    return { error: 'journeys sidecar missing top-level `statechart` field' };
  }
  const chartPath = join(mdDir, sc.statechart);
  const chart = JSON.parse(readFileSync(chartPath, 'utf8'));
  return { sidecar: sc, chart };
}

function deriveOwnedBy(chart) {
  const ownedBy = new Map(); // key → surface
  for (const [stateId, state] of Object.entries(chart.states || {})) {
    const owns = (state.meta && state.meta.context && state.meta.context.owns) || {};
    for (const key of Object.keys(owns)) {
      ownedBy.set(key, stateId);
    }
  }
  return ownedBy;
}

function deriveSetMap(chart) {
  // "fromState.event" → [keys to activate]
  const setMap = new Map();
  for (const [stateId, state] of Object.entries(chart.states || {})) {
    const owns = (state.meta && state.meta.context && state.meta.context.owns) || {};
    for (const [key, decl] of Object.entries(owns)) {
      for (const setEntry of decl.set || []) {
        if (!setMap.has(setEntry)) setMap.set(setEntry, []);
        setMap.get(setEntry).push(key);
      }
    }
  }
  return setMap;
}

// Walk one journey end-to-end. Returns
//   { snapshots: [{ afterEvent, active: Set<key>, stack: surface[] }],
//     brokenAt: { stepIndex, reason } | null,
//     errors: [{ kind, ... }] }
// The walker stops at the first chart-broken step (no transition / no target
// state); errors collects derivation-error events (e.g. impossible stacks).

function deriveActiveContext(chart, journey) {
  const ownedBy = deriveOwnedBy(chart);
  const setMap = deriveSetMap(chart);
  const states = chart.states || {};

  const start = journey.start;
  if (!states[start]) {
    return { snapshots: [], brokenAt: { stepIndex: -1, reason: 'unknown-start' }, errors: [] };
  }

  let stack = [start];
  const active = new Set();
  const snapshots = [];
  const errors = [];

  const events = journey.events || [];
  const targets = journey.targets || [];
  const n = Math.min(events.length, targets.length);

  for (let i = 0; i < n; i++) {
    const event = events[i];
    const declaredTarget = targets[i];
    const from = stack[stack.length - 1];
    const transition = ((states[from] || {}).on || {})[event];

    if (!transition) {
      return { snapshots, brokenAt: { stepIndex: i, reason: 'chart-missing', from, event }, errors };
    }
    const chartTarget = transition.target === undefined ? from : transition.target;
    // We derive against the chart's declared target, not the journey's claim.
    // Mismatches are journeys-verify's job to flag.
    if (!states[chartTarget]) {
      return { snapshots, brokenAt: { stepIndex: i, reason: 'unknown-target', from, event, target: chartTarget }, errors };
    }

    // 1) Apply set side.
    const setKey = `${from}.${event}`;
    for (const key of (setMap.get(setKey) || [])) {
      active.add(key);
    }

    // 2) Apply clears.
    for (const key of (transition.clears || [])) {
      active.delete(key);
    }

    // 3) Compute new stack.
    const targetMeta = (states[chartTarget].meta) || {};
    const targetModality = targetMeta.modality;
    if (chartTarget === stack[stack.length - 1]) {
      // self-transition (any modality): no stack change
    } else if (targetModality === 'fullScreen') {
      {
        const existingIdx = stack.indexOf(chartTarget);
        if (existingIdx >= 0) {
          // BACK-style pop: unmount everything above
          const unmounted = stack.slice(existingIdx + 1);
          for (const surface of unmounted) {
            for (const [k, owner] of ownedBy) {
              if (owner === surface) active.delete(k);
            }
          }
          stack = stack.slice(0, existingIdx + 1);
        } else {
          // fullScreen replace: unmount entire stack
          for (const surface of stack) {
            for (const [k, owner] of ownedBy) {
              if (owner === surface) active.delete(k);
            }
          }
          stack = [chartTarget];
        }
      }
    } else if (targetModality !== undefined) {
      // overlay/sheet: push over host
      const host = targetMeta.host;
      if (!host) {
        errors.push({ stepIndex: i, kind: 'derivation-error', detail: `target ${chartTarget} is overlay but declares no host` });
      } else if (host !== stack[stack.length - 1]) {
        if (stack.includes(host)) {
          errors.push({ stepIndex: i, kind: 'derivation-error', detail: `target ${chartTarget}'s host ${host} is in stack but not top` });
        } else {
          errors.push({ stepIndex: i, kind: 'derivation-error', detail: `target ${chartTarget}'s host ${host} is not in stack` });
        }
        // best-effort: still push
        stack = [...stack, chartTarget];
      } else {
        stack = [...stack, chartTarget];
      }
    } else {
      // unknown modality — synthetic surfaces like ExternalApp
      errors.push({ stepIndex: i, kind: 'derivation-error', detail: `target ${chartTarget} has no modality` });
      // treat as fullScreen-replace
      for (const surface of stack) {
        for (const [k, owner] of ownedBy) {
          if (owner === surface) active.delete(k);
        }
      }
      stack = [chartTarget];
    }

    snapshots.push({ afterEvent: event, active: new Set(active), stack: stack.slice() });
  }

  return { snapshots, brokenAt: null, errors };
}

function verifyJourneysVerify(sidecarPath, mdDir) {
  const loaded = loadJourneysSidecar(sidecarPath, mdDir);
  if (loaded.error) return { pass: false, error: loaded.error };
  const { sidecar, chart } = loaded;
  const states = chart.states || {};

  const issues = []; // failing — currently nothing fails this phase
  const warnings = []; // diff entries + coverage signal

  const journeyTriples = new Set(); // "state|eventA|eventB"

  for (const journey of sidecar.journeys || []) {
    const events = journey.events || [];
    const targets = journey.targets || [];
    let from = journey.start;
    if (!states[from]) {
      warnings.push({ kind: 'unknown-surface', journey: journey.id, surface: from, where: 'start' });
      continue;
    }
    for (let i = 0; i < events.length; i++) {
      const event = events[i];
      const claimedTarget = targets[i];
      const transition = ((states[from] || {}).on || {})[event];
      if (!transition) {
        warnings.push({ kind: 'chart-missing', journey: journey.id, from, event, step: i + 1 });
      } else {
        const chartTarget = transition.target === undefined ? from : transition.target;
        if (chartTarget !== claimedTarget) {
          warnings.push({ kind: 'target-mismatch', journey: journey.id, from, event, step: i + 1, claimed: claimedTarget, chart: chartTarget });
        }
      }
      if (!states[claimedTarget]) {
        warnings.push({ kind: 'unknown-surface', journey: journey.id, surface: claimedTarget, where: `step ${i + 1}` });
      }
      // 2-switch harvesting: previous event → this event at the current `from`.
      if (i > 0) {
        const prevEvent = events[i - 1];
        // The triple is (state, eventIn, eventOut): we arrived at `from` via prevEvent and fire event next.
        journeyTriples.add(`${from}|${prevEvent}|${event}`);
      }
      // Advance from along the journey-claimed target (we report mismatches, but we keep walking).
      from = claimedTarget;
    }
  }

  // Chart-side 2-switch triples: for each state S, every (eventIn, eventOut) pair where
  // some chart transition lands on S via eventIn AND S.on[eventOut] exists.
  const incoming = new Map(); // stateId → Set<event>
  for (const [stateId, state] of Object.entries(states)) {
    for (const [event, t] of Object.entries(state.on || {})) {
      const tgt = t.target === undefined ? stateId : t.target;
      if (!incoming.has(tgt)) incoming.set(tgt, new Set());
      incoming.get(tgt).add(event);
    }
  }
  const chartTriples = new Set();
  for (const [stateId, state] of Object.entries(states)) {
    const incomingEvents = incoming.get(stateId) || new Set();
    const outgoingEvents = Object.keys(state.on || {});
    for (const ein of incomingEvents) {
      for (const eout of outgoingEvents) {
        chartTriples.add(`${stateId}|${ein}|${eout}`);
      }
    }
  }
  const uncovered = [];
  for (const triple of chartTriples) {
    if (!journeyTriples.has(triple)) uncovered.push(triple);
  }
  const uncoveredCount = uncovered.length;
  const sample = uncovered.slice(0, 20).map(t => {
    const [state, ein, eout] = t.split('|');
    return { kind: 'uncovered-triple', state, eventIn: ein, eventOut: eout };
  });

  return {
    pass: issues.length === 0,
    issues,
    warnings: [...warnings, ...sample],
    coverage: { chartTriples: chartTriples.size, journeyTriples: journeyTriples.size, uncovered: uncoveredCount },
  };
}

function verifyJourneyTrace(sidecarPath, mdDir) {
  const loaded = loadJourneysSidecar(sidecarPath, mdDir);
  if (loaded.error) return { pass: false, error: loaded.error };
  const { sidecar, chart } = loaded;
  const issues = [];

  for (const journey of sidecar.journeys || []) {
    const expects = journey.expects || [];
    if (expects.length === 0) continue;

    const { snapshots, brokenAt, errors } = deriveActiveContext(chart, journey);
    for (const e of errors) {
      issues.push({ journey: journey.id, kind: 'derivation-error', stepIndex: e.stepIndex, detail: e.detail });
    }
    if (brokenAt) {
      issues.push({ journey: journey.id, kind: 'trace-broken-by-chart', stepIndex: brokenAt.stepIndex, reason: brokenAt.reason, from: brokenAt.from, event: brokenAt.event });
      continue;
    }

    const events = journey.events || [];
    for (const entry of expects) {
      const anchor = entry.afterEvent;
      if (!anchor) {
        issues.push({ journey: journey.id, kind: 'expects-missing-anchor' });
        continue;
      }
      const matches = [];
      for (let i = 0; i < events.length; i++) {
        if (events[i] === anchor) matches.push(i);
      }
      if (matches.length === 0) {
        issues.push({ journey: journey.id, kind: 'unknown-after-event', afterEvent: anchor });
        continue;
      }
      if (matches.length > 1) {
        issues.push({ journey: journey.id, kind: 'ambiguous-after-event', afterEvent: anchor, count: matches.length });
        continue;
      }
      const snap = snapshots[matches[0]];
      if (!snap) {
        issues.push({ journey: journey.id, kind: 'expects-no-snapshot', afterEvent: anchor });
        continue;
      }
      for (const key of (entry.active || [])) {
        if (!snap.active.has(key)) {
          issues.push({ journey: journey.id, kind: 'expected-active-missing', afterEvent: anchor, key });
        }
      }
      for (const key of (entry.inactive || [])) {
        if (snap.active.has(key)) {
          issues.push({ journey: journey.id, kind: 'expected-inactive-present', afterEvent: anchor, key });
        }
      }
    }
  }

  return { pass: issues.length === 0, issues };
}

// ── Verifier: generated-traces ────────────────────────────────────────────────
//
// Adversarial reachability layer (doc01.06.03 §4 Kind H). Generates random
// transition sequences from the chart's `initial` state, walks them with the
// same host-stack semantics as `deriveActiveContext`, and asserts safety
// invariants after every step. Failing traces shrink to a minimal
// counterexample (bisect-truncate, then drop each index).
//
// Properties checked per step:
//   - owner-in-stack: every active context key's owning surface is in the
//     current stack. Catches the search-context-survives-unmount class.
//   - derivation-error-free: no impossible stacks (sheet whose host is absent
//     from the stack, missing modality). Sibling sheet → sibling sheet over
//     the same host pops the source sheet first, matching doc02.04 Rule 2,
//     which `deriveActiveContext` does not model — captured here as a small
//     extension of the journey-trace semantics.
//
// Guards: evaluated against (active set, current surface's mode). Per-surface
// mode tracking uses event-name conventions (ENTER_*_MODE / EXIT_*_MODE);
// surfaces default to "browse". `eq()` atoms are payload-shape and cannot be
// evaluated — fail-closed (under-generate rather than over-generate).

function verifyGeneratedTraces(statechartPath, opts = {}) {
  const traces = opts.traces ?? 200;
  const length = opts.length ?? 20;
  const seed = opts.seed ?? 1;
  const chart = JSON.parse(readFileSync(statechartPath, 'utf8'));
  const states = chart.states || {};
  const start = chart.initial || Object.keys(states)[0];

  const ownedBy = deriveOwnedBy(chart);
  const setMap = deriveSetMap(chart);

  const guardCache = new Map();
  function getGuardAst(src) {
    if (!guardCache.has(src)) guardCache.set(src, parseGuard(src));
    return guardCache.get(src);
  }

  function evalGuard(ast, world) {
    switch (ast.op) {
      case 'not': return !evalGuard(ast.child, world);
      case 'and': return ast.children.every(c => evalGuard(c, world));
      case 'or':  return ast.children.some(c => evalGuard(c, world));
      case 'has': return world.active.has(ast.key);
      case 'mode': {
        const top = world.stack[world.stack.length - 1];
        return (world.modes.get(top) || 'browse') === ast.name;
      }
      case 'eq': return false;
    }
    return false;
  }

  function freshWorld() {
    return { stack: [start], active: new Set(), modes: new Map() };
  }

  function deactivateOwnedBy(active, modes, surface) {
    for (const [k, owner] of ownedBy) {
      if (owner === surface) active.delete(k);
    }
    modes.delete(surface);
  }

  // Returns { world, errors } or { broken: 'chart-missing' | 'unknown-target' }.
  function step(world, event) {
    const from = world.stack[world.stack.length - 1];
    const transition = ((states[from] || {}).on || {})[event];
    if (!transition) return { broken: 'chart-missing' };
    const target = transition.target === undefined ? from : transition.target;
    if (!states[target]) return { broken: 'unknown-target' };

    const active = new Set(world.active);
    const modes = new Map(world.modes);
    const errors = [];

    for (const k of (setMap.get(`${from}.${event}`) || [])) active.add(k);
    for (const k of (transition.clears || [])) active.delete(k);

    let stack;
    const targetMeta = states[target].meta || {};
    const tMod = targetMeta.modality;

    if (target === from) {
      stack = world.stack.slice();
    } else if (tMod === 'fullScreen') {
      const existing = world.stack.indexOf(target);
      if (existing >= 0) {
        for (const surface of world.stack.slice(existing + 1)) {
          deactivateOwnedBy(active, modes, surface);
        }
        stack = world.stack.slice(0, existing + 1);
      } else {
        for (const surface of world.stack) deactivateOwnedBy(active, modes, surface);
        stack = [target];
      }
    } else if (tMod !== undefined) {
      const host = targetMeta.host;
      if (!host) {
        errors.push({ kind: 'derivation-error', detail: `target ${target} is overlay but declares no host` });
        stack = [...world.stack, target];
      } else if (host === from) {
        stack = [...world.stack, target];
      } else if (world.stack.includes(host)) {
        // Sibling sheet over the same host: pop intermediate sheets, then push.
        // doc02.04 Rule 2 — overlays are owned by the host's UiState.
        const hostIdx = world.stack.indexOf(host);
        for (const surface of world.stack.slice(hostIdx + 1)) {
          deactivateOwnedBy(active, modes, surface);
        }
        stack = world.stack.slice(0, hostIdx + 1).concat([target]);
      } else {
        errors.push({ kind: 'derivation-error', detail: `target ${target}'s host ${host} is not in stack` });
        stack = [...world.stack, target];
      }
    } else {
      errors.push({ kind: 'derivation-error', detail: `target ${target} has no modality` });
      for (const surface of world.stack) deactivateOwnedBy(active, modes, surface);
      stack = [target];
    }

    const top = stack[stack.length - 1];
    const mEnter = /^ENTER_([A-Z_]+)_MODE$/.exec(event);
    const mExit = /^EXIT_([A-Z_]+)_MODE$/.exec(event);
    if (mEnter) modes.set(top, mEnter[1].toLowerCase());
    else if (mExit) modes.set(top, 'browse');

    return { world: { stack, active, modes }, errors };
  }

  function enabledTransitions(world) {
    const from = world.stack[world.stack.length - 1];
    const out = [];
    for (const [event, t] of Object.entries((states[from] || {}).on || {})) {
      if (t.guard) {
        const parsed = getGuardAst(t.guard);
        if (!parsed.ok) continue;
        if (!evalGuard(parsed.ast, world)) continue;
      }
      out.push(event);
    }
    return out;
  }

  function violations(world, stepErrors) {
    const vs = [];
    for (const k of world.active) {
      const owner = ownedBy.get(k);
      if (owner && !world.stack.includes(owner)) {
        vs.push({ kind: 'owner-not-in-stack', key: k, owner, stack: world.stack.slice() });
      }
    }
    for (const e of stepErrors || []) vs.push(e);
    return vs;
  }

  // Replay an event sequence to determine whether it still fails. Events that
  // are no longer enabled at their step are treated as a clean halt (the
  // shrinker may produce sequences that are no longer reachable; halting is
  // safer than synthesizing a fake step).
  function replay(events) {
    let world = freshWorld();
    for (let i = 0; i < events.length; i++) {
      const enabled = enabledTransitions(world);
      const ev = events[i];
      if (!enabled.includes(ev)) return { ok: true };
      const res = step(world, ev);
      if (res.broken) return { ok: true };
      world = res.world;
      const vs = violations(world, res.errors);
      if (vs.length > 0) return { failed: { events: events.slice(0, i + 1), violations: vs } };
    }
    return { ok: true };
  }

  // mulberry32 — small deterministic RNG seeded per trace.
  function makeRng(s) {
    let x = s | 0;
    return () => {
      x = (x + 0x6D2B79F5) | 0;
      let t = x;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function generate(rng) {
    let world = freshWorld();
    const events = [];
    for (let i = 0; i < length; i++) {
      const enabled = enabledTransitions(world);
      if (enabled.length === 0) break;
      const ev = enabled[Math.floor(rng() * enabled.length)];
      const res = step(world, ev);
      if (res.broken) break;
      events.push(ev);
      world = res.world;
      const vs = violations(world, res.errors);
      if (vs.length > 0) return { events, violations: vs };
    }
    return { events, violations: [] };
  }

  function shrink(events) {
    let best = events;
    // 1) Truncate from the end down to the minimal failing prefix.
    let lo = 1, hi = best.length;
    while (lo < hi) {
      const mid = (lo + hi) >> 1;
      const res = replay(best.slice(0, mid));
      if (res.failed) { best = res.failed.events; hi = best.length; }
      else lo = mid + 1;
    }
    // 2) Try dropping each index in turn; repeat until no drop succeeds.
    let changed = true;
    while (changed) {
      changed = false;
      for (let i = 0; i < best.length; i++) {
        const candidate = best.slice(0, i).concat(best.slice(i + 1));
        const res = replay(candidate);
        if (res.failed) {
          best = res.failed.events;
          changed = true;
          break;
        }
      }
    }
    return best;
  }

  const issues = [];
  const rng = makeRng(seed);
  let walks = 0;
  for (let t = 0; t < traces; t++) {
    const subSeed = ((rng() * 0x7fffffff) | 0) || 1;
    const r = generate(makeRng(subSeed));
    walks++;
    if (r.violations.length > 0) {
      const shrunk = shrink(r.events);
      const rerun = replay(shrunk);
      issues.push({
        kind: 'generated-failure',
        trace: shrunk,
        violations: rerun.failed ? rerun.failed.violations : r.violations,
        seed: subSeed,
      });
      if (issues.length >= 5) break;
    }
  }

  return { pass: issues.length === 0, issues, coverage: { walks } };
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
  'modality-host': (entry, mdDir) => {
    const scPath = join(mdDir, entry.sidecar);
    return verifyModalityHost(scPath);
  },
  'invariant-resolution': (entry, mdDir) => {
    const invPath = join(mdDir, entry.sidecar);
    const refMd = resolveDocRef(entry.against.doc);
    const scPath = sidecarPath(refMd, '.statechart.json');
    return verifyInvariantResolution(invPath, scPath, entry.against.key);
  },
  'action-concept': (entry, mdDir) => {
    const conceptsPath = join(mdDir, entry.sidecar);
    return verifyActionConcept(conceptsPath);
  },
  'slot-coverage': (entry, mdDir) => {
    const invPath = join(mdDir, entry.sidecar);
    const refMd = resolveDocRef(entry.against.doc);
    const scPath = sidecarPath(refMd, '.statechart.json');
    return verifySlotCoverage(invPath, scPath, entry.against.key);
  },
  'journeys-verify': (entry, mdDir) => {
    const sidecar = join(mdDir, entry.sidecar);
    return verifyJourneysVerify(sidecar, mdDir);
  },
  'journey-trace': (entry, mdDir) => {
    const sidecar = join(mdDir, entry.sidecar);
    return verifyJourneyTrace(sidecar, mdDir);
  },
  'generated-traces': (entry, mdDir) => {
    const scPath = join(mdDir, entry.sidecar);
    return verifyGeneratedTraces(scPath, { traces: entry.traces, length: entry.length, seed: entry.seed });
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
        const parts = [];
        if (result.deferredCount > 0) parts.push(`${result.deferredCount} deferred`);
        const warnings = result.warnings || [];
        const orphanCount = warnings.filter(w => w.kind === 'orphan').length;
        const diffCount = warnings.filter(w => ['chart-missing', 'target-mismatch', 'unknown-surface'].includes(w.kind)).length;
        const triplesCount = warnings.filter(w => w.kind === 'uncovered-triple').length;
        if (orphanCount > 0) parts.push(`${orphanCount} orphan`);
        if (diffCount > 0) parts.push(`${diffCount} diff`);
        if (result.coverage) {
          if (result.coverage.walks !== undefined) parts.push(`${result.coverage.walks} walks`);
          if (result.coverage.chartTriples !== undefined) parts.push(`2-switch: ${result.coverage.journeyTriples}/${result.coverage.chartTriples} covered, ${result.coverage.uncovered} uncovered`);
        }
        const note = parts.length > 0 ? ` (${parts.join(', ')})` : '';
        console.log(`  ✓ ${label}${note}`);
        for (const w of warnings) {
          if (w.kind === 'orphan') {
            console.log(`    orphan: ${w.concept}.${w.action} — declared but not referenced by any chart or inventory`);
          } else if (w.kind === 'chart-missing') {
            console.log(`    chart-missing: journey "${w.journey}" step ${w.step} — ${w.from}.${w.event} has no transition in chart`);
          } else if (w.kind === 'target-mismatch') {
            console.log(`    target-mismatch: journey "${w.journey}" step ${w.step} — ${w.from}.${w.event} claims "${w.claimed}", chart says "${w.chart}"`);
          } else if (w.kind === 'unknown-surface') {
            console.log(`    unknown-surface: journey "${w.journey}" ${w.where} — "${w.surface}" is not a chart state`);
          } else if (w.kind === 'uncovered-triple') {
            console.log(`    uncovered-triple: ${w.state} via ${w.eventIn} → ${w.eventOut}`);
          }
        }
        if (triplesCount > 0 && result.coverage && result.coverage.uncovered > triplesCount) {
          console.log(`    … and ${result.coverage.uncovered - triplesCount} more uncovered triples`);
        }
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
          const stateless = [];
          const byState = new Map();
          for (const i of result.issues) {
            if (i.state === undefined) { stateless.push(i); continue; }
            if (!byState.has(i.state)) byState.set(i.state, []);
            byState.get(i.state).push(i);
          }
          for (const i of stateless) {
            if (i.kind === 'phantom' && i.action !== undefined) {
              console.log(`    phantom: "${i.action}" referenced by ${i.source} — concept "${i.concept}" has no such action`);
            } else if (i.kind === 'unknown-namespace') {
              console.log(`    unknown-namespace: "${i.action}" referenced by ${i.source} — "${i.namespace}" is not a concept and not in skipNamespaces`);
            } else if (i.kind === 'malformed') {
              console.log(`    malformed: "${i.action}" referenced by ${i.source} — no '.' separator`);
            } else if (i.kind === 'missing-text') {
              console.log(`    invariant ${i.label} has no text`);
            } else if (i.kind === 'duplicate-id') {
              console.log(`    duplicate invariant id "${i.id}"`);
            } else if (i.kind === 'unknown-reference') {
              console.log(`    invariant ${i.label} references unknown symbol \`${i.token}\``);
            } else if (i.kind === 'predicate-parse-error') {
              console.log(`    invariant ${i.label} predicate parse error at offset ${i.offset}: ${i.message} (source: ${JSON.stringify(i.source)})`);
            } else if (i.kind === 'predicate-unknown-key') {
              console.log(`    invariant ${i.label} predicate references unknown context key "${i.key}"`);
            } else if (i.kind === 'predicate-unknown-mode') {
              console.log(`    invariant ${i.label} predicate references unknown mode "${i.name}"`);
            } else if (i.kind === 'unknown-slot') {
              console.log(`    region "${i.region}" declares unknown slot "${i.slot}" (allowed: navigationIcon, fab, bottomBar)`);
            } else if (i.kind === 'unknown-mode' && i.region) {
              console.log(`    region "${i.region}" slot "${i.slot}" references unknown mode "${i.mode}"`);
            } else if (i.kind === 'collision') {
              console.log(`    slot "${i.slot}" in mode "${i.mode}" claimed by [${i.regions.join(', ')}]`);
            } else if (i.kind === 'orphan-slot') {
              console.log(`    slot "${i.slot}" named on this surface but no region fills it in any mode`);
            } else if (i.kind === 'trace-broken-by-chart') {
              console.log(`    journey "${i.journey}" trace broken at step ${i.stepIndex + 1}: ${i.reason} (${i.from}.${i.event})`);
            } else if (i.kind === 'derivation-error') {
              console.log(`    journey "${i.journey}" derivation error at step ${i.stepIndex + 1}: ${i.detail}`);
            } else if (i.kind === 'unknown-after-event') {
              console.log(`    journey "${i.journey}" expects afterEvent "${i.afterEvent}" — no such event in journey`);
            } else if (i.kind === 'ambiguous-after-event') {
              console.log(`    journey "${i.journey}" expects afterEvent "${i.afterEvent}" — appears ${i.count} times; split the journey or disambiguate`);
            } else if (i.kind === 'expected-active-missing') {
              console.log(`    journey "${i.journey}" after "${i.afterEvent}" — expected key "${i.key}" to be active, but it is not`);
            } else if (i.kind === 'expected-inactive-present') {
              console.log(`    journey "${i.journey}" after "${i.afterEvent}" — expected key "${i.key}" to be inactive, but it is active`);
            } else if (i.kind === 'expects-missing-anchor') {
              console.log(`    journey "${i.journey}" expects entry has no afterEvent`);
            } else if (i.kind === 'expects-no-snapshot') {
              console.log(`    journey "${i.journey}" expects afterEvent "${i.afterEvent}" — no derived snapshot at that step`);
            } else if (i.kind === 'generated-failure') {
              console.log(`    generated-failure (seed ${i.seed}): ${i.trace.join(' → ')}`);
              for (const v of i.violations) {
                if (v.kind === 'owner-not-in-stack') {
                  console.log(`      owner-not-in-stack: key "${v.key}" owned by ${v.owner}, stack=[${v.stack.join(', ')}]`);
                } else if (v.kind === 'derivation-error') {
                  console.log(`      derivation-error: ${v.detail}`);
                }
              }
            }
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
              } else if (i.kind === 'missing-host') {
                console.log(`      modality "${i.modality}" but no host declared`);
              } else if (i.kind === 'unknown-host') {
                console.log(`      host "${i.host}" is not a state in the chart`);
              } else if (i.kind === 'host-not-fullscreen') {
                console.log(`      host "${i.host}" has modality "${i.hostModality}", expected fullScreen`);
              } else if (i.kind === 'host-missing-back-ref') {
                console.log(`      host "${i.host}" does not list this state in hostsSheets`);
              } else if (i.kind === 'host-on-fullscreen') {
                console.log(`      modality fullScreen but declares host "${i.host}"`);
              } else if (i.kind === 'hostssheets-on-overlay') {
                console.log(`      modality "${i.modality}" cannot declare hostsSheets`);
              } else if (i.kind === 'phantom-sheet') {
                console.log(`      hostsSheets entry "${i.sheet}" is not a state in the chart`);
              } else if (i.kind === 'sheet-not-overlay') {
                console.log(`      hostsSheets entry "${i.sheet}" has modality "${i.sheetModality}", expected non-fullScreen`);
              } else if (i.kind === 'sheet-host-mismatch') {
                console.log(`      hostsSheets entry "${i.sheet}" has host "${i.sheetHost}", expected this state`);
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
