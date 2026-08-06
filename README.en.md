# wasi-webgpu-jvm-mvp

**experimental Dawn / CPU host mapping for wasi:webgpu**

[中文](README.md) | **English**

> Until a standard full `wasi:webgpu` world / full resource surface is wired, **do not** advertise this as a compliant implementation.  
> Reference: [`wasi-webgpu-wasmtime`](https://crates.io/crates/wasi-webgpu-wasmtime).

## Features

- **L2 Host**: `WasiWebGpuHost` (compute + minimal Android surface/render) + desktop `CpuWasiWebGpuHost`
- **L3 Dawn**: `DawnWasiWebGpuHost` (Android / androidx.webgpu)
- **Runtime**: Wasmtime4j — **abi-mvp** (core wasm) and **abi-cm** (Component Model / `experimental:webgpu-cm@0.3.0`)
- **Guest**: vector-add (abi-mvp + CM); red triangle two ways: Kotlin `SurfaceView` via L2 Host→Dawn, and CM Guest (triangle-cm) via abi-cm → same L2 → Dawn
- **Engineering**: multi-module Gradle, CI (JVM tests + `assembleDebug`), Bionic / desktop CM-patched native scripts

Package: `io.github.fenriliuguang.wasi.webgpu.experimental.*`

## Architecture

Build the **lamp wiring (Host glue)** first, then plug in the **socket (Wasmtime)**.

```text
Kotlin / Demo ──► WasiWebGpuHost (L2) ──► Dawn (Android) or CpuHost (desktop)
Guest.wasm ──► Wasmtime + abi-mvp / abi-cm ──► same L2
```

| Layer | Module |
|-------|--------|
| L2 | `host-api` |
| L3 | `host-webgpu` |
| L1 + ABI | `runtime-wasmtime` + `abi-mvp` / `abi-cm` |
| Guest | `guest/vector-add`, `guest/vector-add-cm`, `guest/triangle-cm` (CM on-screen working) |
| Demo | `android-demo` |

## Repository layout

```text
wasi-webgpu-jvm-mvp/
  host-api/  host-webgpu/  abi-mvp/  abi-cm/  runtime-wasmtime/
  guest/  android-demo/  wit/  patches/  docs/  scripts/  .github/workflows/
```

## Build & test

Requires full **JDK** (not JRE) and Android SDK. If Gradle picks a JRE, set `org.gradle.java.home=...` in `local.properties`.

```bash
./gradlew :host-api:test :abi-mvp:test
./gradlew :runtime-wasmtime:test          # CM tests skip without desktop-natives
./gradlew :android-demo:assembleDebug
```

Instrumented tests (device + WebGPU/Vulkan): run `*InstrumentedTest.kt` in Studio, or:

```powershell
./scripts/run-android-instrumented.ps1
```

Native / Guest rebuilds and pitfalls: [`docs/android-wasmtime.en.md`](docs/android-wasmtime.en.md), [`runtime-wasmtime/android-natives/README.md`](runtime-wasmtime/android-natives/README.md), `scripts/build-*.ps1`.

## Current DoD — Guest CM on-screen (achieved 2026-08-06)

Full plan: [`docs/scheme/guest-onscreen-cm.en.md`](docs/scheme/guest-onscreen-cm.en.md)

- [x] `guest/triangle-cm` (or equivalent) + prebuilt `.wasm`; via abi-cm → same L2 → Dawn red triangle
- [x] Host injects native window; Guest only holds `surface`
- [x] Android instrumented test green (needs CM-patched Bionic `.so`)
- [x] Docs cover Guest on-screen path

**Out of scope this phase:** wasi-gfx, full compliant `wasi:webgpu`, Maven Central, `abi-mvp` flat render imports.

Completed baseline (P0–P1, CM compute, L2 surface, Kotlin triangle): [`docs/scheme/archive-baseline-dod.en.md`](docs/scheme/archive-baseline-dod.en.md).

## References

- [wasi-webgpu](https://github.com/WebAssembly/wasi-webgpu) · [androidx.webgpu](https://developer.android.com/jetpack/androidx/releases/webgpu) · [wasmtime4j](https://github.com/tegmentum/wasmtime4j)
- Changelog: [`CHANGELOG.md`](CHANGELOG.md) · Scheme: [`docs/scheme/README.en.md`](docs/scheme/README.en.md)

## Documentation index

| Document | Link |
|----------|------|
| Root README | [README.en.md](README.en.md) |
| Current plan (Guest CM on-screen) | [docs/scheme/guest-onscreen-cm.en.md](docs/scheme/guest-onscreen-cm.en.md) |
| Baseline DoD archive | [docs/scheme/archive-baseline-dod.en.md](docs/scheme/archive-baseline-dod.en.md) |
| Scheme summary | [docs/scheme/README.en.md](docs/scheme/README.en.md) |
| Android Wasmtime | [docs/android-wasmtime.en.md](docs/android-wasmtime.en.md) |
| Compute / Render mapping | [compute](docs/mapping/compute-subset.en.md) · [render](docs/mapping/render-subset.en.md) |
| Threading / errors | [threading](docs/mapping/threading.en.md) · [errors-async](docs/mapping/errors-async.en.md) |
| WIT / patches / natives / Guest | [wit/](wit/README.en.md) · [patches/](patches/README.en.md) · [natives](runtime-wasmtime/android-natives/README.md) · [guest](guest/vector-add/README.en.md) |
| Changelog | [CHANGELOG.md](CHANGELOG.md) |
