# 线程模型

**中文** | [English](threading.en.md)

## 结论（compute Host）

- Host 调用约定为 **单线程**：同一 `WasiWebGpuHost` 实例不要并发调用。  
- Dawn 异步回调跑在 Host 内部的 inline `Executor` 上；对外 API 同步等待。  
- Host 后台轮询 `GPUInstance.processEvents()`（对齐 `androidx.webgpu.helper` 的事件泵）。  
- Android demo / 仪器测试应在 **后台线程** 调用 Host，避免阻塞主线程。

## Surface / render（L2）

- 同一 `WasiWebGpuHost` 实例上：`surfaceConfigure` / `surfaceGetCurrentTexture` / `surfacePresent` 与 `queueSubmit` **必须在同一渲染线程**。  
- `android-demo` 的 `TriangleRenderer` 使用独立 `HandlerThread`（`webgpu-triangle`）持有该 Host；`SurfaceHolder` 回调只投递到该线程；**主线程不**调用 WebGPU。  
- 帧循环在同一渲染线程 `postDelayed`；`surfaceDestroyed` 时停循环并 `surfaceUnconfigure` / `drop` Surface。  
- 与向量加用的另一个 `DawnWasiWebGpuHost` 实例各用各的 `GPUInstance`，互不共享。

## Instance / Device / Queue

| 对象 | 约定 |
|------|------|
| `GPUInstance` | 每 Host 一个；`close()` 时释放 |
| `GPUDevice` / `GPUQueue` | 由句柄表持有；drop/`close` 时 best-effort `close` |
| `submit` / `mapAsync` / present | 在渲染线程（或单线程 Host 调用方）发起；完成在 callback executor / 同线程 |

## 后续

- 若 L1 Runtime 多线程进入 Host，需加锁或明确线程亲和。
