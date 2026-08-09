# guest/triangle-cm

[中文](README.md) | **English**

Experimental **Component Model** Guest: on-screen red triangle (one-shot + host-driven frame loop).

- Imports `experimental:webgpu-cm/host@0.8.0` (WIT resources + methods; pinned under `wit/`)
- **Not** compliant `wasi:webgpu` / wasi-gfx
- Exports:
  - `run-triangle` — one-shot configure → draw → present → unconfigure
  - `init-triangle` / `draw-frame` / `drop-triangle` — host-driven frame loop
- Host injects Android native window; Guest **only holds** `surface`
- Vertices via `create-buffer` + `write-buffer` + `set-vertex-buffer`; standard `create-render-pipeline` / `begin-render-pass` / `queue.submit(list)`

## Rebuild

Requires Rust (`wasm32-unknown-unknown`) + `wasm-tools` + wit-bindgen **0.55.0**.

```powershell
./scripts/build-triangle-cm.ps1
```

Or manually:

```bash
cd guest/triangle-cm
cargo build --target wasm32-unknown-unknown --release
wasm-tools component new target/wasm32-unknown-unknown/release/triangle_cm.wasm \
  -o triangle_cm.wasm
```

Committed `triangle_cm.wasm` is synced by `android-demo` to `assets/guest/triangle_cm.wasm`;
runtime entry: `WasmtimeCmTriangle` / `WasmtimeCmTriangleAndroid` (plan: `docs/scheme/guest-onscreen-cm.md`).
