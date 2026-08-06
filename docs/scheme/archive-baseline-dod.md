# 基线 DoD 归档（已完成）

**中文** | [English](archive-baseline-dod.en.md)

> 自根 README 迁出的**已完成**验收清单归档。  
> 现行阶段见根 [`README.md`](../../README.md) 与 [`demo-cm-stability.md`](demo-cm-stability.md)。

归档对应提交约至：Surface/render 抬升进 L2 + `experimental:webgpu-cm@0.3.0`（Kotlin demo 经 Host）。

## P0

- [x] compute 子集映射表与偏差列表（`docs/mapping`）
- [x] `WasiWebGpuHost` + Dawn 适配 + 句柄 drop 单测
- [x] 回读结果与 CPU 期望一致（仪器测试绿灯）
- [x] 无 Runtime / CM 依赖即可合并（P0 切片）

## P1

- [x] `abi-mvp` + 桌面 Wasmtime → 同一 L2
- [x] Guest 向量加结果与纯 Kotlin→L2 一致（`:runtime-wasmtime:test`）
- [x] 边界开销备注（`docs/perf/p1-boundary.md`）
- [x] Android 嵌 Wasmtime → 同一 L2 → Dawn（`WasmtimeVectorAddInstrumentedTest`）

## CM（experimental 切片）

- [x] `wit/compute-cm` + `abi-cm` + CM Guest → 同一 L2（桌面 `:runtime-wasmtime:test`）
- [x] WIT resources 替换 flat u32（adapter/device/queue/buffer/…；仍非合规 wasi:webgpu）
- [x] Android CM 仪器测试（`WasmtimeCmVectorAddInstrumentedTest`；需 CM-patched Bionic `.so`）

## 交付巩固

- [x] 桌面 CM native → `runtime-wasmtime/desktop-natives/`（不改写 Gradle cache）
- [x] 无 patched natives 时 CM 单测 skip；abi-mvp 始终跑
- [x] GitHub Actions：`:host-api:test` / `:abi-mvp:test` / `:runtime-wasmtime:test` + `:android-demo:assembleDebug`
- [x] `CHANGELOG.md` + [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)（上游贡献备忘；未强制开 PR）

## 语义扩展（buffer records/flags）

- [x] `experimental:webgpu-cm@0.2.0`：`buffer-descriptor` + usage/map flags；`create-buffer` / `map-async`
- [x] 文档与方案中移除备用 Runtime 路线表述
- [x] `experimental:webgpu-cm@0.3.0`：surface + render 最小面

## 上屏 demo（Kotlin）

- [x] `android-demo`：`SurfaceView` + `TriangleRenderer`（经 L2 Host→Dawn；不经 Guest/wasi-gfx）
- [x] Surface/render 抬升进 `WasiWebGpuHost` / WIT（`experimental:webgpu-cm@0.3.0`；仍无 Guest/wasi-gfx 上屏）
