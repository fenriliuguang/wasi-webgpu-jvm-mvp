# Guest CM 上屏 DoD 归档（已完成）

**中文** | [English](archive-guest-onscreen-cm-dod.en.md)

> 自根 README 迁出的**已完成**验收清单归档。  
> 现行阶段见根 [`README.md`](../../README.md) 与 [`demo-cm-stability.md`](demo-cm-stability.md)。

归档对应提交约至：`07ec669` 仪器绿灯修复（u64 JSON + vivo Scenario）+ `47c342d` DoD 勾选与文档收口（2026-08-06）。原计划页：[`guest-onscreen-cm.md`](guest-onscreen-cm.md)。

## DoD

- [x] `guest/triangle-cm`（或等价 world export）+ 预编译 `.wasm`；复用 `create-render-pipeline-triangle` 等特化 API
- [x] Host 注入 native window；Guest 不创建 window
- [x] Android 仪器测试：CM Guest → Dawn 上屏红三角（需 CM-patched Bionic `.so`）
- [x] 文档：`docs/mapping/render-subset` 补 Guest 路径；根 README / 计划页 DoD 勾选
- [x] 桌面：无 Android Surface 时相关单测 skip（与现 CM 门控一致）

## 关键交付物

- Guest：[`guest/triangle-cm`](../../guest/triangle-cm)（world `triangle`，export `run-triangle`）+ 预编译 `triangle_cm.wasm`
- L1 / Android 入口：`WasmtimeCmTriangle` + `WasmtimeCmTriangleAndroid`（Host 注入 native window；Guest 只持 `surface`）
- 仪器绿灯：`WasmtimeCmTriangleInstrumentedTest`（2026-08-06，vivo V2458A / Mali；单次 draw 验收）
- 修复：P2 `ConcurrentCallCodec` u64 无符号解析（android-demo 覆盖）；P5 vivo `ActivityScenario` Intent 不匹配 → `ActivityLifecycleMonitorRegistry`
- 文档：[`docs/mapping/render-subset.md`](../mapping/render-subset.md) Guest 路径（双语）；踩坑记录 [`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)
- 遗留：P6 手动 Demo 按钮稳定性 → 由 [`demo-cm-stability.md`](demo-cm-stability.md) 承接
