# Guest CM 上屏（triangle-cm）— 已定计划

**中文** | [English](guest-onscreen-cm.en.md)

> **状态：已完成（2026-08-06 仪器绿灯，DoD 全勾选）。** 根 README DoD 与此对齐；遗留项见 blockers P6（非本切片）。

## 一句话

用已有 L2 surface/render + `experimental:webgpu-cm@0.3.0` imports，新增 Guest 经 Wasmtime CM 画红三角。

```text
Guest triangle-cm.wasm
  → Wasmtime ComponentLinker + abi-cm
  → 同一 WasiWebGpuHost / Dawn
  → Android SurfaceView
```

## 已定决策

| 问题 | 决定 |
|------|------|
| 主切片 | A：Guest CM 上屏（非 B records / 非 C wasi-gfx） |
| 验收形态 | **先单次 / 低频 draw**（仪器测试易绿）；帧循环可后续加 |
| 与 Kotlin demo | **并存**：保留 `TriangleRenderer`（L2）；另加 CM Guest 路径 / 仪器测试 |
| Window | Kotlin 注入 native window；Guest **只持** `surface` resource |
| WIT 版本 | 尽量仍挂 `@0.3.0`（仅加 world export / Guest 资产时可不 bump） |

## DoD

- [x] `guest/triangle-cm`（或等价 world export）+ 预编译 `.wasm`；复用 `create-render-pipeline-triangle` 等特化 API
- [x] Host 注入 native window；Guest 不创建 window
- [x] Android 仪器测试：CM Guest → Dawn 上屏红三角（需 CM-patched Bionic `.so`）
- [x] 文档：`docs/mapping/render-subset` 补 Guest 路径；根 README / 本页 DoD 勾选
- [x] 桌面：无 Android Surface 时相关单测 skip（与现 CM 门控一致）

## 本切片不做

- wasi-gfx canvas 抽象
- 通用 render-pipeline descriptor / MSAA / depth
- `abi-mvp` 扁平 render import
- 合规 `wasi:webgpu` 全量 world、Maven Central

## 落地顺序

1. ~~Guest WIT world export（`run-triangle`）+ wit-bindgen 重建~~ — 已完成：`guest/triangle-cm` + `triangle_cm.wasm`
2. ~~Demo / 测试：CM instantiate → 单次 draw~~ — 已接线：`WasmtimeCmTriangle` + Demo 按钮 + 仪器测试骨架（桌面无 Surface 时 skip / CpuHost Unsupported）
3. ~~仪器测试绿灯 + 双语文档索引~~ — 绿灯（2026-08-06，vivo V2458A / Mali）；`render-subset` 双语已补 Guest 路径
4. ~~CHANGELOG~~ — 已补（Unreleased：Guest CM on-screen）

## 风险

帧循环与 CM 调用线程亲和；Surface 生命周期与 Guest 资源 drop；仪器测试稳定性。

踩坑与仪器阻塞记录：[`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)（含 P6：手动 Demo CM 按钮不稳 — **非本切片 DoD**）。

## 链接

- 根 README：[`README.md`](../../README.md)  
- 基线归档：[`archive-baseline-dod.md`](archive-baseline-dod.md)  
- Render 映射：[`docs/mapping/render-subset.md`](../mapping/render-subset.md)  
- 线程：[`docs/mapping/threading.md`](../mapping/threading.md)  
- WIT：[`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)
