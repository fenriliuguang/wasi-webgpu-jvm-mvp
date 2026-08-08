# Demo CM 稳性 — 真机调试阻塞 / 踩坑记录（2026-08-07）

**中文** | 状态：**D2/D3 真机已过（2026-08-08）；D1 缓解保持；D5/D6 仍开放**  
设备：vivo V2458A（PD2415）/ Mali（`libGLES_mali.so` / Vulkan）  
相关计划：[`demo-cm-stability.md`](demo-cm-stability.md)（DoD 已归档，但真机 Demo 路径曾有回归）  
旧记录：[`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)（P1/P6）

> 本文由 2026-08-07 Studio/adb 真机调试会话整理；2026-08-08 在同机复验并收口 D2/D3。  
> **优先级**：D5、D6、D1 根治；D4 已缓解；D7 外围。

---

## 一句话

Demo 启动时 L2 帧循环曾 **SIGSEGV 秒崩**（已缓解）；CM ↔ L2 交接曾卡在 **`VK_ERROR_NATIVE_WINDOW_IN_USE_KHR`**——根因是 CM 后只 `releaseSurfaces` / 未卸掉表内 per-frame Texture，ANativeWindow 未 disconnect。现改为 **CM 每次完整 Host teardown** + 加强 `releaseSurfaces`，真机连点 CM 后可回到 L2。

---

## 现象时间线

| 阶段 | 现象 | 结论 |
|------|------|------|
| A. `am start` MainActivity | ~2s 内进程死；tid `webgpu-triangle` | L2 `drawFrame` → `queueSubmit` → Mali NPE |
| B. 加 gpuLock / 延迟首帧后 | 进程存活；可见 Demo UI | 启动崩溃缓解 |
| C. 点「CM TRIANGLE ONCE」 | status：`CM done but L2 resume failed/timeout` | CM 可能已画，L2 抢不回 Surface |
| D. logcat（C 同期） | `connect: already connected` + `WINDOW_IN_USE` + `getCapabilities failed` | ANativeWindow 仍被上一 Owner 占用 |
| E. L2 渲染中 | `BLASTBufferQueue … NO_BUFFER_AVAILABLE` | 交换链 buffer 未及时归还（伴生） |
| F. 2026-08-08 复验 | 启动存活；CM×2 后 status=`Triangle rendering (L2…)`；无 `WINDOW_IN_USE` | D2/D3 收口 |

---

## 问题清单（按优先级）

| ID | 问题 | 严重度 | 状态 |
|----|------|--------|------|
| D1 | L2 `webgpu-triangle`：`queueSubmit` Mali **SIGSEGV**（fault addr `0x20`） | 启动即崩 | **缓解**（未宣称根治） |
| D2 | CM 后 L2 resume：`VK_ERROR_NATIVE_WINDOW_IN_USE_KHR` | Demo 交接失败 | **已收口**（真机复验） |
| D3 | `DawnWasiWebGpuHost.close()` 曾只 `handles.clear()` 不 `close()` Surface；CM 仅 `releaseSurfaces` 不够 | D2 主因 | **已收口** |
| D4 | CM 期间 `SurfaceView.surfaceChanged` → L2 `Resize FAILED: getCapabilities` | 噪音 / 竞态 | **已缓解**（`pausedForCm`） |
| D5 | `BLASTBufferQueue NO_BUFFER_AVAILABLE` | L2 帧循环不稳 | **开放**（swapchain drop 已改为 table-only） |
| D6 | CM：`wasm trap: cannot enter component instance` | 二次进入同 Session | **部分缓解**（每次 CM 新 Session；失败再 recreate） |
| D7 | Studio/Gradle 仪器 `Process crashed` vs 脚本 `am instrument` | 外围 | 沿用旧结论；脚本旁路 |

---

## D1 — L2 queueSubmit Mali SIGSEGV（缓解）

### 证据

```text
Fatal signal 11 (SIGSEGV), fault addr 0x20
tid: webgpu-triangle  pid: asi.webgpu.demo
#00 libGLES_mali.so
#08 Java_androidx_webgpu_GPUQueue_submit
#14 DawnWasiWebGpuHost.queueSubmit
#19 TriangleRenderer.drawFrame
```

### 已做缓解

- `DawnWasiWebGpuHost`：`gpuLock` 串行化 `processEvents` 与 surface/submit
- `TriangleRenderer`：首帧 `postDelayed(FIRST_FRAME_DELAY_MS)`；swapchain Texture/View **table-only drop**（`closeResource=false`，避免 native close 竞态）
- `BackendType.Vulkan` + `PresentMode.Fifo`（`Undefined` 曾疑似走 GLES，与 CM Vulkan 抢窗）

### 备注

- 仪器路径仍可 `EXTRA_SKIP_L2` 绕过（旧 P1）
- Demo 需要 L2，不能长期靠跳过

---

## D2 / D3 — WINDOW_IN_USE 与 Host.close 泄漏（已收口）

### 证据（修复前）

```text
CREATE_SURFACE_FROM_NATIVE_WINDOW …   # CM 侧常能成功一次
BufferQueueProducer … connect: already connected (cur=1 req=1)
vulkan: native_window_api_connect() failed: Invalid argument (-22)
Dawn: CreateAndroidSurfaceKHR failed with VK_ERROR_NATIVE_WINDOW_IN_USE_KHR
TriangleRenderer: resume attempt 1/3 failed
  WebGpuException: GPUSurface.getCapabilities failed
UI status: CM done but L2 resume failed/timeout
```

### 根因（已证实）

1. 旧 `close()` 实现 `handles.clear()` **不**对表内 `GPUSurface` 调 `close()` → ANativeWindow API 连接泄漏。
2. CM Guest WIT resource destructor **未接线**（wasmtime4j）→ Guest `drop-triangle` / Rust drop 不会自动 Host `drop`；表内残留 per-frame **Texture/View** 会钉住 swapchain，仅 `unconfigure` + 关 Surface **不足以** `api_disconnect`。
3. L2→CM 能成功是因为 L2 `teardownGpu()` = **完整 Host.close()**；CM→L2 失败是因为曾只 `releaseSurfaces` 且未清 Texture。
4. `VkCreateAndroidSurfaceKHR` 时 `native_window_api_connect`——必须销毁 VkSurface / 卸掉钉住引用，不是只 unconfigure。

### 已落地修复

| 位置 | 改动 |
|------|------|
| `DawnWasiWebGpuHost.close()` | 按 kind 顺序 `unconfigure` + `dropLocked(closeResource=true)`，再 `instance.close()` |
| `releaseSurfaces()` | 先卸 TextureView/Texture/encoder 类，再 unconfigure+close Surface + `processEvents` |
| `TriangleCmOneShot` | **每次** CM：新 Host+Session；结束后 `tearDownCmGpu()`（Session close + releaseSurfaces + Host.close）+ settle |
| `TriangleRenderer` | `pausedForCm`；pause=`teardownGpu`；resume 重建 Host + 重试；swapchain table-only drop |
| `MainActivity` | CM 后短 settle 再 resume |

### 真机复验（2026-08-08，V2458A）

```powershell
$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$adb = Join-Path $Sdk "platform-tools\adb.exe"
./gradlew :android-demo:assembleDebug
& $adb install -r -t -g android-demo\build\outputs\apk\debug\android-demo-debug.apk
$pkg = "io.github.fenriliuguang.wasi.webgpu.demo"
& $adb logcat -c
& $adb shell am force-stop $pkg
& $adb shell am start -n "$pkg/.MainActivity"
# 等 5s：pidof 存活；status ≈ Triangle rendering
# 点 CM（input tap 约 955 2656）×2
# 期望每次结束后 status ≈ Triangle rendering (L2 Host→Dawn Surface)
# logcat 无 WINDOW_IN_USE / SIGSEGV / resume attempt failed
```

**结果**：通过（进程同 pid 存活；连点 2 次 CM 均回到 L2；无 `WINDOW_IN_USE`）。

---

## D4 — Resize FAILED during CM（已缓解）

- 原因：CM 占用窗口时 `surfaceChanged` 仍进 L2 `onSurfaceResized` → `getCapabilities` 失败  
- 修复：`pausedForCm == true` 时忽略 available/resize  

---

## D5 — NO_BUFFER_AVAILABLE（开放）

```text
BLASTBufferQueue … acquireNextBufferLocked: Failed to acquire a buffer, err=NO_BUFFER_AVAILABLE
```

- 出现在 L2 帧循环期间（CM 前也可能）  
- 已做：present 后 table-only drop swapchain Texture/View（避免错误 native close）  
- 仍须观察：present 是否偶发失败、Fifo/缓冲数量  
- 不挡 D2 收口

---

## D6 — cannot enter component instance（部分缓解）

```text
WasmRuntimeException: wasm trap: cannot enter component instance
```

- 同 Session 重入 / 上次 trap 后实例不可用  
- 现：每次 CM 新建 Session（+ Host）；失败路径仍 recreate Session 一次  
- 长期：进程级 CM host 注册表 + 背靠背 linker recreate 仍可能踩坑

---

## 建议下一步

1. 盯 D5（L2 长跑 `NO_BUFFER`）与多次 CM 压力（D6 / 注册表）  
2. D1：在去掉首帧 delay / 收紧 gpuLock 范围下尝试复现 SIGSEGV  
3. D7 仍走 `scripts/run-android-instrumented.ps1` 旁路；勿把未复验项标成阶段完成

---

## 相关代码锚点

- L2 帧循环：`android-demo/.../TriangleRenderer.kt`  
- CM 帧循环：`android-demo/.../TriangleCmOneShot.kt`（`tearDownCmGpu`）  
- Dawn Host：`host-webgpu/.../DawnWasiWebGpuHost.kt`（`gpuLock` / `releaseSurfaces` / `close`）  
- 按钮路径：`android-demo/.../MainActivity.kt`（pause → CM → resume）  
