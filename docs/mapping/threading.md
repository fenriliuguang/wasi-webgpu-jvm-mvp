# 线程模型

**中文** | [English](threading.en.md)

## 结论（compute Host）

- Host 调用约定为 **单线程**：同一 `WasiWebGpuHost` 实例不要并发调用。  
- Dawn 异步回调跑在 Host 内部的 inline `Executor` 上；对外 API 同步等待。  
- Host 后台轮询 `GPUInstance.processEvents()`（对齐 `androidx.webgpu.helper` 的事件泵）。  
- Android demo / 仪器测试应在 **后台线程** 调用 Host，避免阻塞主线程。

## Surface / render（L2 API）

- 同一 `WasiWebGpuHost` 实例上：`surfaceConfigure` / `surfaceGetCurrentTexture` / `surfacePresent` 与 `queueSubmit` **必须在同一渲染线程**。  
- Demo **不再**保留独立 L2 `TriangleRenderer` 上屏路径；Surface 仅由 CM Guest 路径消费（见下）。  
- L2 surface API 仍可供宿主直接调用；调用方须自管线程亲和。

## Surface / render（CM Guest，现行 Demo / 仪器）

- CM 路径使用 `DawnWasiWebGpuHost` + `HandlerThread`（`webgpu-cube-cm`）；**主线程不**调用 WebGPU。  
- 宿主驱动帧循环：同线程 `init-cube` → 循环 `draw-frame` → `drop-cube`（见 `WasmtimeCmCube.Session.runFrameLoop`）。  
- Demo `CubeCmOneShot`：复用 Host + Session；每轮前后 `releaseAllGpuObjects` 交还 ANativeWindow（避免背靠背关 linker）；Session 末尾 `releaseLifetimeSafetyNets`。  
- **仪器**：`WasmtimeCmCubeInstrumentedTest`（复用 Session）。  
- 帧资源：present / unconfigure / Session 末尾 `tryDrop` View↔Texture 配对 + `releaseFrameResources`；**仍非真 WIT dtor**（[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) §4）。  
- 历史 L2↔CM pause/resume 与 triangle 两波仪器：见 [`demo-cm-stability-blockers.md`](../scheme/demo-cm-stability-blockers.md)。

## Instance / Device / Queue

| 对象 | 约定 |
|------|------|
| `GPUInstance` | 每 Host 一个；`close()` 时释放 |
| `GPUDevice` / `GPUQueue` | 由句柄表持有；drop/`close` 时 best-effort `close` |
| `submit` / `mapAsync` / present | 在渲染线程（或单线程 Host 调用方）发起；完成在 callback executor / 同线程 |

## 后续

- 若 L1 Runtime 多线程进入 Host，需加锁或明确线程亲和。
