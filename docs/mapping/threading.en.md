# Threading model

[中文](threading.md) | **English**

## Conclusion (compute Host)

- Host call convention is **single-threaded**: do not call the same `WasiWebGpuHost` instance concurrently.  
- Dawn async callbacks run on an inline `Executor` inside the Host; public APIs wait synchronously.  
- Host background-polls `GPUInstance.processEvents()` (aligned with `androidx.webgpu.helper` event pump).  
- Android demo / instrumented tests should call the Host on a **background thread** to avoid blocking the main thread.

## Surface / render (L2)

- On one `WasiWebGpuHost` instance: `surfaceConfigure` / `surfaceGetCurrentTexture` / `surfacePresent` and `queueSubmit` **must run on the same render thread**.  
- `android-demo` `TriangleRenderer` owns that Host on a dedicated `HandlerThread` (`webgpu-triangle`); `SurfaceHolder` callbacks only post to that thread; the **UI thread does not** call WebGPU.  
- The frame loop `postDelayed`s on the same render thread; `surfaceDestroyed` stops the loop and `surfaceUnconfigure`s / `drop`s the Surface.  
- Separate `DawnWasiWebGpuHost` instance (and `GPUInstance`) from vector-add; they do not share.

## Surface / render (CM Guest)

- The CM path uses a **separate** `DawnWasiWebGpuHost` + `HandlerThread` (`webgpu-triangle-cm`); do **not** share it across threads with the L2 Host.  
- Host-driven frame loop: same thread `init-triangle` → loop `draw-frame` → `drop-triangle` (see `WasmtimeCmTriangle.Session.runFrameLoop`).  
- Demo: `pauseSurfaceAndAwait` before CM (L2 `teardownGpu`); after CM `drop-triangle` → `releaseAllGpuObjects` (keep Instance/Session) → settle → `resumeSurfaceAndAwait`.  
- **Demo taps**: reuse Host + Session; each press `releaseAllGpuObjects` to free ANativeWindow (avoid back-to-back linker teardown). See [`demo-cm-stability-blockers.md`](../scheme/demo-cm-stability-blockers.md).  
- **Instrumented**: reuse Session (`cmGuestRepeatTriangleReusesSession`).  
- Frame resources: `releaseFrameResources` after present (Guest WIT destructors unwired).

## Instance / Device / Queue

| Object | Convention |
|--------|------------|
| `GPUInstance` | One per Host; released on `close()` |
| `GPUDevice` / `GPUQueue` | Held by handle table; best-effort `close` on drop/`close` |
| `submit` / `mapAsync` / present | Started on the render thread (or single-threaded Host caller); completion on callback executor / same thread |

## Later

- If the L1 Runtime enters the Host from multiple threads, add locking or explicit thread affinity.
