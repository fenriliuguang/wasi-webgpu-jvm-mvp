# Track A mainline: L2 / cube baseline care + Host follow for Track B (track-a-baseline-host)

[中文](track-a-baseline-host.md) | **English**

> **Status: current mainline (chartered 2026-08-14; implementation not started).**  
> Follows: true CM async A-gate close-out ([`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md)) + dual-track lock ([`dual-runtime-track.en.md`](dual-runtime-track.en.md)).  
> Stack: **A baseline care (CI / instrumented / lifetime checklist)** → **B formal Host surface (follow Track B WIT; no async)**.

## One-liner

This repo (**Track A**) does not open a new product phase. Mainline work is **keep L2 + CM cube device baseline healthy**, and **converge / document the formal L2 Host surface ahead of Track B WIT expansion** (A changes Host first; B follows). Remains **sync-compat locked**, **experimental**, acceptance still `guest/cube-cm`.

```text
A Baseline care (CI gap / repeat cube instrumented / lifetime checklist)
  → B Host follow for Track B (formal beginRenderPass + queueSubmit(list) + texture/view contract)
```

See: [`dual-runtime-track.en.md`](dual-runtime-track.en.md) · [`render-subset.en.md`](../mapping/render-subset.en.md) · [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) (ZH) · Track B [`gap-experimental-vs-wasi-webgpu.md`](../../../wasmtime-android-kt/docs/mapping/gap-experimental-vs-wasi-webgpu.md).

## Decisions

| Topic | Decision |
|-------|----------|
| Role | **Track A**: demo / CI / CM cube; **not** the true CM async product line |
| Scope | Lock **A+B**; “Out of scope” table is hard |
| Order | **A → B**: harden gates first, then formal Host convergence |
| Acceptance | Still **experimental CM cube** (`WasmtimeCmCubeInstrumentedTest` + `scripts/run-android-instrumented.ps1`) |
| Async | **Locked sync-compat**; no Dawn await rewrite / Linker futures / async Guest move |
| Follow B | L2 changes land **here first**; `publishEngineeredToMavenLocal`; **do not** edit Track B in this repo |
| Expansion discipline | Only formalize what Track B **already uses or next cut needs**; no consumer-less gap-matrix sweep |
| Compliance / release | Stay `experimental`; local Publishing **≠** external release; no compliant-product claim |
| Upstream | No default tegmentum/wasmtime4j PR; no true WIT dtor overlay |
| Evidence | A: CI + Cpu tests + (device) instrumented script; B: formal-path tests + mapping/KDoc; CHANGELOG per slice |

## Slices and DoD

### A — Baseline care (reliability)

- [ ] **CI:** [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) JVM job adds `:abi-cm:test` (and cheap `:abi-wasi:test`); add `publishEngineeredToMavenLocal` self-check (fail red; no remote upload)
- [ ] **Instrumented:** [`WasmtimeCmCubeInstrumentedTest`](../../android-demo/src/androidTest/java/io/github/fenriliuguang/wasi/webgpu/demo/WasmtimeCmCubeInstrumentedTest.kt) adds **same-Session `runCube` ×N** (default 3) beyond one-shot + 8 frames; still `releaseAllGpuObjects` after
- [ ] **Cpu non-accumulation:** harden [`AbiCmHostBindingsTest`](../../abi-cm/src/test/); instrumented stays “completes without throw” if no count API
- [ ] **Care checklist** (this page or dual-runtime-track short section): fake WIT dtor → `tryDrop` / `releaseLifetimeSafetyNets` / `releaseAllGpuObjects`; process-global CM linker → `force-stop` between waves + Session reuse; gate commands pinned
- [x] CHANGELOG + root README Status mark this page as **current mainline** (done in charter docs drop; code slices still not started)

### B — Host follow for Track B (formal surface)

Track B may still use deprecated / shortcut paths: `begin-render-pass-clear`, `queueSubmit1`, one-step `surface-get-current-texture-view`. L2 **already has** formal APIs; this slice makes them the dependable surface:

- [ ] **Annotate formal APIs:** KDoc + [`render-subset.en.md`](../mapping/render-subset.en.md) say Track B should move to `commandEncoderBeginRenderPass(descriptor)`, `queueSubmit(list)`, `surfaceGetCurrentTexture` + `textureCreateView`; keep clear helper / `queueSubmit1` as compatibility window with deprecated migration notes
- [ ] **Tests:** abi-cm / abi-mvp Cpu paths add formal `beginRenderPass` + `queueSubmit(list)` minimal clear→finish→submit
- [ ] **texture / view lifetime short contract:** post-present `tryDrop` / frame-pair release documented for Track B’s two-step proposal names
- [ ] **Do not add this round** adapter `features` / `limits` / `info`, `deviceDestroy`, `on-submitted-work-done` (open only when Track B’s next cut needs them)
- [ ] Local `publishEngineeredToMavenLocal`; CHANGELOG; **no** Track B repo edits here

## Care checklist (pin before close-out; tick when executing)

| Item | Command / anchor |
|------|------------------|
| JVM + cube-related unit tests | `./gradlew :host-api:test :abi-cm:test` (also `:abi-mvp:test :abi-wasi:test` recommended) |
| Engineered coords self-check | `./gradlew publishEngineeredToMavenLocal` |
| Device primary gate | `./scripts/run-android-instrumented.ps1` (**do not** rely on Studio UTP) |
| Fake-dtor safety nets | View↔Texture `tryDrop`; Session `releaseLifetimeSafetyNets`; Demo/instrumented `releaseAllGpuObjects` |
| Process-global CM | `am force-stop` between waves; reuse Session in-process |

## Out of scope

| ID | Item |
|----|------|
| — | True CM async / WASI P3 / Java future writer; Dawn main-callback await rewrite |
| — | Silently switch instrumented / Demo default L1 to Track B |
| — | Compliant `wasi:webgpu` marketing; Maven Central / any external-release claim |
| — | True WIT dtor overlay; default tegmentum/wasmtime4j PRs |
| — | Full device-less Dawn JVM suite for `host-webgpu` (primary path stays instrumented) |
| — | natives / Guest wasm artifact packaging |
| — | Gap-matrix long-tail sweep; consumer-less adapter metadata / queue-done expansion |
| — | wasi-gfx / multi-window |

## Landing order

1. **A first** (CI + repeat cube + checklist docs)  
2. **Then B** (formal Host tests + mapping; publish self-check)  
3. Close-out: tick DoD → `archive-track-a-baseline-host-dod.md`; sync root README / scheme / CHANGELOG  

**Current:** plan chartered; **implementation not started** (2026-08-14).

## Links

- Dual-track lock: [`dual-runtime-track.en.md`](dual-runtime-track.en.md)  
- True async gate archive: [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md)  
- Stability blockers: [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) (ZH)  
- Maven local: [`../maven-local.en.md`](../maven-local.en.md)  
- Track B gap: [`../../../wasmtime-android-kt/docs/mapping/gap-experimental-vs-wasi-webgpu.md`](../../../wasmtime-android-kt/docs/mapping/gap-experimental-vs-wasi-webgpu.md)  
- Track B dual-track: [`../../../wasmtime-android-kt/docs/scheme/dual-track.md`](../../../wasmtime-android-kt/docs/scheme/dual-track.md)  
