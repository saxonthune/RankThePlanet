#!/usr/bin/env node
// Local mirror of Luminous's graph-load validation. Catches the errors the
// client throws in `loadGraphFile` (packages/core/src/loader.ts) — plus
// referential integrity — without needing a running Luminous instance.
//
// Two uses:
//   • CLI:    node .luminous/validate-graph.mjs <path-to.graph.json>
//   • module: import { validateGraphPack } from './validate-graph.mjs'
//
// ── What it checks ────────────────────────────────────────────────────────────
//   1. every node/edge `kind` is declared in the pack
//   2. every node/edge `props` matches its kind's JSON Schema
//   3. every edge `from`/`to` references an existing node id
//
// Input contracts
//   graph : { version, pack, nodes[], edges[], defaultView }
//             node = { id, kind, props, tags }
//             edge = { id, kind, from, to, props, tags }
//   pack  : { id, nodeKinds[], edgeKinds[], views[] }
//             kind = { id, props: <JSON Schema>, ... }
//   CLI resolves the pack by the co-location rule: graph.pack names
//   "<pack>.pack.json" in the SAME directory as the graph file.
//
// Output: validateGraphPack returns a string[] of issues (empty = valid).
//
// The schema validator implements the JSON Schema subset our packs use:
// type, properties, required, additionalProperties, items. If a pack starts
// using more, extend `validateSchema` — do not silently pass unknown keywords.

import { readFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// ── JSON Schema -> {ok, errors} ───────────────────────────────────────────────
// `path` is the dotted location used in error messages.
export function validateSchema(schema, data, path = 'data') {
  const errors = [];
  const typeOf = (v) =>
    v === null ? 'null' : Array.isArray(v) ? 'array' : typeof v;

  if (schema.type) {
    const actual = typeOf(data);
    const want = schema.type === 'integer' ? 'number' : schema.type;
    if (actual !== want) {
      errors.push(`${path}: expected ${schema.type}, got ${actual}`);
      return { ok: false, errors }; // type wrong — deeper checks are noise
    }
  }

  if (schema.type === 'object') {
    const props = schema.properties ?? {};
    for (const req of schema.required ?? []) {
      if (!(req in data)) errors.push(`${path}: missing required property "${req}"`);
    }
    for (const [key, val] of Object.entries(data)) {
      if (key in props) {
        errors.push(...validateSchema(props[key], val, `${path}.${key}`).errors);
      } else if (schema.additionalProperties === false) {
        errors.push(`${path}: additional property "${key}" not allowed by schema`);
      }
    }
  }

  if (schema.type === 'array' && schema.items) {
    data.forEach((el, i) => {
      errors.push(...validateSchema(schema.items, el, `${path}[${i}]`).errors);
    });
  }

  return { ok: errors.length === 0, errors };
}

// ── (graph, pack) -> issue strings ────────────────────────────────────────────
export function validateGraphPack(graph, pack) {
  const issues = [];
  const nodeSchemas = new Map(
    (pack.nodeKinds ?? []).map((k) => [k.id, k.props ?? { type: 'object' }]),
  );
  const edgeSchemas = new Map(
    (pack.edgeKinds ?? []).map((k) => [k.id, k.props ?? { type: 'object' }]),
  );
  const nodeIds = new Set((graph.nodes ?? []).map((n) => n.id));

  const checkProps = (item, schemas, what) => {
    const schema = schemas.get(item.kind);
    if (!schema) {
      issues.push(`${what} "${item.id}": unknown kind "${item.kind}"`);
      return;
    }
    const { errors } = validateSchema(schema, item.props ?? {});
    for (const e of errors) issues.push(`${what} "${item.id}": ${e}`);
  };

  for (const n of graph.nodes ?? []) checkProps(n, nodeSchemas, 'node');
  for (const e of graph.edges ?? []) {
    checkProps(e, edgeSchemas, 'edge');
    if (!nodeIds.has(e.from)) issues.push(`edge "${e.id}": dangling from "${e.from}"`);
    if (!nodeIds.has(e.to)) issues.push(`edge "${e.id}": dangling to "${e.to}"`);
  }
  return issues;
}

// ── CLI ───────────────────────────────────────────────────────────────────────
async function loadJson(path) {
  try {
    return JSON.parse(await readFile(path, 'utf8'));
  } catch (e) {
    throw new Error(`cannot read/parse ${path}: ${e.message}`);
  }
}

async function cli() {
  const graphArg = process.argv[2];
  if (!graphArg) {
    console.error('usage: node .luminous/validate-graph.mjs <path-to.graph.json>');
    process.exit(2);
  }
  const graphPath = resolve(graphArg);
  const graph = await loadJson(graphPath);
  const packPath = join(dirname(graphPath), `${graph.pack}.pack.json`);
  const pack = await loadJson(packPath);

  const issues = validateGraphPack(graph, pack);
  console.log(`graph: ${graphPath}`);
  console.log(`pack:  ${packPath}`);
  console.log(
    `checked ${(graph.nodes ?? []).length} nodes, ${(graph.edges ?? []).length} edges`,
  );
  if (issues.length === 0) {
    console.log('✓ valid — props, kinds, and references all check out');
    return;
  }
  console.error(`\n✗ ${issues.length} validation error(s):`);
  for (const i of issues) console.error(`  ${i}`);
  process.exit(1);
}

// run the CLI only when invoked directly, not when imported
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  cli().catch((e) => {
    console.error(`✗ ${e.message}`);
    process.exit(2);
  });
}
