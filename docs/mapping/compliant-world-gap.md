# 合规 world 缺口矩阵（骨架）

**中文** | [English](compliant-world-gap.en.md)

> **状态：** 文档锁定骨架（2026-08-09）；实现切片 A 再按上游 WIT 方法补全。  
> **钉定：** `wasi:webgpu/webgpu@0.3.0-rc.2`（见 [`wit/README.md`](../../wit/README.md)）  
> **现状包：** `experimental:webgpu-cm@0.4.0`（[`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)）  
> **阶段计划：** [`docs/scheme/compliant-world.md`](../scheme/compliant-world.md)

本表对照标准包资源面与本仓 experimental / L2 现状。关门规则：每行最终为 ✅（接线）、⚠️（语义偏差 / sync-compat）、或 ❌（显式 `Unsupported` / 本阶段不做且已文档化）。**不得**留下「无」悬空行。

## 图例

| 标记 | 含义 |
|------|------|
| ✅ | 已有可对齐路径（experimental 或 L2） |
| ⚠️ | 有路径但特化 / 形状偏差 / sync 包装 |
| ❌ | 本仓尚无；目标切片内实现或标 Unsupported |
| — | 本阶段明确不做（见计划页「不做」表；如 wasi-gfx） |

| 列 | 含义 |
|----|------|
| 上游族 / 代表方法 | 标准 `wasi:webgpu` 资源族或代表 API（细目在切片 A 补全） |
| 本仓现状 | 有 / 特化 / 无 |
| 目标切片 | A–G |
| 备注 | 特化名、Host 注入、可 Unsupported 等 |

## Instance / Adapter / Device

| 上游族 / 代表方法 | 本仓现状 | 目标切片 | 备注 |
|------------------|----------|----------|------|
| `gpu.request-adapter` | ✅ | B/C | experimental `request-adapter`；async→sync |
| `gpu-adapter.request-device` | ⚠️ | C/F | 无完整 device descriptor；async→sync |
| `gpu-adapter` features / limits / info | ❌ | G | 可先 Unsupported |
| `gpu-device.get-queue` | ✅ | B/C | |
| `gpu-device` features / limits / destroy | ❌ | G | |
| `gpu.create-surface` / canvas 系 | — / ⚠️ | E | **无 gfx**；现有 `create-surface-from-native-window`（Host 注入）作过渡 |

## Buffer / Bind / Compute

| 上游族 / 代表方法 | 本仓现状 | 目标切片 | 备注 |
|------------------|----------|----------|------|
| `create-buffer` + `buffer-descriptor` | ✅ | C | `@0.2.0+` records 已对齐形状 |
| `map-async` / mapped range / unmap | ⚠️ | C/F | sync 等待；`result` 未抬升 |
| `create-shader-module`（WGSL） | ✅ | C | |
| `create-bind-group-layout`（通用 entries） | ⚠️ | C | 特化：`create-bind-group-layout-storage3` |
| `create-bind-group`（通用 entries） | ⚠️ | C | 特化：`create-bind-group3` |
| `create-pipeline-layout` | ❌ | D | |
| `create-compute-pipeline`（descriptor） | ⚠️ | C | 现为 layout+shader+entry 便捷形 |
| compute pass set/dispatch/end | ✅ | C | |
| `queue.write-buffer` / `submit` | ⚠️ | C | 特化：`write-buffer` + `submit1` |
| `copy-buffer-to-buffer` | ✅ | C | |

## Texture / Sampler / Views

| 上游族 / 代表方法 | 本仓现状 | 目标切片 | 备注 |
|------------------|----------|----------|------|
| `create-texture` / texture 资源方法 | ❌ | D | 上屏路径仅有 surface 当前 texture view |
| `create-sampler` | ❌ | D | |
| `texture.create-view`（通用） | ⚠️ | D/E | 封装在 `get-current-texture-view` |
| copy texture / buffer↔texture | ❌ | D/G | 可阶段性 Unsupported |

## Render / Surface（无 gfx）

| 上游族 / 代表方法 | 本仓现状 | 目标切片 | 备注 |
|------------------|----------|----------|------|
| `create-render-pipeline`（通用 descriptor） | ⚠️ | E | 特化：`create-render-pipeline-triangle` / `-triangle-buffers` |
| vertex-buffer-layout records | ✅ | E | `@0.4.0`；仍挂在 triangle helper |
| `begin-render-pass`（通用 attachments） | ⚠️ | E | 特化：`begin-render-pass-clear` |
| render pass set-pipeline / set-vertex-buffer / draw / end | ✅ | E | draw 形参子集 |
| surface configure / present / unconfigure | ⚠️ | E | Host 注入；非 wasi-gfx canvas |
| MSAA / depth-stencil / multi-target | ❌ | E/G | 可 Unsupported 关门 |
| wasi-gfx / window / canvas | — | — | **本阶段不做** |

## Query / Bundle / 长尾

| 上游族 / 代表方法 | 本仓现状 | 目标切片 | 备注 |
|------------------|----------|----------|------|
| query-set / occlusion / timestamp | ❌ | G | 默认 Unsupported 可关门 |
| render-bundle / bundle encoder | ❌ | G | 同上 |
| external texture / 其他长尾资源 | ❌ | G | 同上 |
| 全量 enum/flags/records 对齐 | ⚠️ | A/F | 切片 A 对照上游 WIT 补全细目 |

## 已知特化 API（须在 C/E 迁出 Guest 依赖）

| experimental API | 替代方向 | 切片 |
|------------------|----------|------|
| `create-bind-group-layout-storage3` | 标准 bind-group-layout descriptor | C |
| `create-bind-group3` | 标准 bind-group descriptor | C |
| `submit1` | 标准 `queue.submit`（list） | C |
| `create-render-pipeline-triangle` | 标准 render-pipeline descriptor | E |
| `create-render-pipeline-triangle-buffers` | 同上（vertex layouts 已部分对齐） | E |
| `begin-render-pass-clear` | 标准 begin-render-pass + color attachment | E |
| `create-surface-from-native-window` | 保留为 Host 注入路径（非 gfx）；或映射到标准 surface 子集 | E |

## 补全约定（实现切片 A）

1. Vendor 上游 `webgpu.wit` / `imports.wit` 后，按 **resource × method** 展开本表细目。  
2. 每行填写现状与目标切片；允许 ❌ +「显式 Unsupported」作为关门。  
3. 升级钉定版本时：先改 PIN 与本矩阵，再改 Host / ABI。  

## 链接

- 阶段计划：[`compliant-world.md`](../scheme/compliant-world.md)  
- Compute / Render 子集：[`compute-subset.md`](compute-subset.md) · [`render-subset.md`](render-subset.md)  
- 错误与 Async：[`errors-async.md`](errors-async.md)  
- 上游 imports：https://github.com/WebAssembly/wasi-webgpu/blob/main/imports.md  
