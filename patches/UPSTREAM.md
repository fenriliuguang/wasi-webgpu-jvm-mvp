# Upstream contribution notes (wasmtime4j)

**中文** | [English](UPSTREAM.en.md)

> **本阶段 C（2026-08-08）**：本文为可外发 contribution brief（非已提 PR）。  
> 对照实现仓：本仓库 `wasi-webgpu-jvm-mvp`；上游 [tegmentum/wasmtime4j](https://github.com/tegmentum/wasmtime4j)。  
> 钉定版本：`v47.0.2-1.5.0`（`ai.tegmentum:wasmtime4j`）。

仅在有精力维护上游时再开 issue/PR。本仓 **overlay / 过滤 jar / 本地 patch 策略保持不变**，上游合并后可再撤本地覆盖。

---

## 一句话状态

| 项 | 状态 | 本仓 workaround |
|----|------|-----------------|
| Native Android / CM resources patches | 已入库 diff，构建脚本 `git apply` | `scripts/build-wasmtime4j-*.ps1` |
| Java `ConcurrentCallCodec` unsigned-u64 | **优先外发**；上游未修 | android-demo 过滤 jar + 本地类 |
| Java `Validation` TBI 句柄 | 可随 u64 一并提 | 同上 |
| CM resource destructor → Host `drop(rep)` | **建议外发**（本阶段 B 记录缺口） | AbiCm View↔Texture 配对 + `releaseFrame*` 保险 |

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

### 建议上游改动（最小）

与 `ComponentTypeCodec` 对齐：

1. **Parse**：`Long.parseLong` 失败时 `Long.parseUnsignedLong(numStr)`（bits 经 `Number.longValue()` 回传）
2. **Serialize U64**：`Long.toUnsignedString(val.asU64())`，勿用有符号 `append(long)`

### 本仓对照实现

- Overlay：[`android-demo/.../ConcurrentCallCodec.java`](../android-demo/src/main/java/ai/tegmentum/wasmtime4j/component/ConcurrentCallCodec.java)
- 构建：[`android-demo/build.gradle.kts`](../android-demo/build.gradle.kts) 过滤 jar，排除 `ConcurrentCallCodec*.class`（含内部类）后用本地类
- 踩坑记录：[`docs/scheme/guest-onscreen-cm-blockers.md`](../docs/scheme/guest-onscreen-cm-blockers.md) **P2**

### 验收建议（上游）

单元测试：`parseNumber("18446744073709551615")`（或任一 `> Long.MAX_VALUE` 的十进制）不抛；U64 serialize → parse 位型相等。

---

## 3. Java：`Validation`（TBI / opaque handle）

| Local class | Problem | Suggested upstream change |
|-------------|---------|---------------------------|
| `ai.tegmentum.wasmtime4j.util.Validation` | ARM64 TBI/PAC 指针按 signed `long` 看像“负数”被拒 | 允许非零 opaque handle（或文档说明 bit-cast） |

本仓同样经过滤 jar + android-demo 本地类覆盖。可与 §2 同 PR 或分 issue。

---

## 4. CM resource destructor → Host `drop(rep)`（建议外发）

### 缺口

本仓 L1 将 WIT resource rep 当作 **L2 `GpuHandle.raw`（u32）** 往返，**不**走 Java `ComponentResourceDefinition.constructor` → private `resourceTable.push`。

因此即使用 `.destructor { … }`：

1. JNI `dispatchDestructor` 先 `resourceTable.delete(rep)`
2. table miss → **Consumer 永不执行**
3. Guest `drop` 资源时 Host 句柄表仍钉住 Dawn 对象（Surface/Texture 等）

本仓对策（本阶段 B）：AbiCm **View↔Texture 配对** `tryDrop` + `releaseFrameResources` / Demo `releaseAllGpuObjects` 保险——**不是**真 WIT destructor。

### 建议上游改动（二选一或组合）

**A（推荐，兼容 L2-rep 模型）**  
`resourceTable` miss 时仍调用可选 `IntConsumer` / destructor，参数为 **rep u32**（由 host 自行映射到自有表）。

**B**  
文档明确：destructor 仅在走了 `.constructor` 填充 `resourceTable` 时生效；并提供「rep-only host resources」官方示例。

相关：已有 CM resources native patch（§1）；destructor 路径是 Java/JNI 侧增量。本仓计划：[`docs/scheme/semantic-hardening.md`](../docs/scheme/semantic-hardening.md) 子切片 B/C。

---

## 5. 本仓 overlay 策略（切换说明）

| 层 | 策略 | 上游合并后 |
|----|------|------------|
| Native `.so` | 构建时 `git apply` 入库 patch | 升依赖版本；可删对应 patch 段 |
| Java Validation / ConcurrentCallCodec | 过滤 Maven jar + android-demo 同名包覆盖 | 去掉 `filterWasmtime4jJar` exclude 与本地类 |
| 帧 Texture 清理 | AbiCm 配对 + Host sweep | 若 §4A 落地，可收窄 sweep 语义依赖 |

**本阶段不强制上游 PR 合并**；brief 就绪即可勾选 C。

---

## 建议外发顺序

1. **Issue / 小 PR**：`ConcurrentCallCodec` unsigned-u64（§2）— 纯 Java、风险低、本仓已验证  
2. **Issue**：`Validation` opaque handles（§3）  
3. **Issue**：CM destructor miss → rep callback（§4）— 需设计评审  
4. Native patches（§1）— 体积更大，可另开里程碑  
