# Demo CM 稳性 — 真机调试阻塞 / 踩坑记录（2026-08-07）

**中文** | 状态：**进行中（根 README 已锁定为当前优先切片）**  
设备：vivo V2458A（PD2415）/ Mali（`libGLES_mali.so` / Vulkan）  
相关计划：[`demo-cm-stability.md`](demo-cm-stability.md)（DoD 已归档，但真机 Demo 路径仍有回归）  
旧记录：[`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)（P1/P6）

> 本文由 2026-08-07 Studio/adb 真机调试会话整理。子 agent 已强行中断；工作区有**未提交**修复草稿，需按下方「待验证」项复测后再合入。  
> **优先级（已写入根 README）**：D2/D3 先于 D1 根治、D5、D6；D4 已缓解；D7 外围。

---

## 一句话

Demo 启动时 L2 帧循环曾 **SIGSEGV 秒崩**；缓解后进程可存活，但 CM ↔ L2 交接仍卡在 **`VK_ERROR_NATIVE_WINDOW_IN_USE_KHR`**，UI 常显示 `CM done but L2 resume failed/timeout`。

---

## 现象时间线

| 阶段 | 现象 | 结论 |
|------|------|------|
| A. `am start` MainActivity | ~2s 内进程死；tid `webgpu-triangle` | L2 `drawFrame` → `queueSubmit` → Mali NPE |
| B. 加 gpuLock / 延迟首帧后 | 进程存活；可见 Demo UI | 启动崩溃缓解 |
| C. 点「CM TRIANGLE ONCE」 | status：`CM done but L2 resume failed/timeout` | CM 可能已画，L2 抢不回 Surface |
| D. logcat（C 同期） | `connect: already connected` + `WINDOW_IN_USE` + `getCapabilities failed` | ANativeWindow 仍被上一 Owner 占用 |
| E. L2 渲染中 | `BLASTBufferQueue … NO_BUFFER_AVAILABLE` | 交换链 buffer 未及时归还（伴生） |

---

## 问题清单（按优先级）

| ID | 问题 | 严重度 | 状态 |
|----|------|--------|------|
| D1 | L2 `webgpu-triangle`：`queueSubmit` Mali **SIGSEGV**（fault addr `0x20`） | 启动即崩 | **缓解**（未宣称根治） |
| D2 | CM 后 L2 resume：`VK_ERROR_NATIVE_WINDOW_IN_USE_KHR` | Demo 交接失败 | **未收口** |
| D3 | `DawnWasiWebGpuHost.close()` 曾只 `handles.clear()` 不 `close()` Surface | D2 主因候选 | **代码已改草稿**（待真机复验） |
| D4 | CM 期间 `SurfaceView.surfaceChanged` → L2 `Resize FAILED: getCapabilities` | 噪音 / 竞态 | **已缓解**（`pausedForCm`） |
| D5 | `BLASTBufferQueue NO_BUFFER_AVAILABLE` | L2 帧循环不稳 | **开放** |
| D6 | CM：`wasm trap: cannot enter component instance` | 二次进入同 Session | **部分缓解**（失败后 recreate Session） |
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

### 已做缓解（工作区，未提交）

- `DawnWasiWebGpuHost`：`gpuLock` 串行化 `processEvents` 与 surface/submit
- `TriangleRenderer`：首帧 `postDelayed(FIRST_FRAME_DELAY_MS)`
- `BackendType.Vulkan` + `PresentMode.Fifo`（`Undefined` 曾疑似走 GLES，与 CM Vulkan 抢窗）

### 备注

- 仪器路径仍可 `EXTRA_SKIP_L2` 绕过（旧 P1）
- Demo 需要 L2，不能长期靠跳过

---

## D2 / D3 — WINDOW_IN_USE 与 Host.close 泄漏（核心未收口）

### 证据

```text
CREATE_SURFACE_FROM_NATIVE_WINDOW …   # CM 侧常能成功一次
BufferQueueProducer … connect: already connected (cur=1 req=1)
vulkan: native_window_api_connect() failed: Invalid argument (-22)
Dawn: CreateAndroidSurfaceKHR failed with VK_ERROR_NATIVE_WINDOW_IN_USE_KHR
TriangleRenderer: resume attempt 1/3 failed
  WebGpuException: GPUSurface.getCapabilities failed
UI status: CM done but L2 resume failed/timeout
```

### 根因判断

1. **高置信**：旧 `close()` 实现 `handles.clear()` **不**对表内 `GPUSurface` 调 `close()` → ANativeWindow API 连接泄漏 → 下一 Owner（L2 resume 或再次 CM）必撞 `WINDOW_IN_USE`。
2. CM Guest WIT resource destructor **未接线**（wasmtime4j）→ Guest `drop-triangle` / Rust drop 不会自动 Host `drop(surface)`；必须 Kotlin 侧 `releaseSurfaces()` 或完整 `host.close()`。
3. 仅 `unconfigure` 不够；pause 路径已改为 L2 `teardownGpu()`（关整个 L2 Host）。

### 工作区已改（待复验）

| 位置 | 改动 |
|------|------|
| `DawnWasiWebGpuHost.close()` | 按 kind 顺序 `unconfigure` + `dropLocked(closeResource=true)`，再 `instance.close()` |
| `releaseSurfaces()` / `flushEvents()` | CM 结束后显式放 Surface + processEvents |
| `TriangleRenderer` | `pausedForCm`；pause=`teardownGpu`；resume 重建 Host + 重试 |
| `TriangleCmOneShot` | 帧循环后 `releaseSurfaces`（是否每次关死 Session/Host 以工作区最新代码为准） |
| `MainActivity` | CM 后短 settle 再 resume |

### 待验证命令

```powershell
$Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$adb = Join-Path $Sdk "platform-tools\adb.exe"
./gradlew :android-demo:assembleDebug
& $adb install -r -t -g android-demo\build\outputs\apk\debug\android-demo-debug.apk
$pkg = "io.github.fenriliuguang.wasi.webgpu.demo"
& $adb logcat -c
& $adb shell am force-stop $pkg
& $adb shell am start -n "$pkg/.MainActivity"
# 等 5s 确认进程仍在：adb shell pidof $pkg
# 点 CM（或 input tap 约 955 2655）
# 期望 status ≈ Triangle rendering (L2 Host→Dawn Surface)
# logcat 不应再出现 WINDOW_IN_USE / SIGSEGV
```

**通过标准**：启动不崩；CM 可跑；L2 resume 成功；无 `WINDOW_IN_USE`。

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
- 疑点：swapchain texture `close()` 时机、present 是否成功、Fifo/缓冲数量  
- 与 D2 可并行，不挡「先让 resume 抢窗成功」

---

## D6 — cannot enter component instance（部分缓解）

```text
WasmRuntimeException: wasm trap: cannot enter component instance
```

- 同 Session 重入 / 上次 trap 后实例不可用  
- 工作区：失败后 recreate Session；长期仍可能受进程级 CM host 注册表限制  

---

## 未提交改动（打断时快照）

```text
M  android-demo/.../MainActivity.kt
M  android-demo/.../TriangleCmOneShot.kt
M  android-demo/.../TriangleRenderer.kt
M  host-api/.../CpuWasiWebGpuHost.kt
M  host-api/.../Handles.kt
M  host-api/.../WasiWebGpuHost.kt
M  host-api/.../HandleTableTest.kt
M  host-webgpu/.../DawnWasiWebGpuHost.kt
```

基准提交：`822d057`（Demo CM 稳性文档归档）。上述均为归档后的真机回归修复草稿。

---

## 建议下一步（给下一 agent）

1. **先复验**当前工作区（含已改的 `close()`）是否已消灭 `WINDOW_IN_USE`  
2. 若仍失败：在 `teardownGpu` / CM `host.close()` 后加更长 settle + 确认 `GPUSurface.close` 真正 disconnect BufferQueue  
3. 再盯 D5（NO_BUFFER）与二次 CM（D6 / 注册表）  
4. 绿灯后：补 blockers 勾选、CHANGELOG、再提交（勿把未验证草稿直接当 DoD 完成）

---

## 相关代码锚点

- L2 帧循环：`android-demo/.../TriangleRenderer.kt`  
- CM 帧循环：`android-demo/.../TriangleCmOneShot.kt`  
- Dawn Host：`host-webgpu/.../DawnWasiWebGpuHost.kt`（`gpuLock` / `releaseSurfaces` / `close`）  
- 按钮路径：`android-demo/.../MainActivity.kt`（pause → CM → resume）  
