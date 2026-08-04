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
| **Delivery harden** | **Done**: desktop CM → `desktop-natives` (no Gradle cache mutation); CM test gate; GitHub Actions (JVM + assemble); `CHANGELOG` / `patches/UPSTREAM` |
| **Semantic expansion** | **Done (first slice)**: CM `buffer-descriptor` + usage/map flags (`experimental:webgpu-cm@0.2.0`); later optional: on-screen / more records (bind-group, etc.) |

## Hard principles (excerpt)

1. L2 must not depend on L1.  
2. Do not reinvent a full Kotlin WebGPU client API.  
3. Package names / README mark `experimental`; do not claim compliant `wasi:webgpu` before the standard full world is wired.  
4. P1 uses hand-written abi-mvp (core wasm), **not** the Component Model.  
5. Android uses Bionic `libwasmtime4j.so` (`runtime-wasmtime/android-natives`; CM path needs both android + cm-resources patches); desktop CM resources land in `runtime-wasmtime/desktop-natives/` via `scripts/build-wasmtime4j-desktop-cm.ps1` (does not mutate Maven/Gradle cache; see `patches/*.patch`).

Android-embedded Wasmtime progress, patches, and pitfalls: [`docs/android-wasmtime.en.md`](../android-wasmtime.en.md).
