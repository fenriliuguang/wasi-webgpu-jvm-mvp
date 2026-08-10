# 工程移交 DoD 归档（已完成）

**中文** | [English](archive-engineering-handoff-dod.en.md)

> 自根 README / 计划页迁出的**已完成**验收清单归档。  
> 原计划页：[`engineering-handoff.md`](engineering-handoff.md)。承接：[`archive-guest-descriptor-cube-dod.md`](archive-guest-descriptor-cube-dod.md)。

归档对应：切片 A（Maven 可发布化，不对外发布）→ B（abi-mvp 扁平 render）→ C（可选 perf 备注），2026-08-10。

**重要：** 仍为 **experimental**；本地 Publishing **≠** 对外发布 / 可供依赖宣称；真异步备忘见 [`true-cm-async-memo.md`](true-cm-async-memo.md)（本阶段后）。主验收仍为 CM cube。

## DoD

### A — Maven 可发布化改造（不对外发布）

- [x] 钉定 `groupId` / `artifactId` / `0.1.0-experimental`；标明 experimental / 非合规 / 不对外发布
- [x] `publishEngineeredToMavenLocal`（host-api / host-webgpu / abi-mvp / abi-cm / abi-wasi）
- [x] [`docs/maven-local.md`](../maven-local.md)；明确不纳入 demo / runtime-wasmtime natives / Guest / jniLibs
- [x] CHANGELOG + README 状态同步

### B — abi-mvp 扁平 render

- [x] 扁平 surface/render import + `WasmtimeAbiLinker` 注册（对照 CM cube 子集）
- [x] Cpu Host 单测（多帧 surface + render 主链）；无新仪器用例
- [x] [`render-subset.md`](../mapping/render-subset.md) abi-mvp 行 ❌ → ⚠️ 子集；**主验收仍 CM cube**
- [x] CHANGELOG

### C — 可选 perf（不阻塞）

- [x] [`docs/perf/p1-boundary.md`](../perf/p1-boundary.md) 锚点迁到 abi-mvp / CM cube；标明非正式基准
- [x] 非门禁 `AbiMvpHostBindingsTest.boundaryNoteTimingSmoke`（无倍率断言）
- [x] CHANGELOG

## 关键交付物

- 本地坐标：[`docs/maven-local.md`](../maven-local.md)
- abi-mvp 扁平 render：`AbiMvp` / `AbiMvpHostBindings` / `WasmtimeAbiLinker`
- perf 备注：[`docs/perf/p1-boundary.md`](../perf/p1-boundary.md)
- 未做（阶段排除）：wasi-gfx、合规宣传、对外发布、真 CM async / WASI P3、上游 PR、真 WIT dtor overlay、JMH / 正式 perf 契约
