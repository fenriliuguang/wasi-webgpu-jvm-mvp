# wasmtime4j patches

**中文** | [English](README.en.md)

Trackable unified diffs against upstream tag **`v47.0.2-1.5.0`**
(`ai.tegmentum:wasmtime4j` / `gradle/libs.versions.toml`).

| 文件 | 用途 | 改动文件 |
|------|------|----------|
| [`wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](wasmtime4j-v47.0.2-1.5.0-cm-resources.patch) | 桌面 + Android CM WIT resources | `component/linker.rs`, `jni/component_linker.rs` |
| [`wasmtime4j-v47.0.2-1.5.0-android.patch`](wasmtime4j-v47.0.2-1.5.0-android.patch) | Android Bionic / ART | `async_runtime.rs`, `jni/memory.rs` |

源码检出目录 `.deps/wasmtime4j` **不入库**（见根 `.gitignore`）。  
补丁本身入库；构建脚本 clone tag 后 `git apply`。

上游缺口备忘（**semantic-hardening slice C**，2026-08-08；**不对上游提 PR**）：[`UPSTREAM.md`](UPSTREAM.md) / [EN](UPSTREAM.en.md) — ConcurrentCallCodec u64、Validation、destructor 与本地 overlay。

## 应用

```powershell
# 桌面 CM → runtime-wasmtime/desktop-natives/（不改写 Gradle cache）
./scripts/build-wasmtime4j-desktop-cm.ps1

# Android .so（默认：JNI 1_6 + 句柄无符号校验 + CM resources）
./scripts/build-wasmtime4j-android.ps1
# 仅 abi-mvp（跳过 CM）：./scripts/build-wasmtime4j-android.ps1 -SkipCmResourcesPatch
```

手动：

```bash
git clone --depth 1 --branch v47.0.2-1.5.0 \
  https://github.com/tegmentum/wasmtime4j.git .deps/wasmtime4j
git -C .deps/wasmtime4j apply --check patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch
git -C .deps/wasmtime4j apply patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch
```

## 重新导出

若在 `.deps/wasmtime4j` 上又改了 native 源码：

```powershell
python ./scripts/export-wasmtime4j-patches.py
```

然后 `git diff patches/` 审阅后提交。

## CM patch 内容摘要

1. Host callback：`Resource` ↔ `U32(rep)` 编组（`vals_to_host_params` / `host_results_to_vals`；**递归**进 list/record/option/…）
2. 同一 interface 多 `resource` 注册（`allow_shadowing` + 批量重挂）
3. 进程级 resource registry，供 `add_registered_host_functions_to_linker` 在 fresh linker 上重放（上游 `nativeInstantiateWithLinker` 不用 caller linker）
4. JNI `defineResource` 的 interface path 使用 `"{ns}/{iface}"`（与 guest import `experimental:webgpu-cm/host@0.7.0` 一致）
