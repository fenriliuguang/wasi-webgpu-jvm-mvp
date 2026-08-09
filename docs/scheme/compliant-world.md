# 合规 wasi:webgpu World（无 gfx）（compliant-world）— 已完成

**中文** | [English](compliant-world.en.md)

> **状态：已完成（2026-08-09）。** DoD 归档：[`archive-compliant-world-dod.md`](archive-compliant-world-dod.md)。  
> 切片 **A–G 全部完成**。矩阵关门 ≠ 合规产品宣传（仍以 experimental 为主轨）。  
> 承接：语义加固 A–E 归档（[`archive-semantic-hardening-dod.md`](archive-semantic-hardening-dod.md)）。  
> 组合：上游钉定（A ✅）→ 双轨 Linker（B ✅）→ Compute 去特化（C ✅）→ 纹理（D ✅）→ 通用 Render（E ✅）→ 错误抬升（F ✅）→ 长尾关门（G ✅）。

## 一句话

在 **不推进 wasi-gfx** 的前提下，把 CM 主线从当时的 `experimental:webgpu-cm@0.4.0` 子集推进到钉定标准包 `wasi:webgpu/webgpu@0.3.0-rc.2` 的 **全量 interface 覆盖**（方法级矩阵：实现或显式 `Unsupported`），并在 experimental 轨上把 Guest 改走标准形 descriptor API（现包 **`@0.7.0`**，主验收仍在 experimental）；上屏仍靠 Host 注入 Android native window。

```text
A 上游钉定说明 + 缺口矩阵
  → B 双轨包身份 / Linker 策略
  → C Compute 去特化（淘汰 *storage3 / *3）
  → D Texture / Sampler / PipelineLayout
  → E 通用 Render（淘汰 *-triangle*；surface 仍 Host 注入）
  → F result / error-kind 抬升（async 仍 sync-compat）
  → G 长尾资源覆盖率关门
```

