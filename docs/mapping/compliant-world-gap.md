# 合规 world 缺口矩阵

**中文** | [English](compliant-world-gap.en.md)

> **状态：** 切片 G 关门（2026-08-09）；DoD [`archive-compliant-world-dod.md`](../scheme/archive-compliant-world-dod.md)。  
> **钉定：** `wasi:webgpu/webgpu@0.3.0-rc.2`（tag `v0.3.0-rc.2`）  
> **现状包：** `experimental:webgpu-cm@0.7.0`（[`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)）  
> **阶段计划：** [`docs/scheme/compliant-world.md`](../scheme/compliant-world.md)  
> **方法数：** 224（resource × method；见 [`_inventory.json`](../../wit/deps/wasi-webgpu/_inventory.json)）

本表对照标准包与本仓 experimental / L2 现状。关门规则：每行最终为 ✅ / ⚠️ / ❌（显式 `Unsupported` 可关门）。**不得**留下悬空「无」。

## 图例

| 标记 | 含义 |
|------|------|
| ✅ | 已有可对齐路径（experimental 或 L2） |
| ⚠️ | 有路径但特化 / 形状偏差 / sync 包装 |
| ❌ | 本仓尚无；目标切片内实现或标 Unsupported |
| — | 本阶段明确不做（如 wasi-gfx） |

| 列 | 含义 |
|----|------|
| 上游方法 | `resource.method`（`async` 已标注） |
| 现状 | ✅ / ⚠️ / ❌ |
| 切片 | A–G |
| 备注 | 特化名、Host 注入、可 Unsupported 等 |

## 汇总

| ✅ | ⚠️ | ❌ | 合计 |
|----|----|----|------|
| 16 | 17 | 191 | 224 |

## 已知特化 API（须在 C/E 迁出 Guest 依赖）

| experimental API | 替代方向 | 切片 |
|------------------|----------|------|
| `create-bind-group-layout-storage3` | 标准 bind-group-layout descriptor | C |
| `create-bind-group3` | 标准 bind-group descriptor | C |
| `submit1` | 标准 `queue.submit`（list） | C |
| `create-render-pipeline-triangle` | 标准 render-pipeline descriptor | E |
| `create-render-pipeline-triangle-buffers` | 同上（vertex layouts 已部分对齐） | E |
| `begin-render-pass-clear` | 标准 begin-render-pass + color attachment | E |
| `create-surface-from-native-window` | 保留 Host 注入（非 gfx）；或映射标准 canvas-context 子集 | E |

## wasi-gfx

本阶段 **不做** wasi-gfx / window / canvas 抽象。上屏继续 Host 注入 Android native window（见 `gpu-canvas-context` 行备注）。

## `gpu`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu.request-adapter` `async` | ✅ | B/C | experimental request-adapter；async→sync；options 子集 |
| `gpu.get-preferred-canvas-format` | ❌ | E/G | G 关门：显式 Unsupported（无 gfx；wasi stub） |
| `gpu.wgsl-language-features` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-adapter`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-adapter.features` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-adapter.limits` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-adapter.info` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-adapter.request-device` `async` | ⚠️ | C/F | 无完整 device descriptor；async→sync；wasi result Err 已抬升（stub） |

## `gpu-adapter-info`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-adapter-info.vendor` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-adapter-info.architecture` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-adapter-info.device` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-adapter-info.description` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-adapter-info.subgroup-min-size` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-adapter-info.subgroup-max-size` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-adapter-info.is-fallback-adapter` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-bind-group`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-bind-group.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-bind-group.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-bind-group-layout`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-bind-group-layout.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-bind-group-layout.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-buffer`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-buffer.size` | ❌ | C/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-buffer.usage` | ❌ | C/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-buffer.map-state` | ❌ | C/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-buffer.map-async` `async` | ⚠️ | C/F | L2 sync 等待；wasi result Err 已抬升（stub）；experimental 仍 trap |
| `gpu-buffer.get-mapped-range-get-with-copy` | ⚠️ | C/F | experimental get-mapped-range → ByteArray 拷贝；wasi result Err 已抬升（stub） |
| `gpu-buffer.unmap` | ✅ | C |  |
| `gpu-buffer.destroy` | ❌ | C/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-buffer.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-buffer.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-buffer.get-mapped-range-set-with-copy` | ❌ | C/G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-canvas-context`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-canvas-context.configure` | ⚠️ | E | experimental surface.*；Host 注入 native window；非 gfx |
| `gpu-canvas-context.unconfigure` | ⚠️ | E | experimental surface.*；Host 注入 native window；非 gfx |
| `gpu-canvas-context.get-configuration` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-canvas-context.get-current-texture` | ⚠️ | E | 封装为 get-current-texture-view |

## `gpu-command-buffer`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-command-buffer.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-buffer.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-command-encoder`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-command-encoder.begin-render-pass` | ✅ | E | experimental descriptor；helper `begin-render-pass-clear` deprecated |
| `gpu-command-encoder.begin-compute-pass` | ✅ | C |  |
| `gpu-command-encoder.copy-buffer-to-buffer` | ✅ | C |  |
| `gpu-command-encoder.copy-buffer-to-texture` | ❌ | D/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-encoder.copy-texture-to-buffer` | ❌ | D/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-encoder.copy-texture-to-texture` | ❌ | D/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-encoder.clear-buffer` | ❌ | D/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-encoder.resolve-query-set` | ❌ | D/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-encoder.finish` | ✅ | C |  |
| `gpu-command-encoder.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-encoder.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-encoder.push-debug-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-encoder.pop-debug-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-command-encoder.insert-debug-marker` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-compilation-info`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-compilation-info.messages` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-compilation-message`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-compilation-message.message` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compilation-message.line-num` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compilation-message.line-pos` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compilation-message.offset` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compilation-message.length` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-compute-pass-encoder`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-compute-pass-encoder.set-pipeline` | ✅ | C |  |
| `gpu-compute-pass-encoder.dispatch-workgroups` | ✅ | C |  |
| `gpu-compute-pass-encoder.dispatch-workgroups-indirect` | ❌ | C/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compute-pass-encoder.end` | ✅ | C |  |
| `gpu-compute-pass-encoder.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compute-pass-encoder.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compute-pass-encoder.push-debug-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compute-pass-encoder.pop-debug-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compute-pass-encoder.insert-debug-marker` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compute-pass-encoder.set-bind-group` | ✅ | C |  |
| `gpu-compute-pass-encoder.set-immediates` | ❌ | C/G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-compute-pipeline`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-compute-pipeline.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compute-pipeline.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-compute-pipeline.get-bind-group-layout` | ❌ | C/E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-device`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-device.features` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.limits` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.adapter-info` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.queue` | ✅ | B/C | experimental get-queue |
| `gpu-device.destroy` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.create-buffer` | ✅ | C | buffer-descriptor 已对齐 |
| `gpu-device.create-texture` | ✅ | D | experimental + L2/Dawn/Cpu |
| `gpu-device.create-sampler` | ✅ | D | experimental + L2/Dawn/Cpu（descriptor 最小/可 option） |
| `gpu-device.create-pipeline-layout` | ✅ | D | experimental + L2/Dawn/Cpu |
| `gpu-device.create-bind-group-layout` | ✅ | C/D | 标准 descriptor；含 sampler/texture 条目（D） |
| `gpu-device.create-bind-group` | ✅ | C/D | 标准 descriptor；含 sampler/texture-view（D）；嵌套 borrow 仍受 .so 限制 |
| `gpu-device.create-shader-module` | ⚠️ | C | 仅 WGSL code 字符串，非完整 descriptor |
| `gpu-device.create-compute-pipeline` | ✅ | C/D | layout 为 pipeline-layout（D）；deprecated BGL helper 仍在 |
| `gpu-device.create-render-pipeline` | ✅ | E | experimental descriptor；`*-triangle*` helpers deprecated |
| `gpu-device.create-compute-pipeline-async` `async` | ⚠️ | F | sync-compat；wasi stub → create-pipeline-error result Err |
| `gpu-device.create-render-pipeline-async` `async` | ⚠️ | F | sync-compat；wasi stub → create-pipeline-error result Err |
| `gpu-device.create-command-encoder` | ✅ | C |  |
| `gpu-device.create-render-bundle-encoder` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.create-query-set` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.lost` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.push-error-scope` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.pop-error-scope` `async` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device.on-uncaptured-error` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-device-lost-info`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-device-lost-info.reason` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-device-lost-info.message` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-error`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-error.message` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-error.kind` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-pipeline-layout`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-pipeline-layout.label` | ❌ | D/G | 显式 Unsupported |
| `gpu-pipeline-layout.set-label` | ❌ | D/G | 显式 Unsupported |

## `gpu-query-set`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-query-set.destroy` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-query-set.count` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-query-set.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-query-set.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-queue`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-queue.submit` | ⚠️ | C | 特化 submit1 |
| `gpu-queue.on-submitted-work-done` `async` | ❌ | D/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-queue.write-buffer-with-copy` | ✅ | C | experimental write-buffer |
| `gpu-queue.write-texture-with-copy` | ❌ | D/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-queue.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-queue.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-render-bundle`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-render-bundle.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-render-bundle-encoder`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-render-bundle-encoder.finish` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.push-debug-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.pop-debug-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.insert-debug-marker` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.set-bind-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.set-immediates` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.set-pipeline` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.set-index-buffer` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.set-vertex-buffer` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.draw` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.draw-indexed` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.draw-indirect` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-bundle-encoder.draw-indexed-indirect` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-render-pass-encoder`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-render-pass-encoder.set-viewport` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.set-scissor-rect` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.set-blend-constant` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.set-stencil-reference` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.begin-occlusion-query` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.end-occlusion-query` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.execute-bundles` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.end` | ✅ | E |  |
| `gpu-render-pass-encoder.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.push-debug-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.pop-debug-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.insert-debug-marker` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.set-bind-group` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.set-immediates` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.set-pipeline` | ✅ | E |  |
| `gpu-render-pass-encoder.set-index-buffer` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.set-vertex-buffer` | ✅ | E |  |
| `gpu-render-pass-encoder.draw` | ⚠️ | E | 形参子集（仅 vertex-count） |
| `gpu-render-pass-encoder.draw-indexed` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.draw-indirect` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pass-encoder.draw-indexed-indirect` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-render-pipeline`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-render-pipeline.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pipeline.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-render-pipeline.get-bind-group-layout` | ❌ | C/E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-sampler`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-sampler.label` | ❌ | D/G | 显式 Unsupported |
| `gpu-sampler.set-label` | ❌ | D/G | 显式 Unsupported |

## `gpu-shader-module`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-shader-module.get-compilation-info` `async` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-shader-module.label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-shader-module.set-label` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-supported-features`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-supported-features.has` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-supported-limits`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-supported-limits.max-texture-dimension1-d` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-texture-dimension2-d` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-texture-dimension3-d` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-texture-array-layers` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-bind-groups` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-bind-groups-plus-vertex-buffers` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-immediate-size` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-bindings-per-bind-group` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-dynamic-uniform-buffers-per-pipeline-layout` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-dynamic-storage-buffers-per-pipeline-layout` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-sampled-textures-per-shader-stage` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-samplers-per-shader-stage` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-storage-buffers-per-shader-stage` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-storage-buffers-in-vertex-stage` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-storage-buffers-in-fragment-stage` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-storage-textures-per-shader-stage` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-storage-textures-in-vertex-stage` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-storage-textures-in-fragment-stage` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-uniform-buffers-per-shader-stage` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-uniform-buffer-binding-size` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-storage-buffer-binding-size` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.min-uniform-buffer-offset-alignment` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.min-storage-buffer-offset-alignment` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-vertex-buffers` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-buffer-size` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-vertex-attributes` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-vertex-buffer-array-stride` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-inter-stage-shader-variables` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-color-attachments` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-color-attachment-bytes-per-sample` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-compute-workgroup-storage-size` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-compute-invocations-per-workgroup` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-compute-workgroup-size-x` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-compute-workgroup-size-y` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-compute-workgroup-size-z` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-supported-limits.max-compute-workgroups-per-dimension` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-texture`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-texture.create-view` | ✅ | D | create-texture 路径 + surface 路径；无 descriptor 形 |
| `gpu-texture.destroy` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.width` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.height` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.depth-or-array-layers` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.mip-level-count` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.sample-count` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.dimension` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.format` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.usage` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.texture-binding-view-dimension` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.label` | ❌ | D/G | 显式 Unsupported |
| `gpu-texture.set-label` | ❌ | D/G | 显式 Unsupported |

## `gpu-texture-view`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-texture-view.label` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `gpu-texture-view.set-label` | ❌ | E/G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `gpu-uncaptured-error-event`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `gpu-uncaptured-error-event.error` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `record-gpu-pipeline-constant-value`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `record-gpu-pipeline-constant-value.add` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-gpu-pipeline-constant-value.get` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-gpu-pipeline-constant-value.has` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-gpu-pipeline-constant-value.remove` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-gpu-pipeline-constant-value.keys` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-gpu-pipeline-constant-value.values` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-gpu-pipeline-constant-value.entries` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `record-option-gpu-size64`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `record-option-gpu-size64.add` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-option-gpu-size64.get` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-option-gpu-size64.has` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-option-gpu-size64.remove` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-option-gpu-size64.keys` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-option-gpu-size64.values` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |
| `record-option-gpu-size64.entries` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## `wgsl-language-features`

| 上游方法 | 现状 | 切片 | 备注 |
|----------|------|------|------|
| `wgsl-language-features.has` | ❌ | G | G 关门：显式 Unsupported（wasi stub / 长尾） |

## 补全 / 升级约定

1. 改钉定版本：先更新 [`PIN.md`](../../wit/deps/wasi-webgpu/PIN.md) 与 `_inventory.json`，再改本矩阵，最后改 Host / ABI。  
2. ❌ +「显式 Unsupported」视为该行可关门。  
3. 重新生成：`python scripts/gen-compliant-world-gap.py` （需先有 `_inventory.json`）。

## 链接

- 阶段计划：[`compliant-world.md`](../scheme/compliant-world.md)
- PIN：[`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)
- Compute / Render 子集：[`compute-subset.md`](compute-subset.md) · [`render-subset.md`](render-subset.md)
- 错误与 Async：[`errors-async.md`](errors-async.md)
- 上游：https://github.com/WebAssembly/wasi-webgpu/tree/v0.3.0-rc.2
