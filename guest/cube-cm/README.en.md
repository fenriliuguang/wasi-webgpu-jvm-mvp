# guest/cube-cm

[中文](README.md) | **English**

Experimental **Component Model** guest: slowly continuously rotating textured cube (one-shot + host-driven frame loop).

- Imports `experimental:webgpu-cm/host@0.8.0` (WIT resources + methods; pinned under `wit/`)
- **Not** compliant `wasi:webgpu` / wasi-gfx
- Exports:
  - `run-cube` — one-shot configure → draw → present → unconfigure
  - `init-cube` / `draw-frame` / `drop-cube` — host-driven frame loop
- Host injects Android native window; Guest **only holds** `surface`
- Standard descriptors: `create-render-pipeline` / `begin-render-pass` / `queue.submit(list)` / bind-group·pipeline-layout / `write-texture` / render-pass `set-bind-group`; depth-stencil + MVP uniform
- Texture: original CC0 64×64 checkerboard (see [ATTRIBUTION.md](ATTRIBUTION.md))

## Rebuild

Requires Rust (prefer 1.97.1) `wasm32-unknown-unknown` + `wasm-tools` + wit-bindgen **0.55.0**.

```powershell
./scripts/build-cube-cm.ps1
```

Committed `cube_cm.wasm` is synced by `android-demo` to `assets/guest/cube_cm.wasm`;
runtime entry: `WasmtimeCmCube` / `WasmtimeCmCubeAndroid`.
