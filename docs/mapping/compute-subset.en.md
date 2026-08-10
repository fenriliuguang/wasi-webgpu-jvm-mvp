# WIT ↔ Dawn mapping (P0 compute subset)

[中文](compute-subset.md) | **English**

> **Status:** experimental (P0 table is host path; CM Guests below)  
> **WIT pin:** `wasi:webgpu/webgpu@0.3.0-rc.2` (see [`wit/`](../../wit/))  
> **Dawn:** `androidx.webgpu:webgpu:1.0.0-alpha05`  
> **Current CM package:** `experimental:webgpu-cm@0.8.0` (primary acceptance; device Guest = `cube-cm`)

This table centers on the P0 acceptance path. Later-slice (C/D, …) capabilities are in the notes and gap matrix; full WebGPU / wasi-gfx is **out of scope**.

## Legend

| Mark | Meaning |
|------|---------|
| ✅ | Direct correspondence |
| ⚠️ | Semantic deviation / sync wrapper / copy boundary |
| ❌ | Not in P0 |

## Instance / Adapter / Device

| WIT | L2 (`WasiWebGpuHost`) | Dawn (`androidx.webgpu`) | Notes |
|-----|------------------------|---------------------------|-------|
| `gpu.request-adapter` | `requestAdapter` | `GPUInstance.requestAdapter` | ⚠️ async → P0 sync wait |
| `gpu-adapter.request-device` | `adapterRequestDevice` | `GPUAdapter.requestDevice` | ⚠️ same; descriptor subset |
| `gpu-device.get-queue` | `deviceGetQueue` | `GPUDevice.queue` | ✅ |

## Buffer / Shader / Bindings / Pipeline

| WIT | L2 | Dawn | Notes |
|-----|----|------|-------|
| `gpu-device.create-buffer` | `deviceCreateBuffer` | `GPUDevice.createBuffer` | ✅ usage flags align with WebGPU; CM `0.2.0` passes `buffer-descriptor` (mapped/label) |
| `gpu-device.create-shader-module` | `deviceCreateShaderModule` | `GPUDevice.createShaderModule` + WGSL | ✅ WGSL only |
| `gpu-device.create-bind-group-layout` | `deviceCreateBindGroupLayout` | `createBindGroupLayout` | ✅ P0: buffer; from slice D also sampler·texture entries |
| `gpu-device.create-bind-group` | `deviceCreateBindGroup` | `createBindGroup` | ✅ P0: buffer; from slice D also sampler·texture |
| `gpu-device.create-compute-pipeline` | `deviceCreateComputePipeline` | `createComputePipeline` | ⚠️ P0 requires explicit layout (no auto) |
| `gpu-device.create-command-encoder` | `deviceCreateCommandEncoder` | `createCommandEncoder` | ✅ |

## Compute pass / Queue / Map

| WIT | L2 | Dawn | Notes |
|-----|----|------|-------|
| `gpu-command-encoder.begin-compute-pass` | `commandEncoderBeginComputePass` | `beginComputePass` | ✅ |
| `gpu-compute-pass-encoder.set-pipeline` | `computePassSetPipeline` | `setPipeline` | ✅ |
| `gpu-compute-pass-encoder.set-bind-group` | `computePassSetBindGroup` | `setBindGroup` | ✅ |
| `gpu-compute-pass-encoder.dispatch-workgroups` | `computePassDispatchWorkgroups` | `dispatchWorkgroups` | ✅ |
| `gpu-compute-pass-encoder.end` | `computePassEnd` | `end` | ✅ pass handle dropped after end |
| `gpu-command-encoder.copy-buffer-to-buffer` | `commandEncoderCopyBufferToBuffer` | `copyBufferToBuffer` | ✅ needed for readback |
| `gpu-command-encoder.finish` | `commandEncoderFinish` | `finish` | ✅ encoder handle drop |
| `gpu-queue.write-buffer-with-copy` | `queueWriteBuffer` | `GPUQueue.writeBuffer` | ⚠️ always host copy |
| `gpu-queue.submit` | `queueSubmit` | `submit` | ✅ |
| `gpu-buffer.map-async` | `bufferMapAsync` | `mapAsync` | ⚠️ async → sync wait; CM `0.2.0` passes `map-mode-flags` |
| `gpu-buffer.get-mapped-range-get-with-copy` | `bufferGetMappedRange` | `getConstMappedRange` + copy | ⚠️ returns `ByteArray` copy |
| `gpu-buffer.unmap` | `bufferUnmap` | `unmap` | ✅ |
| resource `drop` | `drop` | `close` / remove from handle table | ✅ |

## Explicitly out of scope (P0)

| Area | Status |
|------|--------|
| Render pass / surface / canvas | ⚠️ See [render-subset.en.md](render-subset.en.md) (experimental; Guest CM on-screen working; no wasi-gfx) |
| Texture / sampler / query set | ⚠️ slice D: `create-texture` / `create-sampler` / `create-view` wired; query-set still Unsupported; swapchain in render-subset |
| Indirect dispatch | ❌ |
| Pipeline layout auto | ❌ (explicit layout; from slice D layout is pipeline-layout) |
| Component Model / Wasm import | ⚠️ CM slice: `experimental:webgpu-cm@0.8.0` (still not compliant wasi:webgpu; primary acceptance = cube-cm) |
| Full `result` error lifting | ⚠️ experimental track mostly Kotlin exceptions / traps; wasi track result stubs → `ComponentVal.err` (slice F); see [errors-async.en.md](errors-async.en.md) |

## Deviation list (summary)

1. **Async:** Several WIT methods are async; P0 Host blocks synchronously (≤30s).  
2. **Mapped range:** WIT `get-with-copy` ↔ Host returns a copied `ByteArray`.  
3. **Auto layout:** Not implemented; `deviceCreateComputePipeline` requires a layout handle.  
4. **Dawn ≠ wgpu:** Validation failure messages/timing may differ from `wasi-webgpu-wasmtime`; this table + unit tests are authoritative.  
5. **Slice C:** Host/WIT standard-shaped `create-bind-group-layout` / `create-bind-group` / `create-compute-pipeline(descriptor)` / `queue.submit` wired; nested borrow needs recursive `cm-resources`-patched natives (rebuilt in guest-descriptor-cube A).  
6. **Slice D:** `create-texture` / `create-sampler` / `create-pipeline-layout` / `texture.create-view`; BGL/BG support sampler·texture entries; `compute-pipeline.layout` is **pipeline-layout**. storage-texture / texture attributes remain Unsupported; `write-texture` is `@0.8.0` — see [render-subset.en.md](render-subset.en.md).
