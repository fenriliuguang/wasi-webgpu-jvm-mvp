# Render / Surface subset mapping (experimental)

[中文](render-subset.md) | **English**

This table covers methods needed for the red-triangle on-screen path. Still **experimental**; **not** compliant `wasi:webgpu` / wasi-gfx.

Reference: [`TriangleRenderer`](../../android-demo/src/main/java/io/github/fenriliuguang/wasi/webgpu/demo/onscreen/TriangleRenderer.kt) → `WasiWebGpuHost` → Dawn.

| WIT / semantics | L2 (`WasiWebGpuHost`) | Dawn | Notes |
|-----------------|----------------------|------|-------|
| `create-surface-from-native-window` | `instanceCreateSurfaceFromAndroidNativeWindow` | `GPUInstance.createSurface(AndroidNativeWindow)` | Android only; Cpu Host → Unsupported |
| `surface.configure` | `surfaceConfigure` | `getCapabilities` + `configure` | Returns chosen texture format |
| `surface.get-current-texture-view` | `surfaceGetCurrentTexture` + `textureCreateView` | `getCurrentTexture` + `createView` | L2 exposes status; CM wraps and throws Validation if not Success |
| `surface.present` | `surfacePresent` | `present` | |
| `surface.unconfigure` | `surfaceUnconfigure` | `unconfigure` | |
| `device.create-render-pipeline-triangle` | `deviceCreateRenderPipelineTriangle` | empty layout + TriangleList | Requires `vs_main` / `fs_main` |
| `command-encoder.begin-render-pass-clear` | `commandEncoderBeginRenderPassClear` | `beginRenderPass` Clear/Store | Single color attachment |
| `render-pass-encoder.set-pipeline` | `renderPassSetPipeline` | `setPipeline` | |
| `render-pass-encoder.draw` | `renderPassDraw` | `draw` | |
| `render-pass-encoder.end` | `renderPassEnd` | `end` | |

## Explicitly out of scope (this slice)

| Area | Status |
|------|--------|
| Guest / wasi-gfx on-screen | ❌ |
| General render-pipeline descriptor / MSAA / depth | ❌ |
| Canvas / multi-window abstraction | ❌ |
| `abi-mvp` flat render imports | ❌ (P1 remains compute-only) |

## Threading

See [`threading.en.md`](threading.en.md): surface and submit on the same render thread per Host instance.
