#!/usr/bin/env node
// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  statechart → Luminous canvas pipeline                                   ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
// Walks .carta/ for *.statechart.json sidecars and, for each, emits a
// Luminous canvas pair into .luminous/generated/, mirroring the sidecar's
// path under .carta/:
//
//   .carta/<sub>/<base>.statechart.json
//        →  .luminous/generated/<sub>/<base>.canvas.graph.json
//           .luminous/generated/<sub>/<base>.canvas.pack.json
//
// The output tree is fully derived — it is .gitignored and never hand-edited.
// Edit the .statechart.json sidecar and re-run this pipeline instead.
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
//   discover()                       ()              → SidecarRef[]   {path}
//   read(ref)                        SidecarRef      → RawSidecar      {path, sourceRel, text}
//   parse(raw)                       RawSidecar      → Statechart      (asserted)
//   loadInventoryLabels(ref)         SidecarRef      → LabelMap        sibling 02-screens/*.inventory.json
//   extract(chart, labels, ctx)      (Statechart, LabelMap) → NavModel  (asserted)  ← THE boundary
//   projectGraph(model)              NavModel        → Graph
//   projectPack()                    ()              → Pack
//   validate(graph,pack)             (Graph,Pack)    → issue[]         (build gate)
//   emit(...)                        ...             → files written
//
// Transition labels are sourced from the inventories, not the statechart.
// Affordances declare `label` per gesture; the statechart's transition entry
// no longer needs to repeat it (one source of truth). For transitions with
// no covering affordance (system-driven, stub flows), the statechart's
// `t.label` is kept and used as the fallback.
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

import { writeFile, mkdir } from 'node:fs/promises';
import { dirname, join, relative, basename } from 'node:path';
import { validateGraphPack } from './validate-graph.mjs';
import {
  REPO_ROOT, CARTA_ROOT, GENERATED_ROOT,
  discoverBySuffix, readSidecar, parseStatechart, loadInventoryLabels,
} from './statechart-lib.mjs';

// ── Concept palette (presentation concern — applied in projectGraph only) ──────
// Each concept from doc01.03 gets a distinct color. `tone` is the in-vocabulary
// fallback (badge tones are a fixed 4-value enum). `color` is the intended hex.
const CONCEPT_PALETTE = {
  Collection:  { color: '#E8A23D', tone: 'accent'  },
  Location:    { color: '#4C8DD8', tone: 'default' },
  Review:      { color: '#5BA85B', tone: 'danger'  },
  MapOverview: { color: '#9B6FCB', tone: 'muted'   },
  LocationProvider: { color: '#D85C8D', tone: 'accent' },
};
const FALLBACK_CONCEPT = { color: '#888888', tone: 'muted' };

const warnings = [];
const warn = (m) => { warnings.push(m); process.stderr.write(`[warn] ${m}\n`); };

// ── stage: extract — Statechart → NavModel ────────────────────────────────────
// The domain boundary. Output is format-neutral: no Luminous concepts, no
// colors, no XState path syntax. Ids are stable and derived here, once.
const screenId  = (state) => `screen.${state}`;
const conceptId = (name) => `concept.${name}`;
const actionId  = (state, fullName) => `action.${state}.${fullName}`;
const transitionId = (fromId, toId, event) => `edge.transition.${fromId}.${toId}.${event}`;

function extract(chart, labels, ctx) {
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
      host: meta.host ?? null,
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

    const surfaceLabels = labels.get(state) ?? new Map();
    for (const [event, t] of Object.entries(def.on ?? {})) {
      if (t.target === undefined) continue; // self-transition — in-place, not navigation
      // Source of truth for the gesture's label is the affordance in the
      // inventory; the statechart's `t.label` is a fallback for transitions
      // no affordance fires (system-driven, deferred, stub-flow).
      const label = surfaceLabels.get(event) ?? t.label ?? '';
      transitions.push({
        id: transitionId(screenId(state), screenId(t.target), event),
        fromId: screenId(state),
        toId: screenId(t.target),
        event,
        label,
        description: t.description ?? '',
      });
    }
  }

  const concepts = [...conceptNames].sort().map((name) => ({ id: conceptId(name), name }));
  const model = { screens, concepts, actions, transitions };
  assertNavModel(model, ctx);
  return model;
}

