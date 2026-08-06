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
| `device.create-render-pipeline-triangle` | `deviceCreateRenderPipelineTriangle` | empty layout + TriangleList | Requires `vs_main` / `fs_main` |
| `command-encoder.begin-render-pass-clear` | `commandEncoderBeginRenderPassClear` | `beginRenderPass` Clear/Store | Single color attachment |
| `render-pass-encoder.set-pipeline` | `renderPassSetPipeline` | `setPipeline` | |
| `render-pass-encoder.draw` | `renderPassDraw` | `draw` | |
| `render-pass-encoder.end` | `renderPassEnd` | `end` | |

## Guest CM on-screen path (working)

```text
guest/triangle-cm (triangle_cm.wasm, world triangle)
  export run-triangle(window-handle: u64, width: u32, height: u32)
  → WasmtimeCmTriangle (L1) / WasmtimeCmTriangleAndroid (loads from assets)
  → Wasmtime ComponentLinker + abi-cm (imports from the table above)
  → same WasiWebGpuHost → Dawn → SurfaceView
```

- **Window**: the Host side injects the native window (`Surface` → ANativeWindow pointer, passed as u64); the Guest only holds a `surface` resource and never creates windows
- **Guest sequence** (`guest/triangle-cm/src/lib.rs`): `create-surface-from-native-window` → `configure` → `create-render-pipeline-triangle` → `get-current-texture-view` → `begin-render-pass-clear` → `set-pipeline` / `draw(3)` / `end` → `submit1` → `present` → `unconfigure`; shader matches the L2 `TriangleRenderer`
- **Acceptance shape**: one-shot draw; instrumented test `WasmtimeCmTriangleInstrumentedTest` green (vivo V2458A / Mali)
- **Desktop**: without an Android Surface, `CpuWasiWebGpuHost` → Unsupported and related unit tests skip (same gating as CM compute)
- **u64 caveat**: `window-handle` high bits can exceed `Long.MAX_VALUE`; wasmtime4j `ConcurrentCallCodec` must parse it unsigned (overlaid in android-demo, see [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md)); manual Demo-button stability leftover: [`guest-onscreen-cm-blockers.md`](../scheme/guest-onscreen-cm-blockers.md) P6

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
