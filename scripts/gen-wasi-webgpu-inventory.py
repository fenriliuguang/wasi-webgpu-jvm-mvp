#!/usr/bin/env python3
"""Parse wit/deps/wasi-webgpu/webgpu.wit → _inventory.json (resource × method)."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WIT = ROOT / "wit/deps/wasi-webgpu/webgpu.wit"
OUT = ROOT / "wit/deps/wasi-webgpu/_inventory.json"


def main() -> None:
    text = WIT.read_text(encoding="utf-8")
    pkg = re.search(r"package\s+([\w:-]+)@([\w.\-]+)", text)
    resources: dict[str, list] = {}
    for rm in re.finditer(r"(?m)^  resource\s+([\w-]+)\s*\{", text):
        name = rm.group(1)
        start = rm.end()
        depth = 1
        i = start
        while i < len(text) and depth:
            if text[i] == "{":
                depth += 1
            elif text[i] == "}":
                depth -= 1
            i += 1
        body = text[start : i - 1]
        methods = []
        for line in body.splitlines():
            line = line.strip()
            if not line or line.startswith("//"):
                continue
            mm = re.match(r"(static\s+)?([\w-]+):\s*(async\s+)?func\s*\(", line)
            if mm:
                methods.append(
                    {
                        "name": mm.group(2),
                        "async": bool(mm.group(3)),
                        "static": bool(mm.group(1)),
                    }
                )
        resources[name] = methods
    total = sum(len(v) for v in resources.values())
    payload = {
        "package": pkg.group(1) if pkg else "wasi:webgpu",
        "version": pkg.group(2) if pkg else "0.3.0-rc.2",
        "resources": resources,
        "method_count": total,
    }
    OUT.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(f"resources={len(resources)} methods={total} -> {OUT}")


if __name__ == "__main__":
    main()
