# 合规 world 双轨包身份（slice B）

**中文** | [English](compliant-world-dual-track.en.md)

> **状态：** 切片 B（2026-08-09）— Linker 并存；标准包函数多为 stub。  
> 计划：[`compliant-world.md`](../scheme/compliant-world.md) · PIN：[`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)

## 一句话

同一 `WasmtimeCmLinker` 上注册两套 CM import：**experimental**（现有 Guest）与 **wasi:webgpu**（标准包骨架）；双轨是迁移手段，矩阵关门后以标准包为主验收。

## 包身份

| 轨 | Import interface | 模块 | Guest 现状 |
|----|------------------|------|------------|
| experimental | `experimental:webgpu-cm/host@0.5.0` | [`abi-cm`](../../abi-cm/) `AbiCm` | vector-add-cm / triangle-cm **仍走此轨**（C 已去特化 descriptor） |
| 标准包 | `wasi:webgpu/webgpu@0.3.0-rc.2` | [`abi-wasi`](../../abi-wasi/) `AbiWasi` | 尚无 Guest；资源已注册，函数 **Unsupported stub**（C+ 接线） |

## Linker 行为

[`WasmtimeCmLinker`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmLinker.kt) `instantiate`：

1. `registerExperimentalResources`（`AbiCm.Resource.ALL`）
2. `registerWasiResources`（`AbiWasi.Resource.ALL`，33 个）
3. `registerExperimentalImports`（现有 L2 接线）
4. `registerWasiImportStubs`（`AbiWasi.Func.ALL` → `HostException.Unsupported`）

旧 Guest 只解析 experimental 路径，不受 wasi stub 影响。

## 资源名对照（节选）

| experimental (`AbiCm`) | wasi (`AbiWasi`) | 备注 |
|------------------------|------------------|------|
| `adapter` | `gpu-adapter` | |
| `device` | `gpu-device` | |
| `queue` | `gpu-queue` | |
| `buffer` | `gpu-buffer` | |
| `shader-module` | `gpu-shader-module` | |
| `bind-group-layout` | `gpu-bind-group-layout` | |
| `bind-group` | `gpu-bind-group` | |
| `compute-pipeline` | `gpu-compute-pipeline` | |
| `render-pipeline` | `gpu-render-pipeline` | |
| `command-encoder` | `gpu-command-encoder` | |
| `compute-pass-encoder` | `gpu-compute-pass-encoder` | |
| `render-pass-encoder` | `gpu-render-pass-encoder` | |
| `command-buffer` | `gpu-command-buffer` | |
| `texture-view` | `gpu-texture-view` | |
| `surface` | `gpu-canvas-context`（近似） | Host 注入；**非** wasi-gfx |
| （无） | `gpu` | 标准包根资源 |
| （无） | `gpu-texture` / `gpu-sampler` / … | 切片 D/G |

完整方法级缺口见 [`compliant-world-gap.md`](compliant-world-gap.md)。

## 再生

```text
python scripts/gen-wasi-webgpu-inventory.py
python scripts/gen-abi-wasi-constants.py
python scripts/gen-compliant-world-gap.py
```

## 链接

- 缺口矩阵：[`compliant-world-gap.md`](compliant-world-gap.md)  
- wit-lock：[`wit/README.md`](../../wit/README.md)  
