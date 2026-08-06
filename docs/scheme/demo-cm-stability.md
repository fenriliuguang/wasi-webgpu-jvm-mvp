# Demo CM 稳性 + 帧循环（demo-cm-stability）— 已定计划

**中文** | [English](demo-cm-stability.en.md)

> **状态：已锁定（主切片）。** 根 README 状态节与此对齐。承接 P6：[`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)。

## 一句话

收口 P6：手动 Demo「CM 三角」与仪器路径同绿，并把 CM Guest 从单次 draw 升级为宿主驱动帧循环。

```text
MainActivity「CM 三角」
  → pauseSurfaceAndAwait（L2 停帧 + unconfigure）
  → CM 拥有 Surface：init → draw-frame 循环（复用单 Host）
  → 结束：present 后确认 unconfigure + drop → 恢复 L2
```

## 已定决策

| 问题 | 决定 |
|------|------|
| 主切片 | P6 四类症状收口 + 宿主驱动帧循环；不动 WIT 语义面（不加 records）、不碰 wasi-gfx |
| Surface 独占 | CM 期间 L2 完全 pause（复用 `TriangleRenderer.pauseSurfaceAndAwait`）；CM 结束确认 unconfigure 再恢复 L2 |
| Host | Demo 进程内复用单个 `DawnWasiWebGpuHost`（对齐进程级 CM host 注册表限制；`WasmtimeCmTriangleAndroid.runOnce` 已支持注入 host） |
| 帧循环形态 | WIT 仍挂 `@0.3.0`，world `triangle` 追加 export（init / draw-frame / drop 三段式，additive 不 bump）；宿主 `webgpu-triangle-cm` 线程每帧调 `draw-frame`。回退：重复 one-shot（仅作对照，每帧重建 surface 代价高） |
| 验收 | 手动连点 + 仪器重复触发用例双绿 |

## DoD

- [ ] 手动 Demo 重复触发 CM 三角（含连点）无 `VK_ERROR_NATIVE_WINDOW_IN_USE` / `invalid handle` / SIGSEGV
- [ ] 同进程多次 CM instantiate 稳定（进程级注册表复用或隔离修复）
- [ ] CM present 后 unconfigure 确认，L2 可恢复上屏
- [ ] 帧循环：CM Guest 三角持续渲染（宿主驱动）；`docs/mapping/threading` 同步
- [ ] 仪器：新增重复触发用例绿灯；既有 CM 仪器（triangle / vector-add）不回退
- [ ] 文档：blockers P6 标记解决；CHANGELOG

## 本切片不做

- wasi-gfx canvas 抽象、合规全量 `wasi:webgpu` world
- 更多 WIT records（slice B）、Maven Central
- `abi-mvp` 扁平 render import

## 落地顺序

1. Host / Surface 生命周期收口（复用 Host、unconfigure 确认、L2 恢复）
2. 同进程重复 instantiate 修复（进程级注册表）
3. 仪器新增「重复触发」用例 + 手动连点验证
4. WIT export 拆分（init / draw-frame / drop）+ 宿主驱动帧循环
5. 文档 / CHANGELOG

## 风险

- 进程级注册表修复波及桌面单测（`forkEvery=1` 门控）
- 帧循环线程亲和（render 线程 vs CM 调用线程；见 [`docs/mapping/threading.md`](../mapping/threading.md)）
- Dawn present / processEvents 时序（P3 同类 Scudo 风险）

## 链接

- 根 README：[`README.md`](../../README.md)
- 上一切片归档：[`archive-guest-onscreen-cm-dod.md`](archive-guest-onscreen-cm-dod.md)
- P6 明细：[`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)
- Render 映射：[`docs/mapping/render-subset.md`](../mapping/render-subset.md)
- 线程：[`docs/mapping/threading.md`](../mapping/threading.md)
- WIT：[`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)
