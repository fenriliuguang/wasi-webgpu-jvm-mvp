# 双运行时轨（轨 A 锁死 sync-compat / 轨 B 自研 L1）

**中文** | [English](dual-runtime-track.en.md)

> **状态：生效（2026-08-10；轨 B 进度同步 2026-08-12；轨 A 主线关门 2026-08-15）。**  
> 轨 B 仓：[`../wasmtime-android-kt`](../../../wasmtime-android-kt) — 短期 M0–M5 薄 L1 **已归档**；现行主线 WASI 0.3 + `wasi:webgpu`（W1/W2 已交付，W3+ 扩面中）。  
> 轨 A **主线已完成：** [`track-a-baseline-host.md`](track-a-baseline-host.md) → [`archive-track-a-baseline-host-dod.md`](archive-track-a-baseline-host-dod.md)（L2 / cube 基线养护 + 跟 B 扩 Host）。  
> 契约正文以轨 B [`dual-track.md`](../../../wasmtime-android-kt/docs/scheme/dual-track.md) 为准；本页是轨 A 侧锁死声明与索引。

## 一句话

本仓（**轨 A**）继续作为可演示 / CI / 真机 CM cube 主线，**锁死 sync-compat**；真 CM async 与 Android-first 自研 Wasmtime L1 迁到 **轨 B**，互不阻塞。轨 A 主线（养住 L2 + cube，并按轨 B WIT 跟 Host）**已关门**。

## 锁死条款（本仓）

1. 默认路径与主验收保持 **sync-compat**（见 [`errors-async.md`](../mapping/errors-async.md)）。  
2. **不再**为真 CM async 改 `DawnWasiWebGpuHost` 主回调阻塞模型、`WasmtimeCmLinker` 主链 future、或把仪器迁到 async Guest。  
3. [`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md) 闸门结论保持有效（4j 无 Java future writer）。  
4. 允许继续：稳性、非 async 缺口、文档、工程债、Maven 本地自检、**按轨 B WIT 跟 L2 Host** 等。  
5. 将来若改用轨 B 作 L1，须独立 RFC + 双轨绿灯；**禁止**静默替换 `run-android-instrumented.ps1` 主门禁。

## 分工

| | 轨 A（本仓） | 轨 B（`wasmtime-android-kt`） |
|--|-------------|-------------------------------|
| L1 | wasmtime4j + 补丁 | 官方 Wasmtime + 自研 JNI |
| Async | **sync-compat（锁死）** | 目标真 CM async |
| 验收 | CM cube 仪器 | 独立 smoke；不取代本仓门禁 |
| Host | L2 源仓库（主线：养护 + 跟面） | 依赖本仓 L2（代码期） |
| 现行主线 | [`track-a-baseline-host.md`](track-a-baseline-host.md) | 见轨 B roadmap / Project |

## 养护检查清单（轨 A 门禁）

权威表见 [`track-a-baseline-host.md`](track-a-baseline-host.md)。钉死：

- JVM：`./gradlew :host-api:test :abi-cm:test`（建议再加 `:abi-mvp:test :abi-wasi:test`）
- 工程化坐标：`./gradlew publishEngineeredToMavenLocal`（失败即红；不上传远端）
- 真机：`./scripts/run-android-instrumented.ps1`（**勿**依赖 Studio UTP）
- 假 dtor：View↔Texture `tryDrop`；Session `releaseLifetimeSafetyNets`；Demo/仪器 `releaseAllGpuObjects`
- 进程全局 CM：波间 `am force-stop`；同进程复用 Session

## 链接

- 轨 A 主线计划：[`track-a-baseline-host.md`](track-a-baseline-host.md)  
- 轨 A 主线归档：[`archive-track-a-baseline-host-dod.md`](archive-track-a-baseline-host-dod.md)  
- 轨 B 章程：[`wasmtime-android-kt/docs/scheme/charter.md`](../../../wasmtime-android-kt/docs/scheme/charter.md)  
- 轨 B 双轨契约：[`dual-track.md`](../../../wasmtime-android-kt/docs/scheme/dual-track.md)  
- 真 CM async 闸门归档：[`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md)  
- UPSTREAM §5：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
