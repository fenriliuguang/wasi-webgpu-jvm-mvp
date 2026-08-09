# Compliant-world gap matrix

[中文](compliant-world-gap.md) | **English**

> **Status:** slice G close-out (2026-08-09); DoD [`archive-compliant-world-dod.en.md`](../scheme/archive-compliant-world-dod.en.md).  
> **Pin:** `wasi:webgpu/webgpu@0.3.0-rc.2` (tag `v0.3.0-rc.2`)  
> **Current package:** `experimental:webgpu-cm@0.8.0` ([`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit))  
> **Phase plan:** [`docs/scheme/compliant-world.en.md`](../scheme/compliant-world.en.md)  
> **Methods:** 224 (resource × method; see [`_inventory.json`](../../wit/deps/wasi-webgpu/_inventory.json))

Contrasts the standard package with this repo’s experimental / L2 status. Close-out: every row ✅ / ⚠️ / ❌ (explicit `Unsupported` OK). **No** dangling missing rows.

## Legend

| Mark | Meaning |
|------|---------|
| ✅ | Usable aligned path (experimental or L2) |
| ⚠️ | Path exists but specialized / shape skew / sync wrap |
| ❌ | Explicit `Unsupported` / wasi stub (closes a row after G; not a dangling miss) |
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

## Known specialized APIs (Host has standard replacements; device Guests migrate after `.so` rebuild)

| experimental API | Replacement direction | Slice |
|------------------|----------------------|-------|
| `create-bind-group-layout-storage3` | standard bind-group-layout descriptor | C |
| `create-bind-group3` | standard bind-group descriptor | C |
| `submit1` | standard `queue.submit` (list) | C |
| `create-render-pipeline-triangle` | standard render-pipeline descriptor | E |
| `create-render-pipeline-triangle-buffers` | same (vertex layouts partially aligned) | E |
| `begin-render-pass-clear` | standard begin-render-pass + color attachment | E |
| `create-surface-from-native-window` | keep Host inject (not gfx); or map standard canvas-context subset | E |

## wasi-gfx

**Out of this phase:** wasi-gfx / window / canvas. On-screen stays Host-injected Android native window (see `gpu-canvas-context` notes).

## `gpu`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu.request-adapter` `async` | ✅ | B/C | experimental request-adapter; async→sync; options subset |
| `gpu.get-preferred-canvas-format` | ❌ | E/G | G close-out: explicit Unsupported (no gfx; wasi stub) |
| `gpu.wgsl-language-features` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-adapter`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-adapter.features` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-adapter.limits` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-adapter.info` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-adapter.request-device` `async` | ⚠️ | C/F | no full device descriptor; async→sync; wasi result Err lifted (stub) |

## `gpu-adapter-info`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-adapter-info.vendor` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-adapter-info.architecture` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-adapter-info.device` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-adapter-info.description` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-adapter-info.subgroup-min-size` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-adapter-info.subgroup-max-size` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-adapter-info.is-fallback-adapter` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-bind-group`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-bind-group.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-bind-group.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-bind-group-layout`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-bind-group-layout.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-bind-group-layout.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-buffer`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-buffer.size` | ❌ | C/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-buffer.usage` | ❌ | C/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-buffer.map-state` | ❌ | C/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-buffer.map-async` `async` | ⚠️ | C/F | L2 sync wait; wasi result Err lifted (stub); experimental still traps |
| `gpu-buffer.get-mapped-range-get-with-copy` | ⚠️ | C/F | experimental get-mapped-range → ByteArray copy; wasi result Err lifted (stub) |
| `gpu-buffer.unmap` | ✅ | C |  |
| `gpu-buffer.destroy` | ❌ | C/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-buffer.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-buffer.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-buffer.get-mapped-range-set-with-copy` | ❌ | C/G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-canvas-context`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-canvas-context.configure` | ⚠️ | E | experimental surface.*; Host-inject native window; not gfx |
| `gpu-canvas-context.unconfigure` | ⚠️ | E | experimental surface.*; Host-inject native window; not gfx |
| `gpu-canvas-context.get-configuration` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-canvas-context.get-current-texture` | ⚠️ | E | wrapped as get-current-texture-view |

## `gpu-command-buffer`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-command-buffer.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-buffer.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-command-encoder`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-command-encoder.begin-render-pass` | ✅ | E | experimental descriptor; helper `begin-render-pass-clear` deprecated |
| `gpu-command-encoder.begin-compute-pass` | ✅ | C |  |
| `gpu-command-encoder.copy-buffer-to-buffer` | ✅ | C |  |
| `gpu-command-encoder.copy-buffer-to-texture` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-encoder.copy-texture-to-buffer` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-encoder.copy-texture-to-texture` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-encoder.clear-buffer` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-encoder.resolve-query-set` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-encoder.finish` | ✅ | C |  |
| `gpu-command-encoder.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-encoder.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-encoder.push-debug-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-encoder.pop-debug-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-command-encoder.insert-debug-marker` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-compilation-info`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-compilation-info.messages` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-compilation-message`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-compilation-message.message` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compilation-message.line-num` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compilation-message.line-pos` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compilation-message.offset` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compilation-message.length` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-compute-pass-encoder`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-compute-pass-encoder.set-pipeline` | ✅ | C |  |
| `gpu-compute-pass-encoder.dispatch-workgroups` | ✅ | C |  |
| `gpu-compute-pass-encoder.dispatch-workgroups-indirect` | ❌ | C/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compute-pass-encoder.end` | ✅ | C |  |
| `gpu-compute-pass-encoder.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compute-pass-encoder.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compute-pass-encoder.push-debug-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compute-pass-encoder.pop-debug-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compute-pass-encoder.insert-debug-marker` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compute-pass-encoder.set-bind-group` | ✅ | C |  |
| `gpu-compute-pass-encoder.set-immediates` | ❌ | C/G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-compute-pipeline`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-compute-pipeline.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compute-pipeline.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-compute-pipeline.get-bind-group-layout` | ❌ | C/E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-device`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-device.features` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.limits` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.adapter-info` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.queue` | ✅ | B/C | experimental get-queue |
| `gpu-device.destroy` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.create-buffer` | ✅ | C | buffer-descriptor aligned |
| `gpu-device.create-texture` | ✅ | D | experimental + L2/Dawn/Cpu |
| `gpu-device.create-sampler` | ✅ | D | experimental + L2/Dawn/Cpu (minimal / option descriptor) |
| `gpu-device.create-bind-group-layout` | ✅ | C/D | standard descriptor; sampler/texture entries (D) |
| `gpu-device.create-pipeline-layout` | ✅ | D | experimental + L2/Dawn/Cpu |
| `gpu-device.create-bind-group` | ✅ | C/D | standard descriptor; sampler/texture-view (D); nested borrow still .so-limited |
| `gpu-device.create-shader-module` | ⚠️ | C | WGSL code string only, not full descriptor |
| `gpu-device.create-compute-pipeline` | ✅ | C/D | layout is pipeline-layout (D); deprecated BGL helper kept |
| `gpu-device.create-render-pipeline` | ✅ | E | experimental descriptor; `*-triangle*` helpers deprecated |
| `gpu-device.create-compute-pipeline-async` `async` | ⚠️ | F | sync-compat; wasi stub → create-pipeline-error result Err |
| `gpu-device.create-render-pipeline-async` `async` | ⚠️ | F | sync-compat; wasi stub → create-pipeline-error result Err |
| `gpu-device.create-command-encoder` | ✅ | C |  |
| `gpu-device.create-render-bundle-encoder` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.create-query-set` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.lost` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.push-error-scope` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.pop-error-scope` `async` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device.on-uncaptured-error` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-device-lost-info`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-device-lost-info.reason` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-device-lost-info.message` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-error`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-error.message` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-error.kind` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-pipeline-layout`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-pipeline-layout.label` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-pipeline-layout.set-label` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-query-set`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-query-set.destroy` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-query-set.count` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-query-set.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-query-set.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-queue`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-queue.submit` | ⚠️ | C | specialized submit1 |
| `gpu-queue.on-submitted-work-done` `async` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-queue.write-buffer-with-copy` | ✅ | C | experimental write-buffer |
| `gpu-queue.write-texture-with-copy` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-queue.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-queue.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-render-bundle`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-render-bundle.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-render-bundle-encoder`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-render-bundle-encoder.finish` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.push-debug-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.pop-debug-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.insert-debug-marker` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.set-bind-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.set-immediates` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.set-pipeline` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.set-index-buffer` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.set-vertex-buffer` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.draw` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.draw-indexed` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.draw-indirect` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-bundle-encoder.draw-indexed-indirect` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-render-pass-encoder`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-render-pass-encoder.set-viewport` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.set-scissor-rect` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.set-blend-constant` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.set-stencil-reference` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.begin-occlusion-query` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.end-occlusion-query` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.execute-bundles` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.end` | ✅ | E |  |
| `gpu-render-pass-encoder.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.push-debug-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.pop-debug-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.insert-debug-marker` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.set-bind-group` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.set-immediates` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.set-pipeline` | ✅ | E |  |
| `gpu-render-pass-encoder.set-index-buffer` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.set-vertex-buffer` | ✅ | E |  |
| `gpu-render-pass-encoder.draw` | ⚠️ | E | arity subset (vertex-count only) |
| `gpu-render-pass-encoder.draw-indexed` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.draw-indirect` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pass-encoder.draw-indexed-indirect` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-render-pipeline`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-render-pipeline.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pipeline.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-render-pipeline.get-bind-group-layout` | ❌ | C/E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-sampler`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-sampler.label` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-sampler.set-label` | ❌ | D/G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-shader-module`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-shader-module.get-compilation-info` `async` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-shader-module.label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-shader-module.set-label` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-supported-features`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-supported-features.has` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-supported-limits`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-supported-limits.max-texture-dimension1-d` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-texture-dimension2-d` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-texture-dimension3-d` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-texture-array-layers` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-bind-groups` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-bind-groups-plus-vertex-buffers` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-immediate-size` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-bindings-per-bind-group` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-dynamic-uniform-buffers-per-pipeline-layout` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-dynamic-storage-buffers-per-pipeline-layout` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-sampled-textures-per-shader-stage` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-samplers-per-shader-stage` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-storage-buffers-per-shader-stage` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-storage-buffers-in-vertex-stage` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-storage-buffers-in-fragment-stage` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-storage-textures-per-shader-stage` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-storage-textures-in-vertex-stage` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-storage-textures-in-fragment-stage` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-uniform-buffers-per-shader-stage` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-uniform-buffer-binding-size` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-storage-buffer-binding-size` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.min-uniform-buffer-offset-alignment` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.min-storage-buffer-offset-alignment` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-vertex-buffers` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-buffer-size` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-vertex-attributes` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-vertex-buffer-array-stride` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-inter-stage-shader-variables` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-color-attachments` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-color-attachment-bytes-per-sample` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-compute-workgroup-storage-size` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-compute-invocations-per-workgroup` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-compute-workgroup-size-x` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-compute-workgroup-size-y` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-compute-workgroup-size-z` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-supported-limits.max-compute-workgroups-per-dimension` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-texture`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-texture.create-view` | ✅ | D | create-texture path + surface path; no descriptor shape |
| `gpu-texture.destroy` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.width` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.height` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.depth-or-array-layers` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.mip-level-count` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.sample-count` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.dimension` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.format` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.usage` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.texture-binding-view-dimension` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.label` | ❌ | D/G | explicit Unsupported |
| `gpu-texture.set-label` | ❌ | D/G | explicit Unsupported |

## `gpu-texture-view`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-texture-view.label` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `gpu-texture-view.set-label` | ❌ | E/G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `gpu-uncaptured-error-event`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `gpu-uncaptured-error-event.error` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `record-gpu-pipeline-constant-value`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `record-gpu-pipeline-constant-value.add` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-gpu-pipeline-constant-value.get` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-gpu-pipeline-constant-value.has` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-gpu-pipeline-constant-value.remove` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-gpu-pipeline-constant-value.keys` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-gpu-pipeline-constant-value.values` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-gpu-pipeline-constant-value.entries` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `record-option-gpu-size64`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `record-option-gpu-size64.add` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-option-gpu-size64.get` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-option-gpu-size64.has` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-option-gpu-size64.remove` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-option-gpu-size64.keys` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-option-gpu-size64.values` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |
| `record-option-gpu-size64.entries` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

## `wgsl-language-features`

| Upstream method | Status | Slice | Notes |
|-----------------|--------|-------|-------|
| `wgsl-language-features.has` | ❌ | G | G close-out: explicit Unsupported (wasi stub / long-tail) |

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
