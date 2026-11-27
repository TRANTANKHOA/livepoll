#!/usr/bin/env python3
# Vendored from TRANTANKHOA/agent-dotfiles skills/spec-driven/cli.py (spec-driven v1,
# re-vendored 2026-11-28: interactive renderer — collapsible tree, group views,
# light/dark themes; commit f71cffc+). Re-vendor on upstream format changes; the YAML
# subset contract below is stable. CI runs: python3 scripts/validate_specs.py validate
# and python3 scripts/check_spec_coverage.py

#!/usr/bin/env python3
"""Lifecycle CLI for spec-driven development: specs/spec.yml per project.

Specs are hierarchical Given/When/Then behaviors in a strict YAML subset:
each spec has id/title, type, aspect, optional attributes map, at most one
parent (peers share a parent), given/when/then (string or list), and test
links (`path` or `path#anchor`). Rendered to static self-contained HTML.

YAML subset (documented, enforced): 2-space indents, `key: value` plain or
double-quoted scalars, `key: |` block scalars, `- item` lists, list items
that start a mapping (`- key: value`), full-line comments. No flow style,
anchors, tabs, or multi-docs. stdlib-only on purpose (no PyYAML).

Subcommands:
    init PROJECT                     scaffold specs/spec.yml
    add TITLE --type T --aspect A [--parent ID] [--status S] [--priority P]
                                     append a draft spec skeleton, print its id
    validate                         full gate: schema, hierarchy, links; exit 1 on FAIL
    tree                             text tree (parent/peers), needs parse only
    render [--out PATH]              static HTML (specs/spec.html); requires validate OK

Global: --root DIR (default cwd). The spec file path is <root>/specs/spec.yml.
"""
import argparse
import datetime
import html
import json
import pathlib
import re
import sys


class SpecError(Exception):
    pass


# compiled once: parse_map's per-line key match is the parser hot path
KEY_RE = re.compile(r"^([A-Za-z_][\w-]*):(?:\s+(.*))?$")
LIST_MAP_RE = re.compile(r"^[A-Za-z_][\w-]*:(\s|$)")
ANCHOR_RE = re.compile(r'id:\s*("[^"]+"|\'[^\']+\'|[^\s"\']+)')


# ---------------------------------------------------------------- YAML subset

def tokenize(text, path):
    """-> [(indent, content, lineno)]; blank/comment lines dropped (our subset
    defines block scalars as stripped prose, so dropping their blanks is fine)."""
    lines = []
    for n, raw in enumerate(text.splitlines(), 1):
        if "\t" in raw[: len(raw) - len(raw.lstrip())]:
            raise SpecError(f"{path}:{n}: tab in indentation (use spaces)")
        content = raw.strip()
        if not content or content.startswith("#"):
            continue
        lines.append((len(raw) - len(raw.lstrip(" ")), content, n))
    return lines


def _scalar(s, path, n):
    s = s.strip()
    if len(s) >= 2 and s[0] == '"' and s[-1] == '"':
        return s[1:-1]
    if s.startswith(('"', "'", "[", "{", "&", "*")):
        raise SpecError(f"{path}:{n}: value {s[:20]!r} uses unsupported syntax "
                        f"(plain or double-quoted scalars only)")
    return s


def parse_block(lines, i, indent, path):
    """Parse a mapping or list at exactly `indent`. -> (value, next_index)."""
    if i >= len(lines) or lines[i][0] != indent:
        raise SpecError(f"{path}:{lines[i][2] if i < len(lines) else '?'}: "
                        f"expected content at indent {indent}")
    if lines[i][1] == "-" or lines[i][1].startswith("- "):
        return parse_list(lines, i, indent, path)
    return parse_map(lines, i, indent, path)


