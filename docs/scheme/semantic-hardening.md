# 语义加固与工程清债（semantic-hardening）— 已完成

**中文** | [English](semantic-hardening.en.md)

> **状态：已完成（2026-08-09）。** DoD 归档：[`archive-semantic-hardening-dod.md`](archive-semantic-hardening-dod.md)。  
> 承接：真机稳性 D1–D6 收口（[`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)）。  
> 组合：语义加固（A+B）→ 工程清债（C+D）→ Demo 深化（E）。

## 一句话

在不动 wasi-gfx / 合规全量 world 的前提下，把 CM 语义面往标准 descriptor 与资源析构推进，顺手清上游与仪器外围债，并用更丰富的 Guest demo 验收。

```text
A WIT records（render / pipeline 等）
  → B Guest/Host 资源析构接线
  → C 上游缺口本仓备忘（不对上游提 PR）
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
| 上游（C） | 以 [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) 为本仓备忘 + overlay 策略；**不对上游提 issue/PR** |
| 仪器（D） | 收口 D7：Studio 仪器路径与 `scripts/run-android-instrumented.ps1` 对齐或文档标明唯一推荐入口 |
| 验收形态 | 桌面单测（有 natives）+ Android 仪器绿灯 + Demo 手点不回归 D1–D6；每子切片文档 / CHANGELOG |

## 子切片与 DoD

### A — WIT records 扩展

- [x] `experimental:webgpu-cm` **0.4.0**：`vertex-attribute` / `vertex-buffer-layout` + `set-vertex-buffer` + `create-render-pipeline-triangle-buffers`（对照 buffer `0.2.0` 先例）
- [x] L2 + Dawn + Cpu stub + `abi-cm` + WasmtimeCmLinker 接线；旧 `create-render-pipeline-triangle` 保留
- [x] `docs/mapping/render-subset` 更新；Guest wasm 已按 `@0.4.0` 重建（仍走旧 triangle helper）
- [x] Guest 改用 buffers API（**E** 已接线）；仪器真机复验 triangle / vector-add（2026-08-08，V2458A，`run-android-instrumented.ps1` 两波）

### B — Guest 资源析构接线

- [x] 帧内等价 drop：AbiCm 跟踪 View↔Texture 配对，`present` / 下次 acquire 时 `tryDrop`（Texture 非 WIT resource）
- [x] Host `tryDrop` 幂等；`HandleTable.tryDrop`
- [x] 文档标明：配对释放 vs 仍靠 `releaseFrameResources`（encoder 孤儿）/ Demo `releaseAllGpuObjects`（Surface/Device；真 WIT dtor 仍受 wasmtime4j `resourceTable` 阻塞）
- [x] 仪器 CM triangle×N（共享 Session + `releaseAllGpuObjects`）真机复验（对照 D2/D3/D6）；Demo 手点 CM×N + L2 resume 仍建议抽空点一次
- [x] （可选增量说明）wasmtime4j destructor miss → `host.drop(rep)` 已写入 UPSTREAM §4（仅备忘，不向上游提）

### C — 上游缺口备忘（不对上游提 PR）

- [x] `ConcurrentCallCodec` unsigned-u64（及 Validation / destructor / native patches）写入本仓备忘：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)
- [x] 本仓 overlay / 过滤 jar 策略写明并长期自洽；**禁止**对本项目代提上游 issue/PR

### D — D7 仪器外围

- [x] **唯一推荐**入口：`scripts/run-android-instrumented.ps1`（两波 `am instrument` + 波间 `force-stop`；CM vector-add 与 CM triangle 不可同进程背靠背）
- [x] blockers D7 标「文档旁路正式化」；Studio / `:connectedDebugAndroidTest` 仍可能 UTP `Process crashed`（见 `docs/android-wasmtime` §7）

### E — 更丰富 Guest demo（已锁定：顶点缓冲）

- [x] Guest 上传 float32x2 顶点（`VERTEX \| COPY_DST`），`set-vertex-buffer(0, …)` 后 `draw(3)`；shader 读 `@location(0)`
- [x] 使用 `create-render-pipeline-triangle-buffers` + records；Host 仍保留旧 `create-render-pipeline-triangle`（对照）
- [x] 仪器真机复验（2026-08-08，V2458A）；桌面 CpuHost 仍 Unsupported / skip；Demo 手点建议抽空确认

## 本阶段不做

> 下表为 **semantic-hardening 当时**阶段边界。其中「合规全量 world」后由 [`compliant-world.md`](compliant-world.md) A–G 承接并完成（矩阵关门；主验收仍 experimental；仍不宣传合规产品）。

| ID | 项 |
|----|-----|
| F | wasi-gfx / canvas 抽象（compliant-world 仍不做） |
| G | 合规 `wasi:webgpu` 全量 world（当时不做；后见 compliant-world） |
| H | Maven Central / 发包 |
| I | `abi-mvp` 扁平 render import |
| — | 多 window、MSAA/depth 全套（除非 A 的最小 records 必需） |

## 落地顺序

1. **A** WIT records + L2/abi-cm/Guest 接线 + 映射文档  
2. **B** destructor → Host drop；稳性回归  
3. **C** / **D** 工程清债（可与 1–2 交错）  
4. **E** Guest demo 深化 + 仪器/手点验收  
5. 文档收口：本页 DoD 勾选 → [`archive-semantic-hardening-dod.md`](archive-semantic-hardening-dod.md)；根 README / scheme / CHANGELOG

## 链接

- 根 README：[`README.md`](../../README.md)  
- 方案索引：[`docs/scheme/README.md`](README.md)  
- 上阶段 blockers：[`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)  
- Render 映射：[`docs/mapping/render-subset.md`](../mapping/render-subset.md)  
- 上游 brief：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
- WIT：[`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)  
