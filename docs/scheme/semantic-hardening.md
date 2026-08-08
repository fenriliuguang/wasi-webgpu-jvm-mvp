# 语义加固与工程清债（semantic-hardening）— 本阶段

**中文** | [English](semantic-hardening.en.md)

> **状态：进行中（本阶段已锁定，2026-08-08）。**  
> 承接：真机稳性 D1–D6 收口（[`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)）。  
> 组合：语义加固（A+B）→ 工程清债（C+D）→ Demo 深化（E）。

## 一句话

在不动 wasi-gfx / 合规全量 world 的前提下，把 CM 语义面往标准 descriptor 与资源析构推进，顺手清上游与仪器外围债，并用更丰富的 Guest demo 验收。

```text
A WIT records（render / pipeline 等）
  → B Guest/Host 资源析构接线
  → C 上游 wasmtime4j 补丁贡献准备
  → D D7 Studio 仪器与脚本对齐
  → E 更丰富 Guest demo（仍走 experimental WIT）
```

## 已定决策

| 问题 | 决定 |
|------|------|
| 本阶段范围 | 锁定 **A+B+C+D+E** 为当前阶段；**F/G/H/I** 明确不做；**J**（perf）不阻塞、可选旁路 |
| 主线顺序 | 先 **A**（records）再 **B**（destructor）；**C/D** 可与 A/B 交错；**E** 在 A（必要时 B）之后验收 |
| **E 选型** | **顶点缓冲**（Guest `create-buffer` + `write-buffer` + `set-vertex-buffer` + `@location(0)`）；不用每帧变色 / 多 draw 作主验收 |
| WIT 版本 | records 足以改变 import 形状时 **bump** `experimental:webgpu-cm`（预期 `0.3.0` → `0.4.0`）；纯 additive 小改可不 bump（与上一切片惯例一致） |
| 特化 API | `create-render-pipeline-triangle` 等可保留为便捷路径，或标 deprecated；新路径以 records/descriptor 为主 |
| 析构策略 | WIT resource drop → Host `drop*`；目标减少对 `releaseFrameResources` / `releaseAllGpuObjects` 的**语义依赖**（Demo 交接仍可保留 settle 保险） |
| 上游（C） | 以 [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) 为 brief；本阶段目标是可提交的说明 + 本仓可继续用 overlay；**不强制**上游 PR 合并 |
| 仪器（D） | 收口 D7：Studio 仪器路径与 `scripts/run-android-instrumented.ps1` 对齐或文档标明唯一推荐入口 |
| 验收形态 | 桌面单测（有 natives）+ Android 仪器绿灯 + Demo 手点不回归 D1–D6；每子切片文档 / CHANGELOG |

## 子切片与 DoD

### A — WIT records 扩展

- [x] `experimental:webgpu-cm` **0.4.0**：`vertex-attribute` / `vertex-buffer-layout` + `set-vertex-buffer` + `create-render-pipeline-triangle-buffers`（对照 buffer `0.2.0` 先例）
- [x] L2 + Dawn + Cpu stub + `abi-cm` + WasmtimeCmLinker 接线；旧 `create-render-pipeline-triangle` 保留
- [x] `docs/mapping/render-subset` 更新；Guest wasm 已按 `@0.4.0` 重建（仍走旧 triangle helper）
- [x] Guest 改用 buffers API（**E** 已接线）；仪器真机复验 triangle / vector-add 仍待设备

### B — Guest 资源析构接线

- [x] 帧内等价 drop：AbiCm 跟踪 View↔Texture 配对，`present` / 下次 acquire 时 `tryDrop`（Texture 非 WIT resource）
- [x] Host `tryDrop` 幂等；`HandleTable.tryDrop`
- [x] 文档标明：配对释放 vs 仍靠 `releaseFrameResources`（encoder 孤儿）/ Demo `releaseAllGpuObjects`（Surface/Device；真 WIT dtor 仍受 wasmtime4j `resourceTable` 阻塞）
- [ ] Demo CM×N + L2 resume 真机复验（对照 blockers D2/D3/D5/D6）
- [ ] （可选增量）wasmtime4j destructor miss → `host.drop(rep)` 补丁 — 可并入 C

### C — 上游贡献准备

- [ ] `ConcurrentCallCodec` unsigned-u64（及 UPSTREAM 表内相关项）整理为可外发 brief / 补丁说明
- [ ] 本仓 overlay / 过滤 jar 策略不变或可平滑切换；`patches/UPSTREAM.md` 状态更新

### D — D7 仪器外围

- [ ] Studio 跑 `*InstrumentedTest` 与脚本路径行为一致，或 README / `docs/android-wasmtime` **唯一推荐**入口写清且 Studio 失败有已知原因链接
- [ ] blockers D7 标收口或「文档旁路正式化」

### E — 更丰富 Guest demo（已锁定：顶点缓冲）

- [x] Guest 上传 float32x2 顶点（`VERTEX \| COPY_DST`），`set-vertex-buffer(0, …)` 后 `draw(3)`；shader 读 `@location(0)`
- [x] 使用 `create-render-pipeline-triangle-buffers` + records；Host 仍保留旧 `create-render-pipeline-triangle`（对照）
- [ ] Demo / 仪器真机复验（需设备）；桌面 CpuHost 仍 Unsupported / skip

## 本阶段不做

| ID | 项 |
|----|-----|
| F | wasi-gfx / canvas 抽象 |
| G | 合规 `wasi:webgpu` 全量 world |
| H | Maven Central / 发包 |
| I | `abi-mvp` 扁平 render import |
| — | 多 window、MSAA/depth 全套（除非 A 的最小 records 必需） |

## 落地顺序

1. **A** WIT records + L2/abi-cm/Guest 接线 + 映射文档  
2. **B** destructor → Host drop；稳性回归  
3. **C** / **D** 工程清债（可与 1–2 交错）  
4. **E** Guest demo 深化 + 仪器/手点验收  
5. 文档收口：本页 DoD 勾选 → 归档页；根 README / scheme / CHANGELOG

## 链接

- 根 README：[`README.md`](../../README.md)  
- 方案索引：[`docs/scheme/README.md`](README.md)  
- 上阶段 blockers：[`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)  
- Render 映射：[`docs/mapping/render-subset.md`](../mapping/render-subset.md)  
- 上游 brief：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
- WIT：[`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)  
