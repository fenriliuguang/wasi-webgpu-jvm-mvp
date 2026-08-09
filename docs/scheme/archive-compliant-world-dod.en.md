# Compliant wasi:webgpu world (no gfx) DoD archive (complete)

[中文](archive-compliant-world-dod.md) | **English**

> Completed acceptance checklist archived from the root README / plan page.  
> Plan: [`compliant-world.en.md`](compliant-world.en.md). Prior: [`archive-semantic-hardening-dod.en.md`](archive-semantic-hardening-dod.en.md).

Commits roughly: docs lock → A…F → this archive (slice G, 2026-08-09).

**Important:** Matrix close-out means every method is implemented **or** explicit Unsupported — **not** a claim of a shipping “compliant wasi:webgpu product”. Primary Guest path remains `experimental:webgpu-cm`; the standard package stays dual-track stubs.

## DoD

### A — Upstream pin + gap matrix

- [x] Vendored `wasi:webgpu@0.3.0-rc.2`; method-level gap matrix
- [x] wit-lock / PIN / inventory

### B — Dual-track Linker

- [x] `:abi-wasi` + `WasmtimeCmLinker` registers standard resources; func stubs
- [x] experimental Guests unaffected

### C — Compute de-specialize

- [x] `experimental:webgpu-cm@0.5.0` standard descriptors; helpers kept deprecated
- [x] Nested borrow: recursive patch; device still uses top-level helpers (pending Android `.so`)

### D — Texture / Sampler / PipelineLayout

- [x] `@0.6.0`; L2/Dawn/Cpu; compute-pipeline.layout → pipeline-layout

### E — Generic render

- [x] `@0.7.0` generic `create-render-pipeline` / `begin-render-pass`; helpers delegate
- [x] Instrumented two waves green (incl. abi-mvp BGL→PL wrap)

### F — result / error-kind

- [x] `HostErrorMapping` + wasi result stubs → `ComponentVal.err`
- [x] experimental still traps; async sync-compat documented

### G — Long-tail close-out

- [x] query-set / render-bundle / features·limits / adapter-info / label / debug etc.: explicit ❌ Unsupported (wasi stub)
- [x] Gap matrix has no dangling “missing” rows (every row ✅ / ⚠️ / ❌)
- [x] This archive; root README / scheme / CHANGELOG synced; **still no compliance marketing**

## Key deliverables

- Standard-package vendor + dual-track Linker + experimental `@0.7.0` main path
- Gap: [`docs/mapping/compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md)
- Errors: [`docs/mapping/errors-async.en.md`](../mapping/errors-async.en.md)
- Out of scope: wasi-gfx, Maven, `abi-mvp` render, perf, upstream PRs, true CM async
