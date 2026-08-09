# WIT ↔ Dawn mapping (P0 compute subset)

[中文](compute-subset.md) | **English**

> **Status:** experimental / host-only  
> **WIT pin:** `wasi:webgpu/webgpu@0.3.0-rc.2` (see [`wit/`](../../wit/))  
> **Dawn:** `androidx.webgpu:webgpu:1.0.0-alpha05`

This table covers only methods needed for the P0 acceptance path. Full WebGPU / rendering / on-screen is **out of scope**.

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
| `gpu-device.create-bind-group-layout` | `deviceCreateBindGroupLayout` | `createBindGroupLayout` | ✅ buffer binding only |
| `gpu-device.create-bind-group` | `deviceCreateBindGroup` | `createBindGroup` | ✅ buffer resources only |
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
| Render pass / surface / canvas | ⚠️ See [render-subset.en.md](render-subset.en.md) (experimental; not Guest/wasi-gfx) |
| Texture / sampler / query set | ⚠️ Swapchain texture/view only (render-subset); no sampler/query |
| Indirect dispatch | ❌ |
| Pipeline layout auto | ❌ (explicit layout) |
| Component Model / Wasm import | ⚠️ CM slice: `experimental:webgpu-cm@0.7.0` (still not compliant wasi:webgpu; slice D adds texture/sampler/pipeline-layout) |
| Full `result` error lifting | ⚠️ currently Kotlin exceptions; see [errors-async.en.md](errors-async.en.md) |

## Deviation list (summary)

1. **Async:** Several WIT methods are async; P0 Host blocks synchronously (≤30s).  
2. **Mapped range:** WIT `get-with-copy` ↔ Host returns a copied `ByteArray`.  
3. **Auto layout:** Not implemented; `deviceCreateComputePipeline` requires a layout handle.  
4. **Dawn ≠ wgpu:** Validation failure messages/timing may differ from `wasi-webgpu-wasmtime`; this table + unit tests are authoritative.  
5. **Slice C:** Host/WIT standard-shaped `create-bind-group-layout` / `create-bind-group` / `create-compute-pipeline(descriptor)` / `queue.submit` wired; Guest on device: layout descriptor + nested-borrow helpers `*3` / `*-bgl` / `submit1` until `cm-resources` recursive patch rebuilds `.so`.
6. **Slice D:** `create-texture` / `create-sampler` / `create-pipeline-layout` / `texture.create-view`; BGL/BG support sampler·texture entries; `compute-pipeline.layout` is **pipeline-layout**. storage-texture / write-texture / texture attributes remain Unsupported.
