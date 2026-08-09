# Render / Surface subset mapping (experimental)

[中文](render-subset.md) | **English**

This table covers methods needed for the red-triangle on-screen path. Still **experimental**; **not** compliant `wasi:webgpu` / wasi-gfx.

Reference: [`TriangleRenderer`](../../android-demo/src/main/java/io/github/fenriliuguang/wasi/webgpu/demo/onscreen/TriangleRenderer.kt) → `WasiWebGpuHost` → Dawn.
Guest on-screen: [`guest/triangle-cm`](../../guest/triangle-cm) → [`WasmtimeCmTriangle`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmTriangle.kt) → abi-cm → same L2 → Dawn.

| WIT / semantics | L2 (`WasiWebGpuHost`) | Dawn | Notes |
|-----------------|----------------------|------|-------|
| `create-surface-from-native-window` | `instanceCreateSurfaceFromAndroidNativeWindow` | `GPUInstance.createSurface(AndroidNativeWindow)` | Android only; Cpu Host → Unsupported |
| `surface.configure` | `surfaceConfigure` | `getCapabilities` + `configure` | Returns chosen texture format |
| `surface.get-current-texture-view` | `surfaceGetCurrentTexture` + `textureCreateView` | `getCurrentTexture` + `createView` | L2 exposes status; CM wraps and throws Validation if not Success |
| `surface.present` | `surfacePresent` | `present` | |
| `surface.unconfigure` | `surfaceUnconfigure` | `unconfigure` | |
| `device.create-render-pipeline` | `deviceCreateRenderPipeline` | standard descriptor (vertex/fragment/layout/primitive/optional depth-stencil) | `@0.7.0` slice E; `@0.8.0` depth-stencil; layout is pipeline-layout |
| `command-encoder.begin-render-pass` | `commandEncoderBeginRenderPass` | color-attachments + optional depth-stencil-attachment | `@0.7.0` slice E; `@0.8.0` depth |
| `queue.write-texture` | `queueWriteTexture` | 2D texel upload (origin 0; depth 1) | `@0.8.0` guest-descriptor-cube B |
| `render-pass-encoder.set-bind-group` | `renderPassSetBindGroup` | mirrors compute | `@0.8.0` |
| `device.create-render-pipeline-triangle` | `deviceCreateRenderPipelineTriangle` | no vertex buffers + TriangleList | **deprecated** (E); `vertex_index` path |
| `device.create-render-pipeline-triangle-buffers` | `deviceCreateRenderPipelineTriangleBuffers` | `GPUVertexState.buffers` + TriangleList | **deprecated** (E); device Guest interim (nested borrow) |
| `command-encoder.begin-render-pass-clear` | `commandEncoderBeginRenderPassClear` | `beginRenderPass` Clear/Store | **deprecated** (E); device Guest interim |
| `render-pass-encoder.set-pipeline` | `renderPassSetPipeline` | `setPipeline` | |
| `render-pass-encoder.set-vertex-buffer` | `renderPassSetVertexBuffer` | `setVertexBuffer` | `@0.4.0`; slot + buffer + offset/size |
| `render-pass-encoder.draw` | `renderPassDraw` | `draw` | |
| `render-pass-encoder.end` | `renderPassEnd` | `end` | |

Records (`@0.4.0`): `vertex-attribute` / `vertex-buffer-layout`; flag aliases `vertex-format` / `vertex-step-mode` (same numeric values as `androidx.webgpu`; see L2 `GpuVertexFormat`). Guest vertex-buffer acceptance: completed slice E in [`semantic-hardening.en.md`](../scheme/semantic-hardening.en.md).

## Guest CM on-screen path (working)

```text
guest/triangle-cm (triangle_cm.wasm, world triangle)
  export run-triangle | init-triangle / draw-frame / drop-triangle
  → WasmtimeCmTriangle.Session (L1) / WasmtimeCmTriangleAndroid
  → Wasmtime ComponentLinker + abi-cm (imports from the table above)
  → same WasiWebGpuHost → Dawn → SurfaceView

guest/cube-cm (cube_cm.wasm, world cube) @0.8.0
  export run-cube | init-cube / draw-frame / drop-cube
  → WasmtimeCmCube.Session / WasmtimeCmCubeAndroid / CubeCmOneShot
  → standard descriptors + write-texture + depth + set-bind-group + MVP
```

- **Window**: the Host side injects the native window (`Surface` → ANativeWindow pointer, passed as u64); the Guest only holds a `surface` resource and never creates windows
- **One-shot** (`run-triangle` / `run-cube`): configure → draw → present → unconfigure; default instrumented path
- **Vertex buffer:** Guest `create-buffer` + `write-buffer` → standard `create-render-pipeline` → `set-vertex-buffer` + `draw`
- **Frame loop** (host-driven): `init-*` → loop `draw-frame` → `drop-*`; Demo `TriangleCmOneShot` / `CubeCmOneShot` (reuse Session + `releaseAllGpuObjects`); threading: [`threading.en.md`](threading.en.md)
- **Acceptance**: instrumented triangle wave2 + cube wave3 (separate processes); Demo pause→CM→resume
- **Desktop**: without an Android Surface, `CpuWasiWebGpuHost` → Unsupported and related unit tests skip (same gating as CM compute)
- **u64 caveat**: `window-handle` high bits can exceed `Long.MAX_VALUE`; wasmtime4j `ConcurrentCallCodec` must parse it unsigned (overlaid in android-demo, see [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md))

## Explicitly out of scope (this slice)

| Area | Status |
|------|--------|
| Guest CM on-screen (triangle-cm / cube-cm) | ✅ working (see above) |
| wasi-gfx on-screen | ❌ |
| Generic `create-render-pipeline` / `begin-render-pass` descriptor | ✅ `@0.7.0`; depth `@0.8.0` |
| Minimal depth24plus | ✅ `@0.8.0` (no MSAA / stencil faces) |
| `write-texture` / render `set-bind-group` | ✅ `@0.8.0` |
| MSAA | ❌ |
| Canvas / multi-window abstraction | ❌ |
| `abi-mvp` flat render imports | ❌ (P1 remains compute-only) |

## Threading

See [`threading.en.md`](threading.en.md): surface and submit on the same render thread per Host instance.
