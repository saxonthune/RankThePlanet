#!/usr/bin/env node
// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  journeys → Luminous canvas pipeline                                     ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
// Walks .carta/ for *.journeys.json sidecars and emits one Luminous canvas
// pair per sidecar under .luminous/generated/, mirroring the sidecar's path:
//
//   .carta/<sub>/<base>.journeys.json
//        → .luminous/generated/<sub>/<base>.journeys-canvas.graph.json
//          .luminous/generated/<sub>/<base>.journeys-canvas.pack.json
//
// Output tree is .gitignored and never hand-edited. Edit the journeys.json
// sidecar and re-run.
//
// Pipeline:
//   discoverBySuffix('.journeys.json')   → SidecarRef[]
//   readJourneys                         → RawSidecar
//   parseJourneys                        → JourneySidecar (asserted)
//   loadStatechartFor                    → Statechart (parsed via shared lib)
//   resolveJourneys                      → ResolveResult (shared with verifier)
//   projectGraph(result, chart)          → Graph
//   projectPack()                        → Pack
//   validateGraphPack                    → issues (build gate)
//   emit                                 → files
//
// Resolved steps render as rtp.journey-step edges between existing screen/
// sheet nodes (same kinds the statechart-canvas pipeline emits, so the two
// canvases look like siblings). Unresolved steps render as edges to a
// synthetic rtp.broken-target placeholder so the visible break-point IS
// the bug. The pipeline does NOT fail on unresolved steps — that gate
// lives in journeys-verify. The canvas's job is to draw the current state
// of the world, including its gaps.

import { writeFile, mkdir } from 'node:fs/promises';
import { dirname, join, relative, basename } from 'node:path';
import { validateGraphPack } from './validate-graph.mjs';
import {
  REPO_ROOT, CARTA_ROOT, GENERATED_ROOT,
  discoverBySuffix,
} from './statechart-lib.mjs';
import {
  readJourneys, parseJourneys, resolveJourneys, loadStatechartFor,
} from './journeys-lib.mjs';

const warnings = [];
const warn = (m) => { warnings.push(m); process.stderr.write(`[warn] ${m}\n`); };

// ── id derivation (deterministic — content drives ids) ───────────────────────
const screenId = (name) => `screen.${name}`;
const brokenId = (journey, order) => `broken.${journey}.${order}`;
const stepEdgeId = (s) => `edge.step.${s.journey}.${s.order}`;

// ── journey palette ──────────────────────────────────────────────────────────
// Deterministic color per journey id — picked from a small fixed wheel so
// repeated runs are stable. The renderer only uses these for the edge
// `color` prop; Luminous decides how to draw them.
const JOURNEY_COLOR_WHEEL = [
  '#4C8DD8', '#E8A23D', '#5BA85B', '#9B6FCB', '#D85C8D',
  '#3DB9C5', '#C5713D', '#7B8E3D', '#8E3D7B', '#3D7B8E',
];
function colorFor(journeyId, index) {
  return JOURNEY_COLOR_WHEEL[index % JOURNEY_COLOR_WHEEL.length];
}

