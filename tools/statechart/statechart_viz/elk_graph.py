"""Convert a statechart Index into an ELK graph + a flat list of transitions.

The ELK graph is consumed in the browser by elkjs. We build the structure in
Python because it's pure data extraction — no layout work happens here.

Per-node `rtpMeta` fields ride along on the ELK nodes; ELK ignores unknown
keys. Our renderer reads them back when drawing labels and the side panel.
"""
from __future__ import annotations
from .model import Index, Entry, resolve_target

# ELK layout knobs tuned for statecharts: layered algorithm (good for state
# transitions), left-to-right reading order, hierarchical edges allowed across
# composite boundaries, padding leaves room for the composite label.
COMPOUND_LAYOUT_OPTIONS = {
    "elk.algorithm": "layered",
    "elk.direction": "RIGHT",
    "elk.padding": "[top=40,left=20,bottom=20,right=20]",
    "elk.spacing.nodeNode": "30",
    "elk.layered.spacing.nodeNodeBetweenLayers": "60",
    "elk.layered.spacing.edgeNodeBetweenLayers": "30",
}

ROOT_LAYOUT_OPTIONS = {
    "elk.algorithm": "layered",
    "elk.direction": "RIGHT",
    "elk.hierarchyHandling": "INCLUDE_CHILDREN",
    "elk.padding": "[top=20,left=20,bottom=20,right=20]",
}

LEAF_WIDTH = 140
LEAF_HEIGHT = 50


def _elk_node(entry: Entry) -> dict:
    node = entry.node
    label = entry.path_parts[-1] if entry.path_parts else entry.flat_id
    elk = {
        "id": entry.flat_id,
        "labels": [{"text": label}],
        "rtpMeta": {
            "path": ".".join(entry.path_parts) if entry.path_parts else "(root)",
            "description": node.get("description", ""),
            "tags": node.get("tags", []),
            "metaFields": node.get("meta", {}),
            "isParallel": node.get("type") == "parallel",
            "initial": node.get("initial"),
            "isRoot": entry.parent_flat_id is None,
        },
    }
    return elk


def build_elk_graph(index: Index) -> tuple[dict, list[dict]]:
    """Returns (elk_graph, transitions).

    elk_graph: payload for elk.layout() in the browser.
    transitions: full list with resolved/unresolved/cross-region annotations,
                 used by the side panel.
    """
    # Build a flat-id-keyed map of ELK node objects so we can attach children.
    elk_by_flat: dict[str, dict] = {}
    for entry in index.order:
        elk_by_flat[entry.flat_id] = _elk_node(entry)

    # Stitch parent/child.
    for entry in index.order:
        if entry.parent_flat_id is not None:
            parent = elk_by_flat[entry.parent_flat_id]
            parent.setdefault("children", []).append(elk_by_flat[entry.flat_id])

    # Attach layout options + sizes.
    for entry in index.order:
        n = elk_by_flat[entry.flat_id]
        if "children" in n:
            n["layoutOptions"] = dict(COMPOUND_LAYOUT_OPTIONS)
        else:
            n["width"] = LEAF_WIDTH
            n["height"] = LEAF_HEIGHT

    # Edges: walk every state's `on:` block, resolve targets.
    edges: list[dict] = []
    transitions: list[dict] = []
    eid = 0
    for entry in index.order:
        on = entry.node.get("on") or {}
        for event, raw in on.items():
            for t in (raw if isinstance(raw, list) else [raw]):
                target_field = t.get("target")
                target_strs = (
                    target_field if isinstance(target_field, list)
                    else [target_field] if target_field else [None]
                )
                for target_str in target_strs:
                    target_entry = (
                        resolve_target(target_str, entry, index) if target_str else None
                    )
                    src_fid = entry.flat_id
                    target_fid = target_entry.flat_id if target_entry else src_fid
                    is_self = target_entry is None or target_fid == src_fid
                    is_cross_region = (
                        index.root.node.get("type") == "parallel"
                        and target_entry is not None
                        and entry.path_parts[:1] != target_entry.path_parts[:1]
                    )

                    transitions.append({
                        "sourceFlatId": src_fid,
                        "targetFlatId": target_fid,
                        "event": event,
                        "description": t.get("description", ""),
                        "actions": t.get("actions", []),
                        "rawTarget": target_str,
                        "unresolved": bool(target_str) and target_entry is None,
                        "selfLoop": is_self,
                        "crossRegion": is_cross_region,
                    })

                    # Don't ship self-loops or unresolved edges to ELK; they're
                    # decoration at best and crash layout at worst. The side
                    # panel still surfaces them via the transitions list.
                    if is_self:
                        continue
                    edges.append({
                        "id": f"e{eid}",
                        "sources": [src_fid],
                        "targets": [target_fid],
                        "labels": [{"text": event, "width": max(40, len(event) * 6), "height": 14}],
                        "rtpMeta": {
                            "event": event,
                            "actions": t.get("actions", []),
                            "description": t.get("description", ""),
                            "crossRegion": is_cross_region,
                        },
                    })
                    eid += 1

    graph = {
        "id": "root",
        "layoutOptions": ROOT_LAYOUT_OPTIONS,
        "children": [elk_by_flat[index.root.flat_id]],
        "edges": edges,
    }
    return graph, transitions
