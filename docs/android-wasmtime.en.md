# Android-embedded Wasmtime: progress & pitfalls

[中文](android-wasmtime.md) | **English**

> experimental · Guest → Wasmtime (L1) → `WasiWebGpuHost` (L2) → `DawnWasiWebGpuHost`  
> Acceptance: `WasmtimeVectorAddInstrumentedTest` (abi-mvp) + `WasmtimeCmVectorAddInstrumentedTest` (CM)

## Progress

| Item | Status |
|------|--------|
| Desktop P1: Guest → Wasmtime → CpuHost (`:runtime-wasmtime:test`) | Done |
| Desktop CM: Guest.component → ComponentLinker → CpuHost | Done |
| Android: cross-compile Bionic `libwasmtime4j.so` (arm64-v8a / x86_64) | Done |
| Android: jniLibs packaging + exclude Maven desktop `wasmtime4j-native` | Done |
| Android: guest wasm in assets; instrumented vector-add (abi-mvp) | Done (green) |
| Android CM: CM-patched `.so` + `vector_add_cm.wasm` instrumented test | Done (green) |
| Desktop CM: `desktop-natives/` (no Gradle cache mutation) + CM test gate | Done |
| On-screen | Not done |

Path (abi-mvp):

```text
guest/vector_add.wasm
  → System.loadLibrary("wasmtime4j")   # APK jniLibs (Bionic)
  → Wasmtime4j JNI + abi-mvp
  → WasiWebGpuHost
  → DawnWasiWebGpuHost
```

Path (CM):

```text
guest/vector_add_cm.wasm
  → System.loadLibrary("wasmtime4j")   # needs android + cm-resources patches
  → Wasmtime4j ComponentLinker + abi-cm
  → WasiWebGpuHost
  → DawnWasiWebGpuHost
```

Comparisons:

- P0: `VectorAddInstrumentedTest` (Kotlin → Dawn)
- P1 Android: `WasmtimeVectorAddInstrumentedTest` (Guest → Dawn)
- P1 desktop: `WasmtimeVectorAddTest` (Guest → CpuHost)
- CM Android: `WasmtimeCmVectorAddInstrumentedTest` (CM Guest → Dawn)
- CM desktop: `WasmtimeCmVectorAddTest` (CM Guest → CpuHost)

## Pitfall relevance grades

Theme: wire **Wasmtime (incl. CM) → same L2 Host → Dawn on Android**.  
Grade by distance from that theme (triage **core** first):

| Grade | Meaning |
|-------|---------|
| **Core** | Directly blocks Guest→Wasmtime→L2→Dawn (ABI / JNI / Bionic / handles / CM resources) |
| **Adjacent** | Cross-compile / packaging / patch apply — path can look plausible without these, but delivery is unstable |
| **Peripheral** | Instrumented-test harness, OEM install policy, IDE/UTP; unrelated to wasi-webgpu semantics; only affects “how to get a green acceptance run” |

## Pitfalls

### 1. Maven `wasmtime4j-native` cannot ship on Android · **Core**

Upstream jars only cover linux/darwin/windows (glibc, etc.) — **no** Bionic `.so`.  
Android must build `aarch64-linux-android` / `x86_64-linux-android`, load via `jniLibs` + `System.loadLibrary`, and **exclude** `ai.tegmentum:wasmtime4j-native`, or the APK may pick up desktop `natives/**`.

Rebuild: `scripts/build-wasmtime4j-android.ps1` → `runtime-wasmtime/android-natives/jniLibs/`.  
By default applies both the android patch and the CM resources patch (use `-SkipCmResourcesPatch` for abi-mvp-only).

### 2. Cross-compile environment · **Adjacent**

| Issue | Mitigation |
|-------|------------|
| wasmtime 47.x MSRV ≥ 1.94 | Use Rust **1.97+** (script default `RUSTUP_TOOLCHAIN=1.97.1`) |
| `--features jni-bindings` alone fails to build | Need Maven-equivalent default features (script keeps defaults) |
| Link `-lpthread` (Bionic has no separate libpthread) | `android-natives/link-stubs/libpthread.so`: `INPUT(-lc)` |

### 3. `JNI_OnLoad` returns `JNI_VERSION_1_8` → ART rejects · **Core**

