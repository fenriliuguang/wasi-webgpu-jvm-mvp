# Demo CM 稳性 + 帧循环 DoD 归档（已完成）

**中文** | [English](archive-demo-cm-stability-dod.en.md)

> 自根 README / 计划页迁出的**已完成**验收清单归档。  
> 原计划页：[`demo-cm-stability.md`](demo-cm-stability.md)。承接 P6：[`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)。

归档对应提交约至：`b5e6212`（Host/L2 resume）→ `654896a`（Session）→ `110944d`（仪器重复）→ `841b55c`（三段式帧循环）+ 本文档收口（2026-08-07）。

> **后续回归（不改写本 DoD）**：真机手点曾再现 `WINDOW_IN_USE`；`96d594f` 曾短暂改为每次 CM 完整 Host+Session teardown。随后 D1–D6 收口为 **复用 Host+Session** + `releaseAllGpuObjects`（见 [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)）。仪器「复用 Session」验收始终有效。

## DoD

- [x] 手动 Demo 重复触发 CM 三角（pause → 帧循环 → resume；按钮整段 disable）无必现 `VK_ERROR_NATIVE_WINDOW_IN_USE` / `invalid handle`（连点靠 disable 门控）
- [x] 同进程多次 CM：复用 `WasmtimeCmTriangle.Session`（linker/instance），不反复 recreate
- [x] CM `drop-triangle`（Guest unconfigure）后 L2 `resumeSurfaceAndAwait` 可恢复上屏
- [x] 帧循环：`init-triangle` / `draw-frame` / `drop-triangle`（`@0.3.0` additive）；`docs/mapping/threading` 已补 CM 约定
- [x] 仪器：新增 `cmGuestRepeatTriangleReusesSession`；既有 one-shot / vector-add 路径保留（真机复测：`scripts/run-android-instrumented.ps1`）
- [x] 文档：blockers P6 标解决；CHANGELOG；本归档

## 关键交付物

- Demo（归档时）：`TriangleRenderer.resumeSurfaceAndAwait`；`TriangleCmOneShot` + `runFrameLoopAndAwait`（Host/Session 复用；现行见 blockers）
- L1：`WasmtimeCmTriangle.Session`（`runTriangle` / `initTriangle` / `drawFrame` / `dropTriangle` / `runFrameLoop`）
- WIT / Guest：world `triangle` 追加三段 export；预编译 `triangle_cm.wasm`
- 仪器：`WasmtimeCmTriangleInstrumentedTest.cmGuestRepeatTriangleReusesSession`
- 线程：[`docs/mapping/threading.md`](../mapping/threading.md) CM Guest 帧循环约定
