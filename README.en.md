# wasi-webgpu-jvm-mvp

**experimental Dawn / CPU host mapping for wasi:webgpu**

[中文](README.md) | **English**

> Until a standard full `wasi:webgpu` world / full resource surface is wired, **do not** advertise this as a compliant implementation.  
> Reference: [`wasi-webgpu-wasmtime`](https://crates.io/crates/wasi-webgpu-wasmtime).

## Features

- **L2 Host**: `WasiWebGpuHost` (compute + minimal Android surface/render) + desktop `CpuWasiWebGpuHost`
- **L3 Dawn**: `DawnWasiWebGpuHost` (Android / androidx.webgpu)
- **Runtime**: Wasmtime4j — **abi-mvp** (core wasm) and **abi-cm** (Component Model / `experimental:webgpu-cm@0.8.0`)
- **Guest**: CM rotating textured cube (`guest/cube-cm`) via abi-cm → L2 → Dawn; device / Demo acceptance baseline
- **Engineering**: multi-module Gradle, CI (JVM tests + `assembleDebug`), Bionic / desktop CM-patched native scripts

Package: `io.github.fenriliuguang.wasi.webgpu.experimental.*`

## Architecture

Build the **lamp wiring (Host glue)** first, then plug in the **socket (Wasmtime)**.

```text
Kotlin / Demo ──► WasiWebGpuHost (L2) ──► Dawn (Android) or CpuHost (desktop)
Guest.wasm ──► Wasmtime + abi-cm ──► same L2
```

| Layer | Module |
|-------|--------|
| L2 | `host-api` |
| L3 | `host-webgpu` |
| L1 + ABI | `runtime-wasmtime` + `abi-mvp` / `abi-cm` |
| Guest | `guest/cube-cm` (device acceptance baseline) |
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

Instrumented tests (device + WebGPU/Vulkan) — **recommended entry**:

```powershell
./scripts/run-android-instrumented.ps1   # CM cube; do not rely on Studio UTP
```

Native / Guest rebuilds and pitfalls: [`docs/android-wasmtime.en.md`](docs/android-wasmtime.en.md), [`runtime-wasmtime/android-natives/README.md`](runtime-wasmtime/android-natives/README.md), `scripts/build-*.ps1`.

## Status

- **Done**: baseline (P0–P1 / CM compute / L2 on-screen) → [archive](docs/scheme/archive-baseline-dod.en.md); Guest CM on-screen (triangle-cm, 2026-08-06) → [archive](docs/scheme/archive-guest-onscreen-cm-dod.en.md); Demo CM stability + frame loop (2026-08-07) → [archive](docs/scheme/archive-demo-cm-stability-dod.en.md); Demo CM **device stability regression** (D1–D6, 2026-08-08, V2458A) → [blockers](docs/scheme/demo-cm-stability-blockers.md) (ZH); **semantic hardening & engineering debt** (A–E, 2026-08-09) → [archive](docs/scheme/archive-semantic-hardening-dod.en.md); **compliant wasi:webgpu world (no gfx, A–G, 2026-08-09)** → [archive](docs/scheme/archive-compliant-world-dod.en.md); **Guest standard descriptors + rotating textured cube (A–D, 2026-08-10)** → [archive](docs/scheme/archive-guest-descriptor-cube-dod.en.md) (D: **still not true WIT dtor**)
- **Next (locked)**: [Engineering handoff: Maven publishability (no external release) / abi-mvp render / optional perf](docs/scheme/engineering-handoff.en.md) (A–C; C optional, non-blocking; **not started**). Remains **experimental**; A is local Publishing self-check only — **no** external release or “published / ready for consumers” claims. **No** wasi-gfx / compliance marketing / true CM async / upstream PRs / true dtor overlay. **Device acceptance baseline = CM cube**

## References

- [wasi-webgpu](https://github.com/WebAssembly/wasi-webgpu) · [androidx.webgpu](https://developer.android.com/jetpack/androidx/releases/webgpu) · [wasmtime4j](https://github.com/tegmentum/wasmtime4j)
- Changelog: [`CHANGELOG.md`](CHANGELOG.md) · Scheme: [`docs/scheme/README.en.md`](docs/scheme/README.en.md)

## Documentation index

| Document | Link |
|----------|------|
| Root README | [README.en.md](README.en.md) |
| Scheme summary | [docs/scheme/README.en.md](docs/scheme/README.en.md) |
| Engineering handoff (next phase, locked; no external release) | [engineering-handoff.en.md](docs/scheme/engineering-handoff.en.md) |
| Guest standard descriptors + cube DoD archive | [archive-guest-descriptor-cube-dod.en.md](docs/scheme/archive-guest-descriptor-cube-dod.en.md) |
| Guest standard descriptors + cube (plan, complete) | [guest-descriptor-cube.en.md](docs/scheme/guest-descriptor-cube.en.md) |
| Compliant-world DoD archive (no gfx) | [archive-compliant-world-dod.en.md](docs/scheme/archive-compliant-world-dod.en.md) |
| Compliant-world plan (complete) | [compliant-world.en.md](docs/scheme/compliant-world.en.md) |
| Compliant-world gap / dual-track | [gap](docs/mapping/compliant-world-gap.en.md) · [dual-track](docs/mapping/compliant-world-dual-track.en.md) |
| Semantic hardening archive / plan | [archive](docs/scheme/archive-semantic-hardening-dod.en.md) · [plan](docs/scheme/semantic-hardening.en.md) |
| Demo CM stability archive / blockers | [archive](docs/scheme/archive-demo-cm-stability-dod.en.md) · [blockers](docs/scheme/demo-cm-stability-blockers.md) (ZH) |
| Baseline / Guest CM on-screen DoD archives | [baseline](docs/scheme/archive-baseline-dod.en.md) · [onscreen](docs/scheme/archive-guest-onscreen-cm-dod.en.md) |
| Android Wasmtime | [docs/android-wasmtime.en.md](docs/android-wasmtime.en.md) |
| Compute / Render mapping | [compute](docs/mapping/compute-subset.en.md) · [render](docs/mapping/render-subset.en.md) |
| Threading / errors | [threading](docs/mapping/threading.en.md) · [errors-async](docs/mapping/errors-async.en.md) |
| WIT / patches / natives / Guest | [wit/](wit/README.en.md) · [patches/](patches/README.en.md) · [natives](runtime-wasmtime/android-natives/README.md) · [guest/cube-cm](guest/cube-cm/README.en.md) |
| Changelog | [CHANGELOG.md](CHANGELOG.md) |
