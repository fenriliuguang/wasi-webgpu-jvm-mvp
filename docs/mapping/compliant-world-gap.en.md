# Compliant-world gap matrix

[中文](compliant-world-gap.md) | **English**

> **Status:** slice A method-level fill (2026-08-09); pin [`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md).  
> **Pin:** `wasi:webgpu/webgpu@0.3.0-rc.2` (tag `v0.3.0-rc.2`)  
> **Current package:** `experimental:webgpu-cm@0.4.0` ([`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit))  
> **Phase plan:** [`docs/scheme/compliant-world.en.md`](../scheme/compliant-world.en.md)  
> **Methods:** 224 (resource × method; see [`_inventory.json`](../../wit/deps/wasi-webgpu/_inventory.json))

Contrasts the standard package with this repo’s experimental / L2 status. Close-out: every row ✅ / ⚠️ / ❌ (explicit `Unsupported` OK). **No** dangling missing rows.

## Legend

| Mark | Meaning |
|------|---------|
| ✅ | Usable aligned path (experimental or L2) |
| ⚠️ | Path exists but specialized / shape skew / sync wrap |
| ❌ | Missing; implement or mark Unsupported in target slice |
| — | Explicitly out of this phase (e.g. wasi-gfx) |

| Column | Meaning |
|--------|---------|
| Upstream method | `resource.method` (`async` noted) |
| Status | ✅ / ⚠️ / ❌ |
| Slice | A–G |
| Notes | Helpers, Host inject, Unsupported OK, etc. |

## Summary

| ✅ | ⚠️ | ❌ | Total |
|----|----|----|-------|
| 16 | 17 | 191 | 224 |

## Known specialized APIs (Guest must leave in C/E)

| experimental API | Replacement | Slice |
|------------------|-------------|-------|
| `create-bind-group-layout-storage3` | standard bind-group-layout descriptor | C |
| `create-bind-group3` | standard bind-group descriptor | C |
| `submit1` | standard `queue.submit` (list) | C |
| `create-render-pipeline-triangle` | standard render-pipeline descriptor | E |
| `create-render-pipeline-triangle-buffers` | same (vertex layouts partly aligned) | E |
| `begin-render-pass-clear` | standard begin-render-pass + color attachment | E |
| `create-surface-from-native-window` | keep Host inject (not gfx); or map to canvas-context subset | E |

## wasi-gfx

**Out of this phase:** wasi-gfx / window / canvas. On-screen stays Host-injected Android native window (see `gpu-canvas-context` notes).

## `gpu`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu.request-adapter` `async` | ✅ | B/C | experimental request-adapter; async→sync; options subset |
| `gpu.get-preferred-canvas-format` | ❌ | E/G | no gfx canvas; Unsupported OK |
| `gpu.wgsl-language-features` | ❌ | G | Unsupported OK |

## `gpu-adapter`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-adapter.features` | ❌ | G | Unsupported OK |
| `gpu-adapter.limits` | ❌ | G | Unsupported OK |
| `gpu-adapter.info` | ❌ | G | Unsupported OK |
| `gpu-adapter.request-device` `async` | ⚠️ | C/F | no full device descriptor; async→sync |

## `gpu-adapter-info`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-adapter-info.vendor` | ❌ | G | Unsupported OK |
| `gpu-adapter-info.architecture` | ❌ | G | Unsupported OK |
| `gpu-adapter-info.device` | ❌ | G | Unsupported OK |
| `gpu-adapter-info.description` | ❌ | G | Unsupported OK |
| `gpu-adapter-info.subgroup-min-size` | ❌ | G | Unsupported OK |
| `gpu-adapter-info.subgroup-max-size` | ❌ | G | Unsupported OK |
| `gpu-adapter-info.is-fallback-adapter` | ❌ | G | Unsupported OK |

## `gpu-bind-group`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-bind-group.label` | ❌ | G | Unsupported OK |
| `gpu-bind-group.set-label` | ❌ | G | Unsupported OK |

## `gpu-bind-group-layout`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-bind-group-layout.label` | ❌ | G | Unsupported OK |
| `gpu-bind-group-layout.set-label` | ❌ | G | Unsupported OK |

