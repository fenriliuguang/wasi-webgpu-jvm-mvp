# guest/triangle-cm

**English** | [README.md](README.md)

Experimental **Component Model** Guest: single-shot on-screen red triangle.

- Imports `experimental:webgpu-cm/host@0.3.0` (WIT resources + methods; pinned under `wit/`)
- **Not** compliant `wasi:webgpu` / wasi-gfx
- Export: `run-triangle(window-handle, width, height) -> result<_, string>`
- Host injects Android native window; Guest **only holds** `surface`

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

Committed `triangle_cm.wasm` is for upcoming Demo / instrumented tests (wiring: `docs/scheme/guest-onscreen-cm.md`).
