"""HTML emitter — wraps an ELK graph + transitions in a self-contained page.

The generated page loads elkjs from CDN (UMD bundle), runs the layout in the
browser, and renders the result as SVG with our own click handlers and side
panel. No node_modules, no Mermaid, no `securityLevel` games.
"""
from __future__ import annotations
import json

ELK_CDN = "https://cdn.jsdelivr.net/npm/elkjs@0.9.3/lib/elk.bundled.js"


HTML_TEMPLATE = r"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>__TITLE__ — statechart</title>
<style>
  :root { color-scheme: light dark; --bg:#fafafa; --panel:#fff; --border:#ddd; --text:#222; --muted:#666; --node-fill:#fff; --node-stroke:#444; --composite-fill:#f6f6f9; --composite-stroke:#999; --edge:#666; --label-bg:#fff; --selected:#1e88e5; }
  @media (prefers-color-scheme: dark) {
    :root { --bg:#1a1a1a; --panel:#222; --border:#333; --text:#eee; --muted:#aaa; --node-fill:#2c2c2c; --node-stroke:#aaa; --composite-fill:#262630; --composite-stroke:#666; --edge:#aaa; --label-bg:#222; --selected:#64b5f6; }
  }
  html, body { margin:0; padding:0; height:100%; font-family: ui-sans-serif, system-ui, sans-serif; color:var(--text); }
  body { display:grid; grid-template-rows:auto 1fr; grid-template-columns:1fr 380px; height:100vh; overflow:hidden; }
  #header { padding:.5rem 1rem; background:var(--composite-fill); border-bottom:1px solid var(--border); font-size:12px; color:var(--muted); grid-column:1/-1; }
  #chart { grid-row:2; grid-column:1; overflow:auto; background:var(--bg); padding:1rem; }
  #panel { grid-row:2; grid-column:2; border-left:1px solid var(--border); padding:1rem; overflow:auto; background:var(--panel); font-size:14px; line-height:1.45; }
  #panel h1 { font-size:13px; text-transform:uppercase; letter-spacing:.05em; color:var(--muted); margin:0 0 .25rem; }
  #panel h2 { font-size:18px; margin:0 0 .5rem; word-break:break-all; }
  #panel .meta-line { font-size:12px; color:var(--muted); margin-bottom:1rem; }
  #panel .section { margin-top:1rem; }
  #panel .section-title { font-size:11px; text-transform:uppercase; letter-spacing:.05em; color:var(--muted); margin-bottom:.25rem; }
  #panel ul { padding-left:1.25rem; margin:0; }
  #panel code { background:var(--composite-fill); padding:1px 4px; border-radius:3px; font-size:12px; }
  #panel .chip { display:inline-block; background:var(--composite-fill); color:var(--text); padding:1px 8px; border-radius:999px; font-size:11px; margin-right:4px; border:1px solid var(--border); }
  #panel .unresolved { color:#b00; font-weight:bold; }
  svg { font-family: ui-sans-serif, system-ui, sans-serif; }
  .node rect { fill:var(--node-fill); stroke:var(--node-stroke); stroke-width:1.2; }
  .node.composite > rect { fill:var(--composite-fill); stroke:var(--composite-stroke); stroke-dasharray:0; }
  .node.parallel > rect { stroke-dasharray:4 3; }
  .node.root > rect { fill:none; stroke:var(--composite-stroke); stroke-dasharray:6 3; }
  .node text.label { fill:var(--text); font-size:13px; }
  .node.composite > text.label, .node.root > text.label { font-size:11px; text-transform:uppercase; letter-spacing:.06em; fill:var(--muted); font-weight:600; }
  .node { cursor:pointer; }
  .node.selected > rect { stroke:var(--selected); stroke-width:2.5; }
  .edge path { fill:none; stroke:var(--edge); stroke-width:1.1; }
  .edge text { fill:var(--text); font-size:11px; }
  .edge text.bg { stroke:var(--label-bg); stroke-width:3; paint-order:stroke; }
</style>
</head>
<body>
  <div id="header"><strong>__TITLE__</strong> &middot; source: <code>__SOURCE_REL__</code> &middot; click any state for details &middot; layout: elkjs (layered)</div>
  <div id="chart"><div id="chart-status">laying out…</div><svg id="chart-svg"></svg></div>
  <aside id="panel">
    <h1>Selection</h1>
    <h2 id="sel-name">(click a state)</h2>
    <div class="meta-line" id="sel-path"></div>
    <div id="sel-body"></div>
  </aside>

  <script type="application/json" id="rtp-data">__DATA_BLOB__</script>
  <script src="__ELK_CDN__"></script>
  <script>
  (async () => {
    const SVG_NS = 'http://www.w3.org/2000/svg';
    const data = JSON.parse(document.getElementById('rtp-data').textContent);
    const esc = s => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

    // --- Layout ---
    const elk = new ELK();
    let laid;
    try {
      laid = await elk.layout(data.graph);
    } catch (err) {
      document.getElementById('chart-status').textContent = 'layout error: ' + (err.message || err);
      console.error(err);
      return;
    }
    document.getElementById('chart-status').remove();

    // --- Index nodes by id for click selection ---
    const nodeRectById = new Map();

    // --- Render ---
    const svg = document.getElementById('chart-svg');
    const root = laid.children[0]; // single machine root
    const padding = 20;
    svg.setAttribute('width', root.width + padding * 2);
    svg.setAttribute('height', root.height + padding * 2);
    svg.setAttribute('viewBox', `0 0 ${root.width + padding * 2} ${root.height + padding * 2}`);

    // Arrow marker
    const defs = document.createElementNS(SVG_NS, 'defs');
    defs.innerHTML = `<marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="var(--edge)"/></marker>`;
    svg.appendChild(defs);

    // Outer translate so the chart isn't flush against the SVG edge
    const outer = document.createElementNS(SVG_NS, 'g');
    outer.setAttribute('transform', `translate(${padding}, ${padding})`);
    svg.appendChild(outer);

    function renderNode(node, parentG) {
      const g = document.createElementNS(SVG_NS, 'g');
      g.setAttribute('transform', `translate(${node.x || 0}, ${node.y || 0})`);
      g.setAttribute('data-id', node.id);
      const meta = node.rtpMeta || {};
      const hasChildren = node.children && node.children.length > 0;
      let kind = 'atomic';
      if (meta.isRoot) kind = 'root';
      else if (hasChildren) kind = 'composite';
      g.setAttribute('class', 'node ' + kind + (meta.isParallel ? ' parallel' : ''));

      const rect = document.createElementNS(SVG_NS, 'rect');
      rect.setAttribute('width', node.width);
      rect.setAttribute('height', node.height);
      rect.setAttribute('rx', hasChildren ? 6 : 8);
      rect.setAttribute('ry', hasChildren ? 6 : 8);
      g.appendChild(rect);
      nodeRectById.set(node.id, g);

      const labelText = (node.labels && node.labels[0] && node.labels[0].text) || node.id;
      const label = document.createElementNS(SVG_NS, 'text');
      label.setAttribute('class', 'label');
      if (hasChildren) {
        label.setAttribute('x', 10);
        label.setAttribute('y', 18);
      } else {
        label.setAttribute('x', node.width / 2);
        label.setAttribute('y', node.height / 2 + 4);
        label.setAttribute('text-anchor', 'middle');
      }
      label.textContent = labelText;
      g.appendChild(label);

      // Click handler — only on atomic + composite nodes (skip root frame)
      if (!meta.isRoot) {
        g.addEventListener('click', (ev) => {
          ev.stopPropagation();
          selectNode(node.id);
        });
      }

      // Render children + this-level edges *after* the rect/label so they
      // appear on top of the composite background.
      if (hasChildren) {
        for (const child of node.children) renderNode(child, g);
      }
      if (node.edges) {
        for (const edge of node.edges) renderEdge(edge, g);
      }

      parentG.appendChild(g);
      return g;
    }

    function renderEdge(edge, parentG) {
      const g = document.createElementNS(SVG_NS, 'g');
      g.setAttribute('class', 'edge');
      g.setAttribute('data-edge-id', edge.id);
      // ELK puts edge geometry in `sections`; usually one section per edge
      for (const section of (edge.sections || [])) {
        const points = [section.startPoint, ...(section.bendPoints || []), section.endPoint];
        const d = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
        const path = document.createElementNS(SVG_NS, 'path');
        path.setAttribute('d', d);
        path.setAttribute('marker-end', 'url(#arrow)');
        g.appendChild(path);
      }
      // Edge label
      const label = (edge.labels && edge.labels[0]) || null;
      if (label && label.text) {
        const lx = (label.x || 0) + (label.width || 0) / 2;
        const ly = (label.y || 0) + (label.height || 0) / 2;
        const bg = document.createElementNS(SVG_NS, 'text');
        bg.setAttribute('class', 'bg');
        bg.setAttribute('x', lx);
        bg.setAttribute('y', ly);
        bg.setAttribute('text-anchor', 'middle');
        bg.setAttribute('dominant-baseline', 'middle');
        bg.textContent = label.text;
        g.appendChild(bg);
        const fg = document.createElementNS(SVG_NS, 'text');
        fg.setAttribute('x', lx);
        fg.setAttribute('y', ly);
        fg.setAttribute('text-anchor', 'middle');
        fg.setAttribute('dominant-baseline', 'middle');
        fg.textContent = label.text;
        g.appendChild(fg);
      }
      parentG.appendChild(g);
    }

    renderNode(root, outer);

    // --- Selection / side panel ---
    let selectedId = null;
    function selectNode(flatId) {
      if (selectedId && nodeRectById.has(selectedId)) nodeRectById.get(selectedId).classList.remove('selected');
      selectedId = flatId;
      if (nodeRectById.has(flatId)) nodeRectById.get(flatId).classList.add('selected');
      const meta = data.stateMeta[flatId];
      const nameEl = document.getElementById('sel-name');
      const pathEl = document.getElementById('sel-path');
      const bodyEl = document.getElementById('sel-body');
      if (!meta) { nameEl.textContent = flatId; pathEl.textContent = '(unknown)'; bodyEl.innerHTML = ''; return; }
      nameEl.textContent = meta.path === '(root)' ? data.machineId : meta.path.split('.').pop();
      pathEl.innerHTML = '<code>' + esc(meta.path) + '</code> &middot; ' + esc(meta.kind);
      let html = '';
      if (meta.description) html += '<div class="section"><div class="section-title">Description</div>' + esc(meta.description) + '</div>';
      if (meta.tags && meta.tags.length) html += '<div class="section"><div class="section-title">Tags</div>' + meta.tags.map(t => '<span class="chip">' + esc(t) + '</span>').join('') + '</div>';
      if (meta.metaFields && Object.keys(meta.metaFields).length) {
        html += '<div class="section"><div class="section-title">Meta</div><ul>';
        for (const [k, v] of Object.entries(meta.metaFields)) html += '<li><code>' + esc(k) + '</code>: ' + esc(JSON.stringify(v)) + '</li>';
        html += '</ul></div>';
      }
      const out = data.transitions.filter(t => t.sourceFlatId === flatId);
      if (out.length) {
        html += '<div class="section"><div class="section-title">Outgoing</div><ul>';
        for (const t of out) {
          html += '<li><strong>' + esc(t.event) + '</strong>';
          if (t.rawTarget) html += ' &rarr; <code>' + esc(t.rawTarget) + '</code>'; else html += ' (self)';
          if (t.unresolved) html += ' <span class="unresolved">unresolved</span>';
          if (t.crossRegion) html += ' <span class="chip">cross-region</span>';
          if (t.selfLoop && !t.rawTarget) html += ' <span class="chip">self-action</span>';
          if (t.actions && t.actions.length) html += '<br><small>' + t.actions.map(a => '<code>' + esc(a) + '</code>').join(', ') + '</small>';
          if (t.description) html += '<br><small>' + esc(t.description) + '</small>';
          html += '</li>';
        }
        html += '</ul></div>';
      }
      const inc = data.transitions.filter(t => t.targetFlatId === flatId && t.sourceFlatId !== flatId);
      if (inc.length) {
        html += '<div class="section"><div class="section-title">Incoming</div><ul>';
        for (const t of inc) {
          const src = data.stateMeta[t.sourceFlatId];
          html += '<li><code>' + esc(src ? src.path : t.sourceFlatId) + '</code> on <strong>' + esc(t.event) + '</strong></li>';
        }
        html += '</ul></div>';
      }
      bodyEl.innerHTML = html;
    }
    // Default selection: machine root.
    const firstClickable = Object.keys(data.stateMeta).find(k => data.stateMeta[k].kind !== 'root');
    if (firstClickable) selectNode(firstClickable);

    console.log('rtp-viz: rendered', nodeRectById.size, 'nodes,', (data.transitions || []).length, 'transitions');
  })();
  </script>
</body>
</html>
"""


def render_html(*, machine_id_display: str, source_rel: str, graph: dict,
                transitions: list, index) -> str:
    state_meta = {}
    for entry in index.order:
        node = entry.node
        path = ".".join(entry.path_parts) if entry.path_parts else "(root)"
        kind = "root" if entry.parent_flat_id is None else (
            "compound" if node.get("states") else "atomic"
        )
        state_meta[entry.flat_id] = {
            "path": path,
            "kind": kind,
            "description": node.get("description", ""),
            "tags": node.get("tags", []),
            "metaFields": node.get("meta", {}),
            "isParallel": node.get("type") == "parallel",
        }
    data_blob = json.dumps({
        "machineId": machine_id_display,
        "graph": graph,
        "transitions": transitions,
        "stateMeta": state_meta,
    })
    return (HTML_TEMPLATE
            .replace("__TITLE__", machine_id_display)
            .replace("__SOURCE_REL__", source_rel)
            .replace("__DATA_BLOB__", data_blob)
            .replace("__ELK_CDN__", ELK_CDN))


def render_index(generated: list[tuple[str, str, str]]) -> str:
    """generated: list of (output filename, machine id display, source rel)."""
    items = "\n".join(
        f"<li><a href='./{name}'>{mid}</a> &mdash; <code>{src}</code></li>"
        for name, mid, src in generated
    )
    return (
        "<!doctype html><html><head><meta charset='utf-8'><title>RTP statecharts</title>"
        "<style>body{font-family:system-ui,sans-serif;max-width:720px;margin:2rem auto;padding:0 1rem;}li{margin:.5rem 0;}</style>"
        f"</head><body><h1>RTP statecharts</h1><ul>{items}</ul></body></html>"
    )
