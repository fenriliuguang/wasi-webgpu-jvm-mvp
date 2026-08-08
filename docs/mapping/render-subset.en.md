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
| `device.create-render-pipeline-triangle` | `deviceCreateRenderPipelineTriangle` | no vertex buffers + TriangleList | `vertex_index` path; requires `vs_main` / `fs_main` |
| `device.create-render-pipeline-triangle-buffers` | `deviceCreateRenderPipelineTriangleBuffers` | `GPUVertexState.buffers` + TriangleList | `@0.4.0`; `list<vertex-buffer-layout>` |
| `command-encoder.begin-render-pass-clear` | `commandEncoderBeginRenderPassClear` | `beginRenderPass` Clear/Store | Single color attachment |
| `render-pass-encoder.set-pipeline` | `renderPassSetPipeline` | `setPipeline` | |
| `render-pass-encoder.set-vertex-buffer` | `renderPassSetVertexBuffer` | `setVertexBuffer` | `@0.4.0`; slot + buffer + offset/size |
| `render-pass-encoder.draw` | `renderPassDraw` | `draw` | |
| `render-pass-encoder.end` | `renderPassEnd` | `end` | |

Records (`@0.4.0`): `vertex-attribute` / `vertex-buffer-layout`; flag aliases `vertex-format` / `vertex-step-mode` (same numeric values as `androidx.webgpu`; see L2 `GpuVertexFormat`). Guest vertex-buffer acceptance is phase E — [`semantic-hardening.en.md`](../scheme/semantic-hardening.en.md).

## Guest CM on-screen path (working)

```text
guest/triangle-cm (triangle_cm.wasm, world triangle)
  export run-triangle | init-triangle / draw-frame / drop-triangle
  → WasmtimeCmTriangle.Session (L1) / WasmtimeCmTriangleAndroid
  → Wasmtime ComponentLinker + abi-cm (imports from the table above)
  → same WasiWebGpuHost → Dawn → SurfaceView
```

- **Window**: the Host side injects the native window (`Surface` → ANativeWindow pointer, passed as u64); the Guest only holds a `surface` resource and never creates windows
- **One-shot** (`run-triangle`): configure → draw → present → unconfigure; default instrumented path
- **Vertex buffer (phase E)**: Guest `create-buffer` + `write-buffer` → `create-render-pipeline-triangle-buffers` → `set-vertex-buffer` + `draw(3)`; shader `@location(0)`
- **Frame loop** (host-driven): `init-triangle` → loop `draw-frame` → `drop-triangle`; Demo `TriangleCmOneShot.runFrameLoopAndAwait` (reuse Session + `releaseAllGpuObjects`); threading: [`threading.en.md`](threading.en.md)
- **Acceptance**: instrumented one-shot + `cmGuestRepeatTriangleReusesSession`; Demo pause→CM→resume; device regression D1–D6 in [`demo-cm-stability-blockers.md`](../scheme/demo-cm-stability-blockers.md) (ZH)
- **Desktop**: without an Android Surface, `CpuWasiWebGpuHost` → Unsupported and related unit tests skip (same gating as CM compute)
- **u64 caveat**: `window-handle` high bits can exceed `Long.MAX_VALUE`; wasmtime4j `ConcurrentCallCodec` must parse it unsigned (overlaid in android-demo, see [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md)); P6 Demo stability closed (same archive)

## Explicitly out of scope (this slice)

| Area | Status |
|------|--------|
| Guest CM on-screen (triangle-cm one-shot draw) | ✅ working (see above) |
| wasi-gfx on-screen | ❌ |
| General render-pipeline descriptor / MSAA / depth | ❌ |
| Canvas / multi-window abstraction | ❌ |
| `abi-mvp` flat render imports | ❌ (P1 remains compute-only) |

## Threading

See [`threading.en.md`](threading.en.md): surface and submit on the same render thread per Host instance.
