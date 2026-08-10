# 真 CM async DoD 归档（切片 A 闸门关门）

**中文** | [English](archive-true-cm-async-dod.en.md)

> 自根 README / 计划页迁出的**阶段收口**归档。  
> 原计划页：[`true-cm-async.md`](true-cm-async.md)。承接：[`archive-engineering-handoff-dod.md`](archive-engineering-handoff-dod.md)。

归档对应：切片 **A（Runtime Spike）按失败闸门关门**（2026-08-10）；**B–E 按闸门停止**，未改 L2 / Linker 主链 / Guest。

**重要：** 仍为 **experimental**；默认路径仍为 **sync-compat**；主验收仍为 CM cube。真异步 **≠** 合规产品宣称 / 对外发布。

## 闸门结论（一句话）

桌面 / Android CM 补丁 natives 的 Cargo 侧 **已**含 `component-model-async`（经 `component-model` feature），且可开 Engine `concurrencySupport` + `wasmComponentModelAsync` + Linker `defineFunctionAsync`；但 wasmtime4j **47.0.2-1.5.0** Java 面 **无** CM future 创建 / 写 / 完成 / 拒绝 API（仅有不透明 `FutureAny` + sync 形 `ComponentHostFunction`）。按计划「async host import 端到端不可用 → 停止后续切片」关门。

## DoD

### A — Runtime Spike（硬序第 1；失败即闸门）

- [x] 评估：桌面 / Android CM 补丁构建经 `component-model` **已启用** `component-model-async`；与 android / cm-resources 补丁共存；完整 `wasi-p3` **非**关门条件（未编入当前 natives）
- [x] 最小 e2e：**不可行**（Java 无 future writer）；见 [`CmAsyncApiSurfaceTest`](../../runtime-wasmtime/src/test/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/CmAsyncApiSurfaceTest.kt)
- [x] 注册路径：`defineResource` + `defineFunctionAsync` 在 async-capable Engine 上可注册（[`CmAsyncHostImportSpikeTest`](../../runtime-wasmtime/src/test/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/CmAsyncHostImportSpikeTest.kt)）；嵌套 borrow：cm-resources 补丁的 **async 回调分支仍用旧** `val_to_component_value`（未走 `vals_to_host_params`）— 记为残余风险，闸门下不修
- [x] 文档：本归档 + 计划页 Spike 节 + [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) §5
- [x] DoD：书面「不可行 + 停止 B–E」+ 桌面探测单测 + CHANGELOG

### B — L2 非阻塞分轨

- [ ] **停止（闸门）** — 不改 `DawnWasiWebGpuHost` / sync API

### C — Linker 主链 future

- [ ] **停止（闸门）** — 不改 `WasmtimeCmLinker` 主链为 future

### D — Guest smoke + 线程契约

- [ ] **停止（闸门）** — 不建 `guest/async-smoke-cm`；cube 主验收不变

### E — 文档 / 矩阵收口 + 可选 P3 Spike

- [x] 缺口矩阵：**不**把主链抬成真 async（仍 ⚠️ sync-compat）；本归档 + README / scheme / errors-async 收口
- [x] （可选旁路）`enableWasiP3`：当前 desktop/Android CM natives **未**编 `wasi-p3`；不重编、不挡关门
- [x] 本归档页

## 关键交付物

- 探测单测：`CmAsyncApiSurfaceTest`（无 natives）+ `CmAsyncHostImportSpikeTest`（需 desktop-natives）
- 上游备忘：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) §5
- 计划史实：[`true-cm-async.md`](true-cm-async.md)
- 未做（闸门排除）：L2 分轨、Linker future、async Guest、主验收迁轨、合规宣传、对外发布、上游 PR、真 WIT dtor overlay、完整 wasi-p3

## 若上游日后自带 future writer

可重开本阶段（新计划页），再动 B→C→D；**在此之前**默认仍 sync-compat。本仓默认仍 **不**对 tegmentum/wasmtime4j 提 issue/PR。
