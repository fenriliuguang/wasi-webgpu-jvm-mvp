# Android 嵌 Wasmtime：进度与踩坑

**中文** | [English](android-wasmtime.en.md)

> experimental · Guest → Wasmtime (L1) → `WasiWebGpuHost` (L2) → `DawnWasiWebGpuHost`  
> 验收：`WasmtimeVectorAddInstrumentedTest`（abi-mvp）+ `WasmtimeCmVectorAddInstrumentedTest`（CM）+ `WasmtimeCmTriangleInstrumentedTest`（Guest 上屏）

## 进度

| 项 | 状态 |
|----|------|
| 桌面 P1：Guest → Wasmtime → CpuHost（`:runtime-wasmtime:test`） | 完成 |
| 桌面 CM：Guest.component → ComponentLinker → CpuHost | 完成 |
| Android：交叉编译 Bionic `libwasmtime4j.so`（arm64-v8a / x86_64） | 完成 |
| Android：jniLibs 打包 + 排除 Maven 桌面 `wasmtime4j-native` | 完成 |
| Android：guest wasm 进 assets，仪器测试向量加（abi-mvp） | 完成（绿灯） |
| Android CM：CM-patched `.so` + `vector_add_cm.wasm` 仪器测试 | 完成（绿灯） |
| 桌面 CM：`desktop-natives/`（不改 Gradle cache）+ CM 测试门控 | 完成 |
| 上屏（L2 Kotlin） | demo：`TriangleRenderer` → L2 Host → Dawn ✅ |
| 上屏（Guest CM） | `triangle-cm` → abi-cm → 同一 L2 → Dawn ✅（`WasmtimeCmTriangleInstrumentedTest`） |
| wasi-gfx | ❌ 本阶段不做 |

路径（abi-mvp）：

```text
guest/vector_add.wasm
  → System.loadLibrary("wasmtime4j")   # APK jniLibs（Bionic）
  → Wasmtime4j JNI + abi-mvp
  → WasiWebGpuHost
  → DawnWasiWebGpuHost
```

路径（CM）：

```text
guest/vector_add_cm.wasm
  → System.loadLibrary("wasmtime4j")   # 需 android + cm-resources 双补丁
  → Wasmtime4j ComponentLinker + abi-cm
  → WasiWebGpuHost
  → DawnWasiWebGpuHost
```

对照：

- P0：`VectorAddInstrumentedTest`（Kotlin → Dawn）
- P1 Android：`WasmtimeVectorAddInstrumentedTest`（Guest → Dawn）
- P1 桌面：`WasmtimeVectorAddTest`（Guest → CpuHost）
- CM Android：`WasmtimeCmVectorAddInstrumentedTest`（CM Guest → Dawn）
- CM 桌面：`WasmtimeCmVectorAddTest`（CM Guest → CpuHost）
- CM triangle Android：`WasmtimeCmTriangleInstrumentedTest`（Guest 上屏 → Dawn）

## 踩坑分级说明

主题：在 Android 上把 **Wasmtime（含 CM）接到同一 L2 Host → Dawn**。  
按与该主题的距离分级（排查时优先看「核心」）：

| 级别 | 含义 |
|------|------|
| **核心** | 直接卡死 Guest→Wasmtime→L2→Dawn（ABI / JNI / Bionic / 句柄 / CM resources） |
| **周边** | 交叉编译 / 打包 / 补丁应用等，不修也能“看起来像路径通”，但无法稳定交付 |
| **外围** | 仪器测试编排、OEM 安装策略、IDE/UTP；与 wasi-webgpu 语义无关，只影响“怎么跑绿验收” |

## 踩坑清单

### 1. Maven `wasmtime4j-native` 不能直接上 Android · **核心**

官方 jar 只有 linux/darwin/windows（glibc 等），**没有** Bionic `.so`。  
Android 必须自建 `aarch64-linux-android` / `x86_64-linux-android`，经 `jniLibs` + `System.loadLibrary` 加载；并 **exclude** `ai.tegmentum:wasmtime4j-native`，否则 APK 可能带上桌面 `natives/**`。

重建：`scripts/build-wasmtime4j-android.ps1` → `runtime-wasmtime/android-natives/jniLibs/`。  
默认同时 `git apply` android 补丁 + CM resources 补丁（仅 abi-mvp 可用 `-SkipCmResourcesPatch`）。

### 2. 交叉编译环境 · **周边**

| 问题 | 处理 |
|------|------|
| wasmtime 47.x MSRV ≥ 1.94 | 使用 Rust **1.97+**（脚本默认 `RUSTUP_TOOLCHAIN=1.97.1`） |
| 仅 `--features jni-bindings` 编不过 | 需 Maven 同款 default features（脚本不关 default） |
| 链接 `-lpthread`（Bionic 无独立 libpthread） | `android-natives/link-stubs/libpthread.so`：`INPUT(-lc)` |

### 3. `JNI_OnLoad` 返回 `JNI_VERSION_1_8` → ART 拒绝 · **核心**

现象：`Bad JNI version returned from JNI_OnLoad ...: 65544`（即 `0x10008` = 1_8）。  
ART 只接受 `JNI_VERSION_1_2` / `1_4` / `1_6`。

