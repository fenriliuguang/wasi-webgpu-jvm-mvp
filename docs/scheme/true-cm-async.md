# 真 CM async（层 A）/ 可选 P3 Spike（true-cm-async）— 进行中

**中文** | [English](true-cm-async.en.md)

> **状态：已立项 / 计划冻结（2026-08-10）。** 切片 **A–E** 正文与 DoD 已钉定；**代码尚未开工**（先文档、后实现）。  
> 承接备忘 [`true-cm-async-memo.md`](true-cm-async-memo.md) 与工程移交归档（[`archive-engineering-handoff-dod.md`](archive-engineering-handoff-dod.md)）。  
> 组合：Runtime Spike（A）→ L2 非阻塞分轨（B）→ Linker 主链 future（C）→ Guest smoke + 线程契约（D）→ 文档/矩阵收口 + 可选 P3 Spike（E）。  
> 仍为 **experimental**；真异步落地 **≠** 合规产品宣称，也 **≠** 对外发布。

## 一句话

把 wasi:webgpu 主链 `async func`（`request-adapter` / `request-device` / `map-async`）从「L2 内 `CountDownLatch` 阻塞等待」换成 **Component Model async（future）**，使 CM host 回调路径 **不**阻塞 wasm 线程；**WASI Preview3** 仅作不阻塞 Spike。主验收 **全程保持** experimental CM cube + sync-compat。

```text
A Runtime Spike（async host import e2e；失败即闸门）
  → B L2 非阻塞分轨（CM 路径去 awaitRequest 阻塞；sync 保留）
  → C Linker 主链 future（request-adapter / request-device / map-async）
  → D Guest smoke + 线程契约（不迁主验收）
  → E 文档 / 缺口矩阵收口 + 可选 P3 Spike（不阻塞关门）
```

对照：[`errors-async.md`](../mapping/errors-async.md) · [`threading.md`](../mapping/threading.md) · [`compliant-world-gap.md`](../mapping/compliant-world-gap.md) · 备忘 [`true-cm-async-memo.md`](true-cm-async-memo.md)。

## 已定决策

| 问题 | 决定 |
|------|------|
| 本阶段范围 | 锁定切片 **A–E**；**层 A（CM async）** 为正式 DoD；**层 B（WASI Preview3 / `enableWasiP3`）** 仅 Spike/旁路，**不**阻塞关门；下表「本阶段不做」写死 |
| 主线顺序 | **A → B → C** 强序（C 依赖 B 的非阻塞面）；**D** 接 C；**E** 收口；可选 P3 随时可做，不阻塞 A–D |
| 主验收轨 | **全程保持** experimental CM cube + sync-compat（`guest/cube-cm` / `WasmtimeCmCubeInstrumentedTest`）；**不**把主 Demo/仪器迁到 async wasi Guest |
| Guest 策略 | 新增 **最小 async smoke Guest**（优先 wasi 轨单个 `async` import，如 `request-adapter`）；experimental cube **不改**真 async |
| L2 | **分轨**：Cpu/直调/测试保留现有 sync API；CM async 路径用非阻塞完成面（future/callback），**L2 仍不依赖 L1** |
| 主链方法（首批） | `request-adapter` → `request-device` → `map-async`；pipeline-async / 长尾 async **不**扫完 |
| Spike 失败闸门 | 切片 A 若证明「async host import 端到端不可用」，**停止**改 L2/Linker 主链，改为文档化 runtime 边界并按闸门关门 |
| Async / 宣称 | 推进真 CM async；包名 / README 保持 `experimental`；**不**宣称合规 `wasi:webgpu` 产品；**不**对外发布 |
| 上游 | overlay / 补丁自洽；**不对** tegmentum/wasmtime4j 提 issue/PR；**不做**真 WIT dtor overlay |
| 验收形态 | A：桌面 smoke 或「不可行」书面闸门；B/C：`:host-*` / `:runtime-wasmtime:test`（有 desktop-natives）；D：async Guest 桌面 smoke + CM cube 不回归；E：缺口矩阵/CHANGELOG；每子切片 CHANGELOG |

## 子切片与 DoD

### A — Runtime Spike（硬序第 1；失败即闸门）

- [ ] 评估桌面 / Android CM 补丁构建是否可开 `component-model-async`（及与现有 android / cm-resources 补丁共存）；**不**把完整 `wasi-p3` 当关门条件
- [ ] 最小 e2e：一个 async host import 能 **完成 / 拒绝** future（可先假实现，不接 Dawn）
- [ ] 验证 process-global resource registry / 嵌套 borrow 补丁在 async 注册路径下不回归
- [ ] 文档：Spike 结论写入本页或 [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) 短节；缺口 → overlay 边界（仍不提上游 PR）
- [ ] DoD：桌面可复现 smoke **或** 明确「不可行 + 停止后续切片」记录；CHANGELOG