// contract check at the NavModel boundary — every id is unique, and every
// navigation transition carries a human label (either from an inventory
// affordance or the statechart's fallback). A screen transition without a
// label is a spec gap: the picture can't say what triggers the change. The
// gate is strict — there is no silent placeholder, no auto-derived event-name
// fallback. Add a label and re-run.
function assertNavModel(model, ctx) {
  const ids = [
    ...model.screens, ...model.concepts, ...model.actions, ...model.transitions,
  ].map((x) => x.id);
  const seen = new Set();
  for (const id of ids) {
    if (seen.has(id)) warn(`${ctx}: duplicate model id '${id}'.`);
    seen.add(id);
  }
  const screenName = (id) => id.replace(/^screen\./, '');
  const unlabeled = model.transitions.filter((t) => !t.label || !String(t.label).trim());
  if (unlabeled.length) {
    const lines = unlabeled
      .map((t) => `  ${screenName(t.fromId)} --${t.event}--> ${screenName(t.toId)}`)
      .join('\n');
    throw new Error(
      `${ctx}: ${unlabeled.length} navigation transition(s) have no label — every screen transition must describe the gesture that triggers it. Add a label on an affordance in the surface's inventory, or fall back to \`label\` on the statechart transition.\n${lines}`
    );
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

  const ROOT_SCREENS = new Set(['MapOverview', 'Settings']);
  for (const s of model.screens) {
    const tier = ROOT_SCREENS.has(s.name) ? 0 : 1;
    const isSheet = Boolean(s.host);
    const props = { name: s.name, surface: s.surface, description: s.description, reads: s.reads, tier };
    if (isSheet) props.host = s.host;
    nodes.push({
      id: s.id,
      kind: isSheet ? 'rtp.sheet' : 'rtp.screen',
      props,
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
            tier: { type: 'integer', minimum: 0 },
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
        id: 'rtp.sheet',
        label: 'Sheet',
        props: {
          type: 'object',
          properties: {
            name: { type: 'string' },
            surface: { type: 'string' },
            description: { type: 'string' },
            reads: { type: 'array', items: { type: 'string' } },
            host: { type: 'string' },
            tier: { type: 'integer', minimum: 0 },
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
                  { type: 'badge', value: 'sheet', tone: 'accent' },
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
        acceptsSource: ['rtp.screen', 'rtp.sheet'],
        acceptsTarget: ['rtp.screen', 'rtp.sheet'],
      },
      {
        id: 'rtp.contains',
        label: 'nested in',
        directed: true,
        props: { type: 'object', additionalProperties: false },
        acceptsSource: ['rtp.action'],
        acceptsTarget: ['rtp.screen', 'rtp.sheet'],
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
        nodeRoles: { 'rtp.screen': 'spatial', 'rtp.sheet': 'spatial', 'rtp.action': 'spatial', 'rtp.concept': 'hidden' },
        edgeRoles: { 'rtp.transition': 'arrow', 'rtp.contains': 'contain', 'rtp.performs': 'hidden' },
        layers: {},
        layout: { algorithm: 'elk' },
      },
      {
        id: 'concepts',
        name: 'Concept Coverage',
        description: 'Each nested action drawn to the concept it belongs to; concepts carry distinct colors.',
        zoomToLevel: ZOOM_TO_LEVEL,
        nodeRoles: { 'rtp.screen': 'spatial', 'rtp.sheet': 'spatial', 'rtp.action': 'spatial', 'rtp.concept': 'spatial' },
        edgeRoles: { 'rtp.transition': 'hidden', 'rtp.contains': 'contain', 'rtp.performs': 'arrow' },
        layers: {},
        layout: { algorithm: 'elk' },
      },
    ],
    layers: [],
    disclosure: [
      { kind: 'rtp.screen', peek: ['name'], card: ['name', 'description'], open: ['name', 'surface', 'description', 'reads'], deep: ['name', 'surface', 'description', 'reads'] },
      { kind: 'rtp.sheet', peek: ['name'], card: ['name', 'description'], open: ['name', 'surface', 'host', 'description', 'reads'], deep: ['name', 'surface', 'host', 'description', 'reads'] },
      { kind: 'rtp.action', peek: ['action'], card: ['action', 'concept'], open: ['name', 'action', 'concept', 'screen'], deep: ['name', 'action', 'concept', 'color', 'screen'] },
      { kind: 'rtp.concept', peek: ['name'], card: ['name', 'color'], open: ['name', 'color'], deep: ['name', 'color'] },
    ],
  };
}

// ── driver ────────────────────────────────────────────────────────────────────
async function buildOne(ref, dumpStage) {
  const raw = await readSidecar(ref);
  const chart = parseStatechart(raw);
  if (dumpStage === 'statechart') return { dump: chart };

  const labels = await loadInventoryLabels(ref, warn);
  const model = extract(chart, labels, raw.sourceRel);
  if (dumpStage === 'navmodel') return { dump: model };

  const base = basename(ref.path).replace(/\.statechart\.json$/, '');
  const packName = `${base}.canvas`;
  const graph = projectGraph(model, packName);
  const pack = projectPack(packName);
  if (dumpStage === 'graph') return { dump: graph };
  if (dumpStage === 'pack') return { dump: pack };

  const dir = join(GENERATED_ROOT, dirname(relative(CARTA_ROOT, ref.path)));
  await mkdir(dir, { recursive: true });
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

  const sidecars = await discoverBySuffix('.statechart.json');
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
