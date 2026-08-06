# experimental:webgpu-cm@0.3.0

[中文](README.md) | **English**

Component Model compute + minimal surface/render slice.

- Package name is **experimental** — **must not** be called compliant `wasi:webgpu`
- Method names lean toward `wasi:webgpu@0.3.0-rc.2` paths
- Handles are WIT `resource` + methods (still map internally to L2 `GpuHandle`)
- async WIT → sync (same as L2)
- bind-group helpers remain vector-add specialized
- **0.2.0:** `buffer-descriptor` + `buffer-usage-flags` / `map-mode-flags` (u32 aliases); `create-buffer(descriptor)`; `map-async(mode, …)` replaces `map-read`
- **0.3.0:** Android native-window `surface` + triangle-shaped `render-pipeline` / `render-pass`
- World exports: `vector-add` → `run-vector-add`; `triangle` → `run-triangle` (Host injects native window)

Guest: `guest/vector-add-cm/`, `guest/triangle-cm/`  
Host adapter: `abi-cm` → `WasiWebGpuHost`  
Wiring: `runtime-wasmtime` `runtime.cm` (`ComponentLinker` + `defineResource`)

Desktop CM needs a patched wasmtime4j native (resource marshalling / multi-resource registration / replay resources on instantiate).  
Tracked patch: [`patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](../../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch)

```powershell
./scripts/build-wasmtime4j-desktop-cm.ps1
```
