# 备忘：真 CM async / WASI Preview3（true-cm-async-memo）

**中文** | [English](true-cm-async-memo.en.md)

> **状态：备忘（2026-08-10）。** [engineering-handoff](engineering-handoff.md) 已归档；**尚未立项**，评估是否单独开阶段。  
> 现行策略仍为 **sync-compat**（见 [`errors-async.md`](../mapping/errors-async.md)）。  
> 仍为 **experimental**；真异步落地 **≠** 合规产品宣称，也 **≠** 对外发布。

## 一句话

把 wasi:webgpu 0.3 大量 `async func` 从「L2 内 `CountDownLatch` 阻塞等待」换成 **Component Model async（future/stream）**；可选再叠加 **WASI Preview3**。主验收在立项前 **仍保持** experimental CM cube + sync-compat。

## 为何单独备忘

| 现状 | 说明 |
|------|------|
| L2 / Dawn | `requestAdapter` / `requestDevice` / `mapAsync` 等在 host 回调路径上同步等待 + `processEvents` 轮询 |
| Linker | `ComponentHostFunction` 同步注册；**未** `enableWasiP3` |
| 缺口矩阵 | 多数 async 行 ⚠️ sync-compat 或 ❌ stub |
| wasmtime4j（`47.0.2-1.5.0`） | 有实验性 `enableWasiP3` / CM async 类型；native `wasi-p3` **非 default**；本仓补丁构建未开 |

## 目标分层（立项时再选）

| 层 | 含义 | 备注 |
|----|------|------|
| **A. CM async for wasi:webgpu** | 标准包 `async func` 用 future；host **不**阻塞 wasm 线程 | 对齐 0.3 async 的主路径 |
| **B. WASI Preview3 运行时** | `enableWasiP3` / `enableWasiHttpP3` 等 | 加码；上游仍 experimental/unstable |

建议立项时以 **A** 为默认范围；B 单独勾选。

## 工作清单（备忘，未排期）

### Runtime / natives

- [ ] Android / desktop CM 补丁构建评估 `wasi-p3` + 确认 `component-model-async` 可用
- [ ] Spike：wasmtime4j 是否支持 **async host import** 端到端（完成/拒绝 future）；缺口则 overlay 或等上游（本仓默认仍 **不**提上游 PR）
- [ ] 现有 CM resource / process-global registry 补丁与 async 调度共存验证

### Host / ABI

- [ ] 拆 sync-compat：`DawnWasiWebGpuHost.awaitRequest` 不再在 CM host 回调路径阻塞
- [ ] L2 分轨或改形（sync 保留给 Cpu/测试；async 面给 CM），保持 L2 不依赖 L1
- [ ] 重写 wasi 主链接线（`request-adapter` / `request-device` / `map-async` / pipeline-async 等）为 future 语义
- [ ] 错误面：`result` + future / error-context 与 `WasiResultCodec` / `HostErrorMapping` 对齐

### Guest / Demo / 线程

- [ ] Guest / wit-bindgen 切真 async import；决定 experimental 轨是否同步升，或另做 wasi 轨 async Guest
- [ ] 重定 Dawn `processEvents`、CM scheduler、Surface/present 同线程契约（对照 [`threading.md`](../mapping/threading.md)）
- [ ] Demo / 仪器驱动从「同步 host call」改为可复现的 completion / poll 模型

### 验证与文档

- [ ] 缺口矩阵 async 行从 sync-compat/stub → 真 async 状态
- [ ] 桌面 CM smoke +（若迁轨）仪器回归；**未立项前不改** CM cube 主验收
- [ ] README / scheme / CHANGELOG；明确真异步 ≠ 合规宣称 / 对外发布

## 建议 Spike 顺序（仅备忘）

1. 最小 Guest 调一个 `async` import（如 `request-adapter`）验证 runtime  
2. 能通再动 L2 + 主链少数方法；不通先定 runtime/overlay 边界  
3. 长尾 async 与可选 P3  
4. 最后才考虑主验收是否迁到 async wasi Guest  

## 明确不做（在本备忘立项前）

- 不阻塞 [engineering-handoff](engineering-handoff.md) A/B/C  
- 不改现行真机验收基准（CM cube + sync-compat）  
- 不宣传合规 `wasi:webgpu`；不对外发布  
- 不对 tegmentum/wasmtime4j 默认提 issue/PR（与 [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) 一致）

## 链接

- 现行 async 策略：[`errors-async.md`](../mapping/errors-async.md)  
- 缺口矩阵：[`compliant-world-gap.md`](../mapping/compliant-world-gap.md)  
- 线程：[`threading.md`](../mapping/threading.md)  
- 当前阶段：[`engineering-handoff.md`](engineering-handoff.md)  
- Upstream 备忘：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
