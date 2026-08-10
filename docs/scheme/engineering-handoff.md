# 工程移交：Maven / abi-mvp render / 可选 perf（engineering-handoff）— 已完成

**中文** | [English](engineering-handoff.en.md)

> **状态：已完成（2026-08-10）。** 切片 **A–C** 已关门；归档 [`archive-engineering-handoff-dod.md`](archive-engineering-handoff-dod.md)。  
> 承接：Guest 标准 descriptor + 立方体 A–D 归档（[`archive-guest-descriptor-cube-dod.md`](archive-guest-descriptor-cube-dod.md)）。  
> 组合：Maven 可发布化工程改造（A，不对外发布）→ abi-mvp 扁平 render（B）→ 可选 perf 备注（C）。

## 一句话

在 **项目仍为 experimental、不对外发布、不宣传合规产品、不推进 wasi-gfx、不碰真 WIT dtor / 上游 PR / 真 CM async** 的前提下：把多轮阶段移交的三项工程债收口——**Maven 坐标 / 版本与 Publishing 工程化（仅本地可验证，不宣称）**、**abi-mvp（core wasm）扁平 render import 最小面**、以及 **不阻塞的 perf 边界备注更新**。真机 / Demo 验收基准 **仍为** experimental CM cube（`guest/cube-cm` / `@0.8.0`）。

```text
A Maven 可发布化改造（坐标 / 本地 Publishing 自检 / experimental 标注；不对外发布、不宣称）
  → B abi-mvp 扁平 render（对照 CM cube 子集；主验收不迁轨）
  → C 可选 perf（更新 docs/perf；不阻塞 A/B 关门）
```

对照：[`render-subset.md`](../mapping/render-subset.md) · [`docs/perf/p1-boundary.md`](../perf/p1-boundary.md) · [`archive-guest-descriptor-cube-dod.md`](archive-guest-descriptor-cube-dod.md)。

## 已定决策

| 问题 | 决定 |
|------|------|
| 本阶段范围 | 锁定 **A+B+C**；下表「本阶段不做」写死；**不做** wasi-gfx / 合规宣传 / 对外发布与「已发布」宣称 / 真 CM async / 上游 PR / 真 dtor overlay |
| 主线顺序 | **A** 与 **B** 可并行；**C** 可选旁路，**不得**阻塞 A/B 关门 |
| 主验收轨 | **仍为 experimental CM cube**；B 的 abi-mvp Guest / 单测不得取代仪器 `WasmtimeCmCubeInstrumentedTest` |
| **A Maven 目标** | 项目 **仍为 experimental、不对外进行任何发布**；允许完成坐标 / 版本策略 / Gradle Publishing 等 **可发布化工程改造**，并以本地 `publishToMavenLocal`（或等价）作自检绿灯；**不做** Maven Central / Sonatype / 任何远端仓库上传，**不做**「可对外依赖 / 已发布 / 可供消费」类宣称 |
| **A 范围模块** | 至少：`host-api`、`host-webgpu`、`abi-mvp`、`abi-cm`（及已存在的 `abi-wasi` 若随 Host 导出）；**不**把 `android-demo` / Guest wasm / 预编译 Bionic `.so` 当成拟发布主工件 |
| **B abi-mvp render** | 为 **core wasm / abi-mvp** 增加与现行 L2 上屏主链对齐的 **扁平** surface/render import（对照 [`render-subset.md`](../mapping/render-subset.md) 已接线方法的合理子集）；Cpu + 桌面单测优先；**不**要求真机仪器改走 abi-mvp |
| **B 深度 / 纹理** | 允许接入已有 L2：`depth` / `write-texture` / `set-bind-group` 等；**不做** MSAA / 多光源 / PBR / wasi-gfx；**不**新建第二套 CM Guest 作主 Demo |
| **C perf** | 刷新 [`docs/perf/`](../perf/)：去掉已删除 vector-add 仪器锚点；可选桌面烟测对照「Guest→L2」vs「纯 Kotlin→L2」；**不是**正式基准 / JMH / 倍率达标 |
| Async | 仍 **sync-compat** |
| 合规 / 发布宣称 | 包名 / README / POM `description` 保持 `experimental`；**不**宣称合规 `wasi:webgpu` 产品；工程化改造 **不等于** 对外发布或可供依赖的宣称 |
| 上游 | overlay / 补丁自洽；**不对** tegmentum/wasmtime4j 提 issue/PR；**不做**真 WIT dtor overlay |
| 验收形态 | A：工程化配置 + 本地 `publishToMavenLocal`（或等价）自检绿灯（非发布）；B：`:abi-mvp:test` / 相关 runtime 单测 + 映射表勾选；C：文档更新即可；CM cube 仪器不回归；每子切片 CHANGELOG |

## 子切片与 DoD

### A — Maven 可发布化改造（不对外发布）