## `gpu-buffer`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-buffer.size` | ❌ | C/G | props/destroy not exposed; Unsupported OK |
| `gpu-buffer.usage` | ❌ | C/G | props/destroy not exposed; Unsupported OK |
| `gpu-buffer.map-state` | ❌ | C/G | props/destroy not exposed; Unsupported OK |
| `gpu-buffer.map-async` `async` | ⚠️ | C/F | L2 sync wait; result not lifted |
| `gpu-buffer.get-mapped-range-get-with-copy` | ⚠️ | C/F | experimental get-mapped-range → ByteArray copy |
| `gpu-buffer.unmap` | ✅ | C |  |
| `gpu-buffer.destroy` | ❌ | C/G | props/destroy not exposed; Unsupported OK |
| `gpu-buffer.label` | ❌ | G | Unsupported OK |
| `gpu-buffer.set-label` | ❌ | G | Unsupported OK |
| `gpu-buffer.get-mapped-range-set-with-copy` | ❌ | C/G | Unsupported OK |

## `gpu-canvas-context`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-canvas-context.configure` | ⚠️ | E | experimental surface.*; Host-inject native window; not gfx |
| `gpu-canvas-context.unconfigure` | ⚠️ | E | experimental surface.*; Host-inject native window; not gfx |
| `gpu-canvas-context.get-configuration` | ❌ | E/G | Unsupported OK |
| `gpu-canvas-context.get-current-texture` | ⚠️ | E | wrapped as get-current-texture-view |

## `gpu-command-buffer`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-command-buffer.label` | ❌ | G | Unsupported OK |
| `gpu-command-buffer.set-label` | ❌ | G | Unsupported OK |

## `gpu-command-encoder`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-command-encoder.begin-render-pass` | ⚠️ | E | specialized begin-render-pass-clear |
| `gpu-command-encoder.begin-compute-pass` | ✅ | C |  |
| `gpu-command-encoder.copy-buffer-to-buffer` | ✅ | C |  |
| `gpu-command-encoder.copy-buffer-to-texture` | ❌ | D/G | Unsupported OK |
| `gpu-command-encoder.copy-texture-to-buffer` | ❌ | D/G | Unsupported OK |
| `gpu-command-encoder.copy-texture-to-texture` | ❌ | D/G | Unsupported OK |
| `gpu-command-encoder.clear-buffer` | ❌ | D/G | Unsupported OK |
| `gpu-command-encoder.resolve-query-set` | ❌ | D/G | Unsupported OK |
| `gpu-command-encoder.finish` | ✅ | C |  |
| `gpu-command-encoder.label` | ❌ | G | Unsupported OK |
| `gpu-command-encoder.set-label` | ❌ | G | Unsupported OK |
| `gpu-command-encoder.push-debug-group` | ❌ | G | Unsupported OK |
| `gpu-command-encoder.pop-debug-group` | ❌ | G | Unsupported OK |
| `gpu-command-encoder.insert-debug-marker` | ❌ | G | Unsupported OK |

## `gpu-compilation-info`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-compilation-info.messages` | ❌ | G | long-tail; Unsupported closes |

## `gpu-compilation-message`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-compilation-message.message` | ❌ | G | long-tail; Unsupported closes |
| `gpu-compilation-message.line-num` | ❌ | G | long-tail; Unsupported closes |
| `gpu-compilation-message.line-pos` | ❌ | G | long-tail; Unsupported closes |
| `gpu-compilation-message.offset` | ❌ | G | long-tail; Unsupported closes |
| `gpu-compilation-message.length` | ❌ | G | long-tail; Unsupported closes |

## `gpu-compute-pass-encoder`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-compute-pass-encoder.set-pipeline` | ✅ | C |  |
| `gpu-compute-pass-encoder.dispatch-workgroups` | ✅ | C |  |
| `gpu-compute-pass-encoder.dispatch-workgroups-indirect` | ❌ | C/G | Unsupported OK |
| `gpu-compute-pass-encoder.end` | ✅ | C |  |
| `gpu-compute-pass-encoder.label` | ❌ | G | Unsupported OK |
| `gpu-compute-pass-encoder.set-label` | ❌ | G | Unsupported OK |
| `gpu-compute-pass-encoder.push-debug-group` | ❌ | G | Unsupported OK |
| `gpu-compute-pass-encoder.pop-debug-group` | ❌ | G | Unsupported OK |
| `gpu-compute-pass-encoder.insert-debug-marker` | ❌ | G | Unsupported OK |
| `gpu-compute-pass-encoder.set-bind-group` | ✅ | C |  |
| `gpu-compute-pass-encoder.set-immediates` | ❌ | C/G | Unsupported OK |

