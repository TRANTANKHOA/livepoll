#!/usr/bin/env python3
# Vendored from TRANTANKHOA/agent-dotfiles skills/spec-driven/cli.py (spec-driven v1,
# commit f71cffc, 2026-11-28). Re-vendor on upstream format changes; the YAML
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
    by_status, by_type = {}, {}
    for s in doc["specs"]:
        st = (s.get("attributes") or {}).get("status") or "unset"
        ty = s.get("type") or "?"
        by_status[st] = by_status.get(st, 0) + 1
        by_type[ty] = by_type.get(ty, 0) + 1

    def card(s, depth):
        attrs = s.get("attributes") or {}
        badges = []
        for k, v in attrs.items():
            cls = "b-" + k.replace("_", "-")
            if k == "status":
                cls = "b-st-" + str(v).lower().replace("_", "-")
            badges.append(f'<span class="badge {cls}">{e(k)}: {e(str(v))}</span>')
        badge_html = " ".join(badges)
        gwt_html = ""
        for label, field in (("Given", "given"), ("When", "when"), ("Then", "then")):
            vals = gwt(s.get(field), field, s["id"])
            lis = "".join(f"<li>{e(v)}</li>" for v in vals)
            gwt_html += f'<div class="gwt g-{field}"><h4>{label}</h4><ul>{lis}</ul></div>'
        tests = s.get("tests") or []
        tests_html = ("".join(f"<li><code>{e(str(t))}</code></li>" for t in tests)
                      or '<li class="none">(no tests — draft)</li>')
        parent = s.get("parent")
        parent_chip = (f'<span class="parent-chip">↳ child of '
                       f'<a href="#{e(parent)}">{e(parent)}</a></span>' if parent else "")
        depth_cls = f" d{min(depth, 4)}" if depth else ""  # capped: no narrow columns
        child_cls = " child" if depth else ""
        return (f'<section class="spec{child_cls}{depth_cls}" id="{e(s["id"])}">'
                f'<h3>{e(s["id"])} — {e(s["title"])}</h3>'
                f'<p class="meta"><span class="badge b-type">{e(s["type"])}</span>'
                f'<span class="badge b-aspect">{e(s["aspect"])}</span>{badge_html}'
                f'{parent_chip}</p>'
                f'{gwt_html}<p class="tests">Tests:</p><ul class="tests">{tests_html}</ul></section>')

    idx = children_index(doc["specs"])

    def rec(parent, depth):
        out = []
        for s in idx.get(parent, ()):
            out.append(card(s, depth))
            out.append(rec(s["id"], depth + 1))
        return "".join(out)

    body = rec(None, 0)
    stats = " · ".join(f"{e(k)}: {v}" for k, v in sorted(by_status.items()))
    types = " · ".join(f"{e(k)}: {v}" for k, v in sorted(by_type.items()))
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")
    out.write_text(f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{e(doc['project'])} — Specs</title>
<style>
  html {{ background: #f7fafc; }}
  body {{ font-family: system-ui, sans-serif; max-width: 980px; margin: 0 auto; padding: 24px; color: #1a202c; }}
  h1 {{ font-size: 1.6rem; }}
  .summary {{ color: #4a5568; margin-bottom: 24px; }}
  .summary a {{ color: #2b6cb0; }}
  .spec {{ border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px 18px; margin: 14px 0; background: #fff; }}
  .spec.child {{ border-left: 4px solid #90cdf4; }}
  .d1 {{ margin-left: 16px; }} .d2 {{ margin-left: 32px; }}
  .d3 {{ margin-left: 48px; }} .d4 {{ margin-left: 60px; }}
  .spec h3 {{ margin: 0 0 6px; font-size: 1.02rem; }}
  .badge {{ display: inline-block; background: #edf2f7; border-radius: 4px; padding: 1px 8px; margin-right: 6px; font-size: 0.78rem; }}
  .b-type {{ background: #e6fffa; }} .b-aspect {{ background: #ebf8ff; }}
  .b-st-verified {{ background: #c6f6d5; color: #22543d; }}
  .b-st-implemented {{ background: #bee3f8; color: #2a4365; }}
  .b-st-draft {{ background: #e2e8f0; color: #4a5568; }}
  .parent-chip {{ font-size: 0.78rem; color: #4a5568; background: #edf2f7; border-radius: 4px; padding: 1px 8px; white-space: nowrap; }}
  .parent-chip a {{ color: #2b6cb0; text-decoration: none; }}
  .gwt {{ margin: 8px 0; }} .gwt h4 {{ margin: 0; font-size: 0.8rem; text-transform: uppercase; color: #718096; }}
  .gwt ul {{ margin: 4px 0 4px 18px; padding: 0; }} .g-given h4 {{ color: #3182ce; }}
  .g-when h4 {{ color: #d69e2e; }} .g-then h4 {{ color: #38a169; }}
  .tests {{ margin: 4px 0 0; }} .tests ul {{ margin: 4px 0 0 18px; }}
  code {{ background: #f7fafc; overflow-wrap: anywhere; }}
  .none {{ color: #a0aec0; }}
</style>
</head>
<body>
<h1>{e(doc['project'])} — Specs</h1>
<p class="summary">{len(doc['specs'])} specs · {stats}<br>types: {types}<br>
generated {now} · validate OK · <a href="spec.yml"><code>specs/spec.yml</code></a></p>
{body}
</body>
</html>
""")
    print(f"rendered {len(doc['specs'])} specs -> {out}")
    return 0


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
