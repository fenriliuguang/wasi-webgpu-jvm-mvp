# 语义加固与工程清债 DoD 归档（已完成）

**中文** | [English](archive-semantic-hardening-dod.en.md)

> 自根 README / 计划页迁出的**已完成**验收清单归档。  
> 原计划页：[`semantic-hardening.md`](semantic-hardening.md)。承接真机回归：[`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)。

归档对应提交约至：`66bee92`（锁定 A–E）→ `904c36e`（A / 0.4.0 records）→ `61da76d`（E 顶点缓冲）→ `1c53c14`（B 帧内配对释放）→ `71ecc18` / `c22c408`（C 上游备忘）→ `f477c2d`（D7 两波脚本）+ 本文档收口（2026-08-09）。

## DoD

### A — WIT records

- [x] `experimental:webgpu-cm` **0.4.0**：`vertex-attribute` / `vertex-buffer-layout` + `set-vertex-buffer` + `create-render-pipeline-triangle-buffers`
- [x] L2 + Dawn + Cpu stub + `abi-cm` + WasmtimeCmLinker；旧 `create-render-pipeline-triangle` 保留
- [x] `docs/mapping/render-subset`；Guest wasm `@0.4.0`

### B — 资源析构（帧内等价）

- [x] AbiCm View↔Texture 配对；`present` / 下次 acquire 时 `tryDrop`
- [x] Host / `HandleTable.tryDrop` 幂等；真 WIT dtor 仍受 wasmtime4j `resourceTable` 阻塞（见 UPSTREAM）
- [x] 仪器 CM triangle×N + `releaseAllGpuObjects` 真机复验（对照 D2/D3/D6）

### C — 上游缺口备忘

- [x] [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)：ConcurrentCallCodec u64、Validation、destructor、native patches、overlay；**不对上游提 issue/PR**

### D — D7 仪器外围

- [x] 唯一推荐：`scripts/run-android-instrumented.ps1`（两波 + 波间 force-stop）
- [x] blockers D7「文档旁路正式化」；Studio UTP 仍可能 Process crashed

### E — Guest 顶点缓冲

- [x] float32x2 + `create-render-pipeline-triangle-buffers` + `set-vertex-buffer` + `@location(0)`
- [x] 仪器真机复验（2026-08-08，V2458A）；桌面 CpuHost Unsupported / skip

## 关键交付物

- WIT / Guest：`experimental:webgpu-cm@0.4.0`；`triangle_cm.wasm` 走 buffers 路径
- L2 / abi-cm：records 接线 + 帧内 View↔Texture `tryDrop`
- 上游：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) 长期自洽备忘
- 仪器：两波 `am instrument` 脚本为唯一推荐入口
- 映射：[`docs/mapping/render-subset.md`](../mapping/render-subset.md)
