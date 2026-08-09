# wit-lock

**中文** | [English](README.en.md)

钉死上游子集，避免 Host 随 WIT tip 漂移。

| 字段 | 值 |
|------|----|
| 包 | `wasi:webgpu/webgpu` |
| 版本 | `0.3.0-rc.2` |
| 上游 | https://github.com/WebAssembly/wasi-webgpu |
| Tag | `v0.3.0-rc.2` |
| imports 摘要 | https://github.com/WebAssembly/wasi-webgpu/blob/v0.3.0-rc.2/wit/imports.wit |
| **标准包 vendor（切片 A）** | [`deps/wasi-webgpu/`](deps/wasi-webgpu/) — [`PIN.md`](deps/wasi-webgpu/PIN.md) · `webgpu.wit` · `imports.wit` · [`_inventory.json`](deps/wasi-webgpu/_inventory.json) |
| 本仓 L2 范围 | 仍以 **compute + 最小 surface/render 子集** 为主（见映射表）；全量覆盖见缺口矩阵 |
| P1 Guest ABI | **abi-mvp**（`wasi-webgpu-mvp` core imports，**非** CM / 非合规） |
| CM 切片（双轨） | [`compute-cm/`](compute-cm/) — `experimental:webgpu-cm@0.6.0`（**仍非**合规）；与标准包并存直至 Guest 迁完 |
| 标准包 ABI（切片 B） | [`abi-wasi`](../abi-wasi/) `AbiWasi` — import `wasi:webgpu/webgpu@0.3.0-rc.2`；Linker 已注册资源 + Unsupported stub；见 [`compliant-world-dual-track.md`](../docs/mapping/compliant-world-dual-track.md) |
| 阶段计划 | [`docs/scheme/compliant-world.md`](../docs/scheme/compliant-world.md) · 缺口 [`docs/mapping/compliant-world-gap.md`](../docs/mapping/compliant-world-gap.md) |

## 双轨说明

- **experimental**：现有 Guest（vector-add-cm / triangle-cm）与 `abi-cm` / Linker 仍走 `experimental:webgpu-cm@0.6.0`。  
- **标准包**：已 vendor 钉定；接线与双轨 Linker 属切片 **B+**；矩阵关门前**不得**宣传合规。  
- **升级**：先改 [`deps/wasi-webgpu/PIN.md`](deps/wasi-webgpu/PIN.md) → `python scripts/gen-wasi-webgpu-inventory.py` → `python scripts/gen-compliant-world-gap.py` → 再改 Host / ABI。

P0/P1 **不**生成完整 wit-bindgen host。CM 切片仍钉定本仓 `wit/compute-cm/world.wit`。
