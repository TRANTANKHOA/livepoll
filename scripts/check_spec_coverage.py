#!/usr/bin/env python3
"""Fail if any regression test is not linked from at least one spec.

Enforces the spec-driven convention: tests/batch-*.yml defines the test
suite; specs/spec.yml must reference every test id via a `tests/...#ID`
link, and every linked id must still exist (no stale links). Run in CI
(.github/workflows/specs.yml) and before committing new tests.
"""
import pathlib
import re
import sys

root = pathlib.Path(__file__).resolve().parent.parent

test_ids = set()
for f in sorted((root / "tests").glob("batch-*.yml")):
    test_ids |= set(re.findall(r"^\s*- id:\s*(\S+)\s*$", f.read_text(), re.M))

spec_text = (root / "specs" / "spec.yml").read_text()
linked = set(re.findall(r"tests/[\w.-]+\.yml#([A-Za-z0-9_-]+)", spec_text))

missing = sorted(test_ids - linked)
stale = sorted(linked - test_ids)
for tid in missing:
    print(f"FAIL test {tid} has no spec linking it (add a spec with tests: ...#{tid})")
for tid in stale:
    print(f"FAIL spec links unknown test {tid} (removed or renamed in tests/?)")
if missing or stale:
    print(f"\nFAIL: {len(missing)} uncovered test(s), {len(stale)} stale link(s)")
    sys.exit(1)
print(f"OK: all {len(test_ids)} regression tests are spec-linked, no stale links")
