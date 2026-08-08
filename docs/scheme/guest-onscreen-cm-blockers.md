# Guest CM 上屏 — 仪器测试阻塞 / 踩坑记录

**中文** | 状态：**切片完成**（2026-08-06 仪器绿灯；文档 / CHANGELOG 已补，DoD 全勾选）  
计划：[`guest-onscreen-cm.md`](guest-onscreen-cm.md)  
P6（手动 Demo 稳性）已由新切片收口：[`demo-cm-stability.md`](demo-cm-stability.md) → [`archive-demo-cm-stability-dod.md`](archive-demo-cm-stability-dod.md)

## 进度快照

| 落地顺序 | 状态 |
|----------|------|
| 1. WIT `run-triangle` + `guest/triangle-cm` + `.wasm` | **完成**（已提交 `ca4aee9`） |
| 2. Demo / 运行时接线（`WasmtimeCmTriangle` + Demo 按钮） | **完成**（已提交 `adb9c3f`） |
| 3. 仪器测试绿灯 + 双语文档索引 | **完成**（仪器绿灯；`render-subset` 双语已补 Guest 路径） |
| 4. CHANGELOG | **完成**（Unreleased：Guest CM on-screen） |

对照：`WasmtimeCmVectorAddInstrumentedTest`（CM compute）同设备绿灯。本切片 DoD 验收是**仪器单次 draw**，不是 Demo 按钮稳定性。

---

## 踩坑速查（按优先级）

| ID | 问题 | 结论 |
|----|------|------|
| P2 | Host 回调 `NumberFormatException: For input string: "<u64>"` | **核心 / 已修**：`ConcurrentCallCodec.parseNumber` 对高位 u64 用了 `Long.parseLong`；android-demo 本地覆盖 + `parseUnsignedLong` |
| P5 | 仪器断言 `Surface not ready` | **核心 / 已修**：vivo 用 launcher Intent 拉起 MainActivity，`ActivityScenario` Intent 不匹配 → `onActivity` 永不跑；改用 `ActivityLifecycleMonitorRegistry` + 轮询 Surface |
| P1 | L2 `webgpu-triangle` + Mali SIGSEGV | **核心周边 / 已绕过**：仪器路径不启 L2（Instrumentation 检测 + `EXTRA_SKIP_L2`） |
| P3 | `processEvents` / `close()` Scudo | **缓解已做**（`shutdown`+`awaitTermination`）；仪器绿灯下未再复现 |
| P4 | APK UP-TO-DATE / USB 安装 / adb PATH | 流程噪音；`--rerun-tasks` 或 clean；脚本自带 adb 路径 |
| P6 | **手动点 Demo「CM 三角」仍崩溃 / 失败** | **已由 demo-cm-stability 收口**（见下） |

---

## 问题明细

### P2 — Host 回调解析 native window 失败（已修）

```text
E JniComponentLinker: Host function callback failed for ID: 2:
  For input string: "12970367413882346528"
```

- 路径：Rust `serde_json` 发无符号 u64 → `ConcurrentCallCodec.parseNumber` → `Long.parseLong`
- ANativeWindow* / TBI 高位 > `Long.MAX_VALUE` → 回调进不了 Host
- 同库 `ComponentTypeCodec` 已有 `parseUnsignedLong` 回退；`ConcurrentCallCodec` 漏了
- 修复：过滤 jar 排除上游类，android-demo 覆盖（与 `Validation` 同模式）；见 [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)

### P5 — vivo `ActivityScenario` Intent 不匹配（已修）

```text
ActivityScenario: … ignored because the intent does not match
startActivityIntent=…MainActivity (has extras)
activity.getIntent()=…MAIN+LAUNCHER … bnds=[71,1363][323,1664]
```

- OEM 把进程绑回桌面图标任务；Scenario 不认该实例 → Surface 回调未挂 → 永远 `Surface not ready`
- 修复：不用 `ActivityScenario`；`ActivityLifecycleMonitorRegistry` 取 RESUMED `MainActivity` + 轮询 `SurfaceView`
- `MainActivity` 在 androidx.test Instrumentation 下跳过 L2 / Demo CM 按钮

### P1 — L2 帧循环 SIGSEGV（仪器路径已绕过）

- tid `webgpu-triangle`：`drawFrame` → Dawn `queueSubmit` → Mali
- 与 CM Guest 无关时即可炸；仪器勿与 L2 共 Surface

### P3 — Dawn `close()` 与 `processEvents`（已缓解）

- `close()`：`shutdown` + `awaitTermination` 再关 instance（勿 `shutdownNow` 打断 in-flight `processEvents`）

### P4 — 工程噪音

| 项 | 说明 |
|----|------|
| 测试 APK 未更新 | `UP-TO-DATE` → 设备仍跑旧代码；`--rerun-tasks` / clean |
| 过滤 jar 残留内部类 | 排除 `ConcurrentCallCodec*.class`（含 `$JsonParser`），勿只删外层 |
| USB 安装权限 | 手机上点允许 |
| adb PATH | `scripts/run-android-instrumented.ps1` 用 SDK `platform-tools` |
| 进程级 CM host 注册表 | 桌面 `forkEvery=1`；Android 同进程多次 instantiate 需当心 |

### P6 — 手动触发 Demo CM 按钮仍崩溃（已由 demo-cm-stability 收口）

**原不在 Guest CM 上屏 DoD 内**；已由 [`demo-cm-stability.md`](demo-cm-stability.md) / [`archive-demo-cm-stability-dod.md`](archive-demo-cm-stability-dod.md) 收口（2026-08-07）。

原症状与对策：

| 症状 | 对策（现行） |
|------|------|
| `VK_ERROR_NATIVE_WINDOW_IN_USE_KHR` | L2/CM 两侧完整 Host teardown + `releaseSurfaces`（卸 Texture/View）；见 [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) D2/D3 |
| 同进程二次 CM `invalid handle` / trap | 仪器可复用 Session；Demo 手点每次新 Session（D6 仍开放） |
| Host.close / Scudo | `eventPoller.shutdown`+`awaitTermination` 后再关 instance（勿 `shutdownNow` 打断 processEvents） |
| 连点 | 按钮整段 disable 至 pause→CM→resume 结束 |

帧循环：`init-triangle` / `draw-frame` / `drop-triangle`；仪器重复：`cmGuestRepeatTriangleReusesSession`。

---

## 设备

- vivo V2458A（PD2415）/ Mali  
- 仪器：`WasmtimeCmTriangleInstrumentedTest`（含 repeat）、`WasmtimeCmVectorAddInstrumentedTest`

## 下一步

~~Demo 手点稳性（P6）~~ — 已完成（demo-cm-stability）  
~~真机回归 D1–D6~~ — 已收口（[`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)）  
**本阶段**：[`semantic-hardening.md`](semantic-hardening.md)（含上游 `ConcurrentCallCodec` unsigned-u64 = 子切片 C；D7 = 子切片 D）

## 一句话

核心阻塞（P2 u64、P5 vivo Scenario）已修，**仪器单次上屏已绿**；P6 与真机 D1–D6 已收口；后续见本阶段 [`semantic-hardening.md`](semantic-hardening.md)。
