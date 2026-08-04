# Changelog

All notable changes to this experimental MVP are documented here.
Package / marketing claims remain **non-compliant** `wasi:webgpu` until a full standard world is wired.

## Unreleased

### Planning

- Locked next slice: Guest CM on-screen (`docs/scheme/guest-onscreen-cm.md`); baseline DoD archived from root README

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