处理：入库补丁 [`patches/wasmtime4j-v47.0.2-1.5.0-android.patch`](../patches/wasmtime4j-v47.0.2-1.5.0-android.patch)（`async_runtime.rs` 返回 `JNI_VERSION_1_6`）；由 `scripts/build-wasmtime4j-android.ps1` `git apply`。

### 4. Java `Validation.requireValidHandle` 拒绝「负」句柄 · **核心**

现象：`nativeHandle is an invalid native handle (negative value)`。  
ARM64 堆指针 / TBI / PAC 高位 bit-cast 成 signed `long` 可为负；句柄仍有效。

处理：`android-demo` 提供放宽版 `ai.tegmentum.wasmtime4j.util.Validation`（只拒 `0`），并用 Gradle 过滤掉上游 jar 里的同名 class。

### 5. native `memory.rs` signed 比较误杀 MTE 标签指针 · **核心**

现象：`Invalid memory handle (0xb4……) … appears corrupted`。  
`0xb4…` 为 MTE/TBI 顶字节；旧代码 `memory_ptr < 0x1000`（**signed** jlong）对高位置位指针恒为 true。

处理：构建脚本改写为 `(memory_ptr as u64) < 0x1000`（`table_ptr` 同理）。改完后需 **重编并替换** `libwasmtime4j.so`。

### 6. Android CM 需要桌面同款 CM resources 补丁 · **核心**

WIT resources（`own`/`borrow` ↔ `U32(rep)`）编组与进程级 resource registry 在 native 侧。  
仅 android 补丁的 `.so` 不够跑 CM Guest；`build-wasmtime4j-android.ps1` 默认再 apply [`cm-resources.patch`](../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch)。

**嵌套 resource：** 旧补丁只把**顶层** `Val::Resource` 转成 `U32(rep)`；record/list 内的 borrow 若走 `val_to_component_value` 会变成 opaque `Own`/`Borrow` 句柄，Java 侧 `asU32()` 期望的是 Host table rep → trap。补丁现已 **递归** 转换；须重跑 `build-wasmtime4j-android.ps1` 替换 `jniLibs` 后，Guest 才能安全调用 `create-bind-group(descriptor)` / `queue.submit(list)` 等。未重编前 `vector-add-cm` 对嵌套路径暂用 `create-bind-group3` / `create-compute-pipeline-bgl` / `submit1`。

### 7. Studio / Gradle UTP：`Process crashed` / `No UID for androidx.test.services` · **外围**

与 wasi-webgpu / Wasmtime / Dawn **距离较远**：验收编排问题，不是 Guest→Host 语义或 native ABI 问题。  
`adb shell am instrument` 可绿，而 `:connectedDebugAndroidTest` 第一条 FAILED + `Process crashed`。

常见原因：

1. 设备缺 `androidx.test.services`（API 30+ / 部分 OEM）
2. UTP 默认 `uninstall_after_test` 与 vivo 包管理 / 并发安装竞态 → 测试进行中包被 `REPLACED`，进程被杀

处理：

- `gradle.properties`：`android.injected.androidTest.leaveApksInstalledAfterRun=true`
- `androidTestUtil(libs.androidx.test.services)` 让 AGP 安装 test-services
- **唯一推荐**：`./scripts/run-android-instrumented.ps1`（直接 `am instrument`；默认两波 + 波间 `force-stop`，避免同进程 CM linker 背靠背）
- 不要同时开两个 Run / Gradle 任务往同一台机装 APK
- CM triangle 仪器：勿 `ActivityScenario` / `startActivitySync`（vivo 易挂起）；保持亮屏，避免 Surface 长期未就绪

## 补丁落点（备忘）

| 补丁 | 位置 |
|------|------|
| JNI 1_6 / memory 无符号校验 | `patches/wasmtime4j-v47.0.2-1.5.0-android.patch` → `build-wasmtime4j-android.ps1` |
| CM WIT resources | `patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch` → 同脚本（默认） |
| 预编译 `.so` + pthread stub | `runtime-wasmtime/android-natives/` |
| Validation 放宽 | `android-demo/src/main/java/ai/tegmentum/wasmtime4j/util/Validation.java` + `filterWasmtime4jJar` |
| 排除桌面 native jar | `android-demo/build.gradle.kts` `configurations.exclude(wasmtime4j-native)` |
| Strip before package | 脚本 `llvm-strip --strip-unneeded` |
| UTP 外围（留 APK / test-services） | `gradle.properties` + `androidTestUtil` + `scripts/run-android-instrumented.ps1` |

## 验收

```bash
./gradlew :runtime-wasmtime:test
./gradlew :android-demo:assembleDebug
```

仪器测试（设备 + WebGPU/Vulkan）：

```powershell
# 推荐（绕过 UTP）
./scripts/run-android-instrumented.ps1

# 或 Studio / Gradle
./gradlew :android-demo:connectedDebugAndroidTest
```

- `WasmtimeVectorAddInstrumentedTest`（abi-mvp）
- `WasmtimeCmVectorAddInstrumentedTest`（CM；需 CM-patched `.so`）

仍标明 **experimental / 非合规 wasi:webgpu**。
