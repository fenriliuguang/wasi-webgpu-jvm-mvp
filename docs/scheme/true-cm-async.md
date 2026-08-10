# 真 CM async（层 A）/ 可选 P3 Spike（true-cm-async）— 已归档（A 闸门）

**中文** | [English](true-cm-async.en.md)

> **状态：切片 A 闸门关门（2026-08-10）。** 归档见 [`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md)。  
> **本仓（轨 A）已锁死 sync-compat**；真 CM async / 自研 Android Wasmtime L1 → 姊妹仓 [`wasmtime-android-kt`](../../../wasmtime-android-kt)；契约 [`dual-runtime-track.md`](dual-runtime-track.md)。  
> 承接备忘 [`true-cm-async-memo.md`](true-cm-async-memo.md) 与工程移交归档（[`archive-engineering-handoff-dod.md`](archive-engineering-handoff-dod.md)）。  
> **B–E 按闸门停止**（未改 L2 / Linker 主链 / Guest）。主验收仍 CM cube。  
> 仍为 **experimental**；真异步落地 **≠** 合规产品宣称，也 **≠** 对外发布。

## 一句话

原目标：把 wasi:webgpu 主链 `async func` 从 L2 `CountDownLatch` 换成 CM future。  
**闸门结果：** Cargo/`component-model-async` 可用，但 wasmtime4j **47.0.2-1.5.0** Java **无** future 完成/拒绝面 → **停止** B–E。

```text
A Runtime Spike → 闸门：不可行（无 Java future writer）
  ✗ B L2 非阻塞分轨（停止）
  ✗ C Linker 主链 future（停止）
  ✗ D Guest smoke（停止）
  ✓ E 文档收口（本页 + 归档 + UPSTREAM §5）
```

对照：[`errors-async.md`](../mapping/errors-async.md) · [`threading.md`](../mapping/threading.md) · [`compliant-world-gap.md`](../mapping/compliant-world-gap.md) · 备忘 [`true-cm-async-memo.md`](true-cm-async-memo.md)。

## Spike A 结论（2026-08-10）

| 项 | 结果 |
|----|------|
| Cargo `component-model-async` | **已启用**（`component-model` feature 绑定）；桌面/Android CM 补丁构建共存；`wasi-p3` 未编入当前 natives |
| Engine 开关 | 需 `concurrencySupport(true)`（+ `asyncSupport` / `wasmComponentModelAsync`）才能建 CM-async Engine |
| `defineFunctionAsync` | 可注册（`func_new_async`）；回调仍为 **同步** `ComponentHostFunction` |
| Future complete/reject | **无** — `FutureAny` 仅 opaque handle + `close`；无 Writer / write / complete / reject |
| Resource / 嵌套 borrow | 注册路径：`defineResource` + `defineFunctionAsync` OK；补丁 async 回调分支仍 `val_to_component_value`（非 `vals_to_host_params`） |
| 闸门 | **触发** → 停止 B–E；详见 [`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md) · [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) §5 |

探测单测：`CmAsyncApiSurfaceTest`（无 natives）· `CmAsyncHostImportSpikeTest`（需 `desktop-natives`）。

## 已定决策（立项时）

| 问题 | 决定 |
|------|------|
| 本阶段范围 | 锁定切片 **A–E**；**层 A（CM async）** 为正式 DoD；**层 B（WASI Preview3 / `enableWasiP3`）** 仅 Spike/旁路 |
| Spike 失败闸门 | 切片 A 若证明「async host import 端到端不可用」，**停止**改 L2/Linker 主链 → **已触发** |
| 主验收轨 | **全程保持** experimental CM cube + sync-compat |
| 上游 | overlay / 补丁自洽；**不对** tegmentum/wasmtime4j 提 issue/PR |

## 子切片与 DoD

### A — Runtime Spike（硬序第 1；失败即闸门）

- [x] 评估桌面 / Android CM 补丁构建与 `component-model-async`；**不**以完整 `wasi-p3` 为关门条件
- [x] 最小 e2e complete/reject → **不可行**（API 缺口）
- [x] 注册路径探测 + 嵌套 borrow 残余风险文档化
- [x] Spike 结论 → 本页 / UPSTREAM §5
- [x] DoD：书面闸门 + 探测单测 + CHANGELOG

### B — L2 非阻塞分轨

- [ ] **停止（闸门）**

### C — Linker 主链 future

- [ ] **停止（闸门）**

### D — Guest smoke + 线程契约（不迁主验收）

- [ ] **停止（闸门）**

### E — 文档 / 矩阵收口 + 可选 P3 Spike

- [x] 缺口矩阵主链 **不**抬真 async（仍 sync-compat）；README / scheme / errors-async 收口
- [x] （可选）`enableWasiP3`：当前 natives 未编 `wasi-p3`；不挡关门
- [x] → [`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md)

## 本阶段不做

| ID | 项 |
|----|-----|
| — | 将主 Demo / 真机验收迁到 async wasi Guest 或取消 cube sync-compat |
| — | 以完整 WASI Preview3 / `enableWasiHttpP3` 为关门条件 |
| — | 扫完所有 WIT `async`；wasi-gfx；合规宣传；对外发布 |
| — | 对 tegmentum/wasmtime4j 提 issue/PR；真 WIT dtor overlay |
| — | 在无 future writer 前提下改 L2 / Linker 主链（闸门） |

## 链接

- 归档：[`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md)  
- 根 README：[`README.md`](../../README.md)  
- 方案索引：[`docs/scheme/README.md`](README.md)  
- 备忘：[`true-cm-async-memo.md`](true-cm-async-memo.md)  
- Upstream §5：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
- 错误 / Async：[`errors-async.md`](../mapping/errors-async.md)  
