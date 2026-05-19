"""Find *.statechart.json sidecars under .carta/."""
from __future__ import annotations
from pathlib import Path


def find_sidecars(carta_root: Path) -> list[Path]:
    if not carta_root.exists():
        return []
    return sorted(carta_root.rglob("*.statechart.json"))
