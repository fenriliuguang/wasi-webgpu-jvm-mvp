# WIT ↔ Dawn 映射表（P0 compute 子集）

**中文** | [English](compute-subset.en.md)

> **状态：** experimental / host-only  
> **WIT 钉定：** `wasi:webgpu/webgpu@0.3.0-rc.2`（见 [`wit/`](../../wit/)）  
> **Dawn：** `androidx.webgpu:webgpu:1.0.0-alpha05`

本表只覆盖 P0 验收路径所需方法。完整 WebGPU / 渲染 / 上屏 **不在范围**。

## 图例

| 标记 | 含义 |
|------|------|
| ✅ | 直接对应 |
| ⚠️ | 语义偏差 / 同步包装 / 拷贝边界 |
| ❌ | P0 不做 |

## Instance / Adapter / Device

| WIT | L2 (`WasiWebGpuHost`) | Dawn (`androidx.webgpu`) | 备注 |
|-----|------------------------|---------------------------|------|
| `gpu.request-adapter` | `requestAdapter` | `GPUInstance.requestAdapter` | ⚠️ 异步→P0 同步等待 |
| `gpu-adapter.request-device` | `adapterRequestDevice` | `GPUAdapter.requestDevice` | ⚠️ 同上；descriptor 子集 |
| `gpu-device.get-queue` | `deviceGetQueue` | `GPUDevice.queue` | ✅ |

## Buffer / Shader / Bindings / Pipeline

| WIT | L2 | Dawn | 备注 |
|-----|----|------|------|
| `gpu-device.create-buffer` | `deviceCreateBuffer` | `GPUDevice.createBuffer` | ✅ usage flags 对齐 WebGPU；CM `0.2.0` 传 `buffer-descriptor`（含 mapped/label） |
| `gpu-device.create-shader-module` | `deviceCreateShaderModule` | `GPUDevice.createShaderModule` + WGSL | ✅ 仅 WGSL |
| `gpu-device.create-bind-group-layout` | `deviceCreateBindGroupLayout` | `createBindGroupLayout` | ✅ buffer binding only |
| `gpu-device.create-bind-group` | `deviceCreateBindGroup` | `createBindGroup` | ✅ buffer resources only |
| `gpu-device.create-compute-pipeline` | `deviceCreateComputePipeline` | `createComputePipeline` | ⚠️ P0 要求显式 layout（不做 auto） |
| `gpu-device.create-command-encoder` | `deviceCreateCommandEncoder` | `createCommandEncoder` | ✅ |

## Compute pass / Queue / Map

| WIT | L2 | Dawn | 备注 |
|-----|----|------|------|
| `gpu-command-encoder.begin-compute-pass` | `commandEncoderBeginComputePass` | `beginComputePass` | ✅ |
| `gpu-compute-pass-encoder.set-pipeline` | `computePassSetPipeline` | `setPipeline` | ✅ |
| `gpu-compute-pass-encoder.set-bind-group` | `computePassSetBindGroup` | `setBindGroup` | ✅ |
| `gpu-compute-pass-encoder.dispatch-workgroups` | `computePassDispatchWorkgroups` | `dispatchWorkgroups` | ✅ |
| `gpu-compute-pass-encoder.end` | `computePassEnd` | `end` | ✅ pass handle 在 end 后 drop |
| `gpu-command-encoder.copy-buffer-to-buffer` | `commandEncoderCopyBufferToBuffer` | `copyBufferToBuffer` | ✅ 回读路径需要 |
| `gpu-command-encoder.finish` | `commandEncoderFinish` | `finish` | ✅ encoder handle drop |
| `gpu-queue.write-buffer-with-copy` | `queueWriteBuffer` | `GPUQueue.writeBuffer` | ⚠️ 始终走 host 拷贝 |
| `gpu-queue.submit` | `queueSubmit` | `submit` | ✅ |
| `gpu-buffer.map-async` | `bufferMapAsync` | `mapAsync` | ⚠️ 异步→同步等待；CM `0.2.0` 传 `map-mode-flags` |
| `gpu-buffer.get-mapped-range-get-with-copy` | `bufferGetMappedRange` | `getConstMappedRange` + copy | ⚠️ 返回 `ByteArray` 拷贝 |
| `gpu-buffer.unmap` | `bufferUnmap` | `unmap` | ✅ |
| resource `drop` | `drop` | `close` / 句柄表移除 | ✅ |

## 明确不做（P0）

| 区域 | 状态 |
|------|------|
| Render pass / surface / canvas | ❌ |
| Texture / sampler / query set | ❌ |
| Indirect dispatch | ❌ |
| Pipeline layout auto | ❌（显式 layout） |
| Component Model / Wasm import | ⚠️ CM 切片：`experimental:webgpu-cm@0.2.0`（buffer-descriptor；仍非合规 wasi:webgpu） |
| 完整错误 `result` 抬升 | ⚠️ 现为 Kotlin 异常；见 [errors-async.md](errors-async.md) / [EN](errors-async.en.md) |

## 偏差列表（摘要）

1. **Async：** WIT 若干方法为 async；P0 Host 同步阻塞等待（≤30s）。  
2. **Mapped range：** WIT `get-with-copy` ↔ Host 返回拷贝后的 `ByteArray`。  
3. **Auto layout：** 未实现；`deviceCreateComputePipeline` 要求 layout handle。  
4. **Dawn ≠ wgpu：** 校验失败消息/时机可能与 `wasi-webgpu-wasmtime` 不同，以本表 + 单测为准。