// ── projectGraph ─────────────────────────────────────────────────────────────
function projectGraph(chart, sidecar, result, packName) {
  const nodes = [];
  const edges = [];

  // Screen / sheet nodes — every state reachable by ANY journey (resolved
  // `from` or `to`), plus every start surface. This is a subset of the
  // surface graph, not the whole thing; the journeys canvas is intentionally
  // a focused view, not a duplicate of the statechart canvas.
  const visited = new Set();
  for (const j of sidecar.journeys) visited.add(j.start);
  for (const s of result.steps) {
    if (s.from) visited.add(s.from);
    if (s.to) visited.add(s.to);
  }
  for (const stateName of [...visited].sort()) {
    const def = chart.states[stateName];
    if (!def) continue; // start state may be invalid; unresolved step records it elsewhere
    const meta = def.meta ?? {};
    const isSheet = Boolean(meta.host);
    const props = {
      name: stateName,
      surface: meta.surface ?? stateName,
      description: def.description ?? '',
      reads: meta.reads ?? [],
      tier: stateName === 'MapOverview' || stateName === 'Settings' ? 0 : 1,
    };
    if (isSheet) props.host = meta.host;
    nodes.push({
      id: screenId(stateName),
      kind: isSheet ? 'rtp.sheet' : 'rtp.screen',
      props,
      tags: def.tags ?? [],
    });
  }

  // Index journeys for color assignment and metadata lookup
  const journeyIndex = new Map();
  sidecar.journeys.forEach((j, i) => journeyIndex.set(j.id, { ...j, index: i }));

  // One edge per resolved step. For unresolved steps, create a placeholder
  // broken-target node and an edge to it so the break is visible on the
  // canvas. The placeholder carries the failure reason as a prop.
  for (const s of result.steps) {
    const j = journeyIndex.get(s.journey) ?? { index: 0, description: '' };
    const color = colorFor(s.journey, j.index);

    if (s.status === 'resolved') {
      edges.push({
        id: stepEdgeId(s),
        kind: 'rtp.journey-step',
        from: screenId(s.from),
        to: screenId(s.to),
        props: {
          journey: s.journey,
          journeyDescription: j.description ?? '',
          order: s.order,
          event: s.event,
          status: 'resolved',
          color,
          ...(s.note ? { note: s.note } : {}),
        },
        tags: [s.journey],
      });
      continue;
    }

    // Unresolved: synthesize a placeholder target so the canvas shows the gap.
    const placeholderId = brokenId(s.journey, s.order);
    nodes.push({
      id: placeholderId,
      kind: 'rtp.broken-target',
      props: {
        journey: s.journey,
        order: s.order,
        event: s.event,
        reason: s.reason ?? 'unresolved',
        ...(s.note ? { note: s.note } : {}),
      },
      tags: [s.journey, 'unresolved'],
    });

    // Origin: prefer the last known `from`; if even that is missing (bad
    // start state), the edge can't be drawn — record the issue and skip.
    if (!s.from) {
      warn(`${s.journey}: step ${s.order} has no origin (bad start?) — broken-target node emitted without incoming edge.`);
      continue;
    }
    edges.push({
      id: stepEdgeId(s),
      kind: 'rtp.journey-step',
      from: screenId(s.from),
      to: placeholderId,
      props: {
        journey: s.journey,
        journeyDescription: j.description ?? '',
        order: s.order,
        event: s.event,
        status: 'unresolved',
        color,
        reason: s.reason,
        ...(s.note ? { note: s.note } : {}),
      },
      tags: [s.journey, 'unresolved'],
    });
  }

  nodes.sort((a, b) => a.id.localeCompare(b.id));
  edges.sort((a, b) => a.id.localeCompare(b.id));
  return { version: 3, pack: packName, nodes, edges, defaultView: 'journeys' };
}

// ── projectPack ──────────────────────────────────────────────────────────────
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
    description: 'Navigation journeys as a Luminous canvas: screens visited by any journey, with one edge per step. Unresolved steps end in a broken-target placeholder so spec gaps are visible.',
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
        id: 'rtp.broken-target',
        label: 'Broken Target',
        props: {
          type: 'object',
          properties: {
            journey: { type: 'string' },
            order: { type: 'integer' },
            event: { type: 'string' },
            reason: { type: 'string' },
            note: { type: 'string' },
          },
          required: ['journey', 'order', 'event', 'reason'],
          additionalProperties: false,
        },
        render: {
          peek: { type: 'text', value: '⚠ {content.event}', style: 'body' },
          card: {
            type: 'card', shape: 'rectangle', padding: 10,
            children: [
              {
                type: 'hstack', gap: 6, justify: 'space-between',
                children: [
                  { type: 'text', value: '⚠ {content.event}', style: 'heading' },
                  { type: 'badge', value: 'unresolved', tone: 'danger' },
                ],
              },
              { type: 'text', value: '{content.reason}', style: 'caption', tone: 'muted' },
            ],
          },
        },
      },
    ],
    edgeKinds: [
      {
        id: 'rtp.journey-step',
        label: 'step',
        directed: true,
        props: {
          type: 'object',
          properties: {
            journey: { type: 'string' },
            journeyDescription: { type: 'string' },
            order: { type: 'integer' },
            event: { type: 'string' },
            status: { type: 'string', enum: ['resolved', 'unresolved'] },
            color: { type: 'string' },
            reason: { type: 'string' },
            note: { type: 'string' },
          },
          required: ['journey', 'order', 'event', 'status'],
          additionalProperties: false,
        },
        acceptsSource: ['rtp.screen', 'rtp.sheet'],
        acceptsTarget: ['rtp.screen', 'rtp.sheet', 'rtp.broken-target'],
      },
    ],
    views: [
      {
        id: 'journeys',
        name: 'Journey Paths',
        description: 'Every screen visited by a journey, with one colored edge per step. Broken edges end in a ⚠ placeholder so spec gaps are visible.',
        zoomToLevel: ZOOM_TO_LEVEL,
        nodeRoles: {
          'rtp.screen': 'spatial',
          'rtp.sheet': 'spatial',
          'rtp.broken-target': 'spatial',
        },
        edgeRoles: { 'rtp.journey-step': 'arrow' },
        layers: {},
        layout: { algorithm: 'elk' },
      },
    ],
    layers: [],
    disclosure: [
      { kind: 'rtp.screen', peek: ['name'], card: ['name', 'description'], open: ['name', 'surface', 'description', 'reads'], deep: ['name', 'surface', 'description', 'reads'] },
      { kind: 'rtp.sheet', peek: ['name'], card: ['name', 'description'], open: ['name', 'surface', 'host', 'description', 'reads'], deep: ['name', 'surface', 'host', 'description', 'reads'] },
      { kind: 'rtp.broken-target', peek: ['event'], card: ['event', 'reason'], open: ['journey', 'order', 'event', 'reason', 'note'], deep: ['journey', 'order', 'event', 'reason', 'note'] },
    ],
  };
}

