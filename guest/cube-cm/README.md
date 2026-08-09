# guest/cube-cm

**中文** | [English](README.en.md)

Experimental **Component Model** Guest：缓慢持续旋转的纹理正方体（one-shot + 宿主驱动帧循环）。

- Imports `experimental:webgpu-cm/host@0.8.0`（WIT resources + methods；钉在 `wit/`）
- **Not** compliant `wasi:webgpu` / wasi-gfx
- Exports:
  - `run-cube` — 单次 configure → 若干帧等价于 init→draw→drop
  - `init-cube` / `draw-frame` / `drop-cube` — 宿主驱动帧循环
- Host 注入 Android native window；Guest **只持** `surface`
- 标准 descriptor：`create-render-pipeline` / `begin-render-pass` / `queue.submit(list)` / bind-group·pipeline-layout / `write-texture` / render-pass `set-bind-group`；depth-stencil + MVP uniform
- 纹理：项目原创 CC0 64×64 棋盘格（见 [ATTRIBUTION.md](ATTRIBUTION.md)）

## Rebuild

Requires Rust（建议 1.97.1）`wasm32-unknown-unknown` + `wasm-tools` + wit-bindgen **0.55.0**.

```powershell
./scripts/build-cube-cm.ps1
```

Committed `cube_cm.wasm` 由 `android-demo` 同步到 `assets/guest/cube_cm.wasm`；
运行时入口：`WasmtimeCmCube` / `WasmtimeCmCubeAndroid`。
