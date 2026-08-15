# Render / Surface 子集映射（experimental）

**中文** | [English](render-subset.en.md)

本表覆盖 CM 旋转纹理立方体上屏路径所需的 L2 / WIT 方法。仍为 **experimental**；**不是**合规 `wasi:webgpu` / wasi-gfx。

Guest 上屏（现行验收基准）：[`guest/cube-cm`](../../guest/cube-cm) → [`WasmtimeCmCube`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmCube.kt) → abi-cm → 同一 L2 → Dawn。  
历史 triangle-cm / `TriangleRenderer` 路径已移除；见 [`archive-guest-onscreen-cm-dod.md`](../scheme/archive-guest-onscreen-cm-dod.md)。

| WIT / 语义 | L2 (`WasiWebGpuHost`) | Dawn | 备注 |
|------------|----------------------|------|------|
| `create-surface-from-native-window` | `instanceCreateSurfaceFromAndroidNativeWindow` | `GPUInstance.createSurface(AndroidNativeWindow)` | Android only；Cpu Host → Unsupported |
| `surface.configure` | `surfaceConfigure` | `getCapabilities` + `configure` | 返回选用的 texture format |
| `surface.get-current-texture-view` | `surfaceGetCurrentTexture` + `textureCreateView` | `getCurrentTexture` + `createView` | L2 暴露 status；CM 封装且非 Success 时抛 Validation |
| `surface.present` | `surfacePresent` | `present` | |
| `surface.unconfigure` | `surfaceUnconfigure` | `unconfigure` | |
| `device.create-render-pipeline` | `deviceCreateRenderPipeline` | 标准 descriptor（vertex/fragment/layout/primitive/可选 depth-stencil） | `@0.7.0` slice E；`@0.8.0` depth-stencil；layout 为 pipeline-layout |
| `queue.submit` | `queueSubmit(list)` | `GPUQueue.submit` | 正式面（轨 B 应迁此）；abi-cm `queueSubmit1` **deprecated** 兼容窗口 |
| `command-encoder.begin-render-pass` | `commandEncoderBeginRenderPass` | color-attachments + 可选 depth-stencil-attachment | 正式面；`@0.7.0` slice E；`@0.8.0` depth |
| `queue.write-texture` | `queueWriteTexture` | 2D texel 上传（origin 0；depth 1） | `@0.8.0` guest-descriptor-cube B |
| `render-pass-encoder.set-bind-group` | `renderPassSetBindGroup` | 与 compute 对称 | `@0.8.0` |
| `device.create-render-pipeline-triangle` | `deviceCreateRenderPipelineTriangle` | 无 vertex buffer + TriangleList | **deprecated**（E）；历史 `vertex_index` 路径 |
| `device.create-render-pipeline-triangle-buffers` | `deviceCreateRenderPipelineTriangleBuffers` | `GPUVertexState.buffers` + TriangleList | **deprecated**（E） |
| `command-encoder.begin-render-pass-clear` | `commandEncoderBeginRenderPassClear` | `beginRenderPass` Clear/Store | **deprecated**（E）；兼容窗口 → `commandEncoderBeginRenderPass(descriptor)` |
| `render-pass-encoder.set-pipeline` | `renderPassSetPipeline` | `setPipeline` | |
| `render-pass-encoder.set-vertex-buffer` | `renderPassSetVertexBuffer` | `setVertexBuffer` | `@0.4.0`；slot + buffer + offset/size |
| `render-pass-encoder.draw` | `renderPassDraw` | `draw` | |
| `render-pass-encoder.end` | `renderPassEnd` | `end` | |

Records（`@0.4.0`）：`vertex-attribute` / `vertex-buffer-layout`；flag 别名 `vertex-format` / `vertex-step-mode`（与 `androidx.webgpu` 枚举数值一致，见 L2 `GpuVertexFormat`）。Guest 顶点缓冲见已完成的 [`semantic-hardening.md`](../scheme/semantic-hardening.md) 切片 E；立方体路径在 `@0.8.0` 上继续使用。

## Guest CM 上屏路径（现行）

```text
guest/cube-cm（cube_cm.wasm，world cube）@0.8.0
  export run-cube | init-cube / draw-frame / drop-cube
  → WasmtimeCmCube.Session / WasmtimeCmCubeAndroid / CubeCmOneShot
  → 标准 descriptor + write-texture + depth + set-bind-group + MVP
  → 同一 WasiWebGpuHost → Dawn → SurfaceView
```

