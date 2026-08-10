# True CM async (tier A) / optional P3 spike (true-cm-async) — in progress

[中文](true-cm-async.md) | **English**

> **Status: chartered / plan frozen (2026-08-10).** Slice **A–E** text and DoD are locked; **no code started yet** (docs first, implementation later).  
> Continues from memo [`true-cm-async-memo.en.md`](true-cm-async-memo.en.md) and engineering-handoff archive ([`archive-engineering-handoff-dod.en.md`](archive-engineering-handoff-dod.en.md)).  
> Composition: Runtime spike (A) → L2 non-blocking split (B) → Linker primary-path futures (C) → Guest smoke + threading (D) → docs/matrix + optional P3 spike (E).  
> Remains **experimental**; true async **≠** a compliance-product claim and **≠** an external release.

## One-liner

Move wasi:webgpu primary-path `async func`s (`request-adapter` / `request-device` / `map-async`) from “block inside L2 with `CountDownLatch`” to **Component Model async (futures)** so the CM host-callback path does **not** block the wasm thread; **WASI Preview3** is a non-blocking spike only. Primary acceptance **stays** experimental CM cube + sync-compat for the whole phase.

```text
A Runtime spike (async host import e2e; fail = hard gate)
  → B L2 non-blocking split (no awaitRequest on CM path; keep sync)
  → C Linker primary-path futures (request-adapter / request-device / map-async)
  → D Guest smoke + threading contract (do not move primary acceptance)
  → E Docs / gap-matrix close-out + optional P3 spike (must not block)
```

Refs: [`errors-async.en.md`](../mapping/errors-async.en.md) · [`threading.en.md`](../mapping/threading.en.md) · [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md) · memo [`true-cm-async-memo.en.md`](true-cm-async-memo.en.md).

## Decisions

| Topic | Decision |
|-------|----------|
| Phase scope | Lock slices **A–E**; **tier A (CM async)** is the formal DoD; **tier B (WASI Preview3 / `enableWasiP3`)** is spike/optional only and **must not** block close-out; out-of-scope table is hard |
| Order | **A → B → C** hard order (C needs B’s non-blocking surface); **D** after C; **E** close-out; optional P3 anytime without blocking A–D |
| Primary track | **Keep** experimental CM cube + sync-compat (`guest/cube-cm` / `WasmtimeCmCubeInstrumentedTest`); **do not** move primary Demo/instrumentation to an async wasi Guest |
| Guest strategy | Add a **minimal async smoke Guest** (prefer one wasi-track `async` import, e.g. `request-adapter`); experimental cube **does not** switch to true async |
| L2 | **Split**: keep sync API for Cpu/direct Kotlin/tests; CM async path uses a non-blocking completion surface (future/callback); **L2 still must not depend on L1** |
| Primary methods (first batch) | `request-adapter` → `request-device` → `map-async`; **do not** sweep pipeline-async / long-tail async |
| Spike fail gate | If slice A proves async host import e2e is unavailable, **stop** L2/Linker primary-path churn; document the runtime boundary and close via the gate |
| Async / claims | Advance true CM async; package / README stay `experimental`; **no** compliant `wasi:webgpu` product claim; **no** external release |
| Upstream | Overlay/patches self-contained; **no** issues/PRs to tegmentum/wasmtime4j; **no** true WIT dtor overlay |
| Acceptance | A: desktop smoke **or** written “not feasible” gate; B/C: `:host-*` / `:runtime-wasmtime:test` (with desktop-natives); D: async Guest desktop smoke + CM cube non-regression; E: gap matrix/CHANGELOG; per-slice CHANGELOG |

## Slices & DoD

### A — Runtime spike (hard step 1; fail = gate)

- [ ] Evaluate whether desktop / Android CM patched builds can enable `component-model-async` (and coexist with android / cm-resources patches); full `wasi-p3` is **not** a close-out requirement  
- [ ] Minimal e2e: one async host import can **complete / reject** a future (fake impl OK; Dawn not required)  
- [ ] Validate process-global resource registry / nested-borrow patches on the async registration path  
- [ ] Docs: spike outcome in this page or a short [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) section; gaps → overlay boundary (still no upstream PRs)  
- [ ] DoD: reproducible desktop smoke **or** explicit “not feasible + stop later slices” record; CHANGELOG  

