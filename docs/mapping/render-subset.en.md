# Render / Surface subset mapping (experimental)

[中文](render-subset.md) | **English**

This table covers methods needed for the CM rotating textured cube on-screen path. Still **experimental**; **not** compliant `wasi:webgpu` / wasi-gfx.

Guest on-screen (current acceptance baseline): [`guest/cube-cm`](../../guest/cube-cm) → [`WasmtimeCmCube`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmCube.kt) → abi-cm → same L2 → Dawn.  
Historical triangle-cm / `TriangleRenderer` paths removed; see [`archive-guest-onscreen-cm-dod.en.md`](../scheme/archive-guest-onscreen-cm-dod.en.md).

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
| `device.create-render-pipeline-triangle` | `deviceCreateRenderPipelineTriangle` | no vertex buffers + TriangleList | **deprecated** (E); historical `vertex_index` path |
| `device.create-render-pipeline-triangle-buffers` | `deviceCreateRenderPipelineTriangleBuffers` | `GPUVertexState.buffers` + TriangleList | **deprecated** (E) |
| `command-encoder.begin-render-pass-clear` | `commandEncoderBeginRenderPassClear` | `beginRenderPass` Clear/Store | **deprecated** (E) |
| `render-pass-encoder.set-pipeline` | `renderPassSetPipeline` | `setPipeline` | |
| `render-pass-encoder.set-vertex-buffer` | `renderPassSetVertexBuffer` | `setVertexBuffer` | `@0.4.0`; slot + buffer + offset/size |
| `render-pass-encoder.draw` | `renderPassDraw` | `draw` | |
| `render-pass-encoder.end` | `renderPassEnd` | `end` | |

Records (`@0.4.0`): `vertex-attribute` / `vertex-buffer-layout`; flag aliases `vertex-format` / `vertex-step-mode` (same numeric values as `androidx.webgpu`; see L2 `GpuVertexFormat`). Guest vertex-buffer work: completed slice E in [`semantic-hardening.en.md`](../scheme/semantic-hardening.en.md); cube path continues on `@0.8.0`.

## Guest CM on-screen path (current)

```text
guest/cube-cm (cube_cm.wasm, world cube) @0.8.0
  export run-cube | init-cube / draw-frame / drop-cube
  → WasmtimeCmCube.Session / WasmtimeCmCubeAndroid / CubeCmOneShot
  → standard descriptors + write-texture + depth + set-bind-group + MVP
  → same WasiWebGpuHost → Dawn → SurfaceView
```

- **Window**: the Host side injects the native window (`Surface` → ANativeWindow pointer, passed as u64); the Guest only holds a `surface` resource and never creates windows
- **One-shot** (`run-cube`): configure → draw → present → unconfigure; default instrumented path
- **Vertex / texture:** Guest `create-buffer` + `write-buffer` / `write-texture` → standard `create-render-pipeline` → `set-vertex-buffer` / `set-bind-group` + `draw`
- **Frame loop** (host-driven): `init-cube` → loop `draw-frame` → `drop-cube`; Demo `CubeCmOneShot` (reuse Session + `releaseLifetimeSafetyNets` + `releaseAllGpuObjects`); threading: [`threading.en.md`](threading.en.md)
- **Lifetime**: swapchain View↔Texture frame-equivalent `tryDrop`; **still not true WIT dtor** ([`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §4)
- **Acceptance**: instrumented `WasmtimeCmCubeInstrumentedTest`; Demo tap shows slowly rotating textured cube
- **Desktop**: Cpu fake surface supports AbiCm multi-frame lifetime unit tests; full CM Guest on-screen still needs Android Surface / Dawn
- **u64 caveat**: `window-handle` high bits can exceed `Long.MAX_VALUE`; wasmtime4j `ConcurrentCallCodec` must parse it unsigned (overlaid in android-demo, see [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md))

## Explicitly out of scope (this slice)

| Area | Status |
|------|--------|
| Guest CM on-screen (cube-cm) | ✅ working (see above) |
| wasi-gfx on-screen | ❌ |
| Generic `create-render-pipeline` / `begin-render-pass` descriptor | ✅ `@0.7.0`; depth `@0.8.0` |
| Minimal depth24plus | ✅ `@0.8.0` (no MSAA / stencil faces) |
| `write-texture` / render `set-bind-group` | ✅ `@0.8.0` |
| MSAA | ❌ |
| Canvas / multi-window abstraction | ❌ |
| `abi-mvp` flat render imports | ⚠️ subset (engineering-handoff B): surface configure/get-view/present/unconfigure, triangle render-pipeline, begin-pass clear / color+depth, set-pipeline / set-bind-group / set-vertex-buffer / draw / end, create-texture-2d / write-texture; **primary acceptance remains CM cube** |

## Threading

See [`threading.en.md`](threading.en.md): surface and submit on the same render thread per Host instance.
