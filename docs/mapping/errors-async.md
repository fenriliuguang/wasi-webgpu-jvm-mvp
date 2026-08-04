# 错误与 Async 备忘（P0）

## 错误策略

| 层 | P0 行为 |
|----|---------|
| 句柄错误 | `HostException.InvalidHandle` |
| 子集外能力 | `HostException.Unsupported` |
| Dawn / 校验失败 | `HostException.Backend` / `Validation` |
| WIT `result<_, E>` | **未抬升**；P1+ 再映射 |

规范校验与 Dawn 校验可能不一致：优先记录 Dawn 消息，并在映射表偏差列跟踪。

## Async

| 阶段 | 策略 |
|------|------|
| P0 | 同步 MVP：`requestAdapter` / `requestDevice` / `mapAsync` 内部 `CountDownLatch` 等待 |
| P1 | 视 Runtime 能力保留同步或暴露 future |
| WASI 0.3 / CM async | 接口预留；不在 P0 实现 |

超时默认 30s，超时抛 `HostException.Backend`.
