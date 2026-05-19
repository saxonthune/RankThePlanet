"""Index over the statechart tree: flat ids, parent links, target resolution.

Built once per machine. Used by the ELK graph builder to walk the tree and to
resolve transition targets (which may be sibling names like 'EntryDetail' or
absolute paths like '#rtp-navigation.overlay.AddToCollection').
"""
from __future__ import annotations
from dataclasses import dataclass, field
from typing import Any
from .sanitize import safe, flat_id


@dataclass
class Entry:
    flat_id: str
    parent_flat_id: str | None
    node: dict
    path_parts: list[str] = field(default_factory=list)


@dataclass
class Index:
    machine_id: str            # sanitized
    by_path: dict[str, Entry]  # keys: '#machine.a.b' (sanitized) and 'a.b' (sanitized)
    order: list[Entry]         # walk order; root first
    root: Entry


def build_index(machine_id_safe: str, root_node: dict) -> Index:
    by_path: dict[str, Entry] = {}
    order: list[Entry] = []

    def visit(node: dict, path_parts: list[str], parent_flat_id: str) -> None:
        fid = flat_id(path_parts)
        entry = Entry(flat_id=fid, parent_flat_id=parent_flat_id, node=node, path_parts=list(path_parts))
        # Both the absolute (#machine.a.b) and bare (a.b) forms point to the
        # same entry, with all hyphens already collapsed to underscores.
        safe_path = ".".join(safe(p) for p in path_parts)
        by_path["#" + machine_id_safe + ("." + safe_path if path_parts else "")] = entry
        if path_parts:
            by_path[safe_path] = entry
        order.append(entry)

        for child_key, child_node in (node.get("states") or {}).items():
            visit(child_node, path_parts + [child_key], fid)

    root_entry = Entry(flat_id=machine_id_safe, parent_flat_id=None, node=root_node, path_parts=[])
    by_path["#" + machine_id_safe] = root_entry
    order.append(root_entry)
    for child_key, child_node in (root_node.get("states") or {}).items():
        visit(child_node, [child_key], machine_id_safe)

    return Index(machine_id=machine_id_safe, by_path=by_path, order=order, root=root_entry)


def resolve_target(target: str, source_entry: Entry, index: Index) -> Entry | None:
    """Resolve an XState target string against the index.

    Targets are one of:
      - Absolute: '#rtp-navigation.overlay.AddToCollection' (may use original
        hyphenated machine id; sanitized at lookup time so it still matches).
      - Sibling: 'EntryDetail' (looked up at source's parent scope).
      - Dotted relative: 'CollectionDetail.mapProjection' (rare in our use).
    """
    if target.startswith("#"):
        return index.by_path.get(safe(target))
    parent_parts = source_entry.path_parts[:-1]
    candidate = ".".join(safe(p) for p in parent_parts + target.split("."))
    return index.by_path.get(candidate)
