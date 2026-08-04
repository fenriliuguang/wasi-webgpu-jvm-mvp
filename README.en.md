# wasi-webgpu-jvm-mvp

**experimental Dawn / CPU host mapping for wasi:webgpu**

[中文](README.md) | **English**

> Until a standard full `wasi:webgpu` world / full resource surface is wired, **do not** advertise this as a compliant implementation.  
> P1: **abi-mvp** (core wasm imports).  
> CM slice: **experimental:webgpu-cm** (Component Model + typed lists/strings + WIT resources; still experimental).  
> Reference implementation: [`wasi-webgpu-wasmtime`](https://crates.io/crates/wasi-webgpu-wasmtime).

## Goals

Build the **lamp wiring (Host glue)** first, then plug in the **socket (Wasmtime)**.

```text
P0: Kotlin / Demo → WasiWebGpuHost (L2) → Dawn (Android) or CpuHost (desktop)
P1: Guest.wasm → Wasmtime (L1) + abi-mvp → same L2
    Desktop: CpuWasiWebGpuHost
    Android: DawnWasiWebGpuHost + Bionic libwasmtime4j.so
CM: Guest.component → Wasmtime ComponentLinker + abi-cm → same L2 (desktop CpuHost)
```

| Layer | Module | Notes |
|-------|--------|-------|
| L2 Host API | `host-api` | Pure JVM: `WasiWebGpuHost`, handles, `CpuWasiWebGpuHost`, `VectorAddScenario` |
| L3 Dawn | `host-webgpu` | Android: `DawnWasiWebGpuHost` |
| L1 + ABI | `runtime-wasmtime` + `abi-mvp` | Wasmtime ↔ flat imports (desktop + Android) |
| L1 + CM | `runtime-wasmtime` (`runtime.cm`) + `abi-cm` | ComponentLinker ↔ typed WIT imports |
| Guest | `guest/vector-add` | abi-mvp `.wat` + prebuilt `.wasm` |
| Guest CM | `guest/vector-add-cm` | wit-bindgen component + prebuilt `.wasm` |
| Consumer | `android-demo` | Dawn / Guest instrumented tests / thin UI |

**Explicitly out of scope (this phase):** Chicory, on-screen / wasi-gfx, compliant full wasi:webgpu world / record-type alignment.

## Repository layout

```text
wasi-webgpu-jvm-mvp/
  host-api/            # L2 + CPU Host
  host-webgpu/         # Dawn L3 (Android library)
  abi-mvp/             # Flat import bindings
  abi-cm/              # experimental CM host → L2
  runtime-wasmtime/    # Wasmtime4j L1 (abi-mvp + CM)
    android-natives/   # Android Bionic libwasmtime4j.so (jniLibs)
  guest/vector-add/    # abi-mvp Guest assets
  guest/vector-add-cm/ # CM Guest assets
  android-demo/        # Dawn + Guest consumer
  docs/mapping/        # WIT ↔ Dawn
  docs/perf/           # P1 boundary notes
  docs/android-wasmtime.md
  wit/                 # Pinned WIT (incl. compute-cm)
```

## Build & test

Requires: full **JDK** (not JRE), Android SDK (for Dawn paths).  
If Gradle picks a JRE by mistake, set `org.gradle.java.home=...` in `local.properties`.

```bash
# L2 / CPU Host / abi-mvp
./gradlew :host-api:test :abi-mvp:test

# P1 + CM: Guest → Wasmtime → L2 (desktop CpuHost; abi-mvp and CM tests)
./gradlew :runtime-wasmtime:test

# Android assemble (includes jniLibs + guest assets)
./gradlew :android-demo:assembleDebug
```

Instrumented tests (device/emulator + WebGPU/Vulkan): in Android Studio, right-click

- P0 Kotlin→Dawn: `VectorAddInstrumentedTest.kt` → **Run**  
- Guest→Wasmtime→Dawn: `WasmtimeVectorAddInstrumentedTest.kt` → **Run**  

(Gradle panels may not list verification tasks; that is default IDE behavior. Prefer the desktop CM path; Android CM instrumented tests are not required for this slice.)

### Android Wasmtime `.so`

`android-demo` packages Bionic `libwasmtime4j.so` from `runtime-wasmtime/android-natives/jniLibs` and **excludes** Maven desktop `wasmtime4j-native`.  
Rebuild (needs NDK + Rust + `cargo-ndk`):

```powershell
./scripts/build-wasmtime4j-android.ps1
```

See [`runtime-wasmtime/android-natives/README.md`](runtime-wasmtime/android-natives/README.md).

Android-specific patches (summary; details and pitfalls in [`docs/android-wasmtime.en.md`](docs/android-wasmtime.en.md)):

- `JNI_OnLoad` returns `JNI_VERSION_1_6` (ART rejects 1_8)
- Local `Validation.requireValidHandle` allows high-bit-set handles (ARM64 TBI/PAC pointers bit-cast to signed `long` can be negative)
- Native `memory.rs` handle checks use unsigned compare (avoid misclassifying MTE-tagged pointers as corrupted)

Guest rebuild:

```bash
# abi-mvp
wasm-tools parse guest/vector-add/vector_add.wat -o guest/vector-add/vector_add.wasm

# CM (needs Rust wasm32-unknown-unknown + wasm-tools)
./scripts/build-vector-add-cm.ps1

# Desktop CM resources: git apply tracked patches and install wasmtime4j-native (one-shot / re-run after upgrades)
./scripts/build-wasmtime4j-desktop-cm.ps1
# Patch source: patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch (see patches/README.en.md)
```

## Package name

`io.github.fenriliuguang.wasi.webgpu.experimental.*`

## Definition of Done

### P0

- [x] Compute-subset mapping table and deviation list (`docs/mapping`)
- [x] `WasiWebGpuHost` + Dawn adapter + handle-drop unit tests
- [x] Readback matches CPU expectation (instrumented tests green)
- [x] Mergeable without Runtime / Chicory / CM dependencies (P0 slice)

### P1

- [x] `abi-mvp` + desktop Wasmtime → same L2
- [x] Guest vector-add matches pure Kotlin→L2 (`:runtime-wasmtime:test`)
- [x] Boundary cost notes (`docs/perf/p1-boundary.md`)
- [x] Android-embedded Wasmtime → same L2 → Dawn (`WasmtimeVectorAddInstrumentedTest`)

### CM (experimental slice)

- [x] `wit/compute-cm` + `abi-cm` + CM Guest → same L2 (desktop `:runtime-wasmtime:test`)
- [x] WIT resources replace flat u32 (adapter/device/queue/buffer/…; still not compliant wasi:webgpu)
- [ ] Android CM instrumented tests (optional follow-up)

## References

- [wasi-webgpu](https://github.com/WebAssembly/wasi-webgpu)
- [androidx.webgpu](https://developer.android.com/jetpack/androidx/releases/webgpu)
- [wasmtime4j](https://github.com/tegmentum/wasmtime4j)
- Scheme summary: [`docs/scheme/README.en.md`](docs/scheme/README.en.md)
- Android Wasmtime progress & pitfalls: [`docs/android-wasmtime.en.md`](docs/android-wasmtime.en.md)

## Documentation index (English)

| Document | Link |
|----------|------|
| Root README | [README.en.md](README.en.md) |
| Scheme summary | [docs/scheme/README.en.md](docs/scheme/README.en.md) |
| Android Wasmtime | [docs/android-wasmtime.en.md](docs/android-wasmtime.en.md) |
| WIT ↔ Dawn mapping | [docs/mapping/compute-subset.en.md](docs/mapping/compute-subset.en.md) |
| Threading | [docs/mapping/threading.en.md](docs/mapping/threading.en.md) |
| Errors & async | [docs/mapping/errors-async.en.md](docs/mapping/errors-async.en.md) |
| P1 boundary notes | [docs/perf/p1-boundary.en.md](docs/perf/p1-boundary.en.md) |
| WIT lock | [wit/README.en.md](wit/README.en.md) |
| compute-cm WIT | [wit/compute-cm/README.en.md](wit/compute-cm/README.en.md) |
| wasmtime4j patches | [patches/README.en.md](patches/README.en.md) |
| Android natives | [runtime-wasmtime/android-natives/README.md](runtime-wasmtime/android-natives/README.md) (EN) |
| Guest abi-mvp | [guest/vector-add/README.en.md](guest/vector-add/README.en.md) |
| Guest CM | [guest/vector-add-cm/README.en.md](guest/vector-add-cm/README.en.md) |
