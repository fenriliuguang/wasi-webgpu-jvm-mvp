#!/usr/bin/env python3
"""Export trackable unified diffs from a patched .deps/wasmtime4j checkout.

Writes:
  patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch
  patches/wasmtime4j-v47.0.2-1.5.0-android.patch

Run from repo root after the desired edits exist in .deps/wasmtime4j.
"""
from __future__ import annotations

import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
DEPS = ROOT / ".deps" / "wasmtime4j"
PATCHES = ROOT / "patches"

GROUPS = {
    "wasmtime4j-v47.0.2-1.5.0-cm-resources.patch": [
        "wasmtime4j-native/src/component/linker.rs",
        "wasmtime4j-native/src/jni/component_linker.rs",
    ],
    "wasmtime4j-v47.0.2-1.5.0-android.patch": [
        "wasmtime4j-native/src/async_runtime.rs",
        "wasmtime4j-native/src/jni/memory.rs",
    ],
}


def main() -> int:
    if not (DEPS / ".git").exists() and not (DEPS / "wasmtime4j-native").exists():
        print(f"missing checkout {DEPS}", file=sys.stderr)
        return 1
    PATCHES.mkdir(parents=True, exist_ok=True)
    for name, paths in GROUPS.items():
        proc = subprocess.run(
            ["git", "-C", str(DEPS), "diff", "--no-ext-diff", "--text", "--", *paths],
            check=True,
            capture_output=True,
        )
        # git on Windows may emit CRLF; normalize to LF for portable git apply.
        text = proc.stdout.decode("utf-8", errors="strict").replace("\r\n", "\n").replace("\r", "\n")
        if not text.strip():
            print(f"WARNING: empty diff for {name}", file=sys.stderr)
        out = PATCHES / name
        out.write_bytes(text.encode("utf-8"))
        print(f"wrote {out} ({out.stat().st_size} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
