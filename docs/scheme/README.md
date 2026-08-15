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
| **Guest 标准 descriptor 真机 + 旋转纹理立方体** | **已完成（A–D）** — 计划 [`guest-descriptor-cube.md`](guest-descriptor-cube.md)；归档 [`archive-guest-descriptor-cube-dod.md`](archive-guest-descriptor-cube-dod.md)。D：**仍非真 WIT dtor** |
| **工程移交：Maven 可发布化（不对外发布）/ abi-mvp render / 可选 perf** | **已完成（A–C）** — 计划 [`engineering-handoff.md`](engineering-handoff.md)；归档 [`archive-engineering-handoff-dod.md`](archive-engineering-handoff-dod.md)。仍为 **experimental**；本地 Publishing **≠** 对外发布。**真机验收基准 = CM cube**（`guest/cube-cm`） |
| **真 CM async（层 A）/ 可选 P3 Spike** | **A 闸门关门（2026-08-10；B–E 停止）** — 归档 [`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md)。本仓 **锁死 sync-compat**。真 async / 自研 Android Wasmtime L1 → 姊妹仓 [`wasmtime-android-kt`](../../../wasmtime-android-kt)（M0–M5 已归档；`wasi:webgpu` W1 已交付 → **W2+**）；契约 [`dual-runtime-track.md`](dual-runtime-track.md) |
| **轨 A 主线：L2 / cube 基线养护 + 跟 B 扩 Host** | **已完成（A–B，2026-08-15）** — 计划 [`track-a-baseline-host.md`](track-a-baseline-host.md)；归档 [`archive-track-a-baseline-host-dod.md`](archive-track-a-baseline-host-dod.md)。切片 **A→B**：养住 CI/仪器/生命周期 → 正式 Host 面收敛。**不是**真 async / 合规产品阶段。无新产品阶段；继续 experimental + sync-compat |

已完成计划页保留正文作史实；现行口径以根 README + 本表 + 各 `archive-*-dod` 为准。vector-add / triangle Guest 示例已移除。

## 硬原则（摘录）

1. L2 不依赖 L1。  
2. 不重造完整 Kotlin WebGPU 客户端 API。  
3. 包名 / README 标明 `experimental`；未接标准完整 world 前不得声称合规 `wasi:webgpu`。  
4. P1 使用手工 abi-mvp（core wasm），**不是** Component Model。  
5. Android 使用 Bionic `libwasmtime4j.so`（CM 路径需 android + cm-resources 补丁）；桌面 CM 经 `scripts/build-wasmtime4j-desktop-cm.ps1` → `runtime-wasmtime/desktop-natives/`。

Android 嵌 Wasmtime：[`docs/android-wasmtime.md`](../android-wasmtime.md) / [EN](../android-wasmtime.en.md)。
