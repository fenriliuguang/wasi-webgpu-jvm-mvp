# True CM async DoD archive (slice A gate close-out)

[中文](archive-true-cm-async-dod.md) | **English**

> Completed-phase checklist moved out of the root README / plan page.  
> Plan: [`true-cm-async.en.md`](true-cm-async.en.md). Continues from [`archive-engineering-handoff-dod.en.md`](archive-engineering-handoff-dod.en.md).

Archive covers: slice **A (Runtime spike) closed via the fail gate** (2026-08-10); **B–E stopped by the gate** — no L2 / Linker primary-path / Guest churn.

**Important:** Remains **experimental**; default path stays **sync-compat**; primary acceptance stays CM cube. True async **≠** a compliance-product claim / external release.

## Gate verdict (one line)

Desktop / Android CM-patched natives already compile with Cargo `component-model-async` (via the `component-model` feature) and can enable Engine `concurrencySupport` + `wasmComponentModelAsync` + Linker `defineFunctionAsync`; but wasmtime4j **47.0.2-1.5.0** Java has **no** CM future create / write / complete / reject API (opaque `FutureAny` + sync-shaped `ComponentHostFunction` only). Closed per plan: “async host import e2e unavailable → stop later slices.”

## DoD

### A — Runtime spike (hard step 1; fail = gate)

- [x] Evaluate: desktop / Android CM patched builds already enable `component-model-async` via `component-model`; coexist with android / cm-resources patches; full `wasi-p3` is **not** a close-out requirement (not in current natives)
- [x] Minimal e2e: **not feasible** (no Java future writer); see [`CmAsyncApiSurfaceTest`](../../runtime-wasmtime/src/test/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/CmAsyncApiSurfaceTest.kt)
- [x] Registration path: `defineResource` + `defineFunctionAsync` register on an async-capable Engine ([`CmAsyncHostImportSpikeTest`](../../runtime-wasmtime/src/test/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/CmAsyncHostImportSpikeTest.kt)); nested borrow: cm-resources patch **async callback branch still uses** legacy `val_to_component_value` (not `vals_to_host_params`) — residual risk, not fixed under the gate
- [x] Docs: this archive + plan spike section + [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §5
- [x] DoD: written “not feasible + stop B–E” + desktop probe tests + CHANGELOG

### B — L2 non-blocking split

- [ ] **Stopped (gate)** — no `DawnWasiWebGpuHost` / sync API changes

### C — Linker primary-path futures

- [ ] **Stopped (gate)** — no `WasmtimeCmLinker` primary-path future rewiring

### D — Guest smoke + threading

- [ ] **Stopped (gate)** — no `guest/async-smoke-cm`; cube primary acceptance unchanged

### E — Docs / matrix close-out + optional P3 spike

- [x] Gap matrix: **do not** lift primary paths to true async (remain ⚠️ sync-compat); this archive + README / scheme / errors-async close-out
- [x] (Optional) `enableWasiP3`: current desktop/Android CM natives **do not** build `wasi-p3`; no rebuild; does not block close-out
- [x] This archive page

## Key deliverables

- Probe tests: `CmAsyncApiSurfaceTest` (no natives) + `CmAsyncHostImportSpikeTest` (needs desktop-natives)
- Upstream notes: [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §5
- Plan history: [`true-cm-async.en.md`](true-cm-async.en.md)
- Out of scope (gate): L2 split, Linker futures, async Guest, moving primary acceptance, compliance marketing, external release, upstream PRs, true WIT dtor overlay, full wasi-p3

## If upstream later ships a future writer

Re-open this phase (new plan page) before touching B→C→D; **until then** default remains sync-compat. This repo still does **not** open issues/PRs against tegmentum/wasmtime4j by default.
