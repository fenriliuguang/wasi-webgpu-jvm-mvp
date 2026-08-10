# Engineering handoff: Maven / abi-mvp render / optional perf (engineering-handoff)

[中文](engineering-handoff.md) | **English**

> **Status: locked (2026-08-10).** Slices **A–C** (C optional, non-blocking); **implementation not started**.  
> Continues from: Guest standard descriptors + cube A–D archive ([`archive-guest-descriptor-cube-dod.en.md`](archive-guest-descriptor-cube-dod.en.md)).  
> Composition: Maven publish prep (A) → abi-mvp flat render (B) → optional perf notes (C).

## One-liner

Without **wasi-gfx**, **compliance-product marketing**, **true WIT dtor / upstream PRs / true CM async**: close the three engineering items deferred across prior phases — **publishable Maven coordinates and docs**, a **minimal abi-mvp (core wasm) flat render import surface**, and a **non-blocking perf boundary note refresh**. Device / Demo acceptance remains experimental CM cube (`guest/cube-cm` / `@0.8.0`).

```text
A Maven publish prep (coordinates / publish docs or scripts / non-compliance notice)
  → B abi-mvp flat render (mirror CM cube subset; primary acceptance stays on CM)
  → C Optional perf (refresh docs/perf; must not block A/B close-out)
```

Refs: [`render-subset.en.md`](../mapping/render-subset.en.md) · [`docs/perf/p1-boundary.en.md`](../perf/p1-boundary.en.md) · [`archive-guest-descriptor-cube-dod.en.md`](archive-guest-descriptor-cube-dod.en.md).

## Decisions

| Topic | Decision |
|-------|----------|
| Phase scope | Lock **A+B+C**; out-of-scope table is hard; **no** wasi-gfx / compliance marketing / true CM async / upstream PRs / true dtor overlay |
| Order | **A** and **B** may run in parallel; **C** is optional and **must not** block A/B close-out |
| Primary track | Still **experimental CM cube**; abi-mvp Guest / unit tests from B must not replace `WasmtimeCmCubeInstrumentedTest` |
| **A Maven goal** | Produce **publishable** multi-module coordinates, versioning, `experimental` / non-compliance notices, and publish steps (docs or scripts); **actual Maven Central upload** is not required for close-out (Sonatype / local `publishToMavenLocal` green is enough) |
| **A module set** | At least: `host-api`, `host-webgpu`, `abi-mvp`, `abi-cm` (and `abi-wasi` if exported with Host); **do not** treat `android-demo` / Guest wasm / prebuilt Bionic `.so` as primary Central artifacts |
| **B abi-mvp render** | Add **flat** surface/render imports on **core wasm / abi-mvp** aligned with the current L2 on-screen main chain (reasonable subset of [`render-subset.en.md`](../mapping/render-subset.en.md)); prefer Cpu + desktop unit tests; **no** requirement to move device instrumentation onto abi-mvp |
| **B depth / texture** | May use existing L2: `depth` / `write-texture` / `set-bind-group`, etc.; **no** MSAA / multi-light / PBR / wasi-gfx; **no** second CM Guest as primary Demo |
| **C perf** | Refresh [`docs/perf/`](../perf/): drop deleted vector-add instrumented anchors; optional desktop smoke comparing Guest→L2 vs Kotlin→L2; **not** a formal benchmark / JMH / speed-ratio gate |
| Async | Stay **sync-compat** |
| Compliance claims | Package / README / POM `description` stay `experimental`; publishing **must not** claim a compliant `wasi:webgpu` product |
| Upstream | Overlay/patches self-contained; **no** issues/PRs to tegmentum/wasmtime4j; **no** true WIT dtor overlay |
| Acceptance | A: docs + `publishToMavenLocal` (or equivalent) green; B: `:abi-mvp:test` / related runtime unit tests + mapping checkboxes; C: docs update enough; CM cube instrumented path does not regress; per-slice CHANGELOG |

## Slices & DoD

### A — Maven publish prep

