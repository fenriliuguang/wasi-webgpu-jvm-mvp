# experimental:webgpu-cm@0.8.0

[中文](README.md) | **English**

Component Model compute + minimal surface/render slice.

- Package is **experimental** — **not** compliant `wasi:webgpu`
- Method names lean toward `wasi:webgpu@0.3.0-rc.2`
- Handles are WIT `resource` + methods (mapped to L2 `GpuHandle`)
- async WIT → sync (matches L2)
- **0.2.0:** `buffer-descriptor` + `buffer-usage-flags` / `map-mode-flags` (u32 aliases); `create-buffer(descriptor)`; `map-async(mode, …)` replaces `map-read`
- **0.3.0:** Android native-window `surface` + triangle-shaped `render-pipeline` / `render-pass`
- **0.4.0:** `vertex-attribute` / `vertex-buffer-layout`; `set-vertex-buffer`; `create-render-pipeline-triangle-buffers`
- **0.5.0 (slice C):** standard-shaped `create-bind-group-layout` / `create-bind-group` / `create-compute-pipeline(descriptor)` / `queue.submit(list)`; keep `*storage3` / `*3` / `submit1` / `create-compute-pipeline-bgl` (deprecated)
- **0.6.0 (slice D):** `create-texture` / `create-sampler` / `create-pipeline-layout` / `texture.create-view`; BGL/BG sampler·texture entries; `compute-pipeline.layout` → pipeline-layout
- **0.7.0 (slice E):** `create-render-pipeline(descriptor)` / `begin-render-pass(descriptor)`; `*-triangle*` / `begin-render-pass-clear` deprecated
- **0.8.0 (guest-descriptor-cube B):** `write-texture`; render-pass `set-bind-group`; depth-stencil; `world cube`
- World exports (WIT still keeps historical worlds): `vector-add` / `triangle` / `cube`; **in-tree Guest is cube only** → `run-cube` / `init-cube` / `draw-frame` / `drop-cube` (Host injects native window)

Guest (current): [`guest/cube-cm/`](../../guest/cube-cm/) (standard descriptors; nested borrow needs recursive `cm-resources`-patched natives). Historical vector-add-cm / triangle-cm demos removed.  
Host adapter: `abi-cm` → `WasiWebGpuHost`  
Wiring: `runtime-wasmtime` `runtime.cm` (`ComponentLinker` + `defineResource`)

Desktop CM needs patched wasmtime4j natives (resource marshalling / multi-resource registration / replay on instantiate).  
In-repo patch: [`patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](../../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch)

```powershell
./scripts/build-wasmtime4j-desktop-cm.ps1
```
