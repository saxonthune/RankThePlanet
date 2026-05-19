#!/usr/bin/env node
// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  statechart → Luminous canvas pipeline                                   ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
// Walks .carta/ for *.statechart.json sidecars and, for each, emits a
// co-located Luminous canvas pair:
//
//   <base>.statechart.json  →  <base>.canvas.graph.json
//                              <base>.canvas.pack.json
//
// One of several Luminous pipelines living under .luminous/. This one is
// specific to navigation statecharts; others (component trees, schemas, …)
// will be separate files.
//
// ── Pipeline shape ────────────────────────────────────────────────────────────
// Each stage is a PURE function across a named, serializable contract. The
// load-bearing boundary is `NavModel` — a flat, relational, format-neutral
// model of the navigation domain. The input contract — a flat machine with
// object-form transitions naming known states (the sidecar conventions in
// doc02.02.01, NOT XState's full generality) — is enforced strictly in
// `parse`; output quirks (Luminous `kind`, `render`, palette colors) live
// only in `projectGraph`. Neither leaks.
//
//   discover()            ()              → SidecarRef[]   {path}
//   read(ref)             SidecarRef      → RawSidecar      {path, sourceRel, text}
//   parse(raw)            RawSidecar      → Statechart      (asserted)
//   extract(chart, ctx)   Statechart      → NavModel        (asserted)  ← THE boundary
//   projectGraph(model)   NavModel        → Graph
//   projectPack()         ()              → Pack
//   validate(graph,pack)  (Graph,Pack)    → issue[]         (build gate)
//   emit(...)             ...             → files written
//
// NavModel:
//   screens     : { id, surface, name, description, tags[], reads[] }[]
//   concepts    : { id, name }[]
//   actions     : { id, screenId, conceptId, fullName, action }[]
//   transitions : { id, fromId, toId, event, label, description }[]
//
// ── Debugging ─────────────────────────────────────────────────────────────────
//   node .luminous/statechart-canvas.pipeline.mjs                 build all
//   node .luminous/statechart-canvas.pipeline.mjs --dump=navmodel  print an IR,
//        --dump=<statechart|navmodel|graph|pack>                   skip emit
//
// Re-running is deterministic: ids derive from content, nodes/edges are sorted.

import { readFile, writeFile, readdir } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { dirname, join, relative, resolve, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateGraphPack } from './validate-graph.mjs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, '..');
const CARTA_ROOT = join(REPO_ROOT, '.carta');

// ── Concept palette (presentation concern — applied in projectGraph only) ──────
// Each concept from doc01.03 gets a distinct color. `tone` is the in-vocabulary
// fallback (badge tones are a fixed 4-value enum). `color` is the intended hex.
const CONCEPT_PALETTE = {
  Collection:  { color: '#E8A23D', tone: 'accent'  },
  Location:    { color: '#4C8DD8', tone: 'default' },
  Review:      { color: '#5BA85B', tone: 'danger'  },
  MapOverview: { color: '#9B6FCB', tone: 'muted'   },
};
const FALLBACK_CONCEPT = { color: '#888888', tone: 'muted' };

const warnings = [];
const warn = (m) => { warnings.push(m); process.stderr.write(`[warn] ${m}\n`); };

// ── stage: discover ───────────────────────────────────────────────────────────
async function* walk(dir) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) yield* walk(full);
    else yield full;
  }
}
async function discover() {
  if (!existsSync(CARTA_ROOT)) return [];
  const out = [];
  for await (const f of walk(CARTA_ROOT)) {
    if (f.endsWith('.statechart.json')) out.push({ path: f });
  }
  return out.sort((a, b) => a.path.localeCompare(b.path));
}

// ── stage: read ───────────────────────────────────────────────────────────────
async function read(ref) {
  return {
    path: ref.path,
    sourceRel: relative(REPO_ROOT, ref.path),
    text: await readFile(ref.path, 'utf8'),
  };
}

// ── stage: parse ──────────────────────────────────────────────────────────────
function parse(raw) {
  let chart;
  try { chart = JSON.parse(raw.text); }
  catch (e) { throw new Error(`${raw.sourceRel}: invalid JSON — ${e.message}`); }
  if (typeof chart !== 'object' || chart === null || typeof chart.states !== 'object') {
    throw new Error(`${raw.sourceRel}: not a statechart — missing 'states' object`);
  }
  assertStatechart(chart, raw.sourceRel);
  return chart;
}