### B — L2 non-blocking split

- [ ] `DawnWasiWebGpuHost`: CM path must not block inside host callbacks via `awaitRequest`; keep sync wrappers for Cpu / direct Kotlin / existing unit tests  
- [ ] `WasiWebGpuHost` (or a parallel async surface) exposes completable adapter/device/map requests; `processEvents` may still run in the background; contract in [`threading.en.md`](../mapping/threading.en.md)  
- [ ] Cpu Host: immediately-complete / controllable-delay impls for Linker unit tests without Dawn  
- [ ] DoD: L2 unit tests cover “non-blocking start + complete”; existing sync tests stay green; CHANGELOG  

### C — Linker primary-path futures

- [ ] `WasmtimeCmLinker`: rewire wasi (and experimental as needed) primary-path `request-adapter` / `request-device` / `map-async` to CM async/future semantics; other async stays stub / sync-compat  
- [ ] Errors: align future completion `result` / error-context with `WasiResultCodec` / `HostErrorMapping`  
- [ ] DoD: desktop CM unit tests prove complete/reject on the three primary paths; **no** requirement to move instrumentation to async; CHANGELOG  

### D — Guest smoke + threading (do not move primary acceptance)

- [ ] Minimal Guest (e.g. `guest/async-smoke-cm`): call at least one true async import and observe completion  
- [ ] Restate Dawn `processEvents`, CM scheduler, and Surface/present **same-thread** rules ([`threading.en.md`](../mapping/threading.en.md)); cube frame loop stays as-is  
- [ ] Demo / instrumentation: still `run-android-instrumented.ps1` + CM cube; async is desktop smoke only (Android instrumented async **optional**, not a primary gate)  
- [ ] DoD: async smoke green + cube instrumented/regression baseline unchanged; CHANGELOG  

### E — Docs / matrix close-out + optional P3 spike

- [ ] Gap-matrix async rows: three primary paths → true-async status; long tail stays stub/❌  
- [ ] Dual-track and other affected notes; root README / scheme status; CHANGELOG  
- [ ] (Optional) `enableWasiP3` exploration notes; failure or skip **must not** block A–D close-out  
- [ ] After all boxes: [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md) (+ ZH)  

## Out of scope

| ID | Item |
|----|------|
| — | Move primary Demo / device acceptance to an async wasi Guest or drop cube sync-compat |
| — | Make full WASI Preview3 / `enableWasiHttpP3` a close-out requirement |
| — | Sweep every WIT `async` (`on-submitted-work-done`, `get-compilation-info`, `pop-error-scope`, full pipeline-async, …) |
| — | wasi-gfx / canvas / multi-window abstraction |
| — | Compliant `wasi:webgpu` product marketing; any external release |
| — | Issues/PRs to tegmentum/wasmtime4j; true WIT dtor / `JniComponentLinker` rep-only overlay |
| — | Delete sync L2 entirely (Cpu/tests/direct calls may keep sync-compat) |

## Landing order

1. Charter docs: this page (+ZH) + memo/README/scheme/errors-async indexes  
2. **A** spike (gate)  
3. **B** L2 split → **C** Linker futures  
4. **D** Guest smoke + threading docs  
5. **E** matrix/CHANGELOG; optional P3 notes  
6. All boxes → archive DoD; root README / scheme / CHANGELOG close-out  

## Links

- Root README: [`README.en.md`](../../README.en.md)  
- Scheme index: [`docs/scheme/README.en.md`](README.en.md)  
- Memo (chartered): [`true-cm-async-memo.en.md`](true-cm-async-memo.en.md)  
- Prior archive: [`archive-engineering-handoff-dod.en.md`](archive-engineering-handoff-dod.en.md)  
- Errors / async: [`errors-async.en.md`](../mapping/errors-async.en.md)  
- Threading: [`threading.en.md`](../mapping/threading.en.md)  
- Gap / dual-track: [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md) · [`compliant-world-dual-track.en.md`](../mapping/compliant-world-dual-track.en.md)  
- Android natives: [`docs/android-wasmtime.en.md`](../android-wasmtime.en.md) · [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md)  
