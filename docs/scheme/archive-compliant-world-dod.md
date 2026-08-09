# 合规 wasi:webgpu World（无 gfx）DoD 归档（已完成）

**中文** | [English](archive-compliant-world-dod.en.md)

> 自根 README / 计划页迁出的**已完成**验收清单归档。  
> 原计划页：[`compliant-world.md`](compliant-world.md)。承接：[`archive-semantic-hardening-dod.md`](archive-semantic-hardening-dod.md)。

归档对应提交约至：`c39ce17`（文档锁定）→ `e972c32`（A）→ `ac6844e`（B）→ `5518daa`（C）→ `282c5b0`（D）→ `da18928`（E）→ `8d1f23f`（F）+ 本文档收口（切片 G，2026-08-09）。

**重要：** 矩阵关门 = 方法级「实现或显式 Unsupported」齐套；**不等于**可对外宣传「合规 wasi:webgpu 产品」。包名仍以 `experimental:webgpu-cm` 为主验收轨；标准包双轨 stub 并存。

## DoD

### A — 上游钉定 + 缺口矩阵

- [x] vendor `wasi:webgpu@0.3.0-rc.2`；方法级缺口矩阵骨架 → 全量行
- [x] wit-lock / PIN / inventory

### B — 双轨 Linker

- [x] `:abi-wasi` + `WasmtimeCmLinker` 注册标准资源；函数 stub
- [x] experimental Guest 不受影响

### C — Compute 去特化

- [x] `experimental:webgpu-cm@0.5.0` 标准 descriptor；helpers deprecated 保留
- [x] 嵌套 borrow：补丁递归 + 真机仍用顶层 helpers（待 Android `.so`）

### D — Texture / Sampler / PipelineLayout

- [x] `@0.6.0`；L2/Dawn/Cpu；compute-pipeline.layout → pipeline-layout

### E — 通用 Render

- [x] `@0.7.0` 通用 `create-render-pipeline` / `begin-render-pass`；helpers 委托
- [x] 仪器两波绿（含 abi-mvp BGL→PL 包装修复）

### F — result / error-kind

- [x] `HostErrorMapping` + wasi result stub → `ComponentVal.err`
- [x] experimental 仍 trap；async sync-compat 文档化

### G — 长尾关门

- [x] query-set / render-bundle / features·limits / adapter-info / label / debug 等长尾：显式 ❌ Unsupported（wasi stub）
- [x] 缺口矩阵无悬空「无」行（每行 ✅ / ⚠️ / ❌）
- [x] 本文档归档；根 README / scheme / CHANGELOG 同步；**仍不宣传合规产品**

## 关键交付物

- 标准包 vendor + 双轨 Linker + experimental `@0.7.0` 主路径
- 缺口矩阵：[`docs/mapping/compliant-world-gap.md`](../mapping/compliant-world-gap.md)
- 错误面：[`docs/mapping/errors-async.md`](../mapping/errors-async.md)
- 未做（阶段排除）：wasi-gfx、Maven、`abi-mvp` render、perf、上游 PR、真 CM async
