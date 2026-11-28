#!/usr/bin/env python3
"""Palette source-of-truth CLI for LivePulse.

Palettes live in scripts/palettes.yml and are applied to three surfaces:
  - app/src/main/java/com/plutusoft/livepulse/ui/theme/Color.kt   (Compose tokens)
  - docs/vote.html                                                (web CSS vars, light+dark)
  - app/src/main/res/drawable/ic_launcher_background.xml          (launcher gradient)

Commands:
  list                    show available palettes (abstract bases marked)
  show <name>             print the resolved token set
  apply <name> [--dry-run]  regenerate the three surfaces from a palette
  preview <name> [--emulator]  apply + build + install + launch on the emulator

Note: viewing on the WEB is instant (reload docs/vote.html after apply);
viewing in the APP needs the rebuild that `preview` (or gradle) performs.

This parser intentionally supports only the YAML subset used by palettes.yml:
nested string/bool mappings with 2-space indentation and quoted scalars.
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
YML_PATH = ROOT / "scripts" / "palettes.yml"
COLOR_KT = ROOT / "app/src/main/java/com/plutusoft/livepulse/ui/theme/Color.kt"
VOTE_HTML = ROOT / "docs/vote.html"
LAUNCHER_BG = ROOT / "app/src/main/res/drawable/ic_launcher_background.xml"

HEX_RE = re.compile(r"^#[0-9A-Fa-f]{6,8}$")

# vote.html :root blocks — first is light, second is dark (inside @media).
ROOT_BLOCK_RE = re.compile(r":root \{[^}]*\}")

LIGHT_VARS = [
    ("--primary", "primary_light"),
    ("--on-primary", "on_primary_light"),
    ("--primary-container", "primary_container_light"),
    ("--on-primary-container", "on_primary_container_light"),
    ("--bg", "background_light"),
    ("--surface", "surface_light"),
    ("--surface-var", "surface_variant_light"),
    ("--text", "on_surface_light"),
    ("--muted", "on_surface_variant_light"),
    ("--border", "outline_variant_light"),
    ("--green", "status_active"),
    ("--green-bg", "secondary_container_light"),
    ("--amber", "tertiary_light"),
    ("--amber-bg", "tertiary_container_light"),
    ("--danger", "error_light"),
    ("--danger-bg", "error_container_light"),
]
DARK_VARS = [
    ("--primary", "primary_dark"),
    ("--on-primary", "on_primary_dark"),
    ("--primary-container", "primary_container_dark"),
    ("--on-primary-container", "on_primary_container_dark"),
    ("--bg", "background_dark"),
    ("--surface", "surface_dark"),
    ("--surface-var", "surface_variant_dark"),
    ("--text", "on_surface_dark"),
    ("--muted", "on_surface_variant_dark"),
    ("--border", "outline_variant_dark"),
    ("--green", "status_active"),
    ("--green-bg", "secondary_container_dark"),
    ("--amber", "tertiary_dark"),
    ("--amber-bg", "tertiary_container_dark"),
    ("--danger", "error_dark"),
    ("--danger-bg", "error_container_dark"),
]


def fail(msg: str) -> None:
    print(f"error: {msg}", file=sys.stderr)
    sys.exit(1)


# ---------------------------------------------------------------- yaml subset
def parse_yml(path: Path) -> dict:
    """Parse the constrained YAML subset used by palettes.yml into nested dicts.

    Supports: `key:` group headers, `key: value` scalars (quoted or bare),
    comments, blank lines. Values are strings; `true`/`false` become bools.
    """
    root: dict = {}
    stack: list[tuple[int, dict]] = [(-1, root)]
    for lineno, raw in enumerate(path.read_text().splitlines(), start=1):
        line = raw.rstrip()
        if not line.strip() or line.strip().startswith("#"):
            continue
        indent = len(line) - len(line.lstrip(" "))
        if indent % 2 != 0:
            fail(f"{path.name}:{lineno}: indent must be 2-space multiples")
        m = re.match(r"^([A-Za-z0-9_-]+):\s*(.*)$", line.strip())
        if not m:
            fail(f"{path.name}:{lineno}: cannot parse line: {line.strip()!r}")
        key, value = m.group(1), m.group(2).strip()
        while stack and stack[-1][0] >= indent:
            stack.pop()
        parent = stack[-1][1]
        if value == "":
            child: dict = {}
            parent[key] = child
            stack.append((indent, child))
        else:
            if value.startswith(('"', "'")):
                if not value.endswith(value[0]):
                    fail(f"{path.name}:{lineno}: unterminated quote")
                value = value[1:-1]
            elif value.lower() in ("true", "false"):
                parent[key] = value.lower() == "true"
                continue
            parent[key] = value
    return root


def deep_merge(base: dict, override: dict) -> dict:
    import copy

    out = copy.deepcopy(base)
    for key, value in override.items():
        if isinstance(value, dict) and isinstance(out.get(key), dict):
            out[key] = deep_merge(out[key], value)
        else:
            out[key] = copy.deepcopy(value)
    return out


def load_palettes() -> dict:
    raw = parse_yml(YML_PATH)
    resolved: dict = {}

    def resolve(name: str, seen: tuple[str, ...]) -> dict:
        if name in resolved:
            return resolved[name]
        if name in seen:
            fail(f"circular extends chain: {' -> '.join(seen + (name,))}")
        entry = raw.get(name)
        if entry is None:
            fail(f"unknown palette '{name}' in {YML_PATH.name}")
        parent_name = entry.get("extends")
        if parent_name:
            if not isinstance(parent_name, str):
                fail(f"palette '{name}': extends must be a palette name")
            # deep_merge returns a fresh deep copy — children must never alias
            # their parent's cached resolved dict.
            merged = deep_merge(resolve(parent_name, seen + (name,)), {})
        else:
            merged: dict = {}
        for group, tokens in entry.items():
            if group in ("extends", "abstract", "note"):
                continue
            if not isinstance(tokens, dict):
                fail(f"palette '{name}': group '{group}' must be a token mapping")
            merged.setdefault(group, {})
            merged[group].update(tokens)
        # The default six-crayon category set: bases define neutrals only, so
        # palettes override just the crayons they want to re-tint.
        default_categories = {
            "category_soccer": "#15803D",
            "category_drinks": "#0284C7",
            "category_food": "#F97316",
            "category_decision": "#8B5CF6",
            "category_event": "#EC4899",
            "category_feedback": "#14B8A6",
        }
        categories = dict(default_categories)
        categories.update(merged.get("categories", {}))
        merged["categories"] = categories
        merged["note"] = entry.get("note", "")
        merged["abstract"] = bool(entry.get("abstract", False))
        resolved[name] = merged
        return merged

    for name in raw:
        resolve(name, ())
    return resolved


def validate(name: str, palette: dict) -> None:
    if palette.get("abstract"):
        fail(f"'{name}' is an abstract base and cannot be applied")
    flat: dict[str, str] = {}
    for group, tokens in palette.items():
        if group in ("note", "abstract"):
            continue
        if not isinstance(tokens, dict):
            continue
        for token, value in tokens.items():
            if not isinstance(value, str) or not HEX_RE.match(value):
                fail(f"{name}.{group}.{token}: '{value}' is not a quoted #RRGGBB hex")
            flat[token] = value
    brand_required: list[str] = []
    for role in ("primary", "secondary", "tertiary"):
        brand_required += [
            f"{role}_light", f"on_{role}_light",
            f"{role}_container_light", f"on_{role}_container_light",
            f"{role}_dark", f"on_{role}_dark",
            f"{role}_container_dark", f"on_{role}_container_dark",
        ]
    neutrals_required = [
        "background_light", "on_background_light", "surface_light",
        "on_surface_light", "surface_variant_light", "on_surface_variant_light",
        "outline_light", "outline_variant_light",
        "surface_container_lowest_light", "surface_container_low_light",
        "surface_container_light", "surface_container_high_light",
        "surface_container_highest_light",
        "background_dark", "on_background_dark", "surface_dark",
        "on_surface_dark", "surface_variant_dark", "on_surface_variant_dark",
        "outline_dark", "outline_variant_dark",
        "surface_container_lowest_dark", "surface_container_low_dark",
        "surface_container_dark", "surface_container_high_dark",
        "surface_container_highest_dark",
        "error_light", "on_error_light", "error_container_light",
        "on_error_container_light", "error_dark", "on_error_dark",
        "error_container_dark", "on_error_container_dark",
    ]
    required = {
        "brand": brand_required,
        "neutrals": neutrals_required,
        "status": ["status_active", "status_maybe", "status_ending_soon", "status_closed"],
        "categories": [
            "category_soccer", "category_drinks", "category_food",
            "category_decision", "category_event", "category_feedback",
        ],
        "extras": [
            "geometric_bar_fill", "geometric_bar_fill_dark", "geometric_accent",
            "top_winner_gold", "launcher_gradient_start", "launcher_gradient_end",
        ],
    }

    missing = []
    for group, tokens in required.items():
        for token in tokens:
            if token not in flat:
                missing.append(f"{group}.{token}")
    if missing:
        fail(f"palette '{name}' is missing tokens after extends: {', '.join(missing)}")
    return flat


# ------------------------------------------------------------------- emitters
def kotlin_name(token: str) -> str:
    return "".join(part.title() for part in token.split("_"))


def emit_color_kt(name: str, palette: dict, flat: dict) -> str:
    def argb(hex_value: str) -> str:
        return "0xFF" + hex_value.lstrip("#").upper()

    def block(title: str, keys: list[str]) -> str:
        lines = [f"// {title}"]
        for key in keys:
            lines.append(f"val {kotlin_name(key)} = Color({argb(flat[key])})")
        return "\n".join(lines)

    sections = [
        "// Earth-Tone & Multi-Palette System — GENERATED by scripts/palettes.py",
        f"// Active palette: {name} — {palette.get('note', '')}",
        "// Edit scripts/palettes.yml and re-run `apply`; never edit this file by hand.",
        "",
        block("Brand — Primary", [
            "primary_light", "on_primary_light", "primary_container_light",
            "on_primary_container_light", "primary_dark", "on_primary_dark",
            "primary_container_dark", "on_primary_container_dark",
        ]),
        "",
        "// Brand — Secondary",
        block("Brand — Secondary", [
            "secondary_light", "on_secondary_light", "secondary_container_light",
            "on_secondary_container_light", "secondary_dark", "on_secondary_dark",
            "secondary_container_dark", "on_secondary_container_dark",
        ]).split("\n", 1)[1],
        "",
        block("Brand — Tertiary", [
            "tertiary_light", "on_tertiary_light", "tertiary_container_light",
            "on_tertiary_container_light", "tertiary_dark", "on_tertiary_dark",
            "tertiary_container_dark", "on_tertiary_container_dark",
        ]),
        "",
        block("Neutrals — Light", [
            "background_light", "on_background_light", "surface_light",
            "on_surface_light", "surface_variant_light",
            "on_surface_variant_light", "outline_light", "outline_variant_light",
        ]),
        "",
        block("Material 3 Surface Containers (Light)", [
            "surface_container_lowest_light", "surface_container_low_light",
            "surface_container_light", "surface_container_high_light",
            "surface_container_highest_light",
        ]),
        "",
        block("Neutrals — Dark", [
            "background_dark", "on_background_dark", "surface_dark",
            "on_surface_dark", "surface_variant_dark", "on_surface_variant_dark",
            "outline_dark", "outline_variant_dark",
        ]),
        "",
        block("Material 3 Surface Containers (Dark)", [
            "surface_container_lowest_dark", "surface_container_low_dark",
            "surface_container_dark", "surface_container_high_dark",
            "surface_container_highest_dark",
        ]),
        "",
        block("Error Tokens", [
            "error_light", "on_error_light", "error_container_light",
            "on_error_container_light", "error_dark", "on_error_dark",
            "error_container_dark", "on_error_container_dark",
        ]),
        "",
        block("Status Colors", [
            "status_active", "status_maybe", "status_ending_soon", "status_closed",
        ]),
        "",
        block("Category Colors", [
            "category_soccer", "category_drinks", "category_food",
            "category_decision", "category_event", "category_feedback",
        ]),
        "",
        block("Chart & Launcher Accents", [
            "geometric_bar_fill", "geometric_bar_fill_dark", "geometric_accent",
            "top_winner_gold",
        ]),
        "",
        """/** Maps a poll category to its brand color; unknown categories fall back to Decision. */
