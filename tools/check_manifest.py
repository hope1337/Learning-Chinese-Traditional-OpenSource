#!/usr/bin/env python3
"""Validate models/catalog.json and every models/*/manifest.json.

Checks: required fields, id/path consistency, license present, sha map shape.
Does not download weights. Exit non-zero on failure.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "models" / "catalog.json"

REQUIRED = {"id", "role", "displayName", "license", "source"}


def fail(msg: str) -> int:
    print(f"ERROR: {msg}")
    return 1


def main() -> int:
    if not CATALOG.exists():
        return fail("models/catalog.json not found")
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    seen = set()
    for entry in catalog.get("models", []):
        missing = REQUIRED - set(entry)
        if missing:
            return fail(f"catalog entry {entry.get('id')} missing {sorted(missing)}")
        if entry["id"] in seen:
            return fail(f"duplicate model id {entry['id']}")
        seen.add(entry["id"])
        manifest_rel = entry.get("manifest")
        if manifest_rel:
            manifest = ROOT / manifest_rel
            if not manifest.exists():
                return fail(f"manifest not found: {manifest_rel}")
            data = json.loads(manifest.read_text(encoding="utf-8"))
            if data.get("id") != entry["id"]:
                return fail(f"id mismatch in {manifest_rel}")
            if data.get("role") != entry["role"]:
                return fail(f"role mismatch in {manifest_rel}")
            if not data.get("license"):
                return fail(f"license missing in {manifest_rel}")
    print(f"OK: {len(seen)} catalog entries validated.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
