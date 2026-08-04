# Scheme notes (imported)

[中文](README.md) | **English**

This file is a scheme summary migrated from discussion notes. Full narrative lives in the repo root [`README.en.md`](../../README.en.md).

## One-liner

Build the **lamp wiring (Dawn Host glue)** first, then plug in the **socket (Wasm Runtime)** as needed.

## Phases

| Phase | Status in this repo |
|-------|---------------------|
| **P0 · Glue** | **Done**: `host-api` / `host-webgpu` + `docs/mapping` + Android instrumented tests |
| **P1 · Runtime** | **Done**: desktop Wasmtime + Android-embedded Wasmtime → same `abi-mvp` / L2; still experimental / non-CM |
| **CM slice** | **Done (experimental)**: `experimental:webgpu-cm` WIT resources + Guest + `abi-cm` + desktop ComponentLinker → same L2; Android CM → Dawn instrumented tests; still not compliant wasi:webgpu |
| **Optional next** | Not started: on-screen / Chicory; next can align more wasi:webgpu records/flags |

## Hard principles (excerpt)

1. L2 must not depend on L1.  
2. Do not reinvent a full Kotlin WebGPU client API.  
3. Package names / README mark `experimental`; do not claim compliant `wasi:webgpu` before the standard full world is wired.  
4. P1 uses hand-written abi-mvp (core wasm), **not** the Component Model.  
5. Android uses Bionic `libwasmtime4j.so` (`runtime-wasmtime/android-natives`; CM path needs both android + cm-resources patches); desktop CM resources need a patched `wasmtime4j-native` (tracked `patches/*.patch` + `scripts/build-wasmtime4j-desktop-cm.ps1`).

Android-embedded Wasmtime progress, patches, and pitfalls: [`docs/android-wasmtime.en.md`](../android-wasmtime.en.md).