// ── driver ───────────────────────────────────────────────────────────────────
async function buildOne(ref, dumpStage) {
  const raw = await readJourneys(ref);
  const sidecar = parseJourneys(raw);
  if (dumpStage === 'sidecar') return { dump: sidecar };

  const chart = await loadStatechartFor(ref, sidecar);
  const result = resolveJourneys(chart, sidecar);
  if (dumpStage === 'resolved') return { dump: result };

  const base = basename(ref.path).replace(/\.journeys\.json$/, '');
  const packName = `${base}.journeys-canvas`;
  const graph = projectGraph(chart, sidecar, result, packName);
  const pack = projectPack(packName);
  if (dumpStage === 'graph') return { dump: graph };
  if (dumpStage === 'pack') return { dump: pack };

  const dir = join(GENERATED_ROOT, dirname(relative(CARTA_ROOT, ref.path)));
  await mkdir(dir, { recursive: true });
  const graphPath = join(dir, `${packName}.graph.json`);
  const packPath = join(dir, `${packName}.pack.json`);
  await writeFile(graphPath, JSON.stringify(graph, null, 2) + '\n', 'utf8');
  await writeFile(packPath, JSON.stringify(pack, null, 2) + '\n', 'utf8');

  const issues = validateGraphPack(graph, pack);
  return {
    sourceRel: raw.sourceRel,
    graphRel: relative(REPO_ROOT, graphPath),
    packRel: relative(REPO_ROOT, packPath),
    nodes: graph.nodes.length,
    edges: graph.edges.length,
    unresolved: result.steps.filter((s) => s.status === 'unresolved').length,
    issues,
  };
}

async function main() {
  const dumpArg = process.argv.find((a) => a.startsWith('--dump'));
  const dumpStage = dumpArg ? (dumpArg.split('=')[1] ?? 'resolved') : null;
  const valid = ['sidecar', 'resolved', 'graph', 'pack'];
  if (dumpStage && !valid.includes(dumpStage)) {
    console.error(`unknown --dump stage '${dumpStage}'. valid: ${valid.join(', ')}`);
    process.exit(2);
  }

  const sidecars = await discoverBySuffix('.journeys.json');
  if (sidecars.length === 0) {
    console.log('No *.journeys.json sidecars found under .carta/.');
    return;
  }

  let failed = false;
  for (const ref of sidecars) {
    const r = await buildOne(ref, dumpStage);
    if (r.dump) { console.log(JSON.stringify(r.dump, null, 2)); continue; }
    console.log(`  ✓ ${r.sourceRel}`);
    console.log(`    → ${r.graphRel} (${r.nodes} nodes, ${r.edges} edges, ${r.unresolved} unresolved step(s))`);
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

main().catch((e) => { console.error('\n✗ journeys-canvas crashed:\n  ' + e.message); process.exit(1); });
