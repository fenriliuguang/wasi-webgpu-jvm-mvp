# 方案说明（迁入）

**中文** | [English](README.en.md)

本文件是从讨论稿迁入本仓的方案摘要。根 README 侧重**特性与现行状态**；已完成 DoD 见各归档页。

## 一句话

先造 **灯的线路（Dawn Host 胶水）**，再按需插 **插座（Wasm Runtime）**。

## 阶段

| 阶段 | 本仓状态 |
|------|----------|
| **基线（P0–语义扩展 / L2 上屏）** | **完成** — 详见 [`archive-baseline-dod.md`](archive-baseline-dod.md) |
| **Guest CM 上屏（triangle-cm）** | **完成**（2026-08-06）— 详见 [`archive-guest-onscreen-cm-dod.md`](archive-guest-onscreen-cm-dod.md) |
| **Demo CM 稳性 + 帧循环** | **完成**（2026-08-07）— 详见 [`archive-demo-cm-stability-dod.md`](archive-demo-cm-stability-dod.md) |
| **Demo CM 真机稳性回归** | **完成**（2026-08-08，V2458A，D1–D6）— 见 [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) |
| **语义加固与工程清债** | **完成**（2026-08-09）— 详见 [`archive-semantic-hardening-dod.md`](archive-semantic-hardening-dod.md)；计划页 [`semantic-hardening.md`](semantic-hardening.md) |
| **合规 wasi:webgpu World（无 gfx）** | **已完成（A–G）** — 计划 [`compliant-world.md`](compliant-world.md)；归档 [`archive-compliant-world-dod.md`](archive-compliant-world-dod.md)；缺口 [`compliant-world-gap.md`](../mapping/compliant-world-gap.md)。**不做** wasi-gfx；**不宣传**合规产品 |
| **Guest 标准 descriptor 真机 + 旋转纹理立方体** | **进行中** — [`guest-descriptor-cube.md`](guest-descriptor-cube.md)；**A ✅ B ✅**；C wasi 子集接线 → D 生命周期。Maven / `abi-mvp` render / perf 仍移交 |

## 硬原则（摘录）

1. L2 不依赖 L1。  
2. 不重造完整 Kotlin WebGPU 客户端 API。  
3. 包名 / README 标明 `experimental`；未接标准完整 world 前不得声称合规 `wasi:webgpu`。  
4. P1 使用手工 abi-mvp（core wasm），**不是** Component Model。  
5. Android 使用 Bionic `libwasmtime4j.so`（CM 路径需 android + cm-resources 补丁）；桌面 CM 经 `scripts/build-wasmtime4j-desktop-cm.ps1` → `runtime-wasmtime/desktop-natives/`。

Android 嵌 Wasmtime：[`docs/android-wasmtime.md`](../android-wasmtime.md) / [EN](../android-wasmtime.en.md)。