## `gpu-compute-pipeline`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-compute-pipeline.label` | ❌ | G | Unsupported OK |
| `gpu-compute-pipeline.set-label` | ❌ | G | Unsupported OK |
| `gpu-compute-pipeline.get-bind-group-layout` | ❌ | C/E/G | Unsupported OK |

## `gpu-device`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-device.features` | ❌ | G | Unsupported OK |
| `gpu-device.limits` | ❌ | G | Unsupported OK |
| `gpu-device.adapter-info` | ❌ | G | Unsupported OK |
| `gpu-device.queue` | ✅ | B/C | experimental get-queue |
| `gpu-device.destroy` | ❌ | G | Unsupported OK |
| `gpu-device.create-buffer` | ✅ | C | buffer-descriptor aligned |
| `gpu-device.create-texture` | ❌ | D |  |
| `gpu-device.create-sampler` | ❌ | D |  |
| `gpu-device.create-bind-group-layout` | ⚠️ | C | specialized create-bind-group-layout-storage3 |
| `gpu-device.create-pipeline-layout` | ❌ | D |  |
| `gpu-device.create-bind-group` | ⚠️ | C | specialized create-bind-group3 |
| `gpu-device.create-shader-module` | ⚠️ | C | WGSL code string only, not full descriptor |
| `gpu-device.create-compute-pipeline` | ⚠️ | C | layout+shader+entry helper shape |
| `gpu-device.create-render-pipeline` | ⚠️ | E | specialized *-triangle* helpers |
| `gpu-device.create-compute-pipeline-async` `async` | ⚠️ | F | sync-compat this phase; Unsupported first OK |
| `gpu-device.create-render-pipeline-async` `async` | ⚠️ | F | sync-compat this phase; Unsupported first OK |
| `gpu-device.create-command-encoder` | ✅ | C |  |
| `gpu-device.create-render-bundle-encoder` | ❌ | G | Unsupported OK |
| `gpu-device.create-query-set` | ❌ | G | Unsupported OK |
| `gpu-device.label` | ❌ | G | Unsupported OK |
| `gpu-device.set-label` | ❌ | G | Unsupported OK |
| `gpu-device.lost` | ❌ | G | Unsupported OK |
| `gpu-device.push-error-scope` | ❌ | G | Unsupported OK |
| `gpu-device.pop-error-scope` `async` | ❌ | G | Unsupported OK |
| `gpu-device.on-uncaptured-error` | ❌ | G | Unsupported OK |

## `gpu-device-lost-info`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-device-lost-info.reason` | ❌ | G | long-tail; Unsupported closes |
| `gpu-device-lost-info.message` | ❌ | G | long-tail; Unsupported closes |

## `gpu-error`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-error.message` | ❌ | G | long-tail; Unsupported closes |
| `gpu-error.kind` | ❌ | G | long-tail; Unsupported closes |

## `gpu-pipeline-layout`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-pipeline-layout.label` | ❌ | D/G | Unsupported OK |
| `gpu-pipeline-layout.set-label` | ❌ | D/G | Unsupported OK |

## `gpu-query-set`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-query-set.destroy` | ❌ | G | Unsupported OK |
| `gpu-query-set.count` | ❌ | G | Unsupported OK |
| `gpu-query-set.label` | ❌ | G | Unsupported OK |
| `gpu-query-set.set-label` | ❌ | G | Unsupported OK |

## `gpu-queue`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-queue.submit` | ⚠️ | C | specialized submit1 |
| `gpu-queue.on-submitted-work-done` `async` | ❌ | D/G | Unsupported OK |
| `gpu-queue.write-buffer-with-copy` | ✅ | C | experimental write-buffer |
| `gpu-queue.write-texture-with-copy` | ❌ | D/G | Unsupported OK |
| `gpu-queue.label` | ❌ | G | Unsupported OK |
| `gpu-queue.set-label` | ❌ | G | Unsupported OK |

