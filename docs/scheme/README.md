# 方案说明（迁入）

**中文** | [English](README.en.md)

本文件是从讨论稿迁入本仓的方案摘要。完整论述见仓库根 [`README.md`](../../README.md)。

## 一句话

先造 **灯的线路（Dawn Host 胶水）**，再按需插 **插座（Wasm Runtime）**。

## 阶段

| 阶段 | 本仓状态 |
|------|----------|
| **P0 · 胶水** | **完成**：`host-api` / `host-webgpu` + `docs/mapping` + Android 仪器测试 |
| **P1 · Runtime** | **完成**：桌面 Wasmtime + Android 嵌 Wasmtime → 同一 `abi-mvp` / L2；仍是 experimental / 非 CM |
| **CM 切片** | **完成（experimental）**：`experimental:webgpu-cm` WIT resources + Guest + `abi-cm` + 桌面 ComponentLinker → 同一 L2；Android CM → Dawn 仪器测试；仍非合规 wasi:webgpu |
| **交付巩固** | **完成**：桌面 CM → `desktop-natives`（不改 Gradle cache）；CM 测试门控；GitHub Actions（JVM + assemble）；`CHANGELOG` / `patches/UPSTREAM` |
| **语义扩展** | **完成（本阶段 DoD）**：CM buffer records/flags（`@0.2.0`）+ Surface/render 抬升进 L2 / `experimental:webgpu-cm@0.3.0`（Kotlin demo 经 Host）；后续可选：更多 records / Guest·wasi-gfx 上屏 |

## 硬原则（摘录）

1. L2 不依赖 L1。  
2. 不重造完整 Kotlin WebGPU 客户端 API。  
3. 包名 / README 标明 `experimental`；未接标准完整 world 前不得声称合规 `wasi:webgpu`。  
4. P1 使用手工 abi-mvp（core wasm），**不是** Component Model。  
5. Android 使用 Bionic `libwasmtime4j.so`（`runtime-wasmtime/android-natives`；CM 路径需同时应用 android + cm-resources 补丁）；桌面 CM resources 经 `scripts/build-wasmtime4j-desktop-cm.ps1` 产出到 `runtime-wasmtime/desktop-natives/`（不改写 Maven/Gradle cache；见 `patches/*.patch`）。

Android 嵌 Wasmtime 的进度、补丁与踩坑见 [`docs/android-wasmtime.md`](../android-wasmtime.md) / [EN](../android-wasmtime.en.md)。
