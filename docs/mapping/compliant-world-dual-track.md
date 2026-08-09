# 合规 world 双轨包身份

**中文** | [English](compliant-world-dual-track.en.md)

> **状态：** 切片 B–G 关门后现行双轨（2026-08-09）— Linker 并存；标准包多为 stub；**主验收 / Guest 仍走 experimental**。  
> 计划：[`compliant-world.md`](../scheme/compliant-world.md) · PIN：[`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md) · 归档：[`archive-compliant-world-dod.md`](../scheme/archive-compliant-world-dod.md)

## 一句话

同一 `WasmtimeCmLinker` 上注册两套 CM import：**experimental**（现有 Guest 与主验收轨）与 **wasi:webgpu**（标准包骨架 / stub）。双轨是迁移手段；矩阵关门 = 方法级齐套，**不等于**已把 Guest 迁到标准包，也**不等于**可宣传合规产品。

## 包身份

| 轨 | Import interface | 模块 | Guest 现状 |
|----|------------------|------|------------|
| experimental（主轨） | `experimental:webgpu-cm/host@0.8.0` | [`abi-cm`](../../abi-cm/) `AbiCm` | vector-add-cm / triangle-cm / cube-cm **仍走此轨**（标准 descriptor；guest-descriptor-cube B） |
| 标准包（双轨 stub） | `wasi:webgpu/webgpu@0.3.0-rc.2` | [`abi-wasi`](../../abi-wasi/) `AbiWasi` | 尚无 Guest；资源已注册；**result 方法** stub → `ComponentVal.err`（切片 F）；其余 **Unsupported** throw；**尚未**成为主验收路径 |

## Linker 行为

[`WasmtimeCmLinker`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmLinker.kt) `instantiate`:

1. `registerExperimentalResources`（`AbiCm.Resource.ALL`）
2. `registerWasiResources`（`AbiWasi.Resource.ALL`，33 个）
3. `registerExperimentalImports`（现有 L2 接线）
4. `registerWasiImportStubs`（result 方法 → `ComponentVal.err`；其余 → `HostException.Unsupported`）

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
