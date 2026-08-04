#!/usr/bin/env python3
"""Deprecated: CM resources patch is tracked as a unified diff.

Source of truth:
  patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch

Apply via:
  ./scripts/build-wasmtime4j-desktop-cm.ps1
  # or: git -C .deps/wasmtime4j apply patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch

Regenerate after editing .deps/wasmtime4j:
  python ./scripts/export-wasmtime4j-patches.py
"""
from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
PATCH = ROOT / "patches" / "wasmtime4j-v47.0.2-1.5.0-cm-resources.patch"


def main() -> int:
    print(
        "DEPRECATED: use tracked patch instead of this mutator.\n"
        f"  patch: {PATCH}\n"
        "  apply: ./scripts/build-wasmtime4j-desktop-cm.ps1\n"
        "  regen: python ./scripts/export-wasmtime4j-patches.py",
        file=sys.stderr,
    )
    if not PATCH.exists():
        print(f"missing {PATCH}", file=sys.stderr)
        return 1
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
