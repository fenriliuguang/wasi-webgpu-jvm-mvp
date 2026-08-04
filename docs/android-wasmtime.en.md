# Android-embedded Wasmtime: progress & pitfalls

[中文](android-wasmtime.md) | **English**

> experimental · Guest → Wasmtime (L1) + abi-mvp → `WasiWebGpuHost` (L2) → `DawnWasiWebGpuHost`  
> Acceptance: `WasmtimeVectorAddInstrumentedTest` (instrumented tests green)

## Progress

| Item | Status |
|------|--------|
| Desktop P1: Guest → Wasmtime → CpuHost (`:runtime-wasmtime:test`) | Done |
| Android: cross-compile Bionic `libwasmtime4j.so` (arm64-v8a / x86_64) | Done |
| Android: jniLibs packaging + exclude Maven desktop `wasmtime4j-native` | Done |
| Android: guest wasm in assets; instrumented vector-add | Done (green) |
| Component Model / Chicory / on-screen | Not done |

Path:

```text
guest/vector_add.wasm
  → System.loadLibrary("wasmtime4j")   # APK jniLibs (Bionic)
  → Wasmtime4j JNI + abi-mvp
  → WasiWebGpuHost
  → DawnWasiWebGpuHost
```

Comparisons:

- P0: `VectorAddInstrumentedTest` (Kotlin → Dawn)
- P1 Android: `WasmtimeVectorAddInstrumentedTest` (Guest → Dawn)
- P1 desktop: `WasmtimeVectorAddTest` (Guest → CpuHost)

## Pitfalls (in order encountered)

### 1. Maven `wasmtime4j-native` cannot ship on Android

Upstream jars only cover linux/darwin/windows (glibc, etc.) — **no** Bionic `.so`.  
Android must build `aarch64-linux-android` / `x86_64-linux-android`, load via `jniLibs` + `System.loadLibrary`, and **exclude** `ai.tegmentum:wasmtime4j-native`, or the APK may pick up desktop `natives/**`.

Rebuild: `scripts/build-wasmtime4j-android.ps1` → `runtime-wasmtime/android-natives/jniLibs/`.

### 2. Cross-compile environment

| Issue | Mitigation |
|-------|------------|
| wasmtime 47.x MSRV ≥ 1.94 | Use Rust **1.97+** (script default `RUSTUP_TOOLCHAIN=1.97.1`) |
| `--features jni-bindings` alone fails to build | Need Maven-equivalent default features (script keeps defaults) |
| Link `-lpthread` (Bionic has no separate libpthread) | `android-natives/link-stubs/libpthread.so`: `INPUT(-lc)` |

### 3. `JNI_OnLoad` returns `JNI_VERSION_1_8` → ART rejects

Symptom: `Bad JNI version returned from JNI_OnLoad ...: 65544` (`0x10008` = 1_8).  
ART only accepts `JNI_VERSION_1_2` / `1_4` / `1_6`.

Fix: tracked patch [`patches/wasmtime4j-v47.0.2-1.5.0-android.patch`](../patches/wasmtime4j-v47.0.2-1.5.0-android.patch) (`async_runtime.rs` returns `JNI_VERSION_1_6`); applied by `scripts/build-wasmtime4j-android.ps1` via `git apply`.

### 4. Java `Validation.requireValidHandle` rejects “negative” handles

Symptom: `nativeHandle is an invalid native handle (negative value)`.  
ARM64 heap pointers / TBI / PAC high bits bit-cast to signed `long` can be negative; the handle is still valid.

Fix: `android-demo` ships a relaxed `ai.tegmentum.wasmtime4j.util.Validation` (rejects only `0`) and Gradle filters the same class out of the upstream jar.

### 5. Native `memory.rs` signed compare kills MTE-tagged pointers

Symptom: `Invalid memory handle (0xb4……) … appears corrupted`.  
`0xb4…` is an MTE/TBI top byte; old code `memory_ptr < 0x1000` (**signed** jlong) is always true for high-bit-set pointers.

Fix: build script rewrites to `(memory_ptr as u64) < 0x1000` (`table_ptr` likewise). After the change, **rebuild and replace** `libwasmtime4j.so`.

## Patch landing spots (cheat sheet)

| Patch | Location |
|-------|----------|
| JNI 1_6 / unsigned memory checks | `patches/wasmtime4j-v47.0.2-1.5.0-android.patch` → `build-wasmtime4j-android.ps1` `git apply` then build `.so` |
| Prebuilt `.so` + pthread stub | `runtime-wasmtime/android-natives/` |
| Relaxed Validation | `android-demo/src/main/java/ai/tegmentum/wasmtime4j/util/Validation.java` + `filterWasmtime4jJar` |
| Exclude desktop native jar | `android-demo/build.gradle.kts` `configurations.exclude(wasmtime4j-native)` |
| Strip before package | script `llvm-strip --strip-unneeded` |

## Acceptance

```bash
./gradlew :runtime-wasmtime:test
./gradlew :android-demo:assembleDebug
```

Instrumented tests (device + WebGPU/Vulkan): Android Studio right-click  
`WasmtimeVectorAddInstrumentedTest` → **Run**.

Still marked **experimental / non-compliant wasi:webgpu / non–Component Model**.
