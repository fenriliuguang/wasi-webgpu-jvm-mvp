# Scheme notes (imported)

[中文](README.md) | **English**

Imported scheme summary. Root README focuses on **features and current status**; completed DoDs live in the archive pages.

## One-liner

Build the **lamp wiring (Dawn Host glue)** first, then plug in the **socket (Wasm Runtime)** as needed.

## Stages

| Stage | Status here |
|-------|-------------|
| **Baseline (P0–semantic expansion / L2 on-screen)** | **Done** — see [`archive-baseline-dod.en.md`](archive-baseline-dod.en.md) |
| **Guest CM on-screen (triangle-cm)** | **Done** (2026-08-06) — see [`archive-guest-onscreen-cm-dod.en.md`](archive-guest-onscreen-cm-dod.en.md) |
| **Demo CM stability + frame loop** | **In progress** — locked plan [`demo-cm-stability.en.md`](demo-cm-stability.en.md) |
| **Later** | More WIT records; wasi-gfx / full compliant world; Maven Central (not this slice) |

## Hard rules (excerpt)

1. L2 must not depend on L1.  
2. Do not rebuild a full Kotlin WebGPU client API.  
3. Package / README stay `experimental`; no compliant `wasi:webgpu` claim until a full standard world is wired.  
4. P1 uses hand-written abi-mvp (core wasm), **not** Component Model.  
5. Android uses Bionic `libwasmtime4j.so` (CM needs android + cm-resources patches); desktop CM via `scripts/build-wasmtime4j-desktop-cm.ps1` → `runtime-wasmtime/desktop-natives/`.

Android Wasmtime: [`docs/android-wasmtime.en.md`](../android-wasmtime.en.md).
