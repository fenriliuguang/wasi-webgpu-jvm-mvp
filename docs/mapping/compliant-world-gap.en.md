# Compliant-world gap matrix (skeleton)

[中文](compliant-world-gap.md) | **English**

> **Status:** docs-lock skeleton (2026-08-09); expand to per-method rows in implementation slice A.  
> **Pin:** `wasi:webgpu/webgpu@0.3.0-rc.2` (see [`wit/README.en.md`](../../wit/README.en.md))  
> **Current package:** `experimental:webgpu-cm@0.4.0` ([`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit))  
> **Phase plan:** [`docs/scheme/compliant-world.en.md`](../scheme/compliant-world.en.md)

This table contrasts the standard package resource surface with this repo’s experimental / L2 status. Close-out rule: every row ends as ✅ (wired), ⚠️ (semantic skew / sync-compat), or ❌ (explicit `Unsupported` / documented out). **No** dangling “missing” rows.

## Legend

| Mark | Meaning |
|------|---------|
| ✅ | Usable aligned path exists (experimental or L2) |
| ⚠️ | Path exists but specialized / shape skew / sync wrap |
| ❌ | Not present; implement or mark Unsupported in the target slice |
| — | Explicitly out of this phase (see plan “Out of scope”; e.g. wasi-gfx) |

| Column | Meaning |
|--------|---------|
| Upstream family / representative | Standard `wasi:webgpu` resource family or representative API (detail in slice A) |
| Repo status | present / specialized / missing |
| Target slice | A–G |
| Notes | Helper names, Host injection, Unsupported OK, etc. |

## Instance / Adapter / Device

| Upstream family / representative | Repo status | Target slice | Notes |
|----------------------------------|-------------|--------------|-------|
| `gpu.request-adapter` | ✅ | B/C | experimental `request-adapter`; async→sync |
| `gpu-adapter.request-device` | ⚠️ | C/F | no full device descriptor; async→sync |
| `gpu-adapter` features / limits / info | ❌ | G | Unsupported first OK |
| `gpu-device.get-queue` | ✅ | B/C | |
| `gpu-device` features / limits / destroy | ❌ | G | |
| `gpu.create-surface` / canvas family | — / ⚠️ | E | **no gfx**; existing `create-surface-from-native-window` (Host inject) as bridge |

## Buffer / Bind / Compute

| Upstream family / representative | Repo status | Target slice | Notes |
|----------------------------------|-------------|--------------|-------|
| `create-buffer` + `buffer-descriptor` | ✅ | C | `@0.2.0+` records already shape-aligned |
| `map-async` / mapped range / unmap | ⚠️ | C/F | sync wait; `result` not lifted |
| `create-shader-module` (WGSL) | ✅ | C | |
| `create-bind-group-layout` (generic entries) | ⚠️ | C | specialized: `create-bind-group-layout-storage3` |
| `create-bind-group` (generic entries) | ⚠️ | C | specialized: `create-bind-group3` |
| `create-pipeline-layout` | ❌ | D | |
| `create-compute-pipeline` (descriptor) | ⚠️ | C | currently layout+shader+entry helper shape |
| compute pass set/dispatch/end | ✅ | C | |
| `queue.write-buffer` / `submit` | ⚠️ | C | specialized: `write-buffer` + `submit1` |
| `copy-buffer-to-buffer` | ✅ | C | |

## Texture / Sampler / Views

| Upstream family / representative | Repo status | Target slice | Notes |
|----------------------------------|-------------|--------------|-------|
| `create-texture` / texture resource methods | ❌ | D | on-screen path only has surface current texture view |
| `create-sampler` | ❌ | D | |
| `texture.create-view` (generic) | ⚠️ | D/E | wrapped in `get-current-texture-view` |
| copy texture / buffer↔texture | ❌ | D/G | staged Unsupported OK |

## Render / Surface (no gfx)

| Upstream family / representative | Repo status | Target slice | Notes |
|----------------------------------|-------------|--------------|-------|
| `create-render-pipeline` (generic descriptor) | ⚠️ | E | specialized: `create-render-pipeline-triangle` / `-triangle-buffers` |
| vertex-buffer-layout records | ✅ | E | `@0.4.0`; still hung off triangle helpers |
| `begin-render-pass` (generic attachments) | ⚠️ | E | specialized: `begin-render-pass-clear` |
| render pass set-pipeline / set-vertex-buffer / draw / end | ✅ | E | draw arity subset |
| surface configure / present / unconfigure | ⚠️ | E | Host inject; not wasi-gfx canvas |
| MSAA / depth-stencil / multi-target | ❌ | E/G | Unsupported close-out OK |
| wasi-gfx / window / canvas | — | — | **out of this phase** |

## Query / Bundle / long tail

| Upstream family / representative | Repo status | Target slice | Notes |
|----------------------------------|-------------|--------------|-------|
| query-set / occlusion / timestamp | ❌ | G | default Unsupported closes |
| render-bundle / bundle encoder | ❌ | G | same |
| external texture / other long-tail | ❌ | G | same |
| full enum/flags/records alignment | ⚠️ | A/F | slice A fills detail vs upstream WIT |

## Known specialized APIs (Guest must leave in C/E)

| experimental API | Replacement direction | Slice |
|------------------|----------------------|-------|
| `create-bind-group-layout-storage3` | standard bind-group-layout descriptor | C |
| `create-bind-group3` | standard bind-group descriptor | C |
| `submit1` | standard `queue.submit` (list) | C |
| `create-render-pipeline-triangle` | standard render-pipeline descriptor | E |
| `create-render-pipeline-triangle-buffers` | same (vertex layouts partly aligned) | E |
| `begin-render-pass-clear` | standard begin-render-pass + color attachment | E |
| `create-surface-from-native-window` | keep as Host-inject path (not gfx); or map to standard surface subset | E |

## Completion rules (implementation slice A)

1. After vendoring upstream `webgpu.wit` / `imports.wit`, expand this table to **resource × method** rows.  
2. Every row gets status + target slice; ❌ + “explicit Unsupported” is a valid close.  
3. When bumping the pin: update PIN and this matrix first, then Host / ABI.  

## Links

- Phase plan: [`compliant-world.en.md`](../scheme/compliant-world.en.md)  
- Compute / Render subsets: [`compute-subset.en.md`](compute-subset.en.md) · [`render-subset.en.md`](render-subset.en.md)  
- Errors & async: [`errors-async.en.md`](errors-async.en.md)  
- Upstream imports: https://github.com/WebAssembly/wasi-webgpu/blob/main/imports.md  