对照实现：[`wasi-webgpu-wasmtime`](https://crates.io/crates/wasi-webgpu-wasmtime)。  
缺口矩阵：[`docs/mapping/compliant-world-gap.md`](../mapping/compliant-world-gap.md)。

## 已定决策

| 问题 | 决定 |
|------|------|
| 本阶段主线 | 合规 `wasi:webgpu` **全量 interface 覆盖**（方法级矩阵：实现或显式 `Unsupported` 可关门） |
| gfx | **不做** wasi-gfx / canvas；上屏继续 **Host 注入** Android native window |
| WIT 钉定 | `wasi:webgpu@0.3.0-rc.2`（与 [`wit/README.md`](../../wit/README.md) 一致）；升级须先改 PIN + 缺口矩阵 |
| 迁移策略 | **双轨**：保留 experimental 轨直至 Guest 迁到标准包（计划锁定时为 `@0.4.0`，关门后现行 **`@0.7.0`** 仍为主验收）；标准包为 stub 并存 |
| Async | 仍 **sync-compat**（与 L2 / [`errors-async.md`](../mapping/errors-async.md) 一致）；真 CM async 不阻塞本阶段 |
| 合规宣称 | 包名/README 保持 `experimental`；矩阵关门后**仍**不得宣传已合规产品 |
| 明确移交 | Maven Central、`abi-mvp` 扁平 render、可选 perf、对 wasmtime4j 提 PR — 均不做 |
| 验收形态 | 桌面单测（有 natives）+ Android 仪器（现有 vector-add / triangle 路径不回归）+ 缺口矩阵勾选；每子切片文档 / CHANGELOG |

## 子切片与 DoD

### A — 上游钉定 + 缺口矩阵

- [x] Vendor / pin 上游 `wasi:webgpu@0.3.0-rc.2` WIT（[`wit/deps/wasi-webgpu/`](../../wit/deps/wasi-webgpu/) + [`PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)）；**不**随 tip 漂移
- [x] 缺口矩阵按方法补全（224 行：[`compliant-world-gap.md`](../mapping/compliant-world-gap.md)；inventory [`_inventory.json`](../../wit/deps/wasi-webgpu/_inventory.json)）
- [x] 更新 [`wit/README.md`](../../wit/README.md)：标准包钉定路径与 experimental 双轨说明；再生脚本 `scripts/gen-wasi-webgpu-inventory.py` / `gen-compliant-world-gap.py`

### B — 双轨包身份 / Linker

- [x] 标准包 import 路径（`wasi:webgpu/webgpu@0.3.0-rc.2`）与 experimental 轨可并存于 Linker（[`WasmtimeCmLinker`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmLinker.kt)；现行 experimental `@0.7.0`）
- [x] ABI 常量 / 资源名映射文档化：[`abi-wasi`](../../abi-wasi/) `AbiWasi` + [`compliant-world-dual-track.md`](../mapping/compliant-world-dual-track.md)；Guest 仍走 experimental（主验收轨）
- [x] 文档标明：双轨是迁移手段；矩阵关门后 **主验收仍在 experimental**；标准包为 **Unsupported / result stub**（尚未迁 Guest）

### C — Compute 去特化

- [x] 标准 `bind-group-layout` / `bind-group` / `compute-pipeline` descriptor 路径接线（`experimental:webgpu-cm@0.5.0` → L2）
- [x] Host 便捷 API 标 deprecated（`*storage3` / `*3` / `submit1` / `create-compute-pipeline-bgl`）
- [x] `vector-add-cm`：`create-bind-group-layout(descriptor)` 已上真机；含嵌套 borrow 的 `create-bind-group` / `create-compute-pipeline` / `queue.submit(list)` 仍用顶层 helper，直至重编 Android `.so`（`cm-resources` 补丁已改为 **递归** Resource→U32，见 [`android-wasmtime.md`](../android-wasmtime.md) §6）
- [x] 真机 `run-android-instrumented.ps1` 两波绿灯（vivo）；triangle 仅 bump 包版本
- [x] 更新 [`compute-subset.md`](../mapping/compute-subset.md) / EN

### D — Texture / Sampler / PipelineLayout

- [x] L2 + Dawn（及 Cpu stub）覆盖 texture / sampler / pipeline-layout 主路径（可不上屏验收）
- [x] 缺口矩阵对应行标 ✅ 或显式 ❌/`Unsupported`
- [x] 映射文档增量（[`compute-subset.md`](../mapping/compute-subset.md) + gap）；`experimental:webgpu-cm@0.6.0`；compute-pipeline.layout → pipeline-layout

### E — 通用 Render（无 gfx）

- [x] 通用 `create-render-pipeline` / `begin-render-pass` descriptor（`experimental:webgpu-cm@0.7.0` → L2/Dawn）；`*-triangle*` / `begin-render-pass-clear` 标 deprecated 保留
- [x] Surface 仍 Host 注入 native window；**不**引入 wasi-gfx
- [x] `triangle-cm` 包版本 bump；标准 descriptor Guest 路径待 Android `.so` 重编（嵌套 borrow；真机仍用顶层 helpers）
- [x] 更新 [`render-subset.md`](../mapping/render-subset.md) / gap / CHANGELOG

### F — result / error-kind 抬升

- [x] 标准 WIT `result` / error-kind 映射到 Host 错误面（对照 [`errors-async.md`](../mapping/errors-async.md)；`HostErrorMapping` + wasi stub → `ComponentVal.err`）
- [x] Async 方法仍 sync-compat 包装；文档标明与上游 async/p3 的偏差
- [x] 缺口矩阵 F 列收口（experimental 仍 trap；wasi result stub 已抬升）

### G — 长尾覆盖率关门

- [x] query-set / render-bundle / features·limits / adapter-info 等长尾：实现或显式 `Unsupported`
- [x] 缺口矩阵无「无」悬空行（每行 ✅ / ⚠️ / ❌）
- [x] 文档收口：本页 DoD 勾选 → [`archive-compliant-world-dod.md`](archive-compliant-world-dod.md)；根 README / scheme / CHANGELOG；**仍**不得宣传合规产品

## 本阶段不做

| ID | 项 |
|----|-----|
| — | wasi-gfx / canvas / 多 window 抽象 |
| — | Maven Central / 发包 |
| — | `abi-mvp` 扁平 render import |
| — | 可选 perf（[`docs/perf/`](../perf/)）不阻塞 |
| — | 对 tegmentum/wasmtime4j 提 issue/PR（本仓 overlay 自洽） |
| — | 真 CM async / WASI Preview3 异步运行时（本阶段 sync-compat） |

## 落地顺序

1. **A** 上游钉定 + 缺口矩阵补全（文档可先于代码）  
2. **B** 双轨 Linker / ABI 身份  
3. **C** Compute 去特化 + vector-add Guest 迁移  
4. **D** Texture / Sampler / PipelineLayout  
5. **E** 通用 Render + triangle Guest 迁移（仍无 gfx）  
6. **F** result / error-kind  
7. **G** 长尾关门 + DoD 归档  

## 链接

- 根 README：[`README.md`](../../README.md)  
- 方案索引：[`docs/scheme/README.md`](README.md)  
- 上阶段归档：[`archive-semantic-hardening-dod.md`](archive-semantic-hardening-dod.md)  
- 缺口矩阵：[`compliant-world-gap.md`](../mapping/compliant-world-gap.md)  
- 双轨：[`compliant-world-dual-track.md`](../mapping/compliant-world-dual-track.md) · [`abi-wasi`](../../abi-wasi/)  
- Compute / Render 子集：[`compute-subset.md`](../mapping/compute-subset.md) · [`render-subset.md`](../mapping/render-subset.md)  
- 错误与 Async：[`errors-async.md`](../mapping/errors-async.md)  
- WIT：[`wit/README.md`](../../wit/README.md) · [`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)  
- 上游 brief：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
