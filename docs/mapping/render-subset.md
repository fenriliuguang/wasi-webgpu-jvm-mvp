# Render / Surface 子集映射（experimental）

**中文** | [English](render-subset.en.md)

本表覆盖红三角上屏路径所需的 L2 / WIT 方法。仍为 **experimental**；**不是**合规 `wasi:webgpu` / wasi-gfx。

对照实现：[`TriangleRenderer`](../../android-demo/src/main/java/io/github/fenriliuguang/wasi/webgpu/demo/onscreen/TriangleRenderer.kt) → `WasiWebGpuHost` → Dawn。

| WIT / 语义 | L2 (`WasiWebGpuHost`) | Dawn | 备注 |
|------------|----------------------|------|------|
| `create-surface-from-native-window` | `instanceCreateSurfaceFromAndroidNativeWindow` | `GPUInstance.createSurface(AndroidNativeWindow)` | Android only；Cpu Host → Unsupported |
| `surface.configure` | `surfaceConfigure` | `getCapabilities` + `configure` | 返回选用的 texture format |
| `surface.get-current-texture-view` | `surfaceGetCurrentTexture` + `textureCreateView` | `getCurrentTexture` + `createView` | L2 暴露 status；CM 封装且非 Success 时抛 Validation |
| `surface.present` | `surfacePresent` | `present` | |
| `surface.unconfigure` | `surfaceUnconfigure` | `unconfigure` | |
| `device.create-render-pipeline-triangle` | `deviceCreateRenderPipelineTriangle` | empty layout + TriangleList | 要求 `vs_main` / `fs_main` |
| `command-encoder.begin-render-pass-clear` | `commandEncoderBeginRenderPassClear` | `beginRenderPass` Clear/Store | 单 color attachment |
| `render-pass-encoder.set-pipeline` | `renderPassSetPipeline` | `setPipeline` | |
| `render-pass-encoder.draw` | `renderPassDraw` | `draw` | |
| `render-pass-encoder.end` | `renderPassEnd` | `end` | |

## 明确不做（本切片）

| 区域 | 状态 |
|------|------|
| Guest / wasi-gfx 上屏 | ❌ |
| 通用 render-pipeline descriptor / MSAA / depth | ❌ |
| Canvas / 多 window 抽象 | ❌ |
| `abi-mvp` 扁平 render import | ❌（P1 仍仅 compute） |

## 线程

见 [`threading.md`](threading.md)：同一 Host 上 surface 与 submit 同渲染线程。
