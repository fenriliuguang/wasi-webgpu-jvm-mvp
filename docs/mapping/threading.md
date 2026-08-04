# 线程模型（P0 备忘）

**中文** | [English](threading.en.md)

## 结论（P0 / compute Host）

- Host 调用约定为 **单线程**：同一 `WasiWebGpuHost` 实例不要并发调用。  
- Dawn 异步回调跑在 Host 内部的 inline `Executor` 上；对外 API 同步等待。  
- Host 后台轮询 `GPUInstance.processEvents()`（对齐 `androidx.webgpu.helper` 的事件泵）。  
- Android demo / 仪器测试应在 **后台线程** 调用 Host，避免阻塞主线程。

## 上屏 demo（Kotlin，不经 L2）

- `android-demo` 的 `TriangleRenderer` 使用独立 `HandlerThread`（`webgpu-triangle`）拥有 Instance/Device/Surface。  
- `SurfaceHolder` 回调只投递到该线程；**主线程不**调用 WebGPU。  
- 帧循环在同一渲染线程 `postDelayed`；`surfaceDestroyed` 时停循环并 `unconfigure`/`close` Surface。  
- 与 `DawnWasiWebGpuHost`（向量加）各用各的 `GPUInstance`，互不共享。  
- **尚未**定义 L2 Host 与 Surface 线程的正式契约（抬升进 Host 时再钉死）。

## Instance / Device / Queue

| 对象 | 约定 |
|------|------|
| `GPUInstance` | 每 Host（或每 TriangleRenderer）一个；`close()` 时释放 |
| `GPUDevice` / `GPUQueue` | 由句柄表（Host）或渲染器持有；drop/`release` 时 best-effort `close` |
| `submit` / `mapAsync` / present | 可在调用线程发起；完成在 callback executor / 同渲染线程 |

## 后续

- 若 L1 Runtime 多线程进入 Host，需加锁或明确线程亲和。  
- 若将 surface/render 抬进 `WasiWebGpuHost`，再统一 Surface 线程与 Device 线程关系。
