# Compliant wasi:webgpu world (no gfx) (compliant-world) — in progress

[中文](compliant-world.md) | **English**

> **Status: in progress (locked 2026-08-09).** Slices **A–C complete** (vendor + dual-track Linker + compute de-specialize).  
> Continues: semantic-hardening A–E archive ([`archive-semantic-hardening-dod.en.md`](archive-semantic-hardening-dod.en.md)).  
> Packages: upstream pin (A ✅) → dual-track Linker (B ✅) → compute de-specialize (C ✅) → textures (D) → generic render (E) → error lift (F) → long-tail close (G).

## One-liner

Without advancing wasi-gfx, move the CM mainline from the `experimental:webgpu-cm@0.4.0` subset to a pinned, wired standard package `wasi:webgpu/webgpu@0.3.0-rc.2` with **full interface coverage** (method-level matrix: implement or explicit `Unsupported`), migrate vector-add / triangle Guests to standard descriptor APIs, and keep on-screen via Host-injected Android native window.

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
| Migration | **Dual-track**: keep `experimental:webgpu-cm@0.4.0` until Guests migrate; new path uses the standard package |
| Async | Stay **sync-compat** (L2 / [`errors-async.en.md`](../mapping/errors-async.en.md)); true CM async does not block this phase |
| Compliance claims | Package / README stay `experimental` until matrix close-out; do not advertise compliance |
| Explicitly deferred | Maven Central, `abi-mvp` flat render, optional perf, PRs to wasmtime4j — all out |
| Acceptance | Desktop unit tests (with natives) + Android instrumented (no regress on vector-add / triangle) + gap checkboxes; docs / CHANGELOG per sub-slice |

## Sub-slices & DoD

### A — Upstream pin + gap matrix

- [x] Vendor / pin upstream `wasi:webgpu@0.3.0-rc.2` WIT ([`wit/deps/wasi-webgpu/`](../../wit/deps/wasi-webgpu/) + [`PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)); do **not** drift with tip
- [x] Method-level gap matrix complete (224 rows: [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md); inventory [`_inventory.json`](../../wit/deps/wasi-webgpu/_inventory.json))
- [x] Update [`wit/README.en.md`](../../wit/README.en.md): standard-package pin path and experimental dual-track notes; regen scripts `scripts/gen-wasi-webgpu-inventory.py` / `gen-compliant-world-gap.py`

### B — Dual-track package identity / Linker

- [x] Standard import path (`wasi:webgpu/webgpu@0.3.0-rc.2`) coexists with `experimental:webgpu-cm@0.4.0` on the Linker ([`WasmtimeCmLinker`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmLinker.kt))
- [x] ABI constants / resource-name mapping documented: [`abi-wasi`](../../abi-wasi/) `AbiWasi` + [`compliant-world-dual-track.en.md`](../mapping/compliant-world-dual-track.en.md); old Guests stay on experimental until C/E
- [x] Docs: dual-track is transitional; after close-out the standard package is the primary acceptance path; standard funcs are **Unsupported stubs** for now (wire in C+)

### C — Compute de-specialize

- [x] Standard `bind-group-layout` / `bind-group` / `compute-pipeline` descriptor paths wired (`experimental:webgpu-cm@0.5.0` → L2)
- [x] Host helpers marked deprecated (`*storage3` / `*3` / `submit1` / `create-compute-pipeline-bgl`)
- [x] `vector-add-cm`: `create-bind-group-layout(descriptor)` on device; nested-borrow `create-bind-group` / `create-compute-pipeline` / `queue.submit(list)` still use top-level helpers until Android `.so` rebuild (`cm-resources` patch now **recurses** Resource→U32 — see [`android-wasmtime.en.md`](../android-wasmtime.en.md) §6)
- [x] Device `run-android-instrumented.ps1` two waves OK (vivo); triangle package bump only
- [x] Update [`compute-subset.en.md`](../mapping/compute-subset.en.md)
### D — Texture / Sampler / PipelineLayout

- [ ] L2 + Dawn (and Cpu stubs) cover texture / sampler / pipeline-layout main paths (off-screen OK)
- [ ] Gap rows marked ✅ or explicit ❌ / `Unsupported`
- [ ] Mapping doc increments (extend compute-subset / new section, or fold into gap)

### E — Generic Render (no gfx)

- [ ] Generic `create-render-pipeline` / render-pass descriptors; retire Guest dependence on `*-triangle*` helpers
- [ ] Surface still Host-injected native window (keep experimental-equivalent or standard surface subset); **no** wasi-gfx
- [ ] `triangle-cm` on standard descriptors; instrumented no regress vs D1–D6
- [ ] Update [`render-subset.en.md`](../mapping/render-subset.en.md)

### F — result / error-kind lift

- [ ] Map standard WIT `result` / error-kind onto the Host error surface (see [`errors-async.en.md`](../mapping/errors-async.en.md))
- [ ] Async methods stay sync-compat wrappers; document deviation from upstream async/p3
- [ ] Close F columns in the gap matrix

### G — Long-tail coverage close-out

- [ ] query-set / render-bundle / features·limits / adapter-info etc.: implement or explicit `Unsupported`
- [ ] No dangling “missing” rows in the gap matrix (each row ✅ / ⚠️ / ❌)
- [ ] Docs wrap-up: check DoD here → `archive-compliant-world-dod.en.md`; root README / scheme / CHANGELOG; still **no** compliance claim before matrix close-out

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
