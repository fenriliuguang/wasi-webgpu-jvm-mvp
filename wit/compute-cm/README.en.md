# experimental:webgpu-cm@0.2.0

[中文](README.md) | **English**

Component Model compute slice (vector-add).

- Package name is **experimental** — **must not** be called compliant `wasi:webgpu`
- Method names lean toward the `wasi:webgpu@0.3.0-rc.2` compute subset
- Handles are WIT `resource` + methods (still map internally to L2 `GpuHandle`)
- async WIT → sync (same as L2)
- bind-group helpers remain vector-add specialized
- **0.2.0:** `buffer-descriptor` + `buffer-usage-flags` / `map-mode-flags` (u32 aliases); `create-buffer(descriptor)`; `map-async(mode, …)` replaces `map-read`

Guest: `guest/vector-add-cm/`  
Host adapter: `abi-cm` → `WasiWebGpuHost`  
Wiring: `runtime-wasmtime` `runtime.cm` (`ComponentLinker` + `defineResource`)

Desktop CM needs a patched wasmtime4j native (resource marshalling / multi-resource registration / replay resources on instantiate).  
Tracked patch: [`patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](../../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch)

```powershell
./scripts/build-wasmtime4j-desktop-cm.ps1
```