def parse_map(lines, i, indent, path):
    out = {}
    while i < len(lines):
        ind, content, n = lines[i]
        if ind < indent:
            break
        if ind > indent:
            raise SpecError(f"{path}:{n}: unexpected indent (expected {indent})")
        if content == "-" or content.startswith("- "):
            break
        m = KEY_RE.match(content)
        if not m:
            raise SpecError(f"{path}:{n}: expected `key:` or `key: value`, got {content[:40]!r}")
        key, rest = m.group(1), (m.group(2) or "").strip()
        i += 1
        if rest == "|":
            block = []
            while i < len(lines) and lines[i][0] > indent:
                block.append(lines[i][1])
                i += 1
            out[key] = "\n".join(block).strip("\n")
        elif rest:
            out[key] = _scalar(rest, path, n)
        else:  # nested block
            if i < len(lines) and lines[i][0] > indent:
                out[key], i = parse_block(lines, i, lines[i][0], path)
            else:
                out[key] = None
    return out, i


def parse_list(lines, i, indent, path):
    items = []
    while i < len(lines):
        ind, content, n = lines[i]
        if ind != indent or not (content == "-" or content.startswith("- ")):
            break
        rest = content[1:].strip()
        if not rest:
            i += 1
            if i < len(lines) and lines[i][0] > indent:
                val, i = parse_block(lines, i, lines[i][0], path)
                items.append(val)
            else:
                items.append(None)
        elif LIST_MAP_RE.match(rest):
            lines[i] = (indent + 2, rest, n)  # item's mapping starts on the dash line
            val, i = parse_map(lines, i, indent + 2, path)
            items.append(val)
        else:
            items.append(_scalar(rest, path, n))
            i += 1
    return items, i


def load_yaml(path):
    if not path.is_file():
        raise SpecError(f"no spec file at {path} (run init)")
    lines = tokenize(path.read_text(), str(path))
    parsed, i = parse_map(lines, 0, 0, str(path))
    if i < len(lines):
        raise SpecError(f"{path}: trailing content after line {lines[i][2]}")
    return parsed


def fmt_scalar(v):
    s = str(v)
    if s == "" or re.search(r"^['\"&*\[{}#-]|:\s", s) or s != s.strip():
        return '"' + s.replace('"', "'") + '"'
    return s


def dump_map(d, indent=0):
    out = []
    for k, v in d.items():
        pad = "  " * indent
        if isinstance(v, dict):
            out.append(f"{pad}{k}:")
            out += dump_map(v, indent + 1)
        elif isinstance(v, list):
            out.append(f"{pad}{k}:")
            out += dump_list(v, indent + 1)
        elif isinstance(v, str) and ("\n" in v or len(v) > 72):
            out.append(f"{pad}{k}: |")
            out += [f"{pad}  {ln}" for ln in v.splitlines()]
        elif v is None:
            out.append(f"{pad}{k}:")
        else:
            out.append(f"{pad}{k}: {fmt_scalar(v)}")
    return out


def dump_list(items, indent=0):
    out = []
    pad = "  " * indent
    for it in items:
        if isinstance(it, dict):
            first = True
            for ln in dump_map(it, indent + 1):
                out.append(f"{pad}- {ln[len(pad) + 2:]}" if first else ln)
                first = False
        else:
            out.append(f"{pad}- {fmt_scalar(it)}")
    return out


# ---------------------------------------------------------------- validation

def load_spec(root):
    doc = load_yaml(root / "specs" / "spec.yml")
    if not isinstance(doc, dict) or "project" not in doc or "specs" not in doc:
        raise SpecError("spec.yml: top-level needs `project:` and `specs:`")
    if not isinstance(doc["specs"], list):
        raise SpecError("spec.yml: `specs:` must be a list")
    return doc


def gwt(v, what, sid):
    vals = [v] if isinstance(v, str) else v
    if not isinstance(vals, list) or not any(str(x).strip() for x in vals):
        raise SpecError(f"{sid}: `{what}` missing or empty (string or list of strings)")
    return [str(x).strip() for x in vals if str(x).strip()]