### B — L2 非阻塞分轨

- [ ] `DawnWasiWebGpuHost`：CM 路径不再在 host callback 内 `awaitRequest` 阻塞；保留 sync 包装给 Cpu / Kotlin 直调 / 既有单测
- [ ] `WasiWebGpuHost`（或平行 async 面）暴露可完成的 adapter/device/map 请求；后台仍可 `processEvents`，契约写入 [`threading.md`](../mapping/threading.md)
- [ ] Cpu Host：提供可测的立即完成 / 可控延迟实现，供 Linker 单测不依赖 Dawn
- [ ] DoD：L2 单测覆盖「非阻塞发起 + 完成」；既有 sync 单测不红；CHANGELOG

### C — Linker 主链 future

- [ ] `WasmtimeCmLinker`：将 wasi（及必要的 experimental）主链 `request-adapter` / `request-device` / `map-async` 改为 CM async/future 语义；其余 async 仍 stub / sync-compat
- [ ] 错误面：future 完成值的 `result` / error-context 与 `WasiResultCodec` / `HostErrorMapping` 对齐
- [ ] DoD：桌面 CM 单测证明三条主链 future 完成/拒绝；**不**要求仪器改走 async；CHANGELOG

### D — Guest smoke + 线程契约（不迁主验收）

- [ ] 最小 Guest（如 `guest/async-smoke-cm`）：调用至少一条真 async import 并观察完成
- [ ] 重申 Dawn `processEvents`、CM scheduler、Surface/present **同线程**契约（[`threading.md`](../mapping/threading.md)）；cube 帧循环路径保持现状
- [ ] Demo / 仪器：**仍** `run-android-instrumented.ps1` + CM cube；async 仅桌面 smoke（Android 仪器 async **可选**，不进主门禁）
- [ ] DoD：async smoke 绿灯 + cube 仪器/回归口径不变；CHANGELOG

### E — 文档 / 矩阵收口 + 可选 P3 Spike

- [ ] 缺口矩阵 async 相关行：主链三条 → 真 async 状态；长尾保持 stub/❌
- [ ] 更新 dual-track 等受影响处加注；根 README / scheme「状态」；CHANGELOG
- [ ] （可选旁路）`enableWasiP3` 探索笔记；失败或未做 **不**挡 A–D 关门
- [ ] 全勾后 → [`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md)(+EN)

## 本阶段不做

| ID | 项 |
|----|-----|
| — | 将主 Demo / 真机验收迁到 async wasi Guest 或取消 cube sync-compat |
| — | 以完整 WASI Preview3 / `enableWasiHttpP3` 为关门条件 |
| — | 扫完所有 WIT `async`（`on-submitted-work-done`、`get-compilation-info`、`pop-error-scope`、pipeline-async 全量等） |
| — | wasi-gfx / canvas / 多 window 抽象 |
| — | 宣传已合规 `wasi:webgpu` 产品；任何对外发布 |
| — | 对 tegmentum/wasmtime4j 提 issue/PR；真 WIT dtor / `JniComponentLinker` rep-only overlay |
| — | 把 sync L2 全面删除（Cpu/测试/直调仍可用 sync-compat） |

## 落地顺序

1. 文档立项：本页(+EN) + memo/README/scheme/errors-async 索引  
2. **A** Spike（闸门）  
3. **B** L2 分轨 → **C** Linker future  
4. **D** Guest smoke + 线程文档  
5. **E** 矩阵/CHANGELOG；可选 P3 笔记  
6. 全勾后 → archive DoD；根 README / scheme / CHANGELOG 收口  

## 链接

- 根 README：[`README.md`](../../README.md)  
- 方案索引：[`docs/scheme/README.md`](README.md)  
- 备忘（已立项）：[`true-cm-async-memo.md`](true-cm-async-memo.md)  
- 上阶段归档：[`archive-engineering-handoff-dod.md`](archive-engineering-handoff-dod.md)  
- 错误 / Async：[`errors-async.md`](../mapping/errors-async.md)  
- 线程：[`threading.md`](../mapping/threading.md)  
- 缺口 / 双轨：[`compliant-world-gap.md`](../mapping/compliant-world-gap.md) · [`compliant-world-dual-track.md`](../mapping/compliant-world-dual-track.md)  
- Android natives：[`docs/android-wasmtime.md`](../android-wasmtime.md) · [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
