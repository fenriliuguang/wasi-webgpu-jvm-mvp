# 边界开销备注（非正式）

**中文** | [English](p1-boundary.en.md)

> experimental · **非正式基准**（无 JMH / 无倍率 / 无帧率门禁）  
> 现行可复现路径（vector-add 仪器与 Guest 示例已移除）：
>
> - 桌面：abi-mvp 扁平 surface/render → `CpuWasiWebGpuHost`（见下方 timing smoke）  
> - 桌面 CM：`:runtime-wasmtime:test`（`WasmtimeCmCubeTest`；无 `desktop-natives` 时 skip）  
> - Android：`WasmtimeCmCubeInstrumentedTest`（CM cube → Dawn；真机验收基准）

## 测量方式（桌面烟测）

```bash
./gradlew :abi-mvp:test --tests "*.AbiMvpHostBindingsTest.boundaryNoteTimingSmoke"
```

对少量迭代打印：

1. **abi-mvp 扁平路径**：`AbiMvpHostBindings` → 同一 `CpuWasiWebGpuHost`（surface configure / get-view / present + triangle pass 子集）  
2. **纯 Kotlin→L2**：直接调用同一 Cpu Host 的等价 L2 API  

**不是**正式基准：含 Host 创建与句柄建拆；无预热矩阵 / JMH；失败条件仅为路径跑通，**不**断言耗时倍率。

CM cube 真机路径请用仪器脚本（非正式 perf）：

```powershell
./scripts/run-android-instrumented.ps1
```

## 定性结论

| 成本块 | 说明 |
|--------|------|
| GPU / 着色器 | 桌面 Cpu Host 仅为句柄级 stub；Dawn 开销只在 Android CM cube 仪器路径可见 |
| Import 边界 | abi-mvp 每条扁平 import 一次 host 转发；相对直接 L2 通常仅为薄包装 |
| CM / Wasmtime | 引擎创建、模块编译、实例化、CM host 回调往返（桌面 CM 烟测 / 真机仪器） |
| 内存拷贝 | `queue_write_buffer` / `queue_write_texture` / mapped-range 经 Guest 或 Host 缓冲拷贝 |
| Android native | Bionic `libwasmtime4j.so`（`runtime-wasmtime/android-natives`）；与桌面 Maven / `desktop-natives` 隔离 |

相对「纯 Kotlin 调 L2」：带 Wasmtime/CM 的 Guest 路径必然更慢。本备注只服务**语义与工程边界理解**，不设达标倍率。

## 历史锚点（已失效）

以下已随 vector-add Guest / 仪器移除，勿再引用：

- `WasmtimeVectorAddTest.boundaryNoteTimingSmoke`
- `WasmtimeVectorAddInstrumentedTest`

## 后续（不阻塞关门）

- 复用 Engine/Module，只测稳态 Guest 导出（若恢复 abi-mvp Guest）  
- 与 Android CM cube 仪器同分辨率对照（仍非正式）  
- 正式 perf 契约 / JMH：**不做**（见 engineering-handoff 排除项）  