def validate(doc, root):
    specs, by_id, fails = doc["specs"], {}, []

    for s in specs:
        if not isinstance(s, dict):
            fails.append("spec entry is not a mapping")
            continue
        sid = s.get("id") or "(missing id)"
        for field in ("id", "title", "type", "aspect"):
            if not str(s.get(field) or "").strip():
                fails.append(f"{sid}: missing required field `{field}`")
        for field in ("given", "when", "then"):
            try:
                gwt(s.get(field), field, sid)
            except SpecError as e:
                fails.append(str(e))
        if s.get("attributes") is not None and not isinstance(s["attributes"], dict):
            fails.append(f"{sid}: `attributes` must be a mapping")
        if s.get("tests") is not None:
            if not isinstance(s["tests"], list):
                fails.append(f"{sid}: `tests` must be a list")
    for s in specs:
        sid = str(s.get("id") or "")
        if sid in by_id:
            fails.append(f"{sid}: duplicate id")
        by_id[sid] = s

    clean = set()  # ids whose parent chain is known cycle-free (memo for the walk)
    for s in specs:
        sid = str(s.get("id") or "")
        parent = s.get("parent")
        if parent == sid:
            fails.append(f"{sid}: parent is itself")
        elif parent and parent not in by_id:
            fails.append(f"{sid}: unknown parent `{parent}`")
        elif parent:
            seen, cur, failed = {sid}, parent, False
            while cur and cur not in clean:
                if cur in seen:
                    fails.append(f"{sid}: parent cycle via `{cur}`")
                    failed = True
                    break
                seen.add(cur)
                cur = by_id[cur].get("parent")
            if not failed:
                clean |= seen
        status = str((s.get("attributes") or {}).get("status") or "")
        if status != "draft" and not s.get("tests"):
            fails.append(f"{sid}: no linked tests (status {status or 'unset'} != draft)")

    # test links: read each referenced file once, extract its id: anchors once
    anchor_cache = {}  # fpath -> set of anchors, or None = file missing
    for s in specs:
        sid = str(s.get("id") or "")
        for t in s.get("tests") or []:
            ref = str(t)
            if "#" in ref:
                fpath, anchor = ref.rsplit("#", 1)
                if fpath not in anchor_cache:
                    fp = root / fpath
                    anchor_cache[fpath] = (
                        set(ANCHOR_RE.findall(fp.read_text(errors="replace")))
                        if fp.is_file() else None)
                anchors = anchor_cache[fpath]
                if anchors is None:
                    fails.append(f"{sid}: test file not found: {fpath}")
                elif anchor not in anchors:
                    fails.append(f"{sid}: anchor `{anchor}` not found in {fpath}")
            elif not (root / ref).exists():
                fails.append(f"{sid}: test target not found: {ref}")
    return fails


# ---------------------------------------------------------------- commands

def cmd_init(a):
    root = pathlib.Path(a.root).resolve()
    spec = root / "specs" / "spec.yml"
    if spec.exists():
        raise SpecError(f"{spec} already exists")
    spec.parent.mkdir(parents=True, exist_ok=True)
    spec.write_text(f"""# {a.project} — hierarchical Given/When/Then specs
# YAML subset: 2-space indents; key: value; key: | block scalars; - lists; # comments.
# Hierarchy: optional `parent: <id>` (at most one); peers share a parent.
# Tests: `path` or `path#anchor` (anchor = an `id:` in the test file).
# status: draft specs may omit tests; everything else must link at least one.
version: 1
project: "{a.project}"
specs:
  - id: SPEC-001
    title: "Example behavior — replace me"
    type: behavior
    aspect: core
    attributes:
      status: draft
      priority: p2
    given: |
      The system is in a known starting state
    when: |
      The user does the one thing this spec is about
    then: |
      The observable outcome is stated here
""")
    print(f"initialized {spec} — edit the example spec, then `validate`")


def cmd_add(a):
    root = pathlib.Path(a.root).resolve()
    doc = load_spec(root)
    nums = [int(m.group(1)) for s in doc["specs"]
            if isinstance(s, dict) and (m := re.match(r"SPEC-(\d+)$", str(s.get("id", ""))))]
    nid = f"SPEC-{(max(nums) + 1) if nums else len(doc['specs']) + 1:03d}"
    item = {"id": nid, "title": a.title, "type": a.type, "aspect": a.aspect,
            "attributes": {"status": a.status, "priority": a.priority}}
    if a.parent:
        item["parent"] = a.parent
    if a.bug_ref:
        item["attributes"]["bug_ref"] = a.bug_ref
    item.update({"given": "TODO: starting state", "when": "TODO: the trigger",
                 "then": "TODO: observable outcome", "tests": a.test or []})
    doc["specs"].append(item)
    spec = root / "specs" / "spec.yml"
    spec.write_text("\n".join(dump_map(doc)) + "\n")
    print(f"{nid} added ({a.type}/{a.aspect}, status {a.status}) — fill the TODOs")


