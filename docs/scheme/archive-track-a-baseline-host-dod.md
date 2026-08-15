# 轨 A 主线 DoD 归档（已完成）

**中文** | [English](archive-track-a-baseline-host-dod.en.md)

> 自根 README / 计划页迁出的**已完成**验收清单归档。  
> 原计划页：[`track-a-baseline-host.md`](track-a-baseline-host.md)。承接：[`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md) + [`dual-runtime-track.md`](dual-runtime-track.md)。

归档对应：切片 A（CI / 仪器重复 cube / 生命周期检查清单）→ B（正式 Host 面收敛），2026-08-15。

**重要：** 仍为 **experimental**；本地 Publishing **≠** 对外发布；async **锁死 sync-compat**；主验收仍为 CM cube。**不是**真 async / 合规产品阶段。

## DoD

### A — 养住基线（可靠性）

- [x] CI JVM job 增加 `:abi-cm:test` / `:abi-wasi:test`；android-assemble 增加 `publishEngineeredToMavenLocal`（失败即红；不上传远端）
- [x] 仪器 `WasmtimeCmCubeInstrumentedTest` 同 Session 连续 `runCube` ×3（D6）；测后仍 `releaseAllGpuObjects`；首启 RESUMED 等待加固
- [x] Cpu：`AbiCmHostBindingsTest` 多帧 + 正式 beginRenderPass/`queueSubmit(list)` + 两步 acquire 不累积句柄
- [x] 养护检查清单钉死（本计划页 + [`dual-runtime-track.md`](dual-runtime-track.md) 短节）
- [x] CHANGELOG + 根 README 状态同步

### B — 跟轨 B 扩 Host（正式面收敛）

- [x] KDoc + [`render-subset.md`](../mapping/render-subset.md) / EN：轨 B 应迁 `commandEncoderBeginRenderPass(descriptor)`、`queueSubmit(list)`、`surfaceGetCurrentTexture` + `textureCreateView`；clear helper / `queueSubmit1` 保留 deprecated 兼容窗口
- [x] abi-cm / abi-mvp Cpu 正式 `beginRenderPass` + `queueSubmit(list)` 最小 clear→finish→submit 单测
- [x] texture / view 生命周期短契约（present 后 `tryDrop` / 帧对释放）写清
- [x] 本轮不新增 adapter `features` / `limits` / `info`、`deviceDestroy`、`on-submitted-work-done`
- [x] 本地 `publishEngineeredToMavenLocal`；CHANGELOG；**未**改轨 B 仓

## 关键交付物

- CI：[`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)
- 仪器：`cmGuestCubeSameSessionRepeatRunCube` + `scripts/run-android-instrumented.ps1`
- 正式面：`WasiWebGpuHost` / `AbiCmHostBindings` / `AbiMvpHostBindings` KDoc；AbiCm `surfaceGetCurrentTexture` 两步入口
- 映射：[`render-subset.md`](../mapping/render-subset.md)「轨 B 正式 Host 面」节
- 未做（阶段排除）：真 CM async、静默换轨 B L1、合规宣传 / 对外发布、真 WIT dtor overlay、无设备 Dawn JVM 全套、natives/Guest artifact 化、缺口矩阵长尾、wasi-gfx
