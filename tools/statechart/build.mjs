#!/usr/bin/env node
// Codegen: walk .carta/ for *.statechart.json sidecars; emit inline-literal
// `createMachine({...})` TypeScript files into generated/ so the Stately VS
// Code extension can visualize them.

import { readFile, writeFile, mkdir, readdir, stat } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { dirname, join, relative, resolve, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createMachine } from 'xstate';

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, '..', '..');
const CARTA_ROOT = join(REPO_ROOT, '.carta');
const OUT_DIR = join(__dirname, 'generated');

const WATCH = process.argv.includes('--watch');

async function* walk(dir) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) yield* walk(full);
    else yield full;
  }
}

function slugToCamel(slug) {
  // "01-navigation" → "navigation"; "02-foo-bar" → "fooBar"
  const stripped = slug.replace(/^\d+-/, '');
  return stripped.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
}

function exportNameFromFile(filename) {
  // "01-navigation.statechart.json" → "navigationMachine"
  const base = basename(filename).replace(/\.statechart\.json$/, '');
  return slugToCamel(base) + 'Machine';
}

function fileSlugFromFile(filename) {
  // "01-navigation.statechart.json" → "navigation.machine.ts"
  const base = basename(filename).replace(/\.statechart\.json$/, '');
  return slugToCamel(base) + '.machine.ts';
}

async function findSidecars() {
  if (!existsSync(CARTA_ROOT)) return [];
  const out = [];
  for await (const f of walk(CARTA_ROOT)) {
    if (f.endsWith('.statechart.json')) out.push(f);
  }
  return out.sort();
}

async function buildOne(sidecarPath) {
  const raw = await readFile(sidecarPath, 'utf8');
  let config;
  try { config = JSON.parse(raw); }
  catch (e) { throw new Error(`${relative(REPO_ROOT, sidecarPath)}: invalid JSON — ${e.message}`); }

  // Validate by actually creating the machine.
  try { createMachine(config); }
  catch (e) { throw new Error(`${relative(REPO_ROOT, sidecarPath)}: invalid XState config — ${e.message}`); }

  const exportName = exportNameFromFile(sidecarPath);
  const outName = fileSlugFromFile(sidecarPath);
  const outPath = join(OUT_DIR, outName);
  const sourceRel = relative(REPO_ROOT, sidecarPath);

  const body = `// GENERATED FILE — do not edit by hand.
// Source: ${sourceRel}
// Regenerate with: cd tools/statechart && npm run build
//
// This file exists so the Stately VS Code extension can statically follow the
// createMachine literal. The carta sidecar JSON is the source of truth.

import { createMachine } from 'xstate';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const ${exportName} = createMachine(${JSON.stringify(config, null, 2)} as any);
`;

  await mkdir(OUT_DIR, { recursive: true });
  await writeFile(outPath, body, 'utf8');
  return { exportName, outName, sourceRel };
}

async function buildAll() {
  const sidecars = await findSidecars();
  if (sidecars.length === 0) {
    console.log('No *.statechart.json sidecars found under .carta/.');
    return [];
  }
  const results = [];
  for (const s of sidecars) {
    const r = await buildOne(s);
    results.push(r);
    console.log(`  ✓ ${r.sourceRel} → generated/${r.outName}`);
  }
  // Re-export index.
  const indexBody = `// GENERATED — do not edit by hand.\n` +
    results.map(r => `export { ${r.exportName} } from './${r.outName.replace(/\.ts$/, '.js')}';`).join('\n') + '\n';
  await writeFile(join(OUT_DIR, 'index.ts'), indexBody, 'utf8');
  console.log(`\nGenerated ${results.length} machine(s).`);
  return results;
}

async function main() {
  try { await buildAll(); }
  catch (e) { console.error('\n✗ build failed:\n  ' + e.message); process.exit(1); }

  if (!WATCH) return;

  const { default: chokidar } = await import('chokidar');
  console.log('\nwatching .carta/ for *.statechart.json changes...');
  const watcher = chokidar.watch(`${CARTA_ROOT}/**/*.statechart.json`, { ignoreInitial: true });
  watcher.on('all', async (event, path) => {
    console.log(`\n[${event}] ${relative(REPO_ROOT, path)}`);
    try { await buildAll(); }
    catch (e) { console.error('✗ ' + e.message); }
  });
}

main();
