# Memo: true CM async / WASI Preview3 (true-cm-async-memo)

[中文](true-cm-async-memo.md) | **English**

> **Status: memo (2026-08-10).** [engineering-handoff](engineering-handoff.en.md) is archived; **not chartered** — decide separately whether to open a phase.  
> Current strategy remains **sync-compat** (see [`errors-async.en.md`](../mapping/errors-async.en.md)).  
> Remains **experimental**; true async **≠** a compliance-product claim and **≠** an external release.

## One-liner

Move wasi:webgpu 0.3’s many `async func`s from “block inside L2 with `CountDownLatch`” to **Component Model async (futures/streams)**; optionally layer **WASI Preview3**. Until this is chartered, primary acceptance stays experimental CM cube + sync-compat.

## Why a separate memo

| Today | Notes |
|-------|-------|
| L2 / Dawn | `requestAdapter` / `requestDevice` / `mapAsync` wait synchronously on the host-callback path + `processEvents` polling |
| Linker | Synchronous `ComponentHostFunction` registration; **no** `enableWasiP3` |
| Gap matrix | Most async rows ⚠️ sync-compat or ❌ stub |
| wasmtime4j (`47.0.2-1.5.0`) | Experimental `enableWasiP3` / CM async types; native `wasi-p3` **not** default; this repo’s patched builds do not enable it |

## Scope tiers (choose when chartering)

| Tier | Meaning | Notes |
|------|---------|-------|
| **A. CM async for wasi:webgpu** | Standard-package `async func` via futures; host does **not** block the wasm thread | Main path to align 0.3 async |
| **B. WASI Preview3 runtime** | `enableWasiP3` / `enableWasiHttpP3`, etc. | Extra; upstream still experimental/unstable |

Default charter recommendation: **A**; treat **B** as an explicit add-on.

## Work list (memo only; unscheduled)

### Runtime / natives

- [ ] Evaluate Android / desktop CM patched builds with `wasi-p3`; confirm `component-model-async` is usable
- [ ] Spike: whether wasmtime4j can run an **async host import** end-to-end (complete/reject futures); gap → overlay or wait upstream (repo default: **no** upstream PRs)
- [ ] Validate existing CM resource / process-global registry patches under async scheduling

### Host / ABI

- [ ] Remove sync-compat on the CM path: do not block in `DawnWasiWebGpuHost.awaitRequest` from host callbacks
- [ ] Split or reshape L2 (keep sync for Cpu/tests; async surface for CM); L2 must not depend on L1
- [ ] Rewire wasi primary-path methods (`request-adapter` / `request-device` / `map-async` / pipeline-async, …) to future semantics
- [ ] Errors: align `result` + future / error-context with `WasiResultCodec` / `HostErrorMapping`

### Guest / Demo / threading

- [ ] Guest / wit-bindgen on true async imports; decide whether experimental track upgrades or a separate wasi-track async Guest is added
- [ ] Redefine Dawn `processEvents`, CM scheduler, and Surface/present same-thread rules (see [`threading.en.md`](../mapping/threading.en.md))
- [ ] Demo / instrumented drivers: completion / poll model instead of “one sync host call finishes the op”

### Validation / docs

- [ ] Gap-matrix async rows: sync-compat/stub → true-async status
- [ ] Desktop CM smoke + instrumented regression if acceptance moves; **do not** change CM cube baseline before charter
- [ ] README / scheme / CHANGELOG; true async ≠ compliance claim / external release

## Suggested spike order (memo only)

1. Minimal Guest calling one `async` import (e.g. `request-adapter`) to prove the runtime  
2. If green, touch L2 + a few primary-path methods; if red, bound runtime/overlay first  
3. Long-tail async and optional P3  
4. Only then consider moving primary acceptance to an async wasi Guest  

## Explicitly out until chartered

- Must not block [engineering-handoff](engineering-handoff.en.md) A/B/C  
- Must not change the device acceptance baseline (CM cube + sync-compat)  
- No compliant `wasi:webgpu` marketing; no external release  
- No default issues/PRs to tegmentum/wasmtime4j (see [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md))

## Links

- Current async policy: [`errors-async.en.md`](../mapping/errors-async.en.md)  
- Gap matrix: [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md)  
- Threading: [`threading.en.md`](../mapping/threading.en.md)  
- Active phase: [`engineering-handoff.en.md`](engineering-handoff.en.md)  
- Upstream memo: [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md)  
