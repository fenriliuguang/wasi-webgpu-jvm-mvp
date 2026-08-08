# guest/triangle-cm

**中文** | [English](README.en.md)

Experimental **Component Model** Guest：上屏红三角（one-shot + 宿主驱动帧循环）。

- Imports `experimental:webgpu-cm/host@0.4.0`（WIT resources + methods；钉在 `wit/`）
- **Not** compliant `wasi:webgpu` / wasi-gfx
- Exports:
  - `run-triangle` — 单次 configure → draw → present → unconfigure
  - `init-triangle` / `draw-frame` / `drop-triangle` — 宿主驱动帧循环
- Host 注入 Android native window；Guest **只持** `surface`

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

Committed `triangle_cm.wasm` 由 `android-demo` 同步到 `assets/guest/triangle_cm.wasm`；
运行时入口：`WasmtimeCmTriangle` / `WasmtimeCmTriangleAndroid`（计划：`docs/scheme/guest-onscreen-cm.md`）。
