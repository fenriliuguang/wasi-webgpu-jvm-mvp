# Threading model (P0 notes)

[中文](threading.md) | **English**

## Conclusion (P0)

- Host call convention is **single-threaded**: do not call the same `WasiWebGpuHost` instance concurrently.  
- Dawn async callbacks run on an inline `Executor` inside the Host; public APIs wait synchronously.  
- Host background-polls `GPUInstance.processEvents()` (aligned with `androidx.webgpu.helper` event pump).  
- Android demo / instrumented tests should call the Host on a **background thread** to avoid blocking the main thread.  
- P0 has **no on-screen** path, so no render thread / `Choreographer` requirement.

## Instance / Device / Queue

| Object | Convention |
|--------|------------|
| `GPUInstance` | One per Host; released on `close()` |
| `GPUDevice` / `GPUQueue` | Held by handle table; best-effort `close` on drop |
| `submit` / `mapAsync` | May start on the calling thread; completion on the callback executor |

## Later (P1+)

- If the L1 Runtime enters the Host from multiple threads, add locking or explicit thread affinity.  
- When going on-screen (P2), define the relationship between Surface and Device threads.
