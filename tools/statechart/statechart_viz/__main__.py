"""CLI entry: `python3 -m statechart_viz`.

Discovers every *.statechart.json sidecar under .carta/, builds an ELK graph
for each, and writes a self-contained HTML page into ../dist/. Also writes
an index.html listing all generated pages.
"""
from __future__ import annotations
import json
import sys
from pathlib import Path

from .discover import find_sidecars
from .sanitize import safe
from .model import build_index
from .elk_graph import build_elk_graph
from .html import render_html, render_index

# This file lives at tools/statechart/statechart_viz/__main__.py
HERE = Path(__file__).resolve().parent
TOOL_ROOT = HERE.parent
REPO_ROOT = TOOL_ROOT.parent.parent
CARTA_ROOT = REPO_ROOT / ".carta"
OUT_DIR = TOOL_ROOT / "dist"


def main() -> int:
    sidecars = find_sidecars(CARTA_ROOT)
    if not sidecars:
        print(f"No *.statechart.json sidecars found under {CARTA_ROOT.relative_to(REPO_ROOT)}/.")
        return 0

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    generated: list[tuple[str, str, str]] = []
    for sidecar in sidecars:
        config = json.loads(sidecar.read_text())
        machine_id_raw = config.get("id") or sidecar.name.replace(".statechart.json", "")
        machine_id_safe = safe(machine_id_raw)
        index = build_index(machine_id_safe, config)
        graph, transitions = build_elk_graph(index)
        source_rel = sidecar.relative_to(REPO_ROOT).as_posix()
        html = render_html(
            machine_id_display=machine_id_raw,
            source_rel=source_rel,
            graph=graph,
            transitions=transitions,
            index=index,
        )
        out_name = sidecar.name.replace(".statechart.json", ".html")
        (OUT_DIR / out_name).write_text(html)
        generated.append((out_name, machine_id_raw, source_rel))
        print(f"  ✓ {source_rel} → {(OUT_DIR / out_name).relative_to(REPO_ROOT)}")

    (OUT_DIR / "index.html").write_text(render_index(generated))
    print(f"\nOpen: file://{OUT_DIR / 'index.html'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
