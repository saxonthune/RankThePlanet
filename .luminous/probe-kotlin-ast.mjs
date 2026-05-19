// Probe: dump the tree-sitter-kotlin parse tree for one file.
// Usage: node .luminous/probe-kotlin-ast.mjs <path-to.kt>
import Parser from 'tree-sitter';
import Kotlin from 'tree-sitter-kotlin';
import { readFileSync } from 'node:fs';

const file = process.argv[2];
const src = readFileSync(file, 'utf8');
const parser = new Parser();
parser.setLanguage(Kotlin);
const tree = parser.parse(src);

function dump(node, depth = 0) {
  if (depth > 4) return;
  const named = node.isNamed ? '' : ' (anon)';
  const text = node.namedChildCount === 0 ? ` "${node.text.slice(0, 40).replace(/\n/g, '\\n')}"` : '';
  console.log('  '.repeat(depth) + node.type + named + text);
  for (let i = 0; i < node.namedChildCount; i++) dump(node.namedChild(i), depth + 1);
}
dump(tree.rootNode);