// Contract check at the input boundary. Enforces the sidecar conventions from
// doc02.02.01 — a flat machine, every transition object-form with a `target`
// naming a known state — NOT XState's full generality. Parallel regions,
// substates, string-shorthand transitions, and `#machine.region` paths are
// rejected here rather than silently coped with: the design forbids them, so
// their appearance is a spec violation, not a shape to degrade gracefully on.
function assertStatechart(chart, ctx) {
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
      if (t.target === undefined) continue; // self-transition, no target — in-place, valid
      if (typeof t.target !== 'string' || !stateNames.has(t.target)) {
        throw new Error(`${ctx}: transition '${state}.${event}' targets '${t.target}' — not a known state.`);
      }
    }
  }
}

// ── stage: extract — Statechart → NavModel ────────────────────────────────────
// The domain boundary. Output is format-neutral: no Luminous concepts, no
// colors, no XState path syntax. Ids are stable and derived here, once.
const screenId  = (state) => `screen.${state}`;
const conceptId = (name) => `concept.${name}`;
const actionId  = (state, fullName) => `action.${state}.${fullName}`;
const transitionId = (fromId, toId, event) => `edge.transition.${fromId}.${toId}.${event}`;

function extract(chart, ctx) {
  // chart is already known flat, with valid object-form transitions —
  // assertStatechart guaranteed it in `parse`.
  const screens = [];
  const actions = [];
  const transitions = [];
  const conceptNames = new Set();

  for (const [state, def] of Object.entries(chart.states)) {
    const meta = def.meta ?? {};
    screens.push({
      id: screenId(state),
      surface: meta.surface ?? state,
      name: state,
      description: def.description ?? '',
      tags: def.tags ?? [],
      reads: meta.reads ?? [],
    });

    for (const fullName of meta.actions ?? []) {
      const dot = fullName.indexOf('.');
      if (dot < 0) {
        warn(`${ctx}: action '${fullName}' on '${state}' is not 'Concept.action' shaped — skipped.`);
        continue;
      }
      const concept = fullName.slice(0, dot);
      conceptNames.add(concept);
      actions.push({
        id: actionId(state, fullName),
        screenId: screenId(state),
        conceptId: conceptId(concept),
        fullName,
        action: fullName.slice(dot + 1),
      });
    }

    for (const [event, t] of Object.entries(def.on ?? {})) {
      if (t.target === undefined) continue; // self-transition — in-place, not navigation
      transitions.push({
        id: transitionId(screenId(state), screenId(t.target), event),
        fromId: screenId(state),
        toId: screenId(t.target),
        event,
        label: t.label ?? '',
        description: t.description ?? '',
      });
    }
  }

  const concepts = [...conceptNames].sort().map((name) => ({ id: conceptId(name), name }));
  const model = { screens, concepts, actions, transitions };
  assertNavModel(model, ctx);
  return model;
}

// contract check at the NavModel boundary — every id is unique. Transition
// targets are already known-valid (assertStatechart), so they need no recheck.
function assertNavModel(model, ctx) {
  const ids = [
    ...model.screens, ...model.concepts, ...model.actions, ...model.transitions,
  ].map((x) => x.id);
  const seen = new Set();
  for (const id of ids) {
    if (seen.has(id)) warn(`${ctx}: duplicate model id '${id}'.`);
    seen.add(id);
  }
}

// ── stage: projectGraph — NavModel → Graph ────────────────────────────────────
// Near-mechanical field-mapping. The only logic is dropping transitions to
// unknown screens and looking up palette colors (a Luminous-side concern).
const edgeIdC = (from, to) => `edge.contains.${from}.${to}`;
const edgeIdP = (from, to) => `edge.performs.${from}.${to}`;

function projectGraph(model, packName) {
  const nodes = [];
  const edges = [];
  const paletteFor = (concept) => {
    const pal = CONCEPT_PALETTE[concept];
    if (!pal) warn(`concept '${concept}' has no palette entry — using fallback color.`);
    return pal ?? FALLBACK_CONCEPT;
  };

  for (const s of model.screens) {
    nodes.push({
      id: s.id,
      kind: 'rtp.screen',
      props: { name: s.name, surface: s.surface, description: s.description, reads: s.reads },
      tags: s.tags,
    });
  }

  for (const c of model.concepts) {
    const pal = paletteFor(c.name);
    nodes.push({
      id: c.id,
      kind: 'rtp.concept',
      props: { name: c.name, color: pal.color, tone: pal.tone },
      tags: [],
    });
  }

  for (const a of model.actions) {
    const concept = a.conceptId.replace(/^concept\./, '');
    const pal = paletteFor(concept);
    nodes.push({
      id: a.id,
      kind: 'rtp.action',
      props: { name: a.fullName, action: a.action, concept, color: pal.color, tone: pal.tone, screen: a.screenId.replace(/^screen\./, '') },
      tags: [concept],
    });
    // containment: Luminous reads a contain edge as child=from, parent=to
    edges.push({ id: edgeIdC(a.id, a.screenId), kind: 'rtp.contains', from: a.id, to: a.screenId, props: {}, tags: [] });
    // performs: action → its concept
    edges.push({ id: edgeIdP(a.id, a.conceptId), kind: 'rtp.performs', from: a.id, to: a.conceptId, props: {}, tags: [concept] });
  }

  for (const t of model.transitions) {
    edges.push({
      id: t.id,
      kind: 'rtp.transition',
      from: t.fromId,
      to: t.toId,
      // `label` is the short human phrase drawn on the edge (Luminous reads
      // props.label). Return gestures (BACK/CANCEL/CLOSE) carry no label, so a
      // bidi corridor shows one shared label, not two.
      props: { event: t.event, label: t.label, description: t.description },
      tags: [],
    });
  }

  nodes.sort((a, b) => a.id.localeCompare(b.id));
  edges.sort((a, b) => a.id.localeCompare(b.id));
  return { version: 3, pack: packName, nodes, edges, defaultView: 'navigation' };
}

