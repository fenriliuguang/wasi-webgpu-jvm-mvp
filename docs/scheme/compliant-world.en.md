# Compliant wasi:webgpu world (no gfx) (compliant-world) — complete

[中文](compliant-world.md) | **English**

> **Status: complete (2026-08-09).** DoD archive: [`archive-compliant-world-dod.en.md`](archive-compliant-world-dod.en.md).  
> Slices **A–G all done**. Matrix close-out ≠ compliance marketing (experimental remains primary).  
> Continues: semantic-hardening A–E archive ([`archive-semantic-hardening-dod.en.md`](archive-semantic-hardening-dod.en.md)).  
> Packages: upstream pin (A ✅) → dual-track Linker (B ✅) → compute de-specialize (C ✅) → textures (D ✅) → generic render (E ✅) → error lift (F ✅) → long-tail close (G ✅).

## One-liner

Without advancing wasi-gfx, move the CM mainline from the then-current `experimental:webgpu-cm@0.4.0` subset to a pinned standard package `wasi:webgpu/webgpu@0.3.0-rc.2` with **full interface coverage** (method-level matrix: implement or explicit `Unsupported`), and on the experimental track migrate Guests to standard-shaped descriptor APIs (current package **`@0.7.0`**, primary acceptance still experimental); keep on-screen via Host-injected Android native window.

```text
A Upstream pin notes + gap matrix
  → B Dual-track package identity / Linker strategy
  → C Compute de-specialize (retire *storage3 / *3)
  → D Texture / Sampler / PipelineLayout
  → E Generic Render (retire *-triangle*; surface still Host-injected)
  → F result / error-kind lift (async stays sync-compat)
  → G Long-tail coverage close-out
```

Reference: [`wasi-webgpu-wasmtime`](https://crates.io/crates/wasi-webgpu-wasmtime).  
Gap matrix: [`docs/mapping/compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md).

## Locked decisions

| Question | Decision |
|----------|----------|
| Phase mainline | Full compliant `wasi:webgpu` **interface coverage** (method matrix: implement or explicit `Unsupported` closes a row) |
| gfx | **No** wasi-gfx / canvas; on-screen stays **Host-injected** Android native window |
| WIT pin | `wasi:webgpu@0.3.0-rc.2` (same as [`wit/README.en.md`](../../wit/README.en.md)); bump only after PIN + gap matrix |
| Migration | **Dual-track**: keep the experimental track until Guests move to the standard package (plan lock started at `@0.4.0`; after close-out current **`@0.7.0`** remains primary acceptance); standard package coexists as stubs |
| Async | Stay **sync-compat** (L2 / [`errors-async.en.md`](../mapping/errors-async.en.md)); true CM async does not block this phase |
| Compliance claims | Package / README stay `experimental`; after matrix close-out still do **not** advertise a compliant product |
| Explicitly deferred | Maven Central, `abi-mvp` flat render, optional perf, PRs to wasmtime4j — all out |
| Acceptance | Desktop unit tests (with natives) + Android instrumented (no regress on vector-add / triangle) + gap checkboxes; docs / CHANGELOG per sub-slice |

## Sub-slices & DoD

### A — Upstream pin + gap matrix

- [x] Vendor / pin upstream `wasi:webgpu@0.3.0-rc.2` WIT ([`wit/deps/wasi-webgpu/`](../../wit/deps/wasi-webgpu/) + [`PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)); do **not** drift with tip
- [x] Method-level gap matrix complete (224 rows: [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md); inventory [`_inventory.json`](../../wit/deps/wasi-webgpu/_inventory.json))
- [x] Update [`wit/README.en.md`](../../wit/README.en.md): standard-package pin path and experimental dual-track notes; regen scripts `scripts/gen-wasi-webgpu-inventory.py` / `gen-compliant-world-gap.py`

### B — Dual-track package identity / Linker

- [x] Standard import path (`wasi:webgpu/webgpu@0.3.0-rc.2`) coexists with the experimental track on the Linker ([`WasmtimeCmLinker`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmLinker.kt); current experimental `@0.7.0`)
- [x] ABI constants / resource-name mapping documented: [`abi-wasi`](../../abi-wasi/) `AbiWasi` + [`compliant-world-dual-track.en.md`](../mapping/compliant-world-dual-track.en.md); Guests stay on experimental (primary acceptance)
- [x] Docs: dual-track is transitional; after close-out **primary acceptance remains experimental**; standard package is **Unsupported / result stubs** (Guests not migrated yet)

