#!/usr/bin/env node
// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  journeys → Luminous canvas pipeline (trie / branching-paths view)      ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
// Builds a prefix trie keyed by event sequence, rooted at each start surface.
// Journeys sharing event-prefixes share trie nodes; they diverge at the
// first differing event. Leaves are journey endpoints.
//
// Trie nodes are NOT the same as screens — a trie node is "a position in
// some journey", carrying the screen it represents plus the path that got
// there. Two trie nodes may resolve to the same screen if reached by
// structurally different paths; that's the whole point of the view.
//
// Output:
//   .carta/<sub>/<base>.journeys.json
//        → .luminous/generated/<sub>/<base>.journeys-tree.graph.json
//          .luminous/generated/<sub>/<base>.journeys-tree.pack.json

import { writeFile, mkdir } from 'node:fs/promises';
import { dirname, join, relative, basename } from 'node:path';
import { createHash } from 'node:crypto';
import { validateGraphPack } from './validate-graph.mjs';
import {
  REPO_ROOT, CARTA_ROOT, GENERATED_ROOT, discoverBySuffix,
} from './statechart-lib.mjs';
import {
  readJourneys, parseJourneys, walkJourneys, loadStatechartFor,
} from './journeys-lib.mjs';

const warnings = [];
const warn = (m) => { warnings.push(m); process.stderr.write(`[warn] ${m}\n`); };

// ── trie build ───────────────────────────────────────────────────────────────
// Node id derives from (start, event-prefix) — deterministic and stable
// across runs. We hash the path to keep ids short.
function nodeIdFor(start, eventPath) {
  if (eventPath.length === 0) return `tn.${start}.root`;
  const sig = createHash('sha1').update([start, ...eventPath].join('|')).digest('hex').slice(0, 10);
  return `tn.${start}.${sig}`;
}

// Build the trie from walked steps. Output:
//   nodes: [{ id, start, depth, screen, journeys: Set, isLeaf }]
//   edges: [{ id, from, to, event, journeys: Set }]
function buildTrie(sidecar, steps) {
  const stepsByJourney = new Map();
  for (const s of steps) {
    if (!stepsByJourney.has(s.journey)) stepsByJourney.set(s.journey, []);
    stepsByJourney.get(s.journey).push(s);
  }
  for (const list of stepsByJourney.values()) list.sort((a, b) => a.order - b.order);

  const nodes = new Map(); // id → node
  const edges = new Map(); // edgeKey → edge

  const ensureNode = (start, eventPath, screen, depth) => {
    const id = nodeIdFor(start, eventPath);
    if (!nodes.has(id)) {
      nodes.set(id, { id, start, depth, screen, journeys: new Set(), isLeaf: true });
    }
    return nodes.get(id);
  };

  for (const j of sidecar.journeys) {
    const path = [];
    let parent = ensureNode(j.start, path, j.start, 0);
    parent.journeys.add(j.id);
    const journeySteps = stepsByJourney.get(j.id) ?? [];
    for (let i = 0; i < journeySteps.length; i++) {
      const s = journeySteps[i];
      path.push(s.event);
      const child = ensureNode(j.start, path, s.to, i + 1);
      child.journeys.add(j.id);
      parent.isLeaf = false;

      const edgeKey = `${parent.id}->${child.id}@${s.event}`;
      if (!edges.has(edgeKey)) {
        edges.set(edgeKey, { id: `te.${parent.id}.${s.event}`, from: parent.id, to: child.id, event: s.event, journeys: new Set() });
      }
      edges.get(edgeKey).journeys.add(j.id);
      parent = child;
    }
  }

  return {
    nodes: [...nodes.values()].map((n) => ({ ...n, journeys: [...n.journeys].sort() })),
    edges: [...edges.values()].map((e) => ({ ...e, journeys: [...e.journeys].sort() })),
  };
}

// ── project to Luminous graph ────────────────────────────────────────────────
const JOURNEY_COLOR_WHEEL = [
  '#4C8DD8', '#E8A23D', '#5BA85B', '#9B6FCB', '#D85C8D',
  '#3DB9C5', '#C5713D', '#7B8E3D', '#8E3D7B', '#3D7B8E',
];

