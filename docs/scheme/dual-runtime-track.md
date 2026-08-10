# 双运行时轨（轨 A 锁死 sync-compat / 轨 B 自研 L1）

**中文** | [English](dual-runtime-track.en.md)

> **状态：生效（2026-08-10）。**  
> 轨 B 仓：[`d:\projects\wasmtime-android-kt`](../../../wasmtime-android-kt)（计划文档已立项；无代码）。  
> 契约正文以轨 B [`dual-track.md`](../../../wasmtime-android-kt/docs/scheme/dual-track.md) 为准；本页是轨 A 侧锁死声明与索引。

## 一句话

本仓（**轨 A**）继续作为可演示 / CI / 真机 CM cube 主线，**锁死 sync-compat**；真 CM async 与 Android-first 自研 Wasmtime L1 迁到 **轨 B**，互不阻塞。

## 锁死条款（本仓）

1. 默认路径与主验收保持 **sync-compat**（见 [`errors-async.md`](../mapping/errors-async.md)）。  
2. **不再**为真 CM async 改 `DawnWasiWebGpuHost` 主回调阻塞模型、`WasmtimeCmLinker` 主链 future、或把仪器迁到 async Guest。  
3. [`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md) 闸门结论保持有效（4j 无 Java future writer）。  
4. 允许继续：稳性、非 async 缺口、文档、工程债、Maven 本地自检等。  
5. 将来若改用轨 B 作 L1，须独立 RFC + 双轨绿灯；**禁止**静默替换 `run-android-instrumented.ps1` 主门禁。

## 分工

| | 轨 A（本仓） | 轨 B（`wasmtime-android-kt`） |
|--|-------------|-------------------------------|
| L1 | wasmtime4j + 补丁 | 官方 Wasmtime + 自研 JNI |
| Async | **sync-compat（锁死）** | 目标真 CM async |
| 验收 | CM cube 仪器 | 独立 smoke；不取代本仓门禁 |
| Host | L2 源仓库 | 依赖本仓 L2（代码期） |

## 链接

- 轨 B 章程：[`wasmtime-android-kt/docs/scheme/charter.md`](../../../wasmtime-android-kt/docs/scheme/charter.md)  
- 轨 B 双轨契约：[`dual-track.md`](../../../wasmtime-android-kt/docs/scheme/dual-track.md)  
- 真 CM async 闸门归档：[`archive-true-cm-async-dod.md`](archive-true-cm-async-dod.md)  
- UPSTREAM §5：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
