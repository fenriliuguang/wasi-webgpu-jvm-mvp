# Upstream contribution notes (wasmtime4j)

**中文** | [English](UPSTREAM.en.md)

> **本阶段 C（2026-08-08）**：本文为**本仓备忘**（对照上游缺口与本地 workaround）。  
> 对照实现仓：本仓库 `wasi-webgpu-jvm-mvp`；上游参考 [tegmentum/wasmtime4j](https://github.com/tegmentum/wasmtime4j)。  
> 钉定版本：`v47.0.2-1.5.0`（`ai.tegmentum:wasmtime4j`）。

**硬约束：本项目不对上游仓库提交 issue / PR，也不代用户推送。**  
依赖本仓 **overlay / 过滤 jar / 本地 patch** 长期自洽；若上游自行合并同类修复，再考虑撤本地覆盖。

---

## 一句话状态

| 项 | 状态 | 本仓 workaround |
|----|------|-----------------|
| Native Android / CM resources patches | 已入库 diff，构建脚本 `git apply` | `scripts/build-wasmtime4j-*.ps1` |
| Java `ConcurrentCallCodec` unsigned-u64 | 上游未修；备忘优先项 | android-demo 过滤 jar + 本地类 |
| Java `Validation` TBI 句柄 | 上游未修 | 同上 |
| CM resource destructor → Host `drop(rep)` | 缺口已记录（本阶段 B） | AbiCm View↔Texture 配对 + `releaseFrame*` 保险 |

---

## 1. Native patches（已入库）

| Patch | Problem | Suggested upstream change |
|-------|---------|---------------------------|
| [`wasmtime4j-v47.0.2-1.5.0-android.patch`](wasmtime4j-v47.0.2-1.5.0-android.patch) | ART rejects `JNI_VERSION_1_8` from `JNI_OnLoad` | Return `JNI_VERSION_1_6`（或 detect Android / max supported） |
| same android patch | Signed `jlong` compare treats MTE/TBI-tagged pointers as corrupt | Compare handles as `u64`（`memory_ptr as u64 < 0x1000`，tables 同理） |
| [`wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](wasmtime4j-v47.0.2-1.5.0-cm-resources.patch) | CM host callbacks need `Resource` ↔ `U32(rep)`；多 resource 注册；instantiate 时 fresh linker | Resource marshalling helpers；`allow_shadowing` / batch re-define；process-level registry replay for `nativeInstantiateWithLinker` |

### Reproduce

```powershell
# Android .so → runtime-wasmtime/android-natives/jniLibs/
./scripts/build-wasmtime4j-android.ps1

# Desktop CM → runtime-wasmtime/desktop-natives/<platform>/
./scripts/build-wasmtime4j-desktop-cm.ps1
./gradlew :runtime-wasmtime:test
```

Re-export after editing `.deps/wasmtime4j`:

```powershell
python ./scripts/export-wasmtime4j-patches.py
```

---

## 2. Java：`ConcurrentCallCodec` unsigned-u64（优先 brief）

### 现象

CM host 回调经 JSON 编组时：

```text
E JniComponentLinker: Host function callback failed for ID: …
  For input string: "12970367413882346528"
```

该十进制串是 **无符号 u64**（常见于 Android `ANativeWindow*` / TBI·PAC 高位指针），`> Long.MAX_VALUE`。

### 根因

- Rust 侧 `serde_json` 对 u64 发 **无符号十进制**
- 上游 `ConcurrentCallCodec.parseNumber` 用 `Long.parseLong` → `NumberFormatException`
- 同库 `ComponentTypeCodec.parseNumber` **已有** `Long.parseUnsignedLong` 回退；`ConcurrentCallCodec` 漏对齐
- U64 **序列化**若用有符号十进制，高位句柄 round-trip 也会坏

### 期望行为（若上游自行修复；本仓不代提）

与 `ComponentTypeCodec` 对齐即可：

1. **Parse**：`Long.parseLong` 失败时 `Long.parseUnsignedLong(numStr)`（bits 经 `Number.longValue()` 回传）
2. **Serialize U64**：`Long.toUnsignedString(val.asU64())`，勿用有符号 `append(long)`

### 本仓对照实现

- Overlay：[`android-demo/.../ConcurrentCallCodec.java`](../android-demo/src/main/java/ai/tegmentum/wasmtime4j/component/ConcurrentCallCodec.java)
- 构建：[`android-demo/build.gradle.kts`](../android-demo/build.gradle.kts) 过滤 jar，排除 `ConcurrentCallCodec*.class`（含内部类）后用本地类
- 踩坑记录：[`docs/scheme/guest-onscreen-cm-blockers.md`](../docs/scheme/guest-onscreen-cm-blockers.md) **P2**

### 本仓自测要点

`parseNumber` 接受任一 `> Long.MAX_VALUE` 的无符号十进制不抛；U64 serialize → parse 位型相等（见本地 overlay）。

---

## 3. Java：`Validation`（TBI / opaque handle）

| Local class | Problem | Suggested upstream change |
|-------------|---------|---------------------------|
| `ai.tegmentum.wasmtime4j.util.Validation` | ARM64 TBI/PAC 指针按 signed `long` 看像“负数”被拒 | 允许非零 opaque handle（或文档说明 bit-cast） |

本仓同样经过滤 jar + android-demo 本地类覆盖。

---

## 4. CM resource destructor → Host `drop(rep)`（缺口备忘）

### 缺口

本仓 L1 将 WIT resource rep 当作 **L2 `GpuHandle.raw`（u32）** 往返，**不**走 Java `ComponentResourceDefinition.constructor` → private `resourceTable.push`。

因此即使用 `.destructor { … }`：

1. JNI `dispatchDestructor` 先 `resourceTable.delete(rep)`
2. table miss → **Consumer 永不执行**
3. Guest `drop` 资源时 Host 句柄表仍钉住 Dawn 对象（Surface/Texture 等）

本仓对策（本阶段 B）：AbiCm **View↔Texture 配对** `tryDrop` + `releaseFrameResources` / Demo `releaseAllGpuObjects` 保险——**不是**真 WIT destructor。

### 期望行为（若上游自行修复；本仓不代提）

**A（兼容 L2-rep 模型）**  
`resourceTable` miss 时仍调用可选 `IntConsumer` / destructor，参数为 **rep u32**（由 host 自行映射到自有表）。

**B**  
文档明确：destructor 仅在走了 `.constructor` 填充 `resourceTable` 时生效；并提供「rep-only host resources」示例。

相关：已有 CM resources native patch（§1）。本仓计划：[`docs/scheme/semantic-hardening.md`](../docs/scheme/semantic-hardening.md) 子切片 B。

---

## 5. 本仓 overlay 策略

| 层 | 策略 | 若上游日后自带同类修复 |
|----|------|------------------------|
| Native `.so` | 构建时 `git apply` 入库 patch | 升依赖版本；可删对应 patch 段 |
| Java Validation / ConcurrentCallCodec | 过滤 Maven jar + android-demo 同名包覆盖 | 去掉 `filterWasmtime4jJar` exclude 与本地类 |
| 帧 Texture 清理 | AbiCm 配对 + Host sweep | 若 §4A 类行为出现，可收窄 sweep 语义依赖 |

**C 验收**：本仓备忘写清即可；**不对上游提 issue/PR**。
