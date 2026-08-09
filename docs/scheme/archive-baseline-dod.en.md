# Baseline DoD archive (complete)

[中文](archive-baseline-dod.md) | **English**

> Completed acceptance checklist moved out of the root README.  
> Current phase: root [`README.en.md`](../../README.en.md) (next phase unlocked).

Archive covers work through: surface/render lifted into L2 + `experimental:webgpu-cm@0.3.0` (Kotlin demo via Host).

## P0

- [x] Compute-subset mapping table and deviation list (`docs/mapping`)
- [x] `WasiWebGpuHost` + Dawn adapter + handle-drop unit tests
- [x] Readback matches CPU expectation (instrumented tests green)
- [x] Mergeable without Runtime / CM dependencies (P0 slice)

## P1

- [x] `abi-mvp` + desktop Wasmtime → same L2
- [x] Guest vector-add matches pure Kotlin→L2 (`:runtime-wasmtime:test`)
- [x] Boundary cost notes (`docs/perf/p1-boundary.md`)
- [x] Android-embedded Wasmtime → same L2 → Dawn (`WasmtimeVectorAddInstrumentedTest`)

## CM (experimental slice)

- [x] `wit/compute-cm` + `abi-cm` + CM Guest → same L2 (desktop `:runtime-wasmtime:test`)
- [x] WIT resources replace flat u32 (adapter/device/queue/buffer/…; still not compliant wasi:webgpu)
- [x] Android CM instrumented tests (`WasmtimeCmVectorAddInstrumentedTest`; needs CM-patched Bionic `.so`)

## Delivery harden

- [x] Desktop CM native → `runtime-wasmtime/desktop-natives/` (no Gradle cache mutation)
- [x] CM unit tests skip without patched natives; abi-mvp always runs
- [x] GitHub Actions: `:host-api:test` / `:abi-mvp:test` / `:runtime-wasmtime:test` + `:android-demo:assembleDebug`
- [x] `CHANGELOG.md` + [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) (upstream brief; no required PR)

## Semantic expansion (buffer records/flags)

- [x] `experimental:webgpu-cm@0.2.0`: `buffer-descriptor` + usage/map flags; `create-buffer` / `map-async`
- [x] Dropped alternate-runtime mentions from docs / scheme
- [x] `experimental:webgpu-cm@0.3.0`: surface + render minimal surface

## On-screen demo (Kotlin)

- [x] `android-demo`: `SurfaceView` + `TriangleRenderer` (via L2 Host→Dawn; not Guest/wasi-gfx)
- [x] Lift surface/render into `WasiWebGpuHost` / WIT (`experimental:webgpu-cm@0.3.0`; still no Guest/wasi-gfx on-screen)
