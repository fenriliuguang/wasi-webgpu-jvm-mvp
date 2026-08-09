# Demo CM 稳性 + 帧循环（demo-cm-stability）— 已完成

**中文** | [English](demo-cm-stability.en.md)

> **状态：已完成（2026-08-07）。** DoD 归档：[`archive-demo-cm-stability-dod.md`](archive-demo-cm-stability-dod.md)。  
> 承接 P6：[`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)。  
> **真机回归**：D1–D6 已在 V2458A 收口（2026-08-08）→ [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)。

## 一句话

收口 P6：手动 Demo「CM 三角」与仪器路径同绿，并把 CM Guest 从单次 draw 升级为宿主驱动帧循环。

```text
MainActivity「CM 三角」
  → pauseSurfaceAndAwait（L2 停帧 + Host teardown）
  → CM 拥有 Surface：init → draw-frame 循环（复用 Host + Session）
  → 结束：drop-triangle → releaseAllGpuObjects → 恢复 L2
```

## 已定决策

| 问题 | 决定 |
|------|------|
| 主切片 | P6 四类症状收口 + 宿主驱动帧循环；不动 WIT 语义面（不加 records）、不碰 wasi-gfx |
| Surface 独占 | CM 期间 L2 完全 pause（`pauseSurfaceAndAwait` → `teardownGpu`）；CM 结束后再 `resumeSurfaceAndAwait` |
| Host / Session（Demo） | 复用 Host + Session；每轮 `releaseAllGpuObjects`（不断 Instance/linker） |
| 帧循环形态 | WIT 仍挂 `@0.3.0`，world `triangle` 追加 `init-triangle` / `draw-frame` / `drop-triangle`（additive 不 bump）；宿主 `webgpu-triangle-cm` 驱动。保留 `run-triangle` 作仪器 one-shot |
| 同进程重复 | Demo / 仪器均复用 Session；帧资源靠 `releaseFrameResources` |
| 验收 | 手动连点门控 + 仪器重复触发用例 |

## DoD

见归档 [`archive-demo-cm-stability-dod.md`](archive-demo-cm-stability-dod.md)（全勾选）。

## 本切片不做

- wasi-gfx canvas 抽象、合规全量 `wasi:webgpu` world
- 更多 WIT records、Maven Central、`abi-mvp` 扁平 render（已移交并完成于 [`semantic-hardening.md`](semantic-hardening.md) / 合规 World；更远项见根 README「下一阶段未锁定」）

## 落地顺序（已完成）

1. ~~Host / Surface 生命周期收口~~ — `b5e6212`
2. ~~同进程重复 instantiate（Session 复用）~~ — `654896a`
3. ~~仪器「重复触发」用例~~ — `110944d`
4. ~~WIT 三段式 + 宿主帧循环~~ — `841b55c`
5. ~~文档 / CHANGELOG~~ — 本收口

## 链接

- DoD 归档：[`archive-demo-cm-stability-dod.md`](archive-demo-cm-stability-dod.md)
- 真机回归 blockers：[`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)
- 根 README：[`README.md`](../../README.md)
- 上一切片归档：[`archive-guest-onscreen-cm-dod.md`](archive-guest-onscreen-cm-dod.md)
- P6 明细：[`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)
- 线程：[`docs/mapping/threading.md`](../mapping/threading.md)
- WIT：[`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)
