# Demo CM 稳性 — 真机调试阻塞 / 踩坑记录（2026-08-07）

**中文** | 状态：**真机回归项已收口（2026-08-08，V2458A）**；D7 外围旁路  
设备：vivo V2458A（PD2415）/ Mali（`libGLES_mali.so` / Vulkan）  
相关计划：[`demo-cm-stability.md`](demo-cm-stability.md)（DoD 已归档；本文为真机回归跟进）  
旧记录：[`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)（P1/P6）

> 2026-08-07 整理；2026-08-08 同机收口 D2/D3，继而 D5/D6/D1。  
> **剩余**：D7（Studio 仪器 vs 脚本）仍走 `scripts/run-android-instrumented.ps1` — 已纳入本阶段 [`semantic-hardening.md`](semantic-hardening.md) 子切片 D。

---

## 一句话

启动 SIGSEGV、CM↔L2 `WINDOW_IN_USE`、BLAST `NO_BUFFER`、二次 CM trap 均已在 Demo 路径收口：渲染路径 `gpuLock`、present 后 `releaseFrameResources`、CM 复用 Session 并对 Guest 残留调用 `releaseAllGpuObjects`（不断 Instance/linker）。

---

## 现象时间线

| 阶段 | 现象 | 结论 |
|------|------|------|
| A. `am start` MainActivity | ~2s 内进程死；tid `webgpu-triangle` | L2 `drawFrame` → `queueSubmit` → Mali NPE |
| B. 加 gpuLock / 延迟首帧后 | 进程存活；可见 Demo UI | 启动崩溃缓解 |
| C. 点「CM TRIANGLE ONCE」 | status：`CM done but L2 resume failed/timeout` | CM 可能已画，L2 抢不回 Surface |
| D. logcat（C 同期） | `connect: already connected` + `WINDOW_IN_USE` | ANativeWindow 仍被上一 Owner 占用 |
| E. L2 渲染中 | `BLASTBufferQueue … NO_BUFFER_AVAILABLE` | 交换链 buffer 未及时归还 |
| F. 2026-08-08 | CM×2 后回 L2；无 `WINDOW_IN_USE` | D2/D3 收口 |
| G. 2026-08-08 | CM×3 + L2 空闲；无 NO_BUFFER / SIGSEGV / trap | D5/D6/D1 收口 |

---

## 问题清单

| ID | 问题 | 严重度 | 状态 |
|----|------|--------|------|
| D1 | L2 `queueSubmit` Mali **SIGSEGV** | 启动即崩 | **已收口**（渲染路径全 `gpuLock` + 首帧 delay；真机未再复现） |
| D2 | CM 后 L2 resume：`VK_ERROR_NATIVE_WINDOW_IN_USE_KHR` | Demo 交接失败 | **已收口** |
| D3 | `close()` 曾只 `clear()`；仅 `releaseSurfaces` 不够 | D2 主因 | **已收口**（`releaseAllGpuObjects`） |
| D4 | CM 期间 L2 `Resize FAILED` | 噪音 | **已缓解**（`pausedForCm`） |
| D5 | `BLASTBufferQueue NO_BUFFER_AVAILABLE` | 帧循环不稳 | **已收口**（`releaseFrameResources` / AbiCm present 后释放） |
| D6 | `cannot enter component instance` | 二次 CM | **已收口**（复用 Session；失败再 recreate；避免背靠背关死 linker） |
| D7 | Studio 仪器 `Process crashed` vs 脚本 | 外围 | 脚本旁路 → **本阶段 D**（[`semantic-hardening.md`](semantic-hardening.md)） |

---

## D1 — L2 queueSubmit Mali SIGSEGV（已收口）

### 证据（修复前）

```text
Fatal signal 11 (SIGSEGV), fault addr 0x20
tid: webgpu-triangle
#00 libGLES_mali.so
#08 Java_androidx_webgpu_GPUQueue_submit
#14 DawnWasiWebGpuHost.queueSubmit
#19 TriangleRenderer.drawFrame
```

### 对策

- `gpuLock`：`processEvents` 与 surface / encode / submit / present / drop 同锁  
- 首帧 `postDelayed(FIRST_FRAME_DELAY_MS)`  
- `BackendType.Vulkan` + `PresentMode.Fifo`  
- present **之后**再 `releaseFrameResources`（勿在 submit 前关 swapchain）

---

## D2 / D3 — WINDOW_IN_USE（已收口）

### 根因

1. 旧 `close()` 只 `handles.clear()` 不关 `GPUSurface`。  
2. Guest WIT destructor 未接线 → Device/Surface/Texture 留在表内钉住窗口。  
3. 仅 unconfigure / 只丢 Surface 不够；需卸掉 Device 等或完整 `close()`。  
4. 背靠背 `Host.close()` + 新 Session 会踩进程级 CM linker 注册表（与 D6 冲突）。

### 现行对策

| 位置 | 改动 |
|------|------|
| `DawnWasiWebGpuHost.close()` | 按 kind 关闭 GPU 对象再 `instance.close()` |
| `releaseFrameResources()` | present 后关 Texture/View/encoder |
| `releaseAllGpuObjects()` | 清空句柄表全部 GPU 对象，**保留** Instance |
| `TriangleCmOneShot` | 复用 Host+Session；每轮 `releaseAllGpuObjects` + settle |
| `TriangleRenderer` | `pausedForCm`；pause=`teardownGpu`；resume 重建 + 重试 |

### 复验（2026-08-08）

CM×3 后均 `Triangle rendering (L2…)`；同 pid；无 `WINDOW_IN_USE` / SIGSEGV / trap。

---

## D4 — Resize FAILED during CM（已缓解）

`pausedForCm == true` 时忽略 available/resize。

---

## D5 — NO_BUFFER_AVAILABLE（已收口）

### 根因

Guest/L2 未在 present 后释放 swapchain Texture（WIT destructor 未接线；AbiCm 只把 view 句柄回给 Guest，Texture 留在 Host 表）。

### 对策

- `WasiWebGpuHost.releaseFrameResources()`  
- AbiCm：`getCurrentTextureView` 前、`present` 后调用  
- L2：`drawFrame` finally 调用；`Timeout` 跳帧  

---

## D6 — cannot enter component instance（已收口）

### 根因

wasmtime4j CM host 回调进程级注册；背靠背关掉 linker 再 instantiate 易 trap。

### 对策

- Demo：**复用** Host + Session；用 `releaseAllGpuObjects` 交还窗口  
- 失败路径：settle 后 recreate Session 一次  
- 仪器：`cmGuestRepeatTriangleReusesSession` 仍有效  

---

## D7 — 仪器外围

沿用 `scripts/run-android-instrumented.ps1`。收口计划见本阶段 [`semantic-hardening.md`](semantic-hardening.md) 子切片 D。

---

## 相关代码锚点

- L2：`android-demo/.../TriangleRenderer.kt`  
- CM：`android-demo/.../TriangleCmOneShot.kt`  
- Dawn：`host-webgpu/.../DawnWasiWebGpuHost.kt`（`gpuLock` / `releaseFrameResources` / `releaseAllGpuObjects` / `close`）  
- AbiCm：`abi-cm/.../AbiCmHostBindings.kt`（present 后释放帧资源）  
