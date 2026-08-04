# Android 嵌 Wasmtime：进度与踩坑

> experimental · Guest → Wasmtime (L1) + abi-mvp → `WasiWebGpuHost` (L2) → `DawnWasiWebGpuHost`  
> 验收：`WasmtimeVectorAddInstrumentedTest`（仪器测试绿灯）

## 进度

| 项 | 状态 |
|----|------|
| 桌面 P1：Guest → Wasmtime → CpuHost（`:runtime-wasmtime:test`） | 完成 |
| Android：交叉编译 Bionic `libwasmtime4j.so`（arm64-v8a / x86_64） | 完成 |
| Android：jniLibs 打包 + 排除 Maven 桌面 `wasmtime4j-native` | 完成 |
| Android：guest wasm 进 assets，仪器测试向量加 | 完成（绿灯） |
| Component Model / Chicory / 上屏 | 未做 |

路径：

```text
guest/vector_add.wasm
  → System.loadLibrary("wasmtime4j")   # APK jniLibs（Bionic）
  → Wasmtime4j JNI + abi-mvp
  → WasiWebGpuHost
  → DawnWasiWebGpuHost
```

对照：

- P0：`VectorAddInstrumentedTest`（Kotlin → Dawn）
- P1 Android：`WasmtimeVectorAddInstrumentedTest`（Guest → Dawn）
- P1 桌面：`WasmtimeVectorAddTest`（Guest → CpuHost）

## 踩坑清单（按出现顺序）

### 1. Maven `wasmtime4j-native` 不能直接上 Android

官方 jar 只有 linux/darwin/windows（glibc 等），**没有** Bionic `.so`。  
Android 必须自建 `aarch64-linux-android` / `x86_64-linux-android`，经 `jniLibs` + `System.loadLibrary` 加载；并 **exclude** `ai.tegmentum:wasmtime4j-native`，否则 APK 可能带上桌面 `natives/**`。

重建：`scripts/build-wasmtime4j-android.ps1` → `runtime-wasmtime/android-natives/jniLibs/`。

### 2. 交叉编译环境

| 问题 | 处理 |
|------|------|
| wasmtime 47.x MSRV ≥ 1.94 | 使用 Rust **1.97+**（脚本默认 `RUSTUP_TOOLCHAIN=1.97.1`） |
| 仅 `--features jni-bindings` 编不过 | 需 Maven 同款 default features（脚本不关 default） |
| 链接 `-lpthread`（Bionic 无独立 libpthread） | `android-natives/link-stubs/libpthread.so`：`INPUT(-lc)` |

### 3. `JNI_OnLoad` 返回 `JNI_VERSION_1_8` → ART 拒绝

现象：`Bad JNI version returned from JNI_OnLoad ...: 65544`（即 `0x10008` = 1_8）。  
ART 只接受 `JNI_VERSION_1_2` / `1_4` / `1_6`。

处理：入库补丁 [`patches/wasmtime4j-v47.0.2-1.5.0-android.patch`](../patches/wasmtime4j-v47.0.2-1.5.0-android.patch)（`async_runtime.rs` 返回 `JNI_VERSION_1_6`）；由 `scripts/build-wasmtime4j-android.ps1` `git apply`。

### 4. Java `Validation.requireValidHandle` 拒绝「负」句柄

现象：`nativeHandle is an invalid native handle (negative value)`。  
ARM64 堆指针 / TBI / PAC 高位 bit-cast 成 signed `long` 可为负；句柄仍有效。

处理：`android-demo` 提供放宽版 `ai.tegmentum.wasmtime4j.util.Validation`（只拒 `0`），并用 Gradle 过滤掉上游 jar 里的同名 class。

### 5. native `memory.rs` signed 比较误杀 MTE 标签指针

现象：`Invalid memory handle (0xb4……) … appears corrupted`。  
`0xb4…` 为 MTE/TBI 顶字节；旧代码 `memory_ptr < 0x1000`（**signed** jlong）对高位置位指针恒为 true。

处理：构建脚本改写为 `(memory_ptr as u64) < 0x1000`（`table_ptr` 同理）。改完后需 **重编并替换** `libwasmtime4j.so`。

## 补丁落点（备忘）

| 补丁 | 位置 |
|------|------|
| JNI 1_6 / memory 无符号校验 | `patches/wasmtime4j-v47.0.2-1.5.0-android.patch` → `build-wasmtime4j-android.ps1` `git apply` 后编 `.so` |
| 预编译 `.so` + pthread stub | `runtime-wasmtime/android-natives/` |
| Validation 放宽 | `android-demo/src/main/java/ai/tegmentum/wasmtime4j/util/Validation.java` + `filterWasmtime4jJar` |
| 排除桌面 native jar | `android-demo/build.gradle.kts` `configurations.exclude(wasmtime4j-native)` |
| 打包前 strip | 脚本 `llvm-strip --strip-unneeded` |

## 验收

```bash
./gradlew :runtime-wasmtime:test
./gradlew :android-demo:assembleDebug
```

仪器测试（设备 + WebGPU/Vulkan）：Android Studio 右键  
`WasmtimeVectorAddInstrumentedTest` → **Run**。

仍标明 **experimental / 非合规 wasi:webgpu / 非 Component Model**。