fun categoryColor(category: String): Color = when (category.trim().uppercase()) {
    "SOCCER" -> CategorySoccer
    "DRINKS" -> CategoryDrinks
    "FOOD" -> CategoryFood
    "DECISION" -> CategoryDecision
    "EVENT" -> CategoryEvent
    "FEEDBACK" -> CategoryFeedback
    else -> CategoryDecision
}""",
    ]
    body = "\n".join(sections)
    # collapse doubled blank lines from the split-hack above
    body = re.sub(r"\n{3,}", "\n\n", body)
    return f"package com.plutusoft.livepulse.ui.theme\n\nimport androidx.compose.ui.graphics.Color\n\n{body}\n"


def css_vars(flat: dict, mapping: list[tuple[str, str]]) -> str:
    lines = [f"    {var}: {flat[token]};" for var, token in mapping]
    return ":root {\n" + "\n".join(lines) + "\n  }"


def patch_vote_html(flat: dict, dry_run: bool) -> list[str]:
    text = VOTE_HTML.read_text()
    blocks = ROOT_BLOCK_RE.findall(text)
    if len(blocks) < 2:
        fail("vote.html: expected two ':root {...}' blocks (light + dark)")
    light_block = css_vars(flat, LIGHT_VARS)
    dark_block = css_vars(flat, DARK_VARS)
    new_text = text.replace(blocks[0], light_block, 1).replace(blocks[1], dark_block, 1)
    if new_text == text:
        return ["docs/vote.html: already up to date"]
    if not dry_run:
        VOTE_HTML.write_text(new_text)
    return ["docs/vote.html: CSS vars updated (light + dark)"]


def patch_launcher(flat: dict, dry_run: bool) -> list[str]:
    text = LAUNCHER_BG.read_text()
    new_text = re.sub(
        r'android:color="#[0-9A-Fa-f]+" android:offset="0\.0"',
        f'android:color="{flat["launcher_gradient_start"]}" android:offset="0.0"',
        text,
    )
    new_text = re.sub(
        r'android:color="#[0-9A-Fa-f]+" android:offset="1\.0"',
        f'android:color="{flat["launcher_gradient_end"]}" android:offset="1.0"',
        new_text,
    )
    if new_text == text:
        return ["ic_launcher_background.xml: already up to date"]
    if not dry_run:
        LAUNCHER_BG.write_text(new_text)
    return ["ic_launcher_background.xml: gradient updated"]


def apply(name: str, dry_run: bool) -> None:
    palettes = load_palettes()
    if name not in palettes:
        fail(f"unknown palette '{name}'. Available: {', '.join(sorted(palettes))}")
    flat = validate(name, palettes[name])
    kt = emit_color_kt(name, palettes[name], flat)
    changes: list[str] = []
    if dry_run:
        changes.append("Color.kt: (dry run — not written)")
        changes += patch_vote_html(flat, dry_run=True)
        changes += patch_launcher(flat, dry_run=True)
    else:
        old = COLOR_KT.read_text()
        if old != kt:
            COLOR_KT.write_text(kt)
            changes.append("Color.kt: regenerated")
        else:
            changes.append("Color.kt: already up to date")
        changes += patch_vote_html(flat, dry_run=False)
        changes += patch_launcher(flat, dry_run=False)
    print(f"applied palette '{name}':")
    for change in changes:
        print(f"  - {change}")
    if not dry_run:
        print("\nview it:")
        print("  web:  reload docs/vote.html (served via python3 -m http.server in docs/)")
        print("  app:  python3 scripts/palettes.py preview " + name)


# ------------------------------------------------------------------- preview
def sdk_adb() -> Path:
    props = ROOT / "local.properties"
    if props.exists():
        for line in props.read_text().splitlines():
            if line.strip().startswith("sdk.dir="):
                return Path(line.split("=", 1)[1].strip()) / "platform-tools" / "adb"
    return Path.home() / "Library" / "Android" / "sdk" / "platform-tools" / "adb"


def preview(name: str, use_emulator_backend: bool) -> None:
    apply(name, dry_run=False)
    gradle_props = ["-PfirestoreEmulator=true"] if use_emulator_backend else []
    print("\nbuilding…")
    subprocess.run(
        ["./gradlew", ":app:assembleDebug", *gradle_props],
        cwd=ROOT, check=True,
    )
    adb = sdk_adb()
    apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    print("installing…")
    subprocess.run([str(adb), "install", "-r", str(apk)], check=True)
    print("launching…")
    subprocess.run(
        [str(adb), "shell", "am", "force-stop", "com.plutusoft.livepulse"], check=True,
    )
    subprocess.run(
        [str(adb), "shell", "am", "start", "-n",
         "com.plutusoft.livepulse/.MainActivity"],
        check=True,
    )
    print(f"\n'{name}' is live on the emulator — screenshot with adb screencap to judge it.")


# -------------------------------------------------------------------- commands
def cmd_list() -> None:
    palettes = load_palettes()
    for name in sorted(palettes):
        palette = palettes[name]
        tag = "  [abstract base]" if palette.get("abstract") else ""
        print(f"{name}{tag} — {palette.get('note', '')}")


def cmd_show(name: str) -> None:
    palettes = load_palettes()
    if name not in palettes:
        fail(f"unknown palette '{name}'. Available: {', '.join(sorted(palettes))}")
    flat = validate(name, palettes[name])
    width = max(len(key) for key in flat)
    for key in sorted(flat):
        print(f"{key:<{width}}  {flat[key]}")


def main() -> None:
    parser = argparse.ArgumentParser(
        prog="palettes.py",
        description="Switch LivePulse color palettes from scripts/palettes.yml.",
    )
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("list", help="list available palettes")
    show = sub.add_parser("show", help="print a resolved palette's tokens")
    show.add_argument("name")
    apply_p = sub.add_parser("apply", help="regenerate Color.kt + vote.html + launcher")
    apply_p.add_argument("name")
    apply_p.add_argument("--dry-run", action="store_true")
    prev = sub.add_parser("preview", help="apply + build + install + launch on emulator")
    prev.add_argument("name")
    prev.add_argument("--emulator", action="store_true",
                      help="build with -PfirestoreEmulator=true")
    args = parser.parse_args()

    if args.command == "list":
        cmd_list()
    elif args.command == "show":
        cmd_show(args.name)
    elif args.command == "apply":
        apply(args.name, args.dry_run)
    elif args.command == "preview":
        preview(args.name, args.emulator)


if __name__ == "__main__":
    main()
