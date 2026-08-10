# wasi-webgpu-jvm-mvp

**experimental Dawn / CPU host mapping for wasi:webgpu**

**中文** | [English](README.en.md)

> 未接标准 `wasi:webgpu` 完整 world / 全量资源面之前，**不得**宣传为已合规实现。  
> 对照实现：[`wasi-webgpu-wasmtime`](https://crates.io/crates/wasi-webgpu-wasmtime)。

## 特性

- **L2 Host**：`WasiWebGpuHost`（compute + 最小 Android surface/render）+ 桌面 `CpuWasiWebGpuHost`
- **L3 Dawn**：`DawnWasiWebGpuHost`（Android / androidx.webgpu）
- **Runtime**：Wasmtime4j — **abi-mvp**（core wasm）与 **abi-cm**（Component Model / `experimental:webgpu-cm@0.8.0`）
- **Guest**：CM 旋转纹理立方体（`guest/cube-cm`）经 abi-cm→L2→Dawn；真机 / Demo 验收以此为基准
- **工程**：多模块 Gradle、CI（JVM 单测 + `assembleDebug`）、Bionic / 桌面 CM-patched natives 脚本

包名：`io.github.fenriliuguang.wasi.webgpu.experimental.*`

## 架构

先造 **灯的线路（Host 胶水）**，再插 **插座（Wasmtime）**。

```text
Kotlin / Demo ──► WasiWebGpuHost (L2) ──► Dawn (Android) 或 CpuHost (桌面)
Guest.wasm ──► Wasmtime + abi-cm ──► 同一 L2
```

| 层 | 模块 |
|----|------|
| L2 | `host-api` |
| L3 | `host-webgpu` |
| L1 + ABI | `runtime-wasmtime` + `abi-mvp` / `abi-cm` |
| Guest | `guest/cube-cm`（真机验收基准） |
| Demo | `android-demo` |

## 仓库布局

```text
wasi-webgpu-jvm-mvp/
  host-api/  host-webgpu/  abi-mvp/  abi-cm/  runtime-wasmtime/
  guest/  android-demo/  wit/  patches/  docs/  scripts/  .github/workflows/
```

## 构建与测试

要求：完整 **JDK**（非 JRE）、Android SDK。若 Gradle 误选 JRE，在 `local.properties` 设 `org.gradle.java.home=...`。

```bash
./gradlew :host-api:test :abi-mvp:test
./gradlew :runtime-wasmtime:test          # 无 desktop-natives 时 CM 单测 skip
./gradlew :android-demo:assembleDebug
```

仪器测试（设备 + WebGPU/Vulkan）**唯一推荐**：

```powershell
./scripts/run-android-instrumented.ps1   # CM cube；勿依赖 Studio UTP
```

Native / Guest 重建与踩坑：[`docs/android-wasmtime.md`](docs/android-wasmtime.md)、[`runtime-wasmtime/android-natives/README.md`](runtime-wasmtime/android-natives/README.md)、`scripts/build-*.ps1`。

## 状态

- **已完成**：基线（P0–P1 / CM compute / L2 上屏）→ [归档](docs/scheme/archive-baseline-dod.md)；Guest CM 上屏（triangle-cm，2026-08-06）→ [归档](docs/scheme/archive-guest-onscreen-cm-dod.md)；Demo CM 稳性 + 帧循环（2026-08-07）→ [归档](docs/scheme/archive-demo-cm-stability-dod.md)；Demo CM **真机稳性回归**（D1–D6，2026-08-08，V2458A）→ [blockers](docs/scheme/demo-cm-stability-blockers.md)；**语义加固与工程清债**（A–E，2026-08-09）→ [归档](docs/scheme/archive-semantic-hardening-dod.md)；**合规 wasi:webgpu World（无 gfx，A–G，2026-08-09）**→ [归档](docs/scheme/archive-compliant-world-dod.md)；**Guest 标准 descriptor + 旋转纹理立方体（A–D，2026-08-10）**→ [归档](docs/scheme/archive-guest-descriptor-cube-dod.md)（D：**仍非真 WIT dtor**）
- **下一阶段：** 移交项 — Maven / `abi-mvp` render / 可选 perf；**不做** wasi-gfx / 合规宣传 / 真 CM async / 上游 PR / 真 dtor overlay。**真机验收基准 = CM cube**

## 参考

- [wasi-webgpu](https://github.com/WebAssembly/wasi-webgpu) · [androidx.webgpu](https://developer.android.com/jetpack/androidx/releases/webgpu) · [wasmtime4j](https://github.com/tegmentum/wasmtime4j)
- 变更：[`CHANGELOG.md`](CHANGELOG.md) · 方案：[`docs/scheme/README.md`](docs/scheme/README.md)

## 文档索引

| 文档 | 中文 | English |
|------|------|---------|
| 根 README | [README.md](README.md) | [README.en.md](README.en.md) |
| Demo CM 稳性 DoD 归档 | [docs/scheme/archive-demo-cm-stability-dod.md](docs/scheme/archive-demo-cm-stability-dod.md) | [docs/scheme/archive-demo-cm-stability-dod.en.md](docs/scheme/archive-demo-cm-stability-dod.en.md) |
| Demo CM 真机稳性回归 blockers | [docs/scheme/demo-cm-stability-blockers.md](docs/scheme/demo-cm-stability-blockers.md) | （仅中文） |
| 语义加固与工程清债 DoD 归档 | [docs/scheme/archive-semantic-hardening-dod.md](docs/scheme/archive-semantic-hardening-dod.md) | [docs/scheme/archive-semantic-hardening-dod.en.md](docs/scheme/archive-semantic-hardening-dod.en.md) |
| 语义加固计划（已完成） | [docs/scheme/semantic-hardening.md](docs/scheme/semantic-hardening.md) | [docs/scheme/semantic-hardening.en.md](docs/scheme/semantic-hardening.en.md) |
| 合规 World DoD 归档（已完成，无 gfx） | [docs/scheme/archive-compliant-world-dod.md](docs/scheme/archive-compliant-world-dod.md) | [docs/scheme/archive-compliant-world-dod.en.md](docs/scheme/archive-compliant-world-dod.en.md) |
| 合规 World 计划（已完成，无 gfx） | [docs/scheme/compliant-world.md](docs/scheme/compliant-world.md) | [docs/scheme/compliant-world.en.md](docs/scheme/compliant-world.en.md) |
| Guest 标准 descriptor + 立方体 DoD 归档（已完成） | [docs/scheme/archive-guest-descriptor-cube-dod.md](docs/scheme/archive-guest-descriptor-cube-dod.md) | [docs/scheme/archive-guest-descriptor-cube-dod.en.md](docs/scheme/archive-guest-descriptor-cube-dod.en.md) |
| Guest 标准 descriptor + 旋转纹理立方体（计划，已完成） | [docs/scheme/guest-descriptor-cube.md](docs/scheme/guest-descriptor-cube.md) | [docs/scheme/guest-descriptor-cube.en.md](docs/scheme/guest-descriptor-cube.en.md) |
| 合规 World 缺口矩阵 | [docs/mapping/compliant-world-gap.md](docs/mapping/compliant-world-gap.md) | [docs/mapping/compliant-world-gap.en.md](docs/mapping/compliant-world-gap.en.md) |
| 合规 World 双轨 | [docs/mapping/compliant-world-dual-track.md](docs/mapping/compliant-world-dual-track.md) | [docs/mapping/compliant-world-dual-track.en.md](docs/mapping/compliant-world-dual-track.en.md) |
| 基线 DoD 归档 | [docs/scheme/archive-baseline-dod.md](docs/scheme/archive-baseline-dod.md) | [docs/scheme/archive-baseline-dod.en.md](docs/scheme/archive-baseline-dod.en.md) |
| Guest CM 上屏 DoD 归档 | [docs/scheme/archive-guest-onscreen-cm-dod.md](docs/scheme/archive-guest-onscreen-cm-dod.md) | [docs/scheme/archive-guest-onscreen-cm-dod.en.md](docs/scheme/archive-guest-onscreen-cm-dod.en.md) |
| 方案摘要 | [docs/scheme/README.md](docs/scheme/README.md) | [docs/scheme/README.en.md](docs/scheme/README.en.md) |
| Android Wasmtime | [docs/android-wasmtime.md](docs/android-wasmtime.md) | [docs/android-wasmtime.en.md](docs/android-wasmtime.en.md) |
| Compute / Render 映射 | [compute-subset](docs/mapping/compute-subset.md) · [render-subset](docs/mapping/render-subset.md) | [EN](docs/mapping/compute-subset.en.md) · [EN](docs/mapping/render-subset.en.md) |
| 合规缺口矩阵 | [compliant-world-gap](docs/mapping/compliant-world-gap.md) | [EN](docs/mapping/compliant-world-gap.en.md) |
| 线程 / 错误与 Async | [threading](docs/mapping/threading.md) · [errors-async](docs/mapping/errors-async.md) | [EN](docs/mapping/threading.en.md) · [EN](docs/mapping/errors-async.en.md) |
| WIT / 补丁 / natives / Guest | [wit/](wit/README.md) · [patches/](patches/README.md) · [android-natives](runtime-wasmtime/android-natives/README.md) · [guest/cube-cm](guest/cube-cm/README.md) | 见各目录 EN |
| Changelog | [CHANGELOG.md](CHANGELOG.md) | 同上 |
