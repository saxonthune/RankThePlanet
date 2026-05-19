"""ID sanitization shared by indexer, ELK graph builder, and HTML renderer.

Statechart authors write machine ids like "rtp-navigation"; downstream we need
ids that work as DOM ids and JS lookup keys. Replacing '-' with '_' is enough
for our cases — path parts (PascalCase / camelCase) don't contain hyphens.
"""
from __future__ import annotations


def safe(s: str) -> str:
    return s.replace("-", "_")


def flat_id(path_parts) -> str:
    """Compose a flat id like 'overlay__ReviewForm__editing' from path parts."""
    return "__".join(safe(p) for p in path_parts) if path_parts else ""