## `gpu-render-bundle`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-render-bundle.label` | ❌ | G | Unsupported OK |
| `gpu-render-bundle.set-label` | ❌ | G | Unsupported OK |

## `gpu-render-bundle-encoder`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-render-bundle-encoder.finish` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.label` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.set-label` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.push-debug-group` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.pop-debug-group` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.insert-debug-marker` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.set-bind-group` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.set-immediates` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.set-pipeline` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.set-index-buffer` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.set-vertex-buffer` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.draw` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.draw-indexed` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.draw-indirect` | ❌ | G | long-tail; Unsupported closes |
| `gpu-render-bundle-encoder.draw-indexed-indirect` | ❌ | G | long-tail; Unsupported closes |

## `gpu-render-pass-encoder`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-render-pass-encoder.set-viewport` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.set-scissor-rect` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.set-blend-constant` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.set-stencil-reference` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.begin-occlusion-query` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.end-occlusion-query` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.execute-bundles` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.end` | ✅ | E |  |
| `gpu-render-pass-encoder.label` | ❌ | G | Unsupported OK |
| `gpu-render-pass-encoder.set-label` | ❌ | G | Unsupported OK |
| `gpu-render-pass-encoder.push-debug-group` | ❌ | G | Unsupported OK |
| `gpu-render-pass-encoder.pop-debug-group` | ❌ | G | Unsupported OK |
| `gpu-render-pass-encoder.insert-debug-marker` | ❌ | G | Unsupported OK |
| `gpu-render-pass-encoder.set-bind-group` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.set-immediates` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.set-pipeline` | ✅ | E |  |
| `gpu-render-pass-encoder.set-index-buffer` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.set-vertex-buffer` | ✅ | E |  |
| `gpu-render-pass-encoder.draw` | ⚠️ | E | arity subset (vertex-count only) |
| `gpu-render-pass-encoder.draw-indexed` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.draw-indirect` | ❌ | E/G | Unsupported OK |
| `gpu-render-pass-encoder.draw-indexed-indirect` | ❌ | E/G | Unsupported OK |

## `gpu-render-pipeline`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-render-pipeline.label` | ❌ | G | Unsupported OK |
| `gpu-render-pipeline.set-label` | ❌ | G | Unsupported OK |
| `gpu-render-pipeline.get-bind-group-layout` | ❌ | C/E/G | Unsupported OK |

## `gpu-sampler`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-sampler.label` | ❌ | D/G | Unsupported OK |
| `gpu-sampler.set-label` | ❌ | D/G | Unsupported OK |

## `gpu-shader-module`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-shader-module.get-compilation-info` `async` | ❌ | G | Unsupported OK |
| `gpu-shader-module.label` | ❌ | G | Unsupported OK |
| `gpu-shader-module.set-label` | ❌ | G | Unsupported OK |

## `gpu-supported-features`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-supported-features.has` | ❌ | G | long-tail; Unsupported closes |

## `gpu-supported-limits`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-supported-limits.max-texture-dimension1-d` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-texture-dimension2-d` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-texture-dimension3-d` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-texture-array-layers` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-bind-groups` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-bind-groups-plus-vertex-buffers` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-immediate-size` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-bindings-per-bind-group` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-dynamic-uniform-buffers-per-pipeline-layout` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-dynamic-storage-buffers-per-pipeline-layout` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-sampled-textures-per-shader-stage` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-samplers-per-shader-stage` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-storage-buffers-per-shader-stage` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-storage-buffers-in-vertex-stage` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-storage-buffers-in-fragment-stage` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-storage-textures-per-shader-stage` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-storage-textures-in-vertex-stage` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-storage-textures-in-fragment-stage` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-uniform-buffers-per-shader-stage` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-uniform-buffer-binding-size` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-storage-buffer-binding-size` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.min-uniform-buffer-offset-alignment` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.min-storage-buffer-offset-alignment` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-vertex-buffers` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-buffer-size` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-vertex-attributes` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-vertex-buffer-array-stride` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-inter-stage-shader-variables` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-color-attachments` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-color-attachment-bytes-per-sample` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-compute-workgroup-storage-size` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-compute-invocations-per-workgroup` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-compute-workgroup-size-x` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-compute-workgroup-size-y` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-compute-workgroup-size-z` | ❌ | G | long-tail; Unsupported closes |
| `gpu-supported-limits.max-compute-workgroups-per-dimension` | ❌ | G | long-tail; Unsupported closes |

