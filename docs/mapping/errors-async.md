# 错误与 Async 备忘（切片 F）

**中文** | [English](errors-async.en.md)

## 错误策略

| 层 | 行为 |
|----|------|
| 句柄错误 | `HostException.InvalidHandle` |
| 子集外能力 | `HostException.Unsupported` |
| Dawn / 校验失败 | `HostException.Backend` / `Validation` |
| WIT `result<_, E>` | **wasi:webgpu 轨已抬升**（见下）；experimental 轨仍 throw→trap |

规范校验与 Dawn 校验可能不一致：优先记录 Dawn 消息，并在映射表偏差列跟踪。

## 双轨差异

| 轨 | Host 失败如何到达 Guest |
|----|-------------------------|
| `experimental:webgpu-cm` | `HostException` 抛出 → CM host callback **trap**（现有 Guest 不变） |
| `wasi:webgpu@0.3.0-rc.2` | 返回 `result` 的方法：经 `HostErrorMapping` + `WasiResultCodec` 编成 `ComponentVal.err(record{kind,message})`；非 result 方法仍 throw `Unsupported` stub |

代码：`host-api` → `HostErrorMapping` / `WasiWebGpuError.kt`；`runtime-wasmtime` → `WasiResultCodec`；`abi-wasi` → `AbiWasiResults.BY_FUNC`。

## HostException → error-kind（启发式）

| HostException | `gpu-error-kind` | 典型方法级 kind |
|---------------|------------------|-----------------|
| `Validation` / `InvalidHandle` | `validation-error` | `type-error` / `range-error` / pipeline `validation` |
| `Unsupported` / `Backend` | `internal-error` | `operation-error` / pipeline `internal`；`map-async` 的 Backend → `abort-error` |
| 仅单 kind 的方法 | （同上） | `unmap`→`abort-error`；`set-bind-group`→`range-error`；`create-query-set`→`type-error` |

未接线的 wasi **result** stub：以 `Unsupported` 映射进该方法 Err 形状（Guest 得 `result` Err，非 trap）。

## Async

| 阶段 | 策略 |
|------|------|
| 默认（切片落地前 / cube 主验收） | **sync-compat**：`requestAdapter` / `requestDevice` / `mapAsync` 等仍内部 `CountDownLatch` 等待；标准包长尾 `*-async` 可 stub 为 result Err |
| 真 CM async（**已立项，代码未开工**） | 计划 [`true-cm-async.md`](../scheme/true-cm-async.md)：主链 `request-adapter` / `request-device` / `map-async` 拟推进 CM future；主验收 **仍** CM cube + sync-compat；可选 P3 Spike 不阻塞关门 |
| WASI Preview3 | **非**关门条件；见计划页切片 E 可选旁路 |

与上游偏差（默认路径）：WIT 标 `async func` 的方法在 sync-compat 路径仍为同步返回；缺口矩阵对应行标 ⚠️ sync-compat，直至 true-cm-async 切片抬升。

超时默认 30s，超时抛 `HostException.Backend`（experimental）或抬成对应 result Err。
