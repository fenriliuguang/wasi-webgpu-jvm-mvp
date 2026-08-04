# wasi-webgpu-jvm-mvp

**experimental Dawn / CPU host mapping for wasi:webgpu**

> 未接标准 `wasi:webgpu` 完整 world / 全量资源面之前，**不得**宣传为已合规实现。  
> P1：**abi-mvp**（core wasm imports）。  
> CM 切片：**experimental:webgpu-cm**（Component Model + typed lists/strings + WIT resources；仍为 experimental）。  
> 对照实现：[`wasi-webgpu-wasmtime`](https://crates.io/crates/wasi-webgpu-wasmtime)。

## 目标

先造 **灯的线路（Host 胶水）**，再插 **插座（Wasmtime）**。

```text
P0: Kotlin / Demo → WasiWebGpuHost (L2) → Dawn (Android) 或 CpuHost (桌面)
P1: Guest.wasm → Wasmtime (L1) + abi-mvp → 同一 L2
    桌面：CpuWasiWebGpuHost
    Android：DawnWasiWebGpuHost + Bionic libwasmtime4j.so
CM: Guest.component → Wasmtime ComponentLinker + abi-cm → 同一 L2（桌面 CpuHost）
```

| 层 | 模块 | 说明 |
|----|------|------|
| L2 Host API | `host-api` | 纯 JVM：`WasiWebGpuHost`、句柄、`CpuWasiWebGpuHost`、`VectorAddScenario` |
| L3 Dawn | `host-webgpu` | Android：`DawnWasiWebGpuHost` |
| L1 + ABI | `runtime-wasmtime` + `abi-mvp` | Wasmtime ↔ 扁平 import（桌面 + Android） |
| L1 + CM | `runtime-wasmtime` (`runtime.cm`) + `abi-cm` | ComponentLinker ↔ typed WIT imports |
| Guest | `guest/vector-add` | abi-mvp `.wat` + 预编译 `.wasm` |
| Guest CM | `guest/vector-add-cm` | wit-bindgen component + 预编译 `.wasm` |
| Consumer | `android-demo` | Dawn / Guest 仪器测试 / 薄 UI |

**明确不做（本阶段）：** Chicory、上屏 / wasi-gfx、合规 wasi:webgpu 全量 world / 记录类型对齐。

## 仓库布局

```text
wasi-webgpu-jvm-mvp/
  host-api/            # L2 + CPU Host
  host-webgpu/         # Dawn L3（Android library）
  abi-mvp/             # 扁平 import 绑定
  abi-cm/              # experimental CM host → L2
  runtime-wasmtime/    # Wasmtime4j L1（abi-mvp + CM）
    android-natives/   # Android Bionic libwasmtime4j.so（jniLibs）
  guest/vector-add/    # abi-mvp Guest 资产
  guest/vector-add-cm/ # CM Guest 资产
  android-demo/        # Dawn + Guest consumer
  docs/mapping/        # WIT ↔ Dawn
  docs/perf/           # P1 边界备注
  docs/android-wasmtime.md
  wit/                 # WIT 钉定（含 compute-cm）
```

## 构建与测试

要求：完整 **JDK**（非 JRE）、Android SDK（Dawn 路径）。  
若 Gradle 误选 JRE，在 `local.properties` 设置 `org.gradle.java.home=...`。

```bash
# L2 / CPU Host / abi-mvp
./gradlew :host-api:test :abi-mvp:test

# P1 + CM：Guest → Wasmtime → L2（桌面 CpuHost；含 abi-mvp 与 CM 测试）
./gradlew :runtime-wasmtime:test

# Android 组装（含 jniLibs + guest assets）
./gradlew :android-demo:assembleDebug
```

仪器测试（需设备/模拟器 + WebGPU/Vulkan）：在 Android Studio 中右键  

- P0 Kotlin→Dawn：`VectorAddInstrumentedTest.kt` → **Run**  
- Guest→Wasmtime→Dawn：`WasmtimeVectorAddInstrumentedTest.kt` → **Run**  

（Gradle 面板未必列出 verification task，属 IDE 默认行为。CM 桌面路径优先；Android CM 仪器测试未强制本切片。）

### Android Wasmtime `.so`

`android-demo` 从 `runtime-wasmtime/android-natives/jniLibs` 打包 Bionic `libwasmtime4j.so`，并 **排除** Maven 桌面 `wasmtime4j-native`。  
重建（需 NDK + Rust + `cargo-ndk`）：

```powershell
./scripts/build-wasmtime4j-android.ps1
```

详见 [`runtime-wasmtime/android-natives/README.md`](runtime-wasmtime/android-natives/README.md)。

Android 额外补丁（摘要；细节与踩坑见 [`docs/android-wasmtime.md`](docs/android-wasmtime.md)）：

- `JNI_OnLoad` 返回 `JNI_VERSION_1_6`（ART 不接受 1_8）
- 本地 `Validation.requireValidHandle` 允许高位置位的句柄（ARM64 TBI/PAC 指针 bit-cast 成 signed `long` 可能为负）
- native `memory.rs` 句柄校验改为无符号比较（避免 MTE 标签指针被误判为 corrupted）

Guest 重建：

```bash
# abi-mvp
wasm-tools parse guest/vector-add/vector_add.wat -o guest/vector-add/vector_add.wasm

# CM（需 Rust wasm32-unknown-unknown + wasm-tools）
./scripts/build-vector-add-cm.ps1

# 桌面 CM resources：打补丁并安装 wasmtime4j-native（一次性 / 升级后重跑）
./scripts/build-wasmtime4j-desktop-cm.ps1
```

## 包名

`io.github.fenriliuguang.wasi.webgpu.experimental.*`

## DoD

### P0

- [x] compute 子集映射表与偏差列表（`docs/mapping`）
- [x] `WasiWebGpuHost` + Dawn 适配 + 句柄 drop 单测
- [x] 回读结果与 CPU 期望一致（仪器测试绿灯）
- [x] 无 Runtime / Chicory / CM 依赖即可合并（P0 切片）

### P1

- [x] `abi-mvp` + 桌面 Wasmtime → 同一 L2
- [x] Guest 向量加结果与纯 Kotlin→L2 一致（`:runtime-wasmtime:test`）
- [x] 边界开销备注（`docs/perf/p1-boundary.md`）
- [x] Android 嵌 Wasmtime → 同一 L2 → Dawn（`WasmtimeVectorAddInstrumentedTest`）

### CM（experimental 切片）

- [x] `wit/compute-cm` + `abi-cm` + CM Guest → 同一 L2（桌面 `:runtime-wasmtime:test`）
- [x] WIT resources 替换 flat u32（adapter/device/queue/buffer/…；仍非合规 wasi:webgpu）
- [ ] Android CM 仪器测试（可选后续）

## 参考

- [wasi-webgpu](https://github.com/WebAssembly/wasi-webgpu)
- [androidx.webgpu](https://developer.android.com/jetpack/androidx/releases/webgpu)
- [wasmtime4j](https://github.com/tegmentum/wasmtime4j)
- 方案摘要：[`docs/scheme/README.md`](docs/scheme/README.md)
- Android Wasmtime 进度与踩坑：[`docs/android-wasmtime.md`](docs/android-wasmtime.md)
