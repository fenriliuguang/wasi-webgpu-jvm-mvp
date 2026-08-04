# Threading model (P0 notes)

[中文](threading.md) | **English**

## Conclusion (P0 / compute Host)

- Host call convention is **single-threaded**: do not call the same `WasiWebGpuHost` instance concurrently.  
- Dawn async callbacks run on an inline `Executor` inside the Host; public APIs wait synchronously.  
- Host background-polls `GPUInstance.processEvents()` (aligned with `androidx.webgpu.helper` event pump).  
- Android demo / instrumented tests should call the Host on a **background thread** to avoid blocking the main thread.

## On-screen demo (Kotlin, not via L2)

- `android-demo` `TriangleRenderer` owns Instance/Device/Surface on a dedicated `HandlerThread` (`webgpu-triangle`).  
- `SurfaceHolder` callbacks only post work to that thread; the **UI thread does not** call WebGPU.  
- The frame loop `postDelayed`s on the same render thread; `surfaceDestroyed` stops the loop and `unconfigure`s/`close`s the Surface.  
- Separate `GPUInstance` from `DawnWasiWebGpuHost` (vector-add); they do not share.  
- **No** formal L2 Host ↔ Surface-thread contract yet (define when lifting into the Host).

## Instance / Device / Queue

| Object | Convention |
|--------|------------|
| `GPUInstance` | One per Host (or per TriangleRenderer); released on `close()` |
| `GPUDevice` / `GPUQueue` | Held by handle table (Host) or renderer; best-effort `close` on drop/`release` |
| `submit` / `mapAsync` / present | May start on the calling thread; completion on callback executor / same render thread |

## Later

- If the L1 Runtime enters the Host from multiple threads, add locking or explicit thread affinity.  
- When lifting surface/render into `WasiWebGpuHost`, unify Surface-thread vs Device-thread rules.
