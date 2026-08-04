# P1 边界开销备注（初稿）

> experimental · Guest → Wasmtime (L1) → abi-mvp → `WasiWebGpuHost` (L2) → `CpuWasiWebGpuHost`  
> 对照：纯 Kotlin 直接调 L2（同一 CPU Host）  
> Android：同一 Guest / abi-mvp 路径 → `DawnWasiWebGpuHost`（见 `WasmtimeVectorAddInstrumentedTest`）

## 测量方式

```bash
./gradlew :runtime-wasmtime:test --tests "*.WasmtimeVectorAddTest.boundaryNoteTimingSmoke"
```

测试对 `n=256` 向量加跑少量迭代，打印 guest 路径与 direct Kotlin→L2 的平均耗时。  
**不是**正式基准：含 Wasmtime 引擎创建 / 模块编译 / 实例化开销；未做预热矩阵或 JMH。

## 定性结论（P1）

| 成本块 | 说明 |
|--------|------|
| GPU / 着色器 | 桌面用 CPU Host 模拟；Android Dawn 路径走仪器测试 |
| Import 边界 | 每条 abi-mvp import 一次 JNI/host 回调；向量加场景调用次数为 O(常量) |
| 内存拷贝 | `queue_write_buffer` / `buffer_get_mapped_range` 经 Guest 线性内存拷贝 |
| Android native | Bionic `libwasmtime4j.so`（`runtime-wasmtime/android-natives`）；与桌面 Maven natives 隔离 |

相对「纯 Kotlin 调 L2」：Guest 路径必然更慢（引擎启动 + 边界往返）。P1 目标是 **语义闭环**，不是倍率达标。

## 后续（不阻塞 P1）

- 复用 Engine/Module，只测 `run_vector_add` 稳态
- 与 Android Dawn 仪器测试同输入对照（已有 `WasmtimeVectorAddInstrumentedTest`）
- 批量拷贝约定（少次、大批量）写入正式 perf 契约