- **Window**：Host 侧注入 native window（`Surface` → ANativeWindow 指针，按 u64 传）；Guest 只持 `surface` resource，不创建 window
- **One-shot**（`run-cube`）：configure → draw → present → unconfigure；仪器默认路径
- **顶点 / 纹理：** Guest `create-buffer` + `write-buffer` / `write-texture` → 标准 `create-render-pipeline` → `set-vertex-buffer` / `set-bind-group` + `draw`
- **帧循环**（宿主驱动）：`init-cube` → 循环 `draw-frame` → `drop-cube`；Demo `CubeCmOneShot`（复用 Session + `releaseLifetimeSafetyNets` + `releaseAllGpuObjects`）；线程约定见 [`threading.md`](threading.md)
- **生命周期**：swapchain View↔Texture 帧等价 `tryDrop`；**仍非真 WIT dtor**（[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) §4）
- **验收**：仪器 `WasmtimeCmCubeInstrumentedTest`（one-shot + 8 帧 + 同 Session `runCube` ×3）；Demo 手点旋转纹理立方体
- **桌面**：Cpu fake surface 支持 AbiCm 多帧寿命单测；完整 CM Guest 上屏仍需 Android Surface / Dawn
- **u64 注意**：`window-handle` 高位可超 `Long.MAX_VALUE`；wasmtime4j `ConcurrentCallCodec` 须按无符号解析（android-demo 覆盖，见 [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)）

## 轨 B 正式 Host 面（本仓先改）

L2 **已有**下列正式面；轨 B 应从 deprecated / 快捷路径迁过来（本仓不改轨 B 源码）：

| 快捷 / deprecated | 正式面 |
|-------------------|--------|
| `begin-render-pass-clear` / `commandEncoderBeginRenderPassClear` | `commandEncoderBeginRenderPass(RenderPassDescriptor)` |
| abi-cm / abi-mvp `queueSubmit1` | `queueSubmit(list)` |
| 一步 `surface-get-current-texture-view` | `surfaceGetCurrentTexture` + `textureCreateView` |

AbiCm 仍提供一步 `surfaceGetCurrentTextureView` 给 Guest CM cube（内部两步 + 帧对记账）。clear helper / `queueSubmit1` **保留兼容窗口**，KDoc 标 deprecated。

本轮**不**新增 adapter `features` / `limits` / `info`、`deviceDestroy`、`on-submitted-work-done`。

### texture / view 生命周期短契约

仍 **非真 WIT dtor**。帧对释放约定：

1. **Present 之后**再 `tryDrop` View，然后 `tryDrop` Texture（勿在 submit/present 前关 swapchain）。
2. AbiCm 一步 helper 在 present / 下次 acquire / unconfigure 时自动配对 `tryDrop`。
3. 两步路径（轨 B 提案名）须自行按上序释放；可用 `dropRep` / `tryDrop`。
4. Session 交接：`releaseLifetimeSafetyNets`（配对 + encoder 孤儿），再 `releaseAllGpuObjects`（Demo / 仪器；不断 Instance / linker — D6）。
5. Guest 持有的 albedo / depth **不得**被 `releaseFrameResources` 扫掉。

## 明确不做（本切片）

| 区域 | 状态 |
|------|------|
| Guest CM 上屏（cube-cm） | ✅ 已通（见上节） |
| wasi-gfx 上屏 | ❌ |
| 通用 `create-render-pipeline` / `begin-render-pass` descriptor | ✅ `@0.7.0`；depth `@0.8.0` |
| depth（最小 depth24plus） | ✅ `@0.8.0`（无 MSAA / stencil 面） |
| `write-texture` / render `set-bind-group` | ✅ `@0.8.0` |
| MSAA | ❌ |
| Canvas / 多 window 抽象 | ❌ |
| `abi-mvp` 扁平 render import | ⚠️ 子集（engineering-handoff B）：surface configure/get-view/present/unconfigure、triangle render-pipeline、begin-pass clear / color+depth、set-pipeline / set-bind-group / set-vertex-buffer / draw / end、create-texture-2d / write-texture；**主验收仍 CM cube** |

## 线程

见 [`threading.md`](threading.md)：同一 Host 上 surface 与 submit 同渲染线程。
