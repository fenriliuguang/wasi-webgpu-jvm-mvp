# Threading model

[中文](threading.md) | **English**

## Conclusion (compute Host)

- Host call convention is **single-threaded**: do not call the same `WasiWebGpuHost` instance concurrently.  
- Dawn async callbacks run on an inline `Executor` inside the Host; public APIs wait synchronously.  
- Host background-polls `GPUInstance.processEvents()` (aligned with `androidx.webgpu.helper` event pump).  
- Android demo / instrumented tests should call the Host on a **background thread** to avoid blocking the main thread.

## Surface / render (L2 APIs)

- On one `WasiWebGpuHost` instance: `surfaceConfigure` / `surfaceGetCurrentTexture` / `surfacePresent` and `queueSubmit` **must run on the same render thread**.  
- The Demo **no longer** keeps a separate L2 `TriangleRenderer` on-screen path; the Surface is consumed only by the CM Guest path (below).  
- L2 surface APIs remain callable by hosts that manage their own thread affinity.

## Surface / render (CM Guest — current Demo / instrumented)

- The CM path uses `DawnWasiWebGpuHost` + `HandlerThread` (`webgpu-cube-cm`); the **UI thread does not** call WebGPU.  
- Host-driven frame loop: same thread `init-cube` → loop `draw-frame` → `drop-cube` (see `WasmtimeCmCube.Session.runFrameLoop`).  
- Demo `CubeCmOneShot`: reuse Host + Session; `releaseAllGpuObjects` around each press to free ANativeWindow (avoid back-to-back linker teardown); Session ends with `releaseLifetimeSafetyNets`.  
- **Instrumented**: `WasmtimeCmCubeInstrumentedTest` (reuse Session).  
- Frame resources: View↔Texture `tryDrop` on present / unconfigure / Session end + `releaseFrameResources`; **still not true WIT dtor** ([`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §4).  
- Historical L2↔CM pause/resume and two-wave triangle instrumentation: see [`demo-cm-stability-blockers.md`](../scheme/demo-cm-stability-blockers.md) (ZH).

## Instance / Device / Queue

| Object | Convention |
|--------|------------|
| `GPUInstance` | One per Host; released on `close()` |
| `GPUDevice` / `GPUQueue` | Held by handle table; best-effort `close` on drop/`close` |
| `submit` / `mapAsync` / present | Started on the render thread (or single-threaded Host caller); completion on callback executor / same thread |

## Later

- If the L1 Runtime enters the Host from multiple threads, add locking or explicit thread affinity.