// ── stage: projectPack — static Luminous vocabulary ───────────────────────────
const ZOOM_TO_LEVEL = [
  { minZoom: 0,   level: 'peek' },
  { minZoom: 0.4, level: 'card' },
  { minZoom: 1.2, level: 'open' },
  { minZoom: 3.0, level: 'deep' },
];

function projectPack(packName) {
  return {
    id: packName,
    version: '0.1.0',
    description: 'Navigation statechart as a Luminous canvas: screens with nested concept actions, transitions between screens, and the concept each action belongs to.',
    nodeKinds: [
      {
        id: 'rtp.screen',
        label: 'Screen',
        props: {
          type: 'object',
          properties: {
            name: { type: 'string' },
            surface: { type: 'string' },
            description: { type: 'string' },
            reads: { type: 'array', items: { type: 'string' } },
          },
          required: ['name'],
          additionalProperties: false,
        },
        render: {
          peek: { type: 'text', value: '{content.name}', style: 'heading' },
          card: {
            type: 'card', shape: 'rectangle', padding: 12,
            children: [
              {
                type: 'hstack', gap: 6, justify: 'space-between',
                children: [
                  { type: 'text', value: '{content.name}', style: 'heading' },
                  { type: 'badge', value: 'screen', tone: 'muted' },
                ],
              },
              { type: 'text', value: '{content.description}', style: 'caption', tone: 'muted' },
            ],
          },
        },
      },
      {
        id: 'rtp.action',
        label: 'Action',
        props: {
          type: 'object',
          properties: {
            name: { type: 'string' },
            action: { type: 'string' },
            concept: { type: 'string' },
            color: { type: 'string' },
            tone: { type: 'string' },
            screen: { type: 'string' },
          },
          required: ['name', 'action', 'concept'],
          additionalProperties: false,
        },
        render: {
          peek: { type: 'text', value: '{content.action}', style: 'body' },
          card: {
            type: 'card', shape: 'pill', padding: 8,
            children: [
              {
                type: 'hstack', gap: 6, justify: 'space-between',
                children: [
                  { type: 'text', value: '{content.action}', style: 'body' },
                  { type: 'badge', value: '{content.concept}', tone: '{content.tone}' },
                ],
              },
            ],
          },
        },
      },
      {
        id: 'rtp.concept',
        label: 'Concept',
        props: {
          type: 'object',
          properties: {
            name: { type: 'string' },
            color: { type: 'string' },
            tone: { type: 'string' },
          },
          required: ['name'],
          additionalProperties: false,
        },
        render: {
          peek: { type: 'text', value: '{content.name}', style: 'heading' },
          card: {
            type: 'card', shape: 'pill', padding: 10,
            children: [
              {
                type: 'hstack', gap: 6, justify: 'space-between',
                children: [
                  { type: 'text', value: '{content.name}', style: 'heading' },
                  { type: 'badge', value: '{content.color}', tone: '{content.tone}' },
                ],
              },
            ],
          },
        },
      },
    ],
    edgeKinds: [
      {
        id: 'rtp.transition',
        label: 'transition',
        directed: true,
        props: {
          type: 'object',
          properties: { event: { type: 'string' }, label: { type: 'string' }, description: { type: 'string' } },
          additionalProperties: false,
        },
        acceptsSource: ['rtp.screen'],
        acceptsTarget: ['rtp.screen'],
      },
      {
        id: 'rtp.contains',
        label: 'nested in',
        directed: true,
        props: { type: 'object', additionalProperties: false },
        acceptsSource: ['rtp.action'],
        acceptsTarget: ['rtp.screen'],
      },
      {
        id: 'rtp.performs',
        label: 'performs',
        directed: true,
        props: { type: 'object', additionalProperties: false },
        acceptsSource: ['rtp.action'],
        acceptsTarget: ['rtp.concept'],
      },
    ],
    views: [
      {
        id: 'navigation',
        name: 'Navigation Flow',
        description: 'Screens with their concept actions nested inside, and the transition arrows between screens.',
        zoomToLevel: ZOOM_TO_LEVEL,
        nodeRoles: { 'rtp.screen': 'spatial', 'rtp.action': 'spatial', 'rtp.concept': 'hidden' },
        edgeRoles: { 'rtp.transition': 'arrow', 'rtp.contains': 'contain', 'rtp.performs': 'hidden' },
        layers: {},
        layout: { algorithm: 'elk' },
      },
      {
        id: 'concepts',
        name: 'Concept Coverage',
        description: 'Each nested action drawn to the concept it belongs to; concepts carry distinct colors.',
        zoomToLevel: ZOOM_TO_LEVEL,
        nodeRoles: { 'rtp.screen': 'spatial', 'rtp.action': 'spatial', 'rtp.concept': 'spatial' },
        edgeRoles: { 'rtp.transition': 'hidden', 'rtp.contains': 'contain', 'rtp.performs': 'arrow' },
        layers: {},
        layout: { algorithm: 'elk' },
      },
    ],
    layers: [],
    disclosure: [
      { kind: 'rtp.screen', peek: ['name'], card: ['name', 'description'], open: ['name', 'surface', 'description', 'reads'], deep: ['name', 'surface', 'description', 'reads'] },
      { kind: 'rtp.action', peek: ['action'], card: ['action', 'concept'], open: ['name', 'action', 'concept', 'screen'], deep: ['name', 'action', 'concept', 'color', 'screen'] },
      { kind: 'rtp.concept', peek: ['name'], card: ['name', 'color'], open: ['name', 'color'], deep: ['name', 'color'] },
    ],
  };
}

