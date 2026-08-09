#!/usr/bin/env python3
"""Generate docs/mapping/compliant-world-gap{.md,.en.md} from vendored WIT inventory."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INV = ROOT / "wit/deps/wasi-webgpu/_inventory.json"
OUT_ZH = ROOT / "docs/mapping/compliant-world-gap.md"
OUT_EN = ROOT / "docs/mapping/compliant-world-gap.en.md"

inv = json.loads(INV.read_text(encoding="utf-8"))
resources = inv["resources"]
MARK = {"ok": "✅", "skew": "⚠️", "missing": "❌", "out": "—"}


def classify(res: str, method: str, is_async: bool):
    key = f"{res}.{method}"
    if key == "gpu.request-adapter":
        return (
            "ok",
            "B/C",
            "experimental request-adapter；async→sync；options 子集",
            "experimental request-adapter; async→sync; options subset",
        )
    if key == "gpu.get-preferred-canvas-format":
        return ("missing", "E/G", "无 gfx canvas；可 Unsupported", "no gfx canvas; Unsupported OK")
    if key == "gpu.wgsl-language-features":
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if key == "gpu-adapter.request-device":
        return (
            "skew",
            "C/F",
            "wasi 已接线 → result Ok(device)；无完整 device descriptor；async→sync",
            "wasi wired → result Ok(device); no full device descriptor; async→sync",
        )
    if key in ("gpu-adapter.features", "gpu-adapter.limits", "gpu-adapter.info"):
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if res == "gpu-adapter-info":
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if key == "gpu-buffer.map-async":
        return (
            "skew",
            "C/F",
            "wasi 已接线 → result Ok；L2 sync 等待",
            "wasi wired → result Ok; L2 sync wait",
        )
    if key == "gpu-buffer.get-mapped-range-get-with-copy":
        return (
            "skew",
            "C/F",
            "wasi 已接线 → result Ok(list<u8>)；ByteArray 拷贝",
            "wasi wired → result Ok(list<u8>); ByteArray copy",
        )
    if key == "gpu-buffer.get-mapped-range-set-with-copy":
        return ("missing", "C/G", "可 Unsupported", "Unsupported OK")
    if key == "gpu-buffer.unmap":
        return ("ok", "C", "", "")
    if key in ("gpu-buffer.size", "gpu-buffer.usage", "gpu-buffer.map-state", "gpu-buffer.destroy"):
        return (
            "missing",
            "C/G",
            "属性/destroy 未暴露；可 Unsupported",
            "props/destroy not exposed; Unsupported OK",
        )
    if key == "gpu-device.queue":
        return ("ok", "B/C", "experimental get-queue", "experimental get-queue")
    if key == "gpu-device.create-buffer":
        return ("ok", "C", "buffer-descriptor 已对齐", "buffer-descriptor aligned")
    if key == "gpu-device.create-shader-module":
        return (
            "skew",
            "C",
            "wasi 已接线；从 descriptor 取 code（忽略 compilation-hints）",
            "wasi wired; extract code from descriptor (ignore compilation-hints)",
        )
    if key == "gpu-device.create-bind-group-layout":
        return (
            "skew",
            "C",
            "特化 create-bind-group-layout-storage3",
            "specialized create-bind-group-layout-storage3",
        )
    if key == "gpu-device.create-bind-group":
        return ("skew", "C", "特化 create-bind-group3", "specialized create-bind-group3")
    if key == "gpu-device.create-compute-pipeline":
        return ("skew", "C", "layout+shader+entry 便捷形", "layout+shader+entry helper shape")
    if key == "gpu-device.create-command-encoder":
        return ("ok", "C", "", "")
    if key == "gpu-device.create-render-pipeline":
        return ("skew", "E", "特化 *-triangle* helpers", "specialized *-triangle* helpers")
    if key == "gpu-device.create-pipeline-layout":
        return ("missing", "D", "", "")
    if key in ("gpu-device.create-texture", "gpu-device.create-sampler"):
        return ("missing", "D", "", "")
    if key in (
        "gpu-device.create-compute-pipeline-async",
        "gpu-device.create-render-pipeline-async",
    ):
        return (
            "skew",
            "F",
            "本阶段 sync-compat；可先 Unsupported",
            "sync-compat this phase; Unsupported first OK",
        )
    if key in ("gpu-device.create-render-bundle-encoder", "gpu-device.create-query-set"):
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if key in (
        "gpu-device.features",
        "gpu-device.limits",
        "gpu-device.adapter-info",
        "gpu-device.destroy",
        "gpu-device.lost",
        "gpu-device.push-error-scope",
        "gpu-device.pop-error-scope",
        "gpu-device.on-uncaptured-error",
    ):
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if key == "gpu-queue.submit":
        return ("ok", "C", "wasi list submit 已接线", "wasi list submit wired")
    if key == "gpu-queue.write-buffer-with-copy":
        return ("ok", "C", "wasi 已接线 → result Ok", "wasi wired → result Ok")
    if key == "gpu-queue.write-texture-with-copy":
        return (
            "ok",
            "C",
            "wasi 已接线；texel-copy 扁平子集",
            "wasi wired; texel-copy flat subset",
        )
    if key == "gpu-queue.on-submitted-work-done":
        return ("missing", "D/G", "可 Unsupported", "Unsupported OK")
    if key == "gpu-command-encoder.begin-compute-pass":
        return ("ok", "C", "", "")
    if key == "gpu-command-encoder.begin-render-pass":
        return (
            "skew",
            "E",
            "特化 begin-render-pass-clear",
            "specialized begin-render-pass-clear",
        )
    if key == "gpu-command-encoder.copy-buffer-to-buffer":
        return ("ok", "C", "", "")
    if key == "gpu-command-encoder.finish":
        return ("ok", "C", "", "")
    if key in (
        "gpu-command-encoder.copy-buffer-to-texture",
        "gpu-command-encoder.copy-texture-to-buffer",
        "gpu-command-encoder.copy-texture-to-texture",
        "gpu-command-encoder.clear-buffer",
        "gpu-command-encoder.resolve-query-set",
    ):
        return ("missing", "D/G", "可 Unsupported", "Unsupported OK")
    if key in (
        "gpu-command-encoder.push-debug-group",
        "gpu-command-encoder.pop-debug-group",
        "gpu-command-encoder.insert-debug-marker",
    ):
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if key in (
        "gpu-compute-pass-encoder.set-pipeline",
        "gpu-compute-pass-encoder.set-bind-group",
        "gpu-compute-pass-encoder.dispatch-workgroups",
        "gpu-compute-pass-encoder.end",
    ):
        return ("ok", "C", "", "")
    if key in (
        "gpu-compute-pass-encoder.dispatch-workgroups-indirect",
        "gpu-compute-pass-encoder.set-immediates",
    ):
        return ("missing", "C/G", "可 Unsupported", "Unsupported OK")
    if key in (
        "gpu-compute-pass-encoder.push-debug-group",
        "gpu-compute-pass-encoder.pop-debug-group",
        "gpu-compute-pass-encoder.insert-debug-marker",
    ):
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if key in (
        "gpu-render-pass-encoder.set-pipeline",
        "gpu-render-pass-encoder.set-vertex-buffer",
        "gpu-render-pass-encoder.end",
    ):
        return ("ok", "E", "", "")
    if key == "gpu-render-pass-encoder.draw":
        return (
            "skew",
            "E",
            "形参子集（仅 vertex-count）",
            "arity subset (vertex-count only)",
        )
    if key == "gpu-render-pass-encoder.set-bind-group":
        return (
            "ok",
            "C",
            "wasi 已接线 → result Ok；忽略 dynamic-offsets",
            "wasi wired → result Ok; ignore dynamic-offsets",
        )
    if key in (
        "gpu-render-pass-encoder.set-index-buffer",
        "gpu-render-pass-encoder.draw-indexed",
        "gpu-render-pass-encoder.draw-indirect",
        "gpu-render-pass-encoder.draw-indexed-indirect",
        "gpu-render-pass-encoder.set-viewport",
        "gpu-render-pass-encoder.set-scissor-rect",
        "gpu-render-pass-encoder.set-blend-constant",
        "gpu-render-pass-encoder.set-stencil-reference",
        "gpu-render-pass-encoder.begin-occlusion-query",
        "gpu-render-pass-encoder.end-occlusion-query",
        "gpu-render-pass-encoder.execute-bundles",
        "gpu-render-pass-encoder.set-immediates",
    ):
        return ("missing", "E/G", "可 Unsupported", "Unsupported OK")
    if key in (
        "gpu-render-pass-encoder.push-debug-group",
        "gpu-render-pass-encoder.pop-debug-group",
        "gpu-render-pass-encoder.insert-debug-marker",
    ):
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if res == "gpu-canvas-context":
        if method in ("configure", "unconfigure"):
            return (
                "skew",
                "E",
                "experimental surface.*；Host 注入 native window；非 gfx",
                "experimental surface.*; Host-inject native window; not gfx",
            )
        if method == "get-current-texture":
            return (
                "skew",
                "E",
                "封装为 get-current-texture-view",
                "wrapped as get-current-texture-view",
            )
        if method == "get-configuration":
            return ("missing", "E/G", "可 Unsupported", "Unsupported OK")
    if key == "gpu-texture.create-view":
        return (
            "skew",
            "D/E",
            "仅 surface 路径 textureCreateView",
            "only surface-path textureCreateView",
        )
    if res == "gpu-texture":
        return ("missing", "D/G", "可 Unsupported", "Unsupported OK")
    if res == "gpu-texture-view":
        return (
            "missing",
            "E/G",
            "resource 存在；label 可 Unsupported",
            "resource exists; label Unsupported OK",
        )
    if key in (
        "gpu-compute-pipeline.get-bind-group-layout",
        "gpu-render-pipeline.get-bind-group-layout",
    ):
        return ("missing", "C/E/G", "可 Unsupported", "Unsupported OK")
    if res in ("gpu-compute-pipeline", "gpu-render-pipeline", "gpu-shader-module"):
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if res in (
        "gpu-sampler",
        "gpu-pipeline-layout",
        "gpu-bind-group",
        "gpu-bind-group-layout",
        "gpu-command-buffer",
        "gpu-query-set",
        "gpu-render-bundle",
    ):
        slice_ = "D/G" if res in ("gpu-sampler", "gpu-pipeline-layout") else "G"
        return ("missing", slice_, "可 Unsupported", "Unsupported OK")
    if res in (
        "gpu-query-set",
        "gpu-render-bundle",
        "gpu-render-bundle-encoder",
        "gpu-error",
        "gpu-device-lost-info",
        "gpu-compilation-info",
        "gpu-compilation-message",
        "gpu-uncaptured-error-event",
        "gpu-supported-features",
        "gpu-supported-limits",
        "record-gpu-pipeline-constant-value",
        "record-option-gpu-size64",
        "wgsl-language-features",
    ):
        return ("missing", "G", "长尾；可 Unsupported 关门", "long-tail; Unsupported closes")
    if method in ("label", "set-label"):
        return ("missing", "G", "可 Unsupported", "Unsupported OK")
    if is_async:
        return (
            "skew",
            "F",
            "上游 async；本阶段 sync-compat 或不实现",
            "upstream async; sync-compat or skip this phase",
        )
    return ("missing", "G", "可 Unsupported", "Unsupported OK")


def emit(lang: str) -> tuple[str, dict]:
    zh = lang == "zh"
    lines: list[str] = []
    a = lines.append
    counts = {"ok": 0, "skew": 0, "missing": 0, "out": 0}
    rows_by_res: dict[str, list] = {}
    for res, methods in resources.items():
        rows = []
        for m in methods:
            st, sl, nz, ne = classify(res, m["name"], m.get("async", False))
            counts[st] += 1
            async_tag = " `async`" if m.get("async") else ""
            note = nz if zh else ne
            rows.append((f"`{res}.{m['name']}`{async_tag}", MARK[st], sl, note))
        rows_by_res[res] = rows

    if zh:
        a("# 合规 world 缺口矩阵")
        a("")
        a("**中文** | [English](compliant-world-gap.en.md)")
        a("")
        a(
            "> **状态：** 切片 G 关门（2026-08-09）；DoD "
            "[`archive-compliant-world-dod.md`](../scheme/archive-compliant-world-dod.md)。  "
        )
        a("> **钉定：** `wasi:webgpu/webgpu@0.3.0-rc.2`（tag `v0.3.0-rc.2`）  ")
        a(
            "> **现状包：** `experimental:webgpu-cm@0.7.0`"
            "（[`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)）  "
        )
        a("> **阶段计划：** [`docs/scheme/compliant-world.md`](../scheme/compliant-world.md)  ")
        a(
            f"> **方法数：** {inv['method_count']}（resource × method；见 "
            "[`_inventory.json`](../../wit/deps/wasi-webgpu/_inventory.json)）"
        )
        a("")
        a(
            "本表对照标准包与本仓 experimental / L2 现状。"
            "关门规则：每行最终为 ✅ / ⚠️ / ❌（显式 `Unsupported` 可关门）。"
            "**不得**留下悬空「无」。"
        )
        a("")
        a("## 图例")
        a("")
        a("| 标记 | 含义 |")
        a("|------|------|")
        a("| ✅ | 已有可对齐路径（experimental 或 L2） |")
        a("| ⚠️ | 有路径但特化 / 形状偏差 / sync 包装 |")
        a("| ❌ | 显式 `Unsupported` / wasi stub（G 关门后可关门；非悬空「无」） |")
        a("| — | 本阶段明确不做（如 wasi-gfx） |")
        a("")
        a("| 列 | 含义 |")
        a("|----|------|")
        a("| 上游方法 | `resource.method`（`async` 已标注） |")
        a("| 现状 | ✅ / ⚠️ / ❌ |")
        a("| 切片 | A–G |")
        a("| 备注 | 特化名、Host 注入、可 Unsupported 等 |")
        a("")
        a("## 汇总")
        a("")
        a("| ✅ | ⚠️ | ❌ | 合计 |")
        a("|----|----|----|------|")
        a(
            f"| {counts['ok']} | {counts['skew']} | {counts['missing']} | "
            f"{sum(counts.values())} |"
        )
        a("")
        a("## 已知特化 API（Host 已提供标准替代；Guest 真机待 `.so` 后迁完）")
        a("")
        a("| experimental API | 替代方向 | 切片 |")
        a("|------------------|----------|------|")
        a("| `create-bind-group-layout-storage3` | 标准 bind-group-layout descriptor | C |")
        a("| `create-bind-group3` | 标准 bind-group descriptor | C |")
        a("| `submit1` | 标准 `queue.submit`（list） | C |")
        a("| `create-render-pipeline-triangle` | 标准 render-pipeline descriptor | E |")
        a(
            "| `create-render-pipeline-triangle-buffers` | "
            "同上（vertex layouts 已部分对齐） | E |"
        )
        a("| `begin-render-pass-clear` | 标准 begin-render-pass + color attachment | E |")
        a(
            "| `create-surface-from-native-window` | "
            "保留 Host 注入（非 gfx）；或映射标准 canvas-context 子集 | E |"
        )
        a("")
        a("## wasi-gfx")
        a("")
        a(
            "本阶段 **不做** wasi-gfx / window / canvas 抽象。"
            "上屏继续 Host 注入 Android native window（见 `gpu-canvas-context` 行备注）。"
        )
    else:
        a("# Compliant-world gap matrix")
        a("")
        a("[中文](compliant-world-gap.md) | **English**")
        a("")
        a(
            "> **Status:** slice G close-out (2026-08-09); DoD "
            "[`archive-compliant-world-dod.en.md`](../scheme/archive-compliant-world-dod.en.md).  "
        )
        a("> **Pin:** `wasi:webgpu/webgpu@0.3.0-rc.2` (tag `v0.3.0-rc.2`)  ")
        a(
            "> **Current package:** `experimental:webgpu-cm@0.7.0` "
            "([`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit))  "
        )
        a(
            "> **Phase plan:** "
            "[`docs/scheme/compliant-world.en.md`](../scheme/compliant-world.en.md)  "
        )
        a(
            f"> **Methods:** {inv['method_count']} (resource × method; see "
            "[`_inventory.json`](../../wit/deps/wasi-webgpu/_inventory.json))"
        )
        a("")
        a(
            "Contrasts the standard package with this repo’s experimental / L2 status. "
            "Close-out: every row ✅ / ⚠️ / ❌ (explicit `Unsupported` OK). "
            "**No** dangling missing rows."
        )
        a("")
        a("## Legend")
        a("")
        a("| Mark | Meaning |")
        a("|------|---------|")
        a("| ✅ | Usable aligned path (experimental or L2) |")
        a("| ⚠️ | Path exists but specialized / shape skew / sync wrap |")
        a("| ❌ | Explicit `Unsupported` / wasi stub (closes a row after G; not a dangling miss) |")
        a("| — | Explicitly out of this phase (e.g. wasi-gfx) |")
        a("")
        a("| Column | Meaning |")
        a("|--------|---------|")
        a("| Upstream method | `resource.method` (`async` noted) |")
        a("| Status | ✅ / ⚠️ / ❌ |")
        a("| Slice | A–G |")
        a("| Notes | Helpers, Host inject, Unsupported OK, etc. |")
        a("")
        a("## Summary")
        a("")
        a("| ✅ | ⚠️ | ❌ | Total |")
        a("|----|----|----|-------|")
        a(
            f"| {counts['ok']} | {counts['skew']} | {counts['missing']} | "
            f"{sum(counts.values())} |"
        )
        a("")
        a("## Known specialized APIs (Host has standard replacements; device Guests migrate after `.so` rebuild)")
        a("")
        a("| experimental API | Replacement | Slice |")
        a("|------------------|-------------|-------|")
        a("| `create-bind-group-layout-storage3` | standard bind-group-layout descriptor | C |")
        a("| `create-bind-group3` | standard bind-group descriptor | C |")
        a("| `submit1` | standard `queue.submit` (list) | C |")
        a("| `create-render-pipeline-triangle` | standard render-pipeline descriptor | E |")
        a(
            "| `create-render-pipeline-triangle-buffers` | "
            "same (vertex layouts partly aligned) | E |"
        )
        a("| `begin-render-pass-clear` | standard begin-render-pass + color attachment | E |")
        a(
            "| `create-surface-from-native-window` | "
            "keep Host inject (not gfx); or map to canvas-context subset | E |"
        )
        a("")
        a("## wasi-gfx")
        a("")
        a(
            "**Out of this phase:** wasi-gfx / window / canvas. "
            "On-screen stays Host-injected Android native window "
            "(see `gpu-canvas-context` notes)."
        )

    for res in sorted(rows_by_res.keys()):
        a("")
        a(f"## `{res}`")
        a("")
        if zh:
            a("| 上游方法 | 现状 | 切片 | 备注 |")
            a("|----------|------|------|------|")
        else:
            a("| Upstream method | Status | Slice | Notes |")
            a("|-----------------|--------|-------|-------|")
        for path, mark, sl, note in rows_by_res[res]:
            a(f"| {path} | {mark} | {sl} | {note} |")

    a("")
    if zh:
        a("## 补全 / 升级约定")
        a("")
        a(
            "1. 改钉定版本：先更新 [`PIN.md`](../../wit/deps/wasi-webgpu/PIN.md) "
            "与 `_inventory.json`，再改本矩阵，最后改 Host / ABI。  "
        )
        a("2. ❌ +「显式 Unsupported」视为该行可关门。  ")
        a(
            "3. 重新生成：`python scripts/gen-compliant-world-gap.py` "
            "（需先有 `_inventory.json`）。"
        )
        a("")
        a("## 链接")
        a("")
        a("- 阶段计划：[`compliant-world.md`](../scheme/compliant-world.md)")
        a("- PIN：[`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)")
        a(
            "- Compute / Render 子集：[`compute-subset.md`](compute-subset.md) · "
            "[`render-subset.md`](render-subset.md)"
        )
        a("- 错误与 Async：[`errors-async.md`](errors-async.md)")
        a("- 上游：https://github.com/WebAssembly/wasi-webgpu/tree/v0.3.0-rc.2")
    else:
        a("## Update rules")
        a("")
        a(
            "1. Bump pin: update [`PIN.md`](../../wit/deps/wasi-webgpu/PIN.md) "
            "and `_inventory.json`, then this matrix, then Host / ABI.  "
        )
        a("2. ❌ + explicit Unsupported closes that row.  ")
        a(
            "3. Regenerate: `python scripts/gen-compliant-world-gap.py` "
            "(requires `_inventory.json`)."
        )
        a("")
        a("## Links")
        a("")
        a("- Phase plan: [`compliant-world.en.md`](../scheme/compliant-world.en.md)")
        a("- PIN: [`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)")
        a(
            "- Compute / Render: [`compute-subset.en.md`](compute-subset.en.md) · "
            "[`render-subset.en.md`](render-subset.en.md)"
        )
        a("- Errors & async: [`errors-async.en.md`](errors-async.en.md)")
        a("- Upstream: https://github.com/WebAssembly/wasi-webgpu/tree/v0.3.0-rc.2")
    a("")
    return "\n".join(lines), counts


def main() -> None:
    zh_md, counts = emit("zh")
    en_md, _ = emit("en")
    OUT_ZH.write_text(zh_md, encoding="utf-8")
    OUT_EN.write_text(en_md, encoding="utf-8")
    print("counts", counts)
    print("wrote", OUT_ZH, OUT_EN)


if __name__ == "__main__":
    main()