def cmd_validate(a):
    root = pathlib.Path(a.root).resolve()
    doc = load_spec(root)
    fails = validate(doc, root)
    for f in fails:
        print(f"FAIL {f}")
    if fails:
        print(f"\nFAIL: {len(fails)} problem(s) — fix before commit/render")
        return 1
    n = len(doc["specs"])
    linked = sum(1 for s in doc["specs"] if s.get("tests"))
    print(f"OK: {n} specs, {linked} test-linked, hierarchy and links clean")
    return 0


def children_index(specs):
    """One pass: parent id -> [child specs] (document order). O(n) tree walks."""
    idx = {}
    for s in specs:
        idx.setdefault(s.get("parent") or None, []).append(s)
    return idx


def cmd_tree(a):
    root = pathlib.Path(a.root).resolve()
    doc = load_spec(root)
    idx = children_index(doc["specs"])
    nt = sum(len(s.get("tests") or []) for s in doc["specs"])

    def rec(parent, depth):
        for s in idx.get(parent, ()):
            sid = s["id"]
            st = f" [{s['type']}/{s['aspect']}]"
            t = f" ({len(s.get('tests') or [])} test{'s' if len(s.get('tests') or []) != 1 else ''})"
            print(f"{'  ' * depth}{sid}{st} {s['title']}{t}")
            rec(sid, depth + 1)

    print(f"{doc['project']} — {len(doc['specs'])} specs, {nt} test links")
    rec(None, 0)
    return 0


def cmd_render(a):
    root = pathlib.Path(a.root).resolve()
    doc = load_spec(root)
    fails = validate(doc, root)
    if fails:
        for f in fails:
            print(f"FAIL {f}")
        raise SpecError("refusing to render an invalid spec file — fix the above")

    out = pathlib.Path(a.out) if a.out else root / "specs" / "spec.html"
    e = html.escape

    def status_cls(v):
        k = str(v).lower()
        if k == "verified":
            return "st-verified"
        if k == "implemented":
            return "st-implemented"
        if k in ("partial", "blocked-ui"):
            return "st-partial"
        if k in ("open-defect", "latent-defect"):
            return "st-open-defect"
        return "st-draft"

    by_status, by_type = {}, {}
    payload = []
    for s in doc["specs"]:
        attrs = {str(k): str(v) for k, v in (s.get("attributes") or {}).items()}
        st = attrs.get("status") or "unset"
        ty = s.get("type") or "?"
        by_status[st] = by_status.get(st, 0) + 1
        by_type[ty] = by_type.get(ty, 0) + 1
        payload.append({
            "id": s["id"], "title": s.get("title") or "",
            "type": ty, "aspect": s.get("aspect") or "?",
            "parent": s.get("parent"), "attrs": attrs,
            "given": gwt(s.get("given"), "given", s["id"]),
            "when": gwt(s.get("when"), "when", s["id"]),
            "then": gwt(s.get("then"), "then", s["id"]),
            "tests": [str(t) for t in (s.get("tests") or [])],
        })
    pills = " ".join(
        f'<button class="pill {status_cls(k)}" data-status="{e(k)}">{e(k)}: {v}</button>'
        for k, v in sorted(by_status.items()))
    types = " · ".join(f"{e(k)}: {v}" for k, v in sorted(by_type.items()))
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")
    body_json = json.dumps(payload).replace("</", "<\\/")
    html_out = (TEMPLATE
                .replace("__PROJECT__", e(doc["project"]))
                .replace("__NSPECS__", str(len(doc["specs"])))
                .replace("__PILLS__", pills)
                .replace("__TYPES__", types)
                .replace("__NOW__", now)
                .replace("__PAYLOAD__", body_json))
    out.write_text(html_out)
    print(f"rendered {len(doc['specs'])} specs -> {out}")
    return 0