// ── driver ────────────────────────────────────────────────────────────────────
async function buildOne(ref, dumpStage) {
  const raw = await read(ref);
  const chart = parse(raw);
  if (dumpStage === 'statechart') return { dump: chart };

  const model = extract(chart, raw.sourceRel);
  if (dumpStage === 'navmodel') return { dump: model };

  const base = basename(ref.path).replace(/\.statechart\.json$/, '');
  const packName = `${base}.canvas`;
  const graph = projectGraph(model, packName);
  const pack = projectPack(packName);
  if (dumpStage === 'graph') return { dump: graph };
  if (dumpStage === 'pack') return { dump: pack };

  const dir = dirname(ref.path);
  const graphPath = join(dir, `${packName}.graph.json`);
  const packPath = join(dir, `${packName}.pack.json`);
  await writeFile(graphPath, JSON.stringify(graph, null, 2) + '\n', 'utf8');
  await writeFile(packPath, JSON.stringify(pack, null, 2) + '\n', 'utf8');

  // build gate: the emitted pair must satisfy Luminous's load validation
  const issues = validateGraphPack(graph, pack);
  return {
    sourceRel: raw.sourceRel,
    graphRel: relative(REPO_ROOT, graphPath),
    packRel: relative(REPO_ROOT, packPath),
    nodes: graph.nodes.length,
    edges: graph.edges.length,
    issues,
  };
}

async function main() {
  const dumpArg = process.argv.find((a) => a.startsWith('--dump'));
  const dumpStage = dumpArg ? (dumpArg.split('=')[1] ?? 'navmodel') : null;
  const valid = ['statechart', 'navmodel', 'graph', 'pack'];
  if (dumpStage && !valid.includes(dumpStage)) {
    console.error(`unknown --dump stage '${dumpStage}'. valid: ${valid.join(', ')}`);
    process.exit(2);
  }

  const sidecars = await discover();
  if (sidecars.length === 0) {
    console.log('No *.statechart.json sidecars found under .carta/.');
    return;
  }

  let failed = false;
  for (const ref of sidecars) {
    const r = await buildOne(ref, dumpStage);
    if (r.dump) { console.log(JSON.stringify(r.dump, null, 2)); continue; }
    console.log(`  ✓ ${r.sourceRel}`);
    console.log(`    → ${r.graphRel} (${r.nodes} nodes, ${r.edges} edges)`);
    console.log(`    → ${r.packRel}`);
    if (r.issues.length) {
      failed = true;
      console.error(`    ✗ ${r.issues.length} validation error(s):`);
      for (const i of r.issues) console.error(`      ${i}`);
    }
  }

  if (dumpStage) return;
  console.log(`\nGenerated ${sidecars.length} canvas pair(s).`);
  if (warnings.length) console.log(`${warnings.length} warning(s) — see [warn] lines above.`);
  if (failed) {
    console.error('\n✗ validation gate failed — emitted files do not satisfy Luminous load.');
    process.exit(1);
  }
}

main().catch((e) => { console.error('\n✗ pipeline failed:\n  ' + e.message); process.exit(1); });
