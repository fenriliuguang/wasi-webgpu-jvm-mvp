# experimental:webgpu-cm@0.4.0

[中文](README.md) | **English**

Component Model compute + minimal surface/render slice.

- Package stays **experimental** — **not** a compliant `wasi:webgpu`
- Method names lean toward `wasi:webgpu@0.3.0-rc.2` paths
- Handles are WIT `resource` + methods (still mapped to L2 `GpuHandle` internally)
- async WIT → sync (same as L2)
- bind-group helpers remain vector-add shaped
- **0.2.0:** `buffer-descriptor` + `buffer-usage-flags` / `map-mode-flags` (u32 aliases); `create-buffer(descriptor)`; `map-async(mode, …)` replaces `map-read`
- **0.3.0:** Android native-window `surface` + triangle-shaped `render-pipeline` / `render-pass`
- **0.4.0:** `vertex-attribute` / `vertex-buffer-layout`; `set-vertex-buffer`; `create-render-pipeline-triangle-buffers`
- World exports: `vector-add` → `run-vector-add`; `triangle` → `run-triangle` / `init-triangle` / `draw-frame` / `drop-triangle` (Host injects native window)

Guests: `guest/vector-add-cm/`, `guest/triangle-cm/`  
Host adapter: `abi-cm` → `WasiWebGpuHost`  
Wiring: `runtime-wasmtime` `runtime.cm` (`ComponentLinker` + `defineResource`)

Desktop CM needs a patched wasmtime4j native (resource marshalling / multi-resource registration / resource replay on instantiate).  
In-tree patch: [`patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](../../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch)

```powershell
./scripts/build-wasmtime4j-desktop-cm.ps1
```