- [ ] Pin `groupId` / module `artifactId` / versioning (aligned with root README package `io.github.fenriliuguang.wasi.webgpu.experimental.*`); mark **experimental / non-compliant**
- [ ] Gradle Publishing (or equivalent) so core libraries can `publishToMavenLocal`; list intended publish set and **explicit non-publish** items (demo, huge jniLibs, Guest sources)
- [ ] Docs: publish steps, consumer coordinates, and boundary vs self-built Bionic / desktop-natives (CM consumers still follow [`android-wasmtime.en.md`](../android-wasmtime.en.md))
- [ ] CHANGELOG + root README status sync; **no** requirement that artifacts already appear on Maven Central search

### B — abi-mvp flat render

- [ ] Wire flat surface/render imports in `abi-mvp` / related runtime (mirror CM cube main chain: configure / get-current-texture-view / present / render-pipeline / begin-render-pass / draw, etc.; depth / `write-texture` may be additive from existing L2)
- [ ] Cpu Host unit coverage for the main chain; Dawn stays on existing L2 — **no** mandatory new instrumented cases
- [ ] Update [`render-subset.en.md`](../mapping/render-subset.en.md): `abi-mvp` flat render row ❌ → this phase’s subset status; note **primary acceptance remains CM cube**
- [ ] CHANGELOG; confirm `run-android-instrumented.ps1` (CM cube) does not regress

### C — Optional perf (non-blocking)

- [ ] Update [`docs/perf/p1-boundary.en.md`](../perf/p1-boundary.en.md): move anchors off deleted vector-add instrumented tests onto a reproducible current path (desktop CM cube smoke or abi-mvp subset; keep “not a formal benchmark”)
- [ ] (Optional) Keep or add a non-gating timing smoke; failures must not block A/B
- [ ] One-line CHANGELOG is enough; **no** FPS / speed-ratio DoD

## Out of scope

| ID | Item |
|----|------|
| — | wasi-gfx / canvas / multi-window (on-screen stays Host-injected) |
| — | Advertising a compliant `wasi:webgpu` product (publishing ≠ compliance claim) |
| — | True CM async / WASI Preview3 async runtime (keep sync-compat) |
| — | Issues/PRs to tegmentum/wasmtime4j; true WIT dtor / `JniComponentLinker` rep-only overlay |
| — | Moving primary Demo / device acceptance onto abi-mvp or a `wasi:webgpu` Guest |
| — | Clearing gap-matrix long tail (query-set / render-bundle / features·limits, …) |
| — | MSAA, multi-light, PBR, runtime-downloaded textures |
| — | Requiring Maven Central to be live; requiring JMH / formal perf contracts |
| — | Shipping prebuilt Bionic `libwasmtime4j.so` / full jniLibs as primary Central artifacts |

## Landing order

1. **A** and **B** may proceed in parallel; prefer pinning A coordinates before B so artifact boundaries do not thrash  
2. **C** anytime; non-blocking  
3. Docs close-out: check DoD on this page → [`archive-engineering-handoff-dod.en.md`](archive-engineering-handoff-dod.en.md); root README / scheme / CHANGELOG  

## Links

- Root README: [`README.en.md`](../../README.en.md)  
- Scheme index: [`docs/scheme/README.en.md`](README.en.md)  
- Prior archive: [`archive-guest-descriptor-cube-dod.en.md`](archive-guest-descriptor-cube-dod.en.md)  
- Render / Compute: [`render-subset.en.md`](../mapping/render-subset.en.md) · [`compute-subset.en.md`](../mapping/compute-subset.en.md)  
- Perf draft: [`docs/perf/p1-boundary.en.md`](../perf/p1-boundary.en.md)  
- Android natives: [`docs/android-wasmtime.en.md`](../android-wasmtime.en.md) · [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md)  
- Dual-track / gap (read-only; no long-tail sweep): [`compliant-world-dual-track.en.md`](../mapping/compliant-world-dual-track.en.md) · [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md)  