## `gpu-texture`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-texture.create-view` | ⚠️ | D/E | only surface-path textureCreateView |
| `gpu-texture.destroy` | ❌ | D/G | Unsupported OK |
| `gpu-texture.width` | ❌ | D/G | Unsupported OK |
| `gpu-texture.height` | ❌ | D/G | Unsupported OK |
| `gpu-texture.depth-or-array-layers` | ❌ | D/G | Unsupported OK |
| `gpu-texture.mip-level-count` | ❌ | D/G | Unsupported OK |
| `gpu-texture.sample-count` | ❌ | D/G | Unsupported OK |
| `gpu-texture.dimension` | ❌ | D/G | Unsupported OK |
| `gpu-texture.format` | ❌ | D/G | Unsupported OK |
| `gpu-texture.usage` | ❌ | D/G | Unsupported OK |
| `gpu-texture.texture-binding-view-dimension` | ❌ | D/G | Unsupported OK |
| `gpu-texture.label` | ❌ | D/G | Unsupported OK |
| `gpu-texture.set-label` | ❌ | D/G | Unsupported OK |

## `gpu-texture-view`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-texture-view.label` | ❌ | E/G | resource exists; label Unsupported OK |
| `gpu-texture-view.set-label` | ❌ | E/G | resource exists; label Unsupported OK |

## `gpu-uncaptured-error-event`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-uncaptured-error-event.error` | ❌ | G | long-tail; Unsupported closes |

## `record-gpu-pipeline-constant-value`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `record-gpu-pipeline-constant-value.add` | ❌ | G | long-tail; Unsupported closes |
| `record-gpu-pipeline-constant-value.get` | ❌ | G | long-tail; Unsupported closes |
| `record-gpu-pipeline-constant-value.has` | ❌ | G | long-tail; Unsupported closes |
| `record-gpu-pipeline-constant-value.remove` | ❌ | G | long-tail; Unsupported closes |
| `record-gpu-pipeline-constant-value.keys` | ❌ | G | long-tail; Unsupported closes |
| `record-gpu-pipeline-constant-value.values` | ❌ | G | long-tail; Unsupported closes |
| `record-gpu-pipeline-constant-value.entries` | ❌ | G | long-tail; Unsupported closes |

## `record-option-gpu-size64`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `record-option-gpu-size64.add` | ❌ | G | long-tail; Unsupported closes |
| `record-option-gpu-size64.get` | ❌ | G | long-tail; Unsupported closes |
| `record-option-gpu-size64.has` | ❌ | G | long-tail; Unsupported closes |
| `record-option-gpu-size64.remove` | ❌ | G | long-tail; Unsupported closes |
| `record-option-gpu-size64.keys` | ❌ | G | long-tail; Unsupported closes |
| `record-option-gpu-size64.values` | ❌ | G | long-tail; Unsupported closes |
| `record-option-gpu-size64.entries` | ❌ | G | long-tail; Unsupported closes |

## `wgsl-language-features`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `wgsl-language-features.has` | ❌ | G | long-tail; Unsupported closes |

## Update rules

1. Bump pin: update [`PIN.md`](../../wit/deps/wasi-webgpu/PIN.md) and `_inventory.json`, then this matrix, then Host / ABI.  
2. ❌ + explicit Unsupported closes that row.  
3. Regenerate: `python scripts/gen-compliant-world-gap.py` (requires `_inventory.json`).

## Links

- Phase plan: [`compliant-world.en.md`](../scheme/compliant-world.en.md)
- PIN: [`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)
- Compute / Render: [`compute-subset.en.md`](compute-subset.en.md) · [`render-subset.en.md`](render-subset.en.md)
- Errors & async: [`errors-async.en.md`](errors-async.en.md)
- Upstream: https://github.com/WebAssembly/wasi-webgpu/tree/v0.3.0-rc.2
