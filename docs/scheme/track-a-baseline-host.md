# 轨 A 主线：L2 / cube 基线养护 + 跟 B 扩 Host（track-a-baseline-host）

**中文** | [English](track-a-baseline-host.en.md)

> **状态：已完成（2026-08-15）。** 切片 **A→B** 已关门；归档 [`archive-track-a-baseline-host-dod.md`](archive-track-a-baseline-host-dod.md)。  
> 承接：真 CM async A 闸门关门（[`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md)）+ 双轨锁死（[`dual-runtime-track.md`](dual-runtime-track.md)）。  
> 组合：**A 养住基线（CI / 仪器 / 生命周期契约）** → **B 正式 Host 面收敛（跟轨 B WIT，不碰 async）**。

## 一句话

本仓（**轨 A**）主线不再开产品新阶段；工作定为 **养住 L2 + CM cube 真机基线**，并按姊妹仓轨 B 的 WIT 扩面 **先行收敛 / 文档化正式 L2 Host 面**（轨 A 先改，轨 B 跟）。仍 **锁死 sync-compat**；仍为 **experimental**；主验收仍为 `guest/cube-cm`。

```text
A 养住基线（CI 补漏 / 仪器重复 cube / 生命周期检查清单）
  → B 跟轨 B 扩 Host（正式 beginRenderPass + queueSubmit(list) + texture/view 契约）
```

对照：[`dual-runtime-track.md`](dual-runtime-track.md) · [`render-subset.md`](../mapping/render-subset.md) · [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) · 轨 B [`gap-experimental-vs-wasi-webgpu.md`](../../../wasmtime-android-kt/docs/mapping/gap-experimental-vs-wasi-webgpu.md)。

## 已定决策

| 问题 | 决定 |
|------|------|
| 本仓角色 | **轨 A**：可演示 / CI / 真机 CM cube；**不是**真 CM async 产品线 |
| 本阶段范围 | 锁定 **A+B**；下表「本阶段不做」写死 |
| 主线顺序 | **A → B** 强序：先把门禁与生命周期写稳，再收敛正式 Host 面 |
| 主验收轨 | **仍为 experimental CM cube**（`WasmtimeCmCubeInstrumentedTest` + `scripts/run-android-instrumented.ps1`） |
| Async | **锁死 sync-compat**；禁止为真 async 改 Dawn 主回调 / Linker future / 仪器迁 async Guest |
| 跟轨 B | L2 接口变更 **轨 A 先改**；本地 `publishEngineeredToMavenLocal`；**不**在本仓改轨 B 源码 |
| 扩面纪律 | 只收敛轨 B **已用或明确下一刀要用** 的正式面；**不**无消费者扫缺口矩阵长尾 |
| 合规 / 发布 | 保持 `experimental`；本地 Publishing **≠** 对外发布；**不**宣传合规 `wasi:webgpu` |
| 上游 | 不对 tegmentum/wasmtime4j 默认提 issue/PR；**不做**真 WIT dtor overlay |
| 验收形态 | A：CI + Cpu 单测 +（有设备）仪器脚本；B：正式路径单测 + mapping/KDoc；每子切片 CHANGELOG |

## 子切片与 DoD

### A — 养住基线（可靠性）

- [x] **CI：** [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) JVM job 增加 `:abi-cm:test`（及低成本 `:abi-wasi:test`）；android-assemble 增加 `publishEngineeredToMavenLocal` 自检（失败即红；不上传远端）
- [x] **仪器：** [`WasmtimeCmCubeInstrumentedTest`](../../android-demo/src/androidTest/java/io/github/fenriliuguang/wasi/webgpu/demo/WasmtimeCmCubeInstrumentedTest.kt) 在现有 one-shot + 8 帧之外，增加 **同 Session 连续 `runCube` ×N**（默认 3），覆盖历史 D6 二次 CM；测后仍 `releaseAllGpuObjects`
- [x] **Cpu 不累积：** 巩固 [`AbiCmHostBindingsTest`](../../abi-cm/src/test/) 帧/句柄不累积断言；仪器侧若无计数 API 则保持「无异常完成」
- [x] **养护检查清单（本页或 dual-runtime-track 短节）：** 假 WIT dtor → `tryDrop` / `releaseLifetimeSafetyNets` / `releaseAllGpuObjects`；进程级 CM linker → 波间 `force-stop` + 单进程复用 Session；门禁命令钉死
- [x] CHANGELOG + 根 README「状态」同步；归档为本阶段关门

### B — 跟轨 B 扩 Host（正式面收敛）

轨 B 现状仍可能走 deprecated / 快捷路径：`begin-render-pass-clear`、`queueSubmit1`、一步 `surface-get-current-texture-view`。L2 **已有**正式面，本切片负责可依赖化：

- [x] **正式面标注：** KDoc + [`render-subset.md`](../mapping/render-subset.md) / EN 写明轨 B 应迁到 `commandEncoderBeginRenderPass(descriptor)`、`queueSubmit(list)`、`surfaceGetCurrentTexture` + `textureCreateView`；clear helper / `queueSubmit1` 保留兼容窗口并标 deprecated 迁移目标
- [x] **单测：** abi-cm / abi-mvp Cpu 路径增加正式 `beginRenderPass` + `queueSubmit(list)` 最小 clear→finish→submit
- [x] **texture / view 生命周期短契约：** present 后 `tryDrop` / 帧对释放写清，供轨 B 拆提案两步名
- [x] **本轮不新增** adapter `features` / `limits` / `info`、`deviceDestroy`、`on-submitted-work-done`（等轨 B 明确下一刀再开）
- [x] 本地 `publishEngineeredToMavenLocal`；CHANGELOG；**不**改轨 B 仓

## 养护检查清单（已执行勾选）

| 项 | 命令 / 锚点 |
|----|-------------|
| JVM 与 cube 相关单测 | `./gradlew :host-api:test :abi-cm:test`（建议再加 `:abi-mvp:test :abi-wasi:test`） ✅ |
| 工程化坐标自检 | `./gradlew publishEngineeredToMavenLocal` ✅ |
| 真机主门禁 | `./scripts/run-android-instrumented.ps1`（**勿**依赖 Studio UTP） ✅ `OK (3 tests)` |
| 假 dtor 保险 | View↔Texture `tryDrop`；Session `releaseLifetimeSafetyNets`；Demo/仪器 `releaseAllGpuObjects` ✅ |
| 进程全局 CM | 波间 `am force-stop`；同进程复用 Session，避免背靠背关死 linker ✅ |

## 本阶段不做

| ID | 项 |
|----|-----|
| — | 真 CM async / WASI P3 / Java future writer；改 Dawn 主回调阻塞模型 |
| — | 静默把仪器 / Demo 默认 L1 换成轨 B |
| — | 宣传合规 `wasi:webgpu`；Maven Central / 任何对外发布宣称 |
| — | 真 WIT dtor overlay；对 tegmentum/wasmtime4j 默认提 PR |
| — | 为 `host-webgpu` 上完整无设备 Dawn JVM 单测（主路径仍靠仪器） |
| — | natives / Guest wasm artifact 化 |
| — | 扫完缺口矩阵长尾；无消费者的 adapter 元数据 / query 完成回调扩面 |
| — | wasi-gfx / 多 window |

## 落地顺序（已完成）

1. ~~**先 A**（CI + 仪器重复 cube + 检查清单文档对齐）~~  
2. ~~**再 B**（正式 Host 面单测与 mapping；publish 自检）~~  
3. ~~关门：本页 DoD 勾选 → 写 `archive-track-a-baseline-host-dod.md`；同步根 README / scheme / CHANGELOG~~  

**已完成：** DoD → [`archive-track-a-baseline-host-dod.md`](archive-track-a-baseline-host-dod.md)；根 README / scheme / CHANGELOG 已同步（2026-08-15）。

## 链接

- 本阶段归档：[`archive-track-a-baseline-host-dod.md`](archive-track-a-baseline-host-dod.md)  
- 双轨锁死：[`dual-runtime-track.md`](dual-runtime-track.md)  
- 真 async 闸门归档：[`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md)  
- 稳性 blockers：[`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md)  
- Maven 本地自检：[`../maven-local.md`](../maven-local.md)  
- 轨 B 差距表：[`../../../wasmtime-android-kt/docs/mapping/gap-experimental-vs-wasi-webgpu.md`](../../../wasmtime-android-kt/docs/mapping/gap-experimental-vs-wasi-webgpu.md)  
- 轨 B 双轨契约：[`../../../wasmtime-android-kt/docs/scheme/dual-track.md`](../../../wasmtime-android-kt/docs/scheme/dual-track.md)  