function projectGraph(chart, sidecar, trie, packName) {
  const journeyIndex = new Map();
  sidecar.journeys.forEach((j, i) => journeyIndex.set(j.id, { ...j, index: i }));
  const colorForJourney = (id) => JOURNEY_COLOR_WHEEL[(journeyIndex.get(id)?.index ?? 0) % JOURNEY_COLOR_WHEEL.length];

  const screenInfo = (name) => {
    const def = chart.states[name];
    if (!def) return { surface: name, description: '(not defined in statechart)', isSheet: false, undeclared: true };
    const meta = def.meta ?? {};
    return { surface: meta.surface ?? name, description: def.description ?? '', isSheet: Boolean(meta.host), host: meta.host ?? null, undeclared: false };
  };

  const nodes = [];
  for (const tn of trie.nodes) {
    const info = screenInfo(tn.screen);
    nodes.push({
      id: tn.id,
      kind: 'rtp.path-step',
      props: {
        screen: tn.screen,
        surface: info.surface,
        description: info.description,
        depth: tn.depth,
        isLeaf: tn.isLeaf,
        isRoot: tn.depth === 0,
        journeys: tn.journeys,
        journeyCount: tn.journeys.length,
        sheet: info.isSheet,
        ...(info.undeclared ? { undeclared: true } : {}),
      },
      tags: [info.isSheet ? 'sheet' : 'screen', ...(tn.isLeaf ? ['leaf'] : []), ...(tn.depth === 0 ? ['root'] : [])],
    });
  }

  const edges = [];
  for (const e of trie.edges) {
    // If multiple journeys share this trie edge, the edge IS shared; pick a
    // representative color (first journey by author order). The full list
    // lives in `journeys`.
    const repColor = colorForJourney(e.journeys[0]);
    edges.push({
      id: e.id,
      kind: 'rtp.trie-edge',
      from: e.from,
      to: e.to,
      props: {
        event: e.event,
        label: e.event,
        journeys: e.journeys,
        journeyCount: e.journeys.length,
        color: repColor,
        shared: e.journeys.length > 1,
      },
      tags: e.journeys,
    });
  }

  nodes.sort((a, b) => a.id.localeCompare(b.id));
  edges.sort((a, b) => a.id.localeCompare(b.id));
  return { version: 3, pack: packName, nodes, edges, defaultView: 'tree' };
}

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
    description: 'Navigation journeys as a prefix trie: shared event-prefixes collapse into shared trie nodes; leaves are journey endpoints. Visualizes the branching structure of declared user paths.',
    nodeKinds: [
      {
        id: 'rtp.path-step',
        label: 'Path Step',
        props: { type: 'object', properties: {
          screen: { type: 'string' },
          surface: { type: 'string' },
          description: { type: 'string' },
          depth: { type: 'integer', minimum: 0 },
          isLeaf: { type: 'boolean' },
          isRoot: { type: 'boolean' },
          journeys: { type: 'array', items: { type: 'string' } },
          journeyCount: { type: 'integer', minimum: 1 },
          sheet: { type: 'boolean' },
          undeclared: { type: 'boolean' },
        }, required: ['screen', 'depth', 'isLeaf', 'isRoot', 'journeys', 'journeyCount'], additionalProperties: false },
        render: {
          peek: { type: 'text', value: '{content.screen}', style: 'heading' },
          card: { type: 'card', shape: 'rectangle', padding: 12, children: [
            { type: 'hstack', gap: 6, justify: 'space-between', children: [
              { type: 'text', value: '{content.screen}', style: 'heading' },
              { type: 'badge', value: '{content.journeyCount}× journey', tone: 'accent' },
            ] },
            { type: 'text', value: '{content.description}', style: 'caption', tone: 'muted' },
          ] },
        },
      },
    ],
    edgeKinds: [
      {
        id: 'rtp.trie-edge',
        label: 'event',
        directed: true,
        props: { type: 'object', properties: {
          event: { type: 'string' },
          label: { type: 'string' },
          journeys: { type: 'array', items: { type: 'string' } },
          journeyCount: { type: 'integer', minimum: 1 },
          color: { type: 'string' },
          shared: { type: 'boolean' },
        }, required: ['event', 'label', 'journeys', 'journeyCount'], additionalProperties: false },
        acceptsSource: ['rtp.path-step'],
        acceptsTarget: ['rtp.path-step'],
      },
    ],
    views: [
      {
        id: 'tree',
        name: 'Branching Paths',
        description: 'Prefix trie of declared journeys. Shared event prefixes collapse; leaves are journey endpoints.',
        zoomToLevel: ZOOM_TO_LEVEL,
        nodeRoles: { 'rtp.path-step': 'spatial' },
        edgeRoles: { 'rtp.trie-edge': 'arrow' },
        layers: {},
        layout: { algorithm: 'elk' },
      },
    ],
    layers: [],
    disclosure: [
      { kind: 'rtp.path-step', peek: ['screen'], card: ['screen', 'journeyCount'], open: ['screen', 'surface', 'depth', 'journeys'], deep: ['screen', 'surface', 'description', 'depth', 'isLeaf', 'journeys'] },
    ],
  };
}

async function buildOne(ref, dumpStage) {
  const raw = await readJourneys(ref);
  const sidecar = parseJourneys(raw);
  if (dumpStage === 'sidecar') return { dump: sidecar };
  const chart = await loadStatechartFor(ref, sidecar);
  const { steps } = walkJourneys(sidecar);
  if (dumpStage === 'walked') return { dump: { steps } };
  const trie = buildTrie(sidecar, steps);
  if (dumpStage === 'trie') return { dump: trie };

  const base = basename(ref.path).replace(/\.journeys\.json$/, '');
  const packName = `${base}.journeys-tree`;
  const graph = projectGraph(chart, sidecar, trie, packName);
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
    nodes: graph.nodes.length, edges: graph.edges.length,
    sharedEdges: trie.edges.filter((e) => e.journeys.length > 1).length,
    leaves: trie.nodes.filter((n) => n.isLeaf).length,
    issues,
  };
}

async function main() {
  const dumpArg = process.argv.find((a) => a.startsWith('--dump'));
  const dumpStage = dumpArg ? (dumpArg.split('=')[1] ?? 'trie') : null;
  const valid = ['sidecar', 'walked', 'trie', 'graph', 'pack'];
  if (dumpStage && !valid.includes(dumpStage)) {
    console.error(`unknown --dump stage '${dumpStage}'. valid: ${valid.join(', ')}`);
    process.exit(2);
  }
  const sidecars = await discoverBySuffix('.journeys.json');
  if (sidecars.length === 0) { console.log('No *.journeys.json sidecars found under .carta/.'); return; }

  let failed = false;
  for (const ref of sidecars) {
    const r = await buildOne(ref, dumpStage);
    if (r.dump) { console.log(JSON.stringify(r.dump, null, 2)); continue; }
    console.log(`  ✓ ${r.sourceRel}`);
    console.log(`    → ${r.graphRel} (${r.nodes} trie nodes, ${r.edges} edges, ${r.sharedEdges} shared, ${r.leaves} leaves)`);
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

main().catch((e) => { console.error('\n✗ journeys-tree crashed:\n  ' + e.message); process.exit(1); });