### C — Compute de-specialize

- [x] Standard `bind-group-layout` / `bind-group` / `compute-pipeline` descriptor paths wired (`experimental:webgpu-cm@0.5.0` → L2)
- [x] Host helpers marked deprecated (`*storage3` / `*3` / `submit1` / `create-compute-pipeline-bgl`)
- [x] `vector-add-cm`: `create-bind-group-layout(descriptor)` on device; nested-borrow `create-bind-group` / `create-compute-pipeline` / `queue.submit(list)` still use top-level helpers until Android `.so` rebuild (`cm-resources` patch now **recurses** Resource→U32 — see [`android-wasmtime.en.md`](../android-wasmtime.en.md) §6)
- [x] Device `run-android-instrumented.ps1` two waves OK (vivo); triangle package bump only
- [x] Update [`compute-subset.en.md`](../mapping/compute-subset.en.md)
### D — Texture / Sampler / PipelineLayout

- [x] L2 + Dawn (and Cpu stubs) cover texture / sampler / pipeline-layout main paths (off-screen OK)
- [x] Gap rows marked ✅ or explicit ❌ / `Unsupported`
- [x] Mapping doc increments (`compute-subset` + gap); `experimental:webgpu-cm@0.6.0`; compute-pipeline.layout → pipeline-layout

### E — Generic Render (no gfx)

- [x] Generic `create-render-pipeline` / `begin-render-pass` descriptors (`experimental:webgpu-cm@0.7.0` → L2/Dawn); `*-triangle*` / `begin-render-pass-clear` kept deprecated
- [x] Surface still Host-injected native window; **no** wasi-gfx
- [x] `triangle-cm` package bump; standard-descriptor Guest path waits on Android `.so` rebuild (nested borrow; device still uses top-level helpers)
- [x] Update [`render-subset.en.md`](../mapping/render-subset.en.md) / gap / CHANGELOG

### F — result / error-kind lift

- [x] Map standard WIT `result` / error-kind onto the Host error surface (see [`errors-async.en.md`](../mapping/errors-async.en.md); `HostErrorMapping` + wasi stub → `ComponentVal.err`)
- [x] Async methods stay sync-compat wrappers; document deviation from upstream async/p3
- [x] Close F columns in the gap matrix (experimental still traps; wasi result stubs lifted)

### G — Long-tail coverage close-out

- [x] query-set / render-bundle / features·limits / adapter-info etc.: implement or explicit `Unsupported`
- [x] No dangling “missing” rows in the gap matrix (each row ✅ / ⚠️ / ❌)
- [x] Docs wrap-up: check DoD here → [`archive-compliant-world-dod.en.md`](archive-compliant-world-dod.en.md); root README / scheme / CHANGELOG; still **no** compliance-product marketing

## Out of scope (this phase)

| ID | Item |
|----|------|
| — | wasi-gfx / canvas / multi-window abstraction |
| — | Maven Central / publishing |
| — | `abi-mvp` flat render imports |
| — | Optional perf ([`docs/perf/`](../perf/)) non-blocking |
| — | Issues/PRs to tegmentum/wasmtime4j (in-repo overlays stay self-contained) |
| — | True CM async / WASI Preview3 async runtime (sync-compat this phase) |

## Sequence

1. **A** Upstream pin + gap matrix completion (docs may lead code)  
2. **B** Dual-track Linker / ABI identity  
3. **C** Compute de-specialize + vector-add Guest migration  
4. **D** Texture / Sampler / PipelineLayout  
5. **E** Generic Render + triangle Guest migration (still no gfx)  
6. **F** result / error-kind  
7. **G** Long-tail close-out + DoD archive  

## Links

- Root README: [`README.en.md`](../../README.en.md)  
- Scheme index: [`docs/scheme/README.en.md`](README.en.md)  
- Prior archive: [`archive-semantic-hardening-dod.en.md`](archive-semantic-hardening-dod.en.md)  
- Gap matrix: [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md)  
- Dual-track: [`compliant-world-dual-track.en.md`](../mapping/compliant-world-dual-track.en.md) · [`abi-wasi`](../../abi-wasi/)  
- Compute / Render subsets: [`compute-subset.en.md`](../mapping/compute-subset.en.md) · [`render-subset.en.md`](../mapping/render-subset.en.md)  
- Errors & async: [`errors-async.en.md`](../mapping/errors-async.en.md)  
- WIT: [`wit/README.en.md`](../../wit/README.en.md) · [`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)  
- Upstream brief: [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
