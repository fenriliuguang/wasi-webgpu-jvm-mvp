# experimental:webgpu-cm@0.5.0

[中文](README.md) | **English**

Component Model compute + minimal surface/render slice.

- Package is **experimental** — **not** compliant `wasi:webgpu`
- Method names lean toward `wasi:webgpu@0.3.0-rc.2`
- Handles are WIT `resource` + methods (mapped to L2 `GpuHandle`)
- async WIT → sync (matches L2)
- **0.2.0:** `buffer-descriptor` + `buffer-usage-flags` / `map-mode-flags` (u32 aliases); `create-buffer(descriptor)`; `map-async(mode, …)` replaces `map-read`
- **0.3.0:** Android native-window `surface` + triangle-shaped `render-pipeline` / `render-pass`
- **0.4.0:** `vertex-attribute` / `vertex-buffer-layout`; `set-vertex-buffer`; `create-render-pipeline-triangle-buffers`
- **0.5.0 (slice C):** standard-shaped `create-bind-group-layout` / `create-bind-group` / `create-compute-pipeline(descriptor)` / `queue.submit(list)`; keep `*storage3` / `*3` / `submit1` / `create-compute-pipeline-bgl` (deprecated). `compute-pipeline.layout` remains bind-group-layout (L2; wasi pipeline-layout → D)
- World exports: `vector-add` → `run-vector-add`; `triangle` → `run-triangle` / `init-triangle` / `draw-frame` / `drop-triangle` (Host injects native window)

Guests: `guest/vector-add-cm/` (migrated to standard descriptors), `guest/triangle-cm/`  
Host adapter: `abi-cm` → `WasiWebGpuHost`  
Wiring: `runtime-wasmtime` `runtime.cm` (`ComponentLinker` + `defineResource`)

Desktop CM needs patched wasmtime4j natives (resource marshalling / multi-resource registration / replay on instantiate).  
In-repo patch: [`patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](../../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch)

```powershell
./scripts/build-wasmtime4j-desktop-cm.ps1
```
