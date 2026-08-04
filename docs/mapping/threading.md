# 线程模型（P0 备忘）

## 结论（P0）

- Host 调用约定为 **单线程**：同一 `WasiWebGpuHost` 实例不要并发调用。  
- Dawn 异步回调跑在 Host 内部的 inline `Executor` 上；对外 API 同步等待。  
- Host 后台轮询 `GPUInstance.processEvents()`（对齐 `androidx.webgpu.helper` 的事件泵）。  
- Android demo / 仪器测试应在 **后台线程** 调用 Host，避免阻塞主线程。  
- P0 **不上屏**，无渲染线程 / `Choreographer` 要求。

## Instance / Device / Queue

| 对象 | 约定 |
|------|------|
| `GPUInstance` | 每 Host 一个；`close()` 时释放 |
| `GPUDevice` / `GPUQueue` | 由句柄表持有；drop 时 best-effort `close` |
| `submit` / `mapAsync` | 可在调用线程发起；完成在 callback executor |

## 后续（P1+）

- 若 L1 Runtime 多线程进入 Host，需加锁或明确线程亲和。  
- 上屏（P2）时再定义 Surface 线程与 Device 线程的关系。
