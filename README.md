# wasi-webgpu-jvm-mvp

**experimental Dawn / CPU host mapping for wasi:webgpu**

**中文** | [English](README.en.md)

> 未接标准 `wasi:webgpu` 完整 world / 全量资源面之前，**不得**宣传为已合规实现。  
> 对照实现：[`wasi-webgpu-wasmtime`](https://crates.io/crates/wasi-webgpu-wasmtime)。

## 特性

- **L2 Host**：`WasiWebGpuHost`（compute + 最小 Android surface/render）+ 桌面 `CpuWasiWebGpuHost`
- **L3 Dawn**：`DawnWasiWebGpuHost`（Android / androidx.webgpu）
- **Runtime**：Wasmtime4j — **abi-mvp**（core wasm）与 **abi-cm**（Component Model / `experimental:webgpu-cm@0.3.0`）
- **Guest**：向量加（abi-mvp + CM）；Kotlin `SurfaceView` 红三角经 L2 Host→Dawn
- **工程**：多模块 Gradle、CI（JVM 单测 + `assembleDebug`）、Bionic / 桌面 CM-patched natives 脚本

包名：`io.github.fenriliuguang.wasi.webgpu.experimental.*`

## 架构

先造 **灯的线路（Host 胶水）**，再插 **插座（Wasmtime）**。

```text
Kotlin / Demo ──► WasiWebGpuHost (L2) ──► Dawn (Android) 或 CpuHost (桌面)
Guest.wasm ──► Wasmtime + abi-mvp / abi-cm ──► 同一 L2
```

| 层 | 模块 |
|----|------|
| L2 | `host-api` |
| L3 | `host-webgpu` |
| L1 + ABI | `runtime-wasmtime` + `abi-mvp` / `abi-cm` |
| Guest | `guest/vector-add`、`guest/vector-add-cm`、`guest/triangle-cm`（CM 上屏接线中） |
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

仪器测试（设备 + WebGPU/Vulkan）：Studio 右键跑 `*InstrumentedTest.kt`，或：

```powershell
./scripts/run-android-instrumented.ps1
```

Native / Guest 重建与踩坑：[`docs/android-wasmtime.md`](docs/android-wasmtime.md)、[`runtime-wasmtime/android-natives/README.md`](runtime-wasmtime/android-natives/README.md)、`scripts/build-*.ps1`。

## 当前阶段 DoD — Guest CM 上屏

计划全文：[`docs/scheme/guest-onscreen-cm.md`](docs/scheme/guest-onscreen-cm.md)

- [ ] `guest/triangle-cm`（或等价）+ 预编译 `.wasm`；经 abi-cm → 同一 L2 → Dawn 画红三角
- [ ] Host 注入 native window；Guest 只持 `surface`
- [ ] Android 仪器测试绿灯（需 CM-patched Bionic `.so`）
- [ ] 文档补 Guest 上屏路径

**本阶段不做：** wasi-gfx、合规全量 `wasi:webgpu`、Maven Central、`abi-mvp` render 扁平 import。

已完成基线（P0–P1、CM compute、L2 surface、Kotlin 红三角）见归档：[`docs/scheme/archive-baseline-dod.md`](docs/scheme/archive-baseline-dod.md)。

## 参考

- [wasi-webgpu](https://github.com/WebAssembly/wasi-webgpu) · [androidx.webgpu](https://developer.android.com/jetpack/androidx/releases/webgpu) · [wasmtime4j](https://github.com/tegmentum/wasmtime4j)
- 变更：[`CHANGELOG.md`](CHANGELOG.md) · 方案：[`docs/scheme/README.md`](docs/scheme/README.md)

## 文档索引

| 文档 | 中文 | English |
|------|------|---------|
| 根 README | [README.md](README.md) | [README.en.md](README.en.md) |
| 现行计划（Guest CM 上屏） | [docs/scheme/guest-onscreen-cm.md](docs/scheme/guest-onscreen-cm.md) | [docs/scheme/guest-onscreen-cm.en.md](docs/scheme/guest-onscreen-cm.en.md) |
| 基线 DoD 归档 | [docs/scheme/archive-baseline-dod.md](docs/scheme/archive-baseline-dod.md) | [docs/scheme/archive-baseline-dod.en.md](docs/scheme/archive-baseline-dod.en.md) |
| 方案摘要 | [docs/scheme/README.md](docs/scheme/README.md) | [docs/scheme/README.en.md](docs/scheme/README.en.md) |
| Android Wasmtime | [docs/android-wasmtime.md](docs/android-wasmtime.md) | [docs/android-wasmtime.en.md](docs/android-wasmtime.en.md) |
| Compute / Render 映射 | [compute-subset](docs/mapping/compute-subset.md) · [render-subset](docs/mapping/render-subset.md) | [EN](docs/mapping/compute-subset.en.md) · [EN](docs/mapping/render-subset.en.md) |
| 线程 / 错误与 Async | [threading](docs/mapping/threading.md) · [errors-async](docs/mapping/errors-async.md) | [EN](docs/mapping/threading.en.md) · [EN](docs/mapping/errors-async.en.md) |
| WIT / 补丁 / natives / Guest | [wit/](wit/README.md) · [patches/](patches/README.md) · [android-natives](runtime-wasmtime/android-natives/README.md) · [guest](guest/vector-add/README.md) | 见各目录 EN |
| Changelog | [CHANGELOG.md](CHANGELOG.md) | 同上 |
