# True CM async (tier A) / optional P3 spike (true-cm-async) — archived (A gate)

[中文](true-cm-async.md) | **English**

> **Status: slice A gate close-out (2026-08-10).** Archive: [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md).  
> Continues from memo [`true-cm-async-memo.en.md`](true-cm-async-memo.en.md) and engineering-handoff archive ([`archive-engineering-handoff-dod.en.md`](archive-engineering-handoff-dod.en.md)).  
> **B–E stopped by the gate** (no L2 / Linker primary-path / Guest churn). Default remains **sync-compat**; primary acceptance stays CM cube.  
> Remains **experimental**; true async **≠** a compliance-product claim and **≠** an external release.

## One-liner

Original goal: move wasi:webgpu primary-path `async func`s from L2 `CountDownLatch` to CM futures.  
**Gate result:** Cargo/`component-model-async` is available, but wasmtime4j **47.0.2-1.5.0** Java has **no** future complete/reject surface → **stop** B–E.

```text
A Runtime spike → gate: not feasible (no Java future writer)
  ✗ B L2 non-blocking split (stopped)
  ✗ C Linker primary-path futures (stopped)
  ✗ D Guest smoke (stopped)
  ✓ E Docs close-out (this page + archive + UPSTREAM §5)
```

Refs: [`errors-async.en.md`](../mapping/errors-async.en.md) · [`threading.en.md`](../mapping/threading.en.md) · [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md) · memo [`true-cm-async-memo.en.md`](true-cm-async-memo.en.md).

## Spike A outcome (2026-08-10)

| Item | Result |
|------|--------|
| Cargo `component-model-async` | **Enabled** (bound to `component-model`); coexists with android / cm-resources patches; `wasi-p3` not in current natives |
| Engine flags | Need `concurrencySupport(true)` (+ `asyncSupport` / `wasmComponentModelAsync`) to create a CM-async Engine |
| `defineFunctionAsync` | Registers (`func_new_async`); callback remains **sync** `ComponentHostFunction` |
| Future complete/reject | **Missing** — `FutureAny` is opaque handle + `close` only; no Writer / write / complete / reject |
| Resource / nested borrow | Registration: `defineResource` + `defineFunctionAsync` OK; patch async callback branch still uses `val_to_component_value` (not `vals_to_host_params`) |
| Gate | **Triggered** → stop B–E; see [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md) · [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §5 |

Probe tests: `CmAsyncApiSurfaceTest` (no natives) · `CmAsyncHostImportSpikeTest` (needs `desktop-natives`).

## Decisions (at charter)

| Topic | Decision |
|-------|----------|
| Phase scope | Lock slices **A–E**; **tier A (CM async)** is the formal DoD; **tier B (WASI Preview3 / `enableWasiP3`)** is spike/optional only |
| Spike fail gate | If slice A proves async host import e2e unavailable, **stop** L2/Linker primary-path churn → **triggered** |
| Primary track | **Keep** experimental CM cube + sync-compat |
| Upstream | Overlay/patches self-contained; **no** issues/PRs to tegmentum/wasmtime4j |

## Slices & DoD

### A — Runtime spike (hard step 1; fail = gate)

- [x] Evaluate desktop / Android CM patched builds vs `component-model-async`; full `wasi-p3` is **not** a close-out requirement
- [x] Minimal e2e complete/reject → **not feasible** (API gap)
- [x] Registration-path probe + nested-borrow residual risk documented
- [x] Spike outcome → this page / UPSTREAM §5
- [x] DoD: written gate + probe tests + CHANGELOG

### B — L2 non-blocking split

- [ ] **Stopped (gate)**

### C — Linker primary-path futures

- [ ] **Stopped (gate)**

### D — Guest smoke + threading (do not move primary acceptance)

- [ ] **Stopped (gate)**

### E — Docs / matrix close-out + optional P3 spike

- [x] Gap-matrix primary paths **not** lifted to true async (remain sync-compat); README / scheme / errors-async close-out
- [x] (Optional) `enableWasiP3`: current natives lack `wasi-p3`; does not block close-out
- [x] → [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md)

## Out of scope

| ID | Item |
|----|------|
| — | Move primary Demo / device acceptance to an async wasi Guest or drop cube sync-compat |
| — | Full WASI Preview3 / `enableWasiHttpP3` as a close-out requirement |
| — | Sweep every WIT `async`; wasi-gfx; compliance marketing; external release |
| — | Issues/PRs to tegmentum/wasmtime4j; true WIT dtor overlay |
| — | Change L2 / Linker primary path without a future writer (gate) |

## Links

- Archive: [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md)  
- Root README: [`README.en.md`](../../README.en.md)  
- Scheme index: [`docs/scheme/README.en.md`](README.en.md)  
- Memo: [`true-cm-async-memo.en.md`](true-cm-async-memo.en.md)  
- Upstream §5: [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md)  
- Errors / async: [`errors-async.en.md`](../mapping/errors-async.en.md)  
