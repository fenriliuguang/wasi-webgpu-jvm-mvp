# Changelog

All notable changes to this experimental MVP are documented here.
Package / marketing claims remain **non-compliant** `wasi:webgpu` until a full standard world is wired.

## Unreleased

### Planning

- Locked next slice: Guest CM on-screen (`docs/scheme/guest-onscreen-cm.md`); baseline DoD archived from root README

### Guest CM on-screen (triangle-cm) — DoD complete 2026-08-06

- `guest/triangle-cm` + prebuilt `triangle_cm.wasm`: world `triangle` exports `run-triangle(window-handle: u64, width: u32, height: u32)`; Guest only holds `surface`, Host injects the Android native window
- `WasmtimeCmTriangle` (L1) + android-demo `WasmtimeCmTriangleAndroid` / "CM 三角" button: Wasmtime ComponentLinker + abi-cm → same L2 → Dawn one-shot red triangle
- Instrumented green: `WasmtimeCmTriangleInstrumentedTest` (vivo V2458A / Mali); desktop CpuHost → Unsupported, surface unit tests skip (same CM gating)
- Fixes: wasmtime4j `ConcurrentCallCodec` unsigned-u64 window-handle parse (android-demo overlay, `patches/UPSTREAM.md`); vivo `ActivityScenario` intent mismatch → `ActivityLifecycleMonitorRegistry`; L2 skipped under androidx.test Instrumentation
- Docs: Guest path in `docs/mapping/render-subset`; pitfall log `docs/scheme/guest-onscreen-cm-blockers.md`

### Surface/render lift (L2 + WIT)

- L2: `WasiWebGpuHost` surface/render minimal API; `DawnWasiWebGpuHost` implements; `CpuWasiWebGpuHost` → Unsupported
- `experimental:webgpu-cm` **0.2.0 → 0.3.0**: surface + render-pipeline / render-pass helpers (Android native window)
- `TriangleRenderer` draws via L2 Host (not direct androidx.webgpu); still no Guest/wasi-gfx on-screen
- Docs: `docs/mapping/render-subset.md`; threading contract for surface/submit

### On-screen demo (Kotlin)

- `android-demo`: `SurfaceView` + `TriangleRenderer` red triangle (now L2 Host→Dawn)

### Semantic expansion (CM buffer records/flags)

- `experimental:webgpu-cm` **0.1.0 → 0.2.0**: `buffer-descriptor`, `buffer-usage-flags` / `map-mode-flags` (u32 aliases)
- `create-buffer(descriptor)`; `map-async(mode, …)` replaces `map-read`
- Docs: dropped alternate-runtime mentions from roadmap / out-of-scope lists

## 0.1.0-experimental

### Features (DoD complete)

- **P0:** `WasiWebGpuHost` + Dawn / CPU Host compute subset; Android instrumented vector-add
- **P1:** `abi-mvp` Guest → Wasmtime → same L2 (desktop + Android Bionic `libwasmtime4j.so`)
- **CM slice:** `experimental:webgpu-cm` WIT resources + `abi-cm` + desktop/Android CM Guest path

### Delivery harden (this release slice)

- Desktop CM natives install to `runtime-wasmtime/desktop-natives/` (no Gradle cache mutation)
- CM unit tests skip when patched desktop natives are absent (CI-friendly)
- GitHub Actions: JVM unit tests + `:android-demo:assembleDebug`
- Patch upstream notes: `patches/UPSTREAM.md`

### Explicitly out of scope

- Maven Central / GitHub Packages publishing
- Guest / wasi-gfx on-screen; full wasi:webgpu compliance
- Opening upstream PRs to tegmentum/wasmtime4j (notes only)