TEMPLATE = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>__PROJECT__ — Specs</title>
<style>
  :root { --bg:#f7fafc; --card:#ffffff; --text:#1a202c; --muted:#4a5568; --faint:#718096;
    --border:#e2e8f0; --accent:#2b6cb0;
    --shadow:0 1px 3px rgba(0,0,0,.08),0 4px 14px rgba(0,0,0,.05);
    --ok-bg:#c6f6d5; --ok-tx:#22543d; --impl-bg:#bee3f8; --impl-tx:#2a4365;
    --warn-bg:#feebc8; --warn-tx:#744210; --bad-bg:#fed7d7; --bad-tx:#742a2a;
    --gray-bg:#e2e8f0; --gray-tx:#4a5568;
    --c-given:#3182ce; --c-when:#d69e2e; --c-then:#38a169; }
  html[data-theme="dark"] { --bg:#0f172a; --card:#1e293b; --text:#e2e8f0; --muted:#94a3b8; --faint:#64748b;
    --border:#334155; --accent:#63b3ed;
    --shadow:0 1px 3px rgba(0,0,0,.5),0 6px 18px rgba(0,0,0,.35);
    --ok-bg:rgba(72,187,120,.16); --ok-tx:#68d391; --impl-bg:rgba(99,179,237,.16); --impl-tx:#90cdf4;
    --warn-bg:rgba(237,137,54,.16); --warn-tx:#f6ad55; --bad-bg:rgba(245,101,101,.16); --bad-tx:#fc8181;
    --gray-bg:rgba(160,174,192,.14); --gray-tx:#a0aec0;
    --c-given:#63b3ed; --c-when:#f6ad55; --c-then:#68d391; }
  html { background: var(--bg); }
  body { font-family: system-ui, sans-serif; max-width: 1060px; margin: 0 auto;
    padding: 24px 24px 110px; color: var(--text); }
  header h1 { font-size: 1.7rem; margin: 0 0 6px; letter-spacing: -0.02em; }
  .summary { color: var(--muted); margin: 10px 0 24px; }
  .summary a { color: var(--accent); }
  .summary code { background: var(--gray-bg); color: var(--gray-tx); border-radius: 5px; padding: 1px 7px; }
  .pill { border: 0; border-radius: 999px; padding: 2px 10px; margin: 2px 4px 2px 0;
    font-size: 0.78rem; cursor: pointer; font-family: inherit; }
  .pill:hover { filter: brightness(1.06); }
  .st-verified { background: var(--ok-bg); color: var(--ok-tx); }
  .st-implemented { background: var(--impl-bg); color: var(--impl-tx); }
  .st-partial { background: var(--warn-bg); color: var(--warn-tx); }
  .st-open-defect { background: var(--bad-bg); color: var(--bad-tx); }
  .st-draft { background: var(--gray-bg); color: var(--gray-tx); }
  .spec { background: var(--card); border: 1px solid var(--border); border-radius: 10px;
    padding: 14px 16px; margin: 12px 0; box-shadow: var(--shadow);
    transition: transform .12s, box-shadow .12s, border-color .2s; }
  .spec:hover { transform: translateY(-1px); }
  .spec.child { border-left: 4px solid var(--accent); }
  .children { margin-left: 12px; padding-left: 8px; border-left: 2px solid var(--border); }
  .children:empty { display: none; }
  .spec.collapsed > .children { display: none; }
  .spec.collapsed { opacity: 0.82; }
  .spec-head { display: flex; align-items: baseline; gap: 8px; }
  .spec-head h3 { flex: 1; margin: 0; font-size: 1.02rem; }
  .chev { border: 0; background: transparent; color: var(--faint); cursor: pointer;
    font-size: 0.9rem; padding: 0 2px; line-height: 1; }
  .chev:hover { color: var(--accent); }
  .anchor { color: inherit; text-decoration: none; }
  .anchor:hover { color: var(--accent); }
  .meta { margin: 6px 0 10px; display: flex; flex-wrap: wrap; align-items: center; gap: 5px; }
  .badge { display: inline-block; background: var(--gray-bg); color: var(--gray-tx);
    border-radius: 5px; padding: 1px 8px; font-size: 0.74rem; }
  .b-type { background: transparent; border: 1px solid var(--border); color: var(--muted); }
  .b-aspect { background: transparent; border: 1px dashed var(--border); color: var(--muted); }
  .badge.b-st-verified { background: var(--ok-bg); color: var(--ok-tx); }
  .badge.b-st-implemented { background: var(--impl-bg); color: var(--impl-tx); }
  .badge.b-st-partial, .badge.b-st-blocked-ui { background: var(--warn-bg); color: var(--warn-tx); }
  .badge.b-st-open-defect, .badge.b-st-latent-defect { background: var(--bad-bg); color: var(--bad-tx); }
  .badge.b-st-draft, .badge.b-st-documented, .badge.b-st-informational,
  .badge.b-st-conditional, .badge.b-st-unset { background: var(--gray-bg); color: var(--gray-tx); }
  .parent-chip, .kids-chip { font-size: 0.74rem; background: var(--gray-bg); color: var(--gray-tx);
    border-radius: 5px; padding: 1px 8px; white-space: nowrap; }
  .parent-chip a { color: var(--accent); text-decoration: none; }
  .gwt-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
  @media (max-width: 720px) { .gwt-grid { grid-template-columns: 1fr; } }
  .gwt { border-left: 3px solid; border-radius: 0 6px 6px 0; padding: 2px 0 2px 9px; }
  .col-g { border-color: var(--c-given); } .col-a { border-color: var(--c-when); } .col-t { border-color: var(--c-then); }
  .gwt h4 { margin: 0 0 3px; font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em; }
  .col-g h4 { color: var(--c-given); } .col-a h4 { color: var(--c-when); } .col-t h4 { color: var(--c-then); }
  .gwt ul { margin: 0; padding-left: 15px; }
  .gwt li { font-size: 0.86rem; margin: 2px 0; }
  .tests { margin-top: 10px; }
  .tests h4 { margin: 0 0 4px; font-size: 0.7rem; text-transform: uppercase; color: var(--faint); letter-spacing: 0.06em; }
  .chips { display: flex; flex-wrap: wrap; gap: 5px; }
  .chips code { background: var(--gray-bg); color: var(--gray-tx); border-radius: 5px;
    padding: 2px 7px; font-size: 0.74rem; overflow-wrap: anywhere; }
  .none { color: var(--faint); font-size: 0.8rem; }
  .group h2 { font-size: 1.1rem; margin: 26px 0 2px; }
  .count { display: inline-block; background: var(--gray-bg); color: var(--gray-tx);
    border-radius: 999px; padding: 0 9px; font-size: 0.75rem; margin-left: 7px; }
  #ctrl { position: fixed; right: 14px; bottom: 14px; display: flex; flex-direction: column; gap: 6px;
    background: var(--card); border: 1px solid var(--border); border-radius: 12px;
    padding: 8px; box-shadow: var(--shadow); z-index: 50; }
  .seg { display: flex; gap: 2px; background: var(--bg); border-radius: 8px; padding: 2px; }
  .seg button { border: 0; background: transparent; padding: 3px 9px; font-size: 0.75rem;
    border-radius: 6px; color: var(--muted); cursor: pointer; font-family: inherit; }
  .seg button.on { background: var(--card); color: var(--text); box-shadow: var(--shadow); font-weight: 600; }
  .seg button:hover, .row2 button:hover { color: var(--text); }
  .row2 { display: flex; gap: 2px; }
  .row2 button { flex: 1; border: 0; background: transparent; color: var(--muted); cursor: pointer;
    border-radius: 6px; padding: 4px 0; font-size: 0.9rem; }
  .row2 button.off { opacity: 0.3; cursor: default; }
</style>
</head>
<body>
<header>
<h1>__PROJECT__ — Specs</h1>
<p class="summary">__NSPECS__ specs · <span id="statpills">__PILLS__</span><br>types: __TYPES__<br>
generated __NOW__ · validate OK · <a href="spec.yml"><code>specs/spec.yml</code></a></p>
</header>
<main id="content"></main>
<div id="ctrl">
  <div class="seg" id="views">
    <button data-v="tree" class="on">Tree</button>
    <button data-v="type">Type</button>
    <button data-v="aspect">Aspect</button>
    <button data-v="status">Status</button>
  </div>
  <div class="row2">
    <button id="exp" title="Expand all">⤢</button>
    <button id="col" title="Collapse all">⤡</button>
    <button id="theme" title="Theme">◐</button>
  </div>
</div>
<script>
const SPECS = __PAYLOAD__;
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const kidsOf = {};
SPECS.forEach(s => { const p = s.parent || null; (kidsOf[p] = kidsOf[p] || []).push(s); });
const stCls = v => {
  const k = String(v).toLowerCase();
  if (k === 'verified') return 'b-st-verified';
  if (k === 'implemented') return 'b-st-implemented';
  if (k === 'partial' || k === 'blocked-ui') return 'b-st-partial';
  if (k === 'open-defect' || k === 'latent-defect') return 'b-st-open-defect';
  return 'b-st-draft';
};
function cardEl(s) {
  const kids = kidsOf[s.id] || [];
  const el = document.createElement('section');
  el.className = 'spec' + (s.parent ? ' child' : '');
  el.id = s.id;
  el.dataset.type = s.type;
  el.dataset.aspect = s.aspect;
  const st = (s.attrs && s.attrs.status) || 'unset';
  el.dataset.status = st;
  const badges = Object.entries(s.attrs || {}).map(([k, v]) => {
    const cls = k === 'status' ? stCls(v) : 'b-' + k.replace(/_/g, '-');
    return '<span class="badge ' + cls + '">' + esc(k) + ': ' + esc(v) + '</span>';
  }).join('');
  const chip = s.parent
    ? '<span class="parent-chip">↳ child of <a href="#' + esc(s.parent) + '">' + esc(s.parent) + '</a></span>'
    : '';
  const kidsChip = kids.length
    ? '<span class="kids-chip">' + kids.length + (kids.length === 1 ? ' child' : ' children') + '</span>'
    : '';
  const g = (lbl, key, cls) => '<div class="gwt col-' + cls + '"><h4>' + lbl + '</h4><ul>' +
    s[key].map(x => '<li>' + esc(x) + '</li>').join('') + '</ul></div>';
  const tests = s.tests.length
    ? '<div class="tests"><h4>Tests</h4><div class="chips">' + s.tests.map(t => '<code>' + esc(t) + '</code>').join('') + '</div></div>'
    : '<div class="tests"><h4>Tests</h4><span class="none">no tests — draft</span></div>';
  el.innerHTML =
    '<div class="spec-head">' +
    (kids.length ? '<button class="chev" title="Collapse children">▾</button>' : '') +
    '<h3><a class="anchor" href="#' + esc(s.id) + '">' + esc(s.id) + '</a> — ' + esc(s.title) + '</h3>' +
    '</div>' +
    '<p class="meta"><span class="badge b-type">' + esc(s.type) + '</span>' +
    '<span class="badge b-aspect">' + esc(s.aspect) + '</span>' + badges + chip + kidsChip + '</p>' +
    '<div class="gwt-grid">' + g('Given', 'given', 'g') + g('When', 'when', 'a') + g('Then', 'then', 't') + '</div>' +
    tests;
  if (kids.length) {
    const wrap = document.createElement('div');
    wrap.className = 'children';
    el.appendChild(wrap);
    const btn = el.querySelector('.chev');
    const set = c => {
      el.classList.toggle('collapsed', c);
      btn.textContent = c ? '▸' : '▾';
      btn.title = c ? 'Expand children' : 'Collapse children';
    };
    btn.addEventListener('click', () => set(!el.classList.contains('collapsed')));
    el._set = set;
  }
  return el;
}
const pool = {};
SPECS.forEach(s => pool[s.id] = cardEl(s));
const content = document.getElementById('content');
function build(parent, container) {
  (kidsOf[parent] || []).forEach(s => {
    const el = pool[s.id];
    container.appendChild(el);
    build(s.id, el.querySelector('.children'));
  });
}
function setView(v) {
  document.querySelectorAll('#views button').forEach(b => b.classList.toggle('on', b.dataset.v === v));
  document.getElementById('exp').classList.toggle('off', v !== 'tree');
  document.getElementById('col').classList.toggle('off', v !== 'tree');
  content.innerHTML = '';
  if (v === 'tree') { build(null, content); return; }
  const buckets = {};
  SPECS.forEach(s => {
    const k = v === 'type' ? s.type : v === 'aspect' ? s.aspect : ((s.attrs && s.attrs.status) || 'unset');
    (buckets[k] = buckets[k] || []).push(pool[s.id]);
  });
  Object.keys(buckets).sort().forEach(k => {
    const g = document.createElement('div');
    g.className = 'group';
    g.innerHTML = '<h2>' + esc(v) + ': ' + esc(k) + '<span class="count">' + buckets[k].length + '</span></h2>';
    const box = document.createElement('div');
    buckets[k].forEach(el => box.appendChild(el));
    g.appendChild(box);
    content.appendChild(g);
  });
}
document.querySelectorAll('#views button').forEach(b => b.addEventListener('click', () => setView(b.dataset.v)));
document.getElementById('exp').addEventListener('click', () => Object.values(pool).forEach(el => el._set && el._set(false)));
document.getElementById('col').addEventListener('click', () => Object.values(pool).forEach(el => el._set && el._set(true)));
const themeBtn = document.getElementById('theme');
const THEMES = ['auto', 'light', 'dark'];
function setTheme(t) {
  const eff = t === 'auto' ? (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light') : t;
  document.documentElement.dataset.theme = eff;
  themeBtn.textContent = eff === 'dark' ? '☀️' : '🌙';
  themeBtn.title = 'Theme: ' + t + (t === 'auto' ? ' (now ' + eff + ')' : '');
  try { localStorage.setItem('spec-theme', t); } catch (e) {}
}
themeBtn.addEventListener('click', () => {
  let cur = 'auto';
  try { cur = localStorage.getItem('spec-theme') || 'auto'; } catch (e) {}
  setTheme(THEMES[(THEMES.indexOf(cur) + 1) % THEMES.length]);
});
let savedTheme = 'auto';
try { savedTheme = localStorage.getItem('spec-theme') || 'auto'; } catch (e) {}
setTheme(savedTheme);
setView('tree');
document.querySelectorAll('.pill[data-status]').forEach(p => p.addEventListener('click', () => setView('status')));
</script>
</body>
</html>"""


def main():
    ap = argparse.ArgumentParser(description="spec-driven lifecycle (see module docstring)")
    ap.add_argument("--root", default=".", help="project root (default: cwd)")
    sub = ap.add_subparsers(dest="cmd", required=True)
    i = sub.add_parser("init", help="scaffold specs/spec.yml")
    i.add_argument("project")
    i.set_defaults(func=cmd_init)
    d = sub.add_parser("add", help="append a draft spec skeleton")
    d.add_argument("title")
    d.add_argument("--type", default="behavior")
    d.add_argument("--aspect", default="core")
    d.add_argument("--parent", default=None)
    d.add_argument("--status", default="draft")
    d.add_argument("--priority", default="p2")
    d.add_argument("--bug-ref", default=None)
    d.add_argument("--test", action="append", help="test link (repeatable)")
    d.set_defaults(func=cmd_add)
    v = sub.add_parser("validate", help="full gate: schema, hierarchy, links")
    v.set_defaults(func=cmd_validate)
    t = sub.add_parser("tree", help="text tree")
    t.set_defaults(func=cmd_tree)
    r = sub.add_parser("render", help="static HTML; requires validate OK")
    r.add_argument("--out", default=None)
    r.set_defaults(func=cmd_render)
    a = ap.parse_args()
    try:
        sys.exit(a.func(a))
    except SpecError as exc:
        sys.exit(f"error: {exc}")


if __name__ == "__main__":
    main()