Symptom: `Bad JNI version returned from JNI_OnLoad ...: 65544` (`0x10008` = 1_8).  
ART only accepts `JNI_VERSION_1_2` / `1_4` / `1_6`.

Fix: tracked patch [`patches/wasmtime4j-v47.0.2-1.5.0-android.patch`](../patches/wasmtime4j-v47.0.2-1.5.0-android.patch) (`async_runtime.rs` returns `JNI_VERSION_1_6`); applied by `scripts/build-wasmtime4j-android.ps1` via `git apply`.

### 4. Java `Validation.requireValidHandle` rejects “negative” handles · **Core**

Symptom: `nativeHandle is an invalid native handle (negative value)`.  
ARM64 heap pointers / TBI / PAC high bits bit-cast to signed `long` can be negative; the handle is still valid.

Fix: `android-demo` ships a relaxed `ai.tegmentum.wasmtime4j.util.Validation` (rejects only `0`) and Gradle filters the same class out of the upstream jar.

### 5. Native `memory.rs` signed compare kills MTE-tagged pointers · **Core**

Symptom: `Invalid memory handle (0xb4……) … appears corrupted`.  
`0xb4…` is an MTE/TBI top byte; old code `memory_ptr < 0x1000` (**signed** jlong) is always true for high-bit-set pointers.

Fix: build script rewrites to `(memory_ptr as u64) < 0x1000` (`table_ptr` likewise). After the change, **rebuild and replace** `libwasmtime4j.so`.

### 6. Android CM needs the same CM resources patch as desktop · **Core**

WIT resource (`own`/`borrow` ↔ `U32(rep)`) marshalling and the process-wide resource registry live in native code.  
An android-only-patched `.so` cannot run the CM Guest; `build-wasmtime4j-android.ps1` also applies [`cm-resources.patch`](../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch) by default.

### 7. Studio / Gradle UTP: `Process crashed` / `No UID for androidx.test.services` · **Peripheral**

**Far** from wasi-webgpu / Wasmtime / Dawn: an acceptance-harness issue, not Guest→Host semantics or native ABI.  
`adb shell am instrument` can be green while `:connectedDebugAndroidTest` fails on the first test with `Process crashed`.

Common causes:

1. Missing `androidx.test.services` on the device (API 30+ / some OEMs)
2. UTP default `uninstall_after_test` racing with vivo package manager / concurrent installs → package `REPLACED` mid-test, process killed

Mitigations:

- `gradle.properties`: `android.injected.androidTest.leaveApksInstalledAfterRun=true`
- `androidTestUtil(libs.androidx.test.services)` so AGP installs test-services
- Reliable bypass: `./scripts/run-android-instrumented.ps1` (plain `am instrument`)
- Do not run two Studio/Gradle install sessions against the same device at once

## Patch landing spots (cheat sheet)

| Patch | Location |
|-------|----------|
| JNI 1_6 / unsigned memory checks | `patches/wasmtime4j-v47.0.2-1.5.0-android.patch` → `build-wasmtime4j-android.ps1` |
| CM WIT resources | `patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch` → same script (default) |
| Prebuilt `.so` + pthread stub | `runtime-wasmtime/android-natives/` |
| Relaxed Validation | `android-demo/src/main/java/ai/tegmentum/wasmtime4j/util/Validation.java` + `filterWasmtime4jJar` |
| Exclude desktop native jar | `android-demo/build.gradle.kts` `configurations.exclude(wasmtime4j-native)` |
| Strip before package | script `llvm-strip --strip-unneeded` |
| UTP peripheral (leave APKs / test-services) | `gradle.properties` + `androidTestUtil` + `scripts/run-android-instrumented.ps1` |

## Acceptance

```bash
./gradlew :runtime-wasmtime:test
./gradlew :android-demo:assembleDebug
```

Instrumented tests (device + WebGPU/Vulkan):

```powershell
# Preferred (bypass UTP)
./scripts/run-android-instrumented.ps1

# Or Studio / Gradle
./gradlew :android-demo:connectedDebugAndroidTest
```

- `WasmtimeVectorAddInstrumentedTest` (abi-mvp)
- `WasmtimeCmVectorAddInstrumentedTest` (CM; needs CM-patched `.so`)

Still marked **experimental / non-compliant wasi:webgpu**.