- [x] 钉定 `groupId` / 模块 `artifactId` / 版本策略（与根 README 包名 `io.github.fenriliuguang.wasi.webgpu.experimental.*` 一致）；标明 **experimental / 非合规**；文档写明 **不对外发布**
- [x] Gradle Publishing（或等价）使核心库可本地 `publishToMavenLocal` 自检；列出工程化模块集合与**明确不纳入**项（demo、jniLibs 巨包、Guest 源）
- [x] 文档：本地坐标 / 工程边界示例，以及与 Bionic / desktop-natives「自建 `.so`」边界（按 [`android-wasmtime.md`](../android-wasmtime.md)）；**不**写对外发布步骤或「可供依赖」指引
- [x] CHANGELOG + 根 README「状态」同步；强调仍为实验性、**无**对外发布与发布宣称

### B — abi-mvp 扁平 render

- [x] 在 `abi-mvp` / 相关 runtime 接线中增加扁平 surface/render import（对照 CM cube 主链：configure / get-current-texture-view / present / render-pipeline / begin-render-pass / draw 等合理子集；深度与 `write-texture` 可按 L2 已有能力 additive）
- [x] Cpu Host 路径单测覆盖主链；Dawn 路径以现有 L2 为准，**不**强制新增仪器用例
- [x] 更新 [`render-subset.md`](../mapping/render-subset.md) / EN：`abi-mvp` 扁平 render 行由 ❌ → 本阶段子集状态；注明 **主验收仍 CM cube**
- [x] CHANGELOG；确认 `run-android-instrumented.ps1`（CM cube）不回归

### C — 可选 perf（不阻塞）

- [x] 更新 [`docs/perf/p1-boundary.md`](../perf/p1-boundary.md) / EN：锚点从已删除 vector-add 仪器迁到现行可复现路径（桌面 CM cube 烟测或 abi-mvp 子集；写明「非正式基准」）
- [x] （可选）保留或新增不进 CI 门禁的 timing smoke；失败不挡 A/B
- [x] CHANGELOG 一行即可；**无**倍率 / 帧率 DoD

## 本阶段不做

| ID | 项 |
|----|-----|
| — | wasi-gfx / canvas / 多 window 抽象（上屏继续 Host 注入） |
| — | 宣传已合规 `wasi:webgpu` 产品；任何「已发布 / 可供外部依赖」宣称（工程化 ≠ 发布宣称） |
| — | 真 CM async / WASI Preview3 异步运行时（保持 sync-compat；备忘见 [`true-cm-async-memo.md`](true-cm-async-memo.md)，**本阶段后**再议） |
| — | 对 tegmentum/wasmtime4j 提 issue/PR；真 WIT dtor / `JniComponentLinker` rep-only overlay |
| — | 将主 Demo / 真机验收迁到 abi-mvp 或 `wasi:webgpu` Guest |
| — | 扫完缺口矩阵长尾（query-set / render-bundle / features·limits 等） |
| — | MSAA、多光源、PBR、运行时下载纹理 |
| — | 任何对外发布（Maven Central / Sonatype / 远端仓库上传）；强制 JMH / 正式 perf 契约 |
| — | 把预编译 Bionic `libwasmtime4j.so` / 全量 jniLibs 当成拟发布主工件 |

## 落地顺序（已完成）

1. ~~**A** 与 **B** 可并行；建议先 A 钉坐标再 B 避免 artifact 边界摇摆~~  
2. ~~**C** 随时可做，不阻塞关门~~  
3. ~~文档收口：本页 DoD 勾选 → archive；根 README / scheme / CHANGELOG~~  

**已完成：** DoD → [`archive-engineering-handoff-dod.md`](archive-engineering-handoff-dod.md)；根 README / scheme / CHANGELOG 已同步。

## 链接

- 根 README：[`README.md`](../../README.md)  
- 方案索引：[`docs/scheme/README.md`](README.md)  
- 本阶段归档：[`archive-engineering-handoff-dod.md`](archive-engineering-handoff-dod.md)  
- 上阶段归档：[`archive-guest-descriptor-cube-dod.md`](archive-guest-descriptor-cube-dod.md)  
- Render / Compute：[`render-subset.md`](../mapping/render-subset.md) · [`compute-subset.md`](../mapping/compute-subset.md)  
- Perf 备注：[`docs/perf/p1-boundary.md`](../perf/p1-boundary.md)  
- 本地 Maven：[`docs/maven-local.md`](../maven-local.md)  
- Android natives：[`docs/android-wasmtime.md`](../android-wasmtime.md) · [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
- 双轨 / 缺口（只读）：[`compliant-world-dual-track.md`](../mapping/compliant-world-dual-track.md) · [`compliant-world-gap.md`](../mapping/compliant-world-gap.md)  
- 后续立项：[`true-cm-async.md`](true-cm-async.md)（备忘史实 [`true-cm-async-memo.md`](true-cm-async-memo.md)）  

