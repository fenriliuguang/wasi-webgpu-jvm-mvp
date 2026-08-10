# Engineering handoff DoD archive (complete)

[中文](archive-engineering-handoff-dod.md) | **English**

> Completed acceptance checklist moved out of the root README / plan page.  
> Plan page: [`engineering-handoff.en.md`](engineering-handoff.en.md). Continues from: [`archive-guest-descriptor-cube-dod.en.md`](archive-guest-descriptor-cube-dod.en.md).

Archive covers slices A (Maven publishability, no external release) → B (abi-mvp flat render) → C (optional perf notes), 2026-08-10.

**Important:** Still **experimental**; local Publishing **≠** external release / consumer-ready claims; true async is chartered at [`true-cm-async.en.md`](true-cm-async.en.md) (memo history [`true-cm-async-memo.en.md`](true-cm-async-memo.en.md)). Primary acceptance remains CM cube.

## DoD

### A — Maven publishability engineering (no external release)

- [x] Pin `groupId` / `artifactId` / `0.1.0-experimental`; mark experimental / non-compliant / no external release
- [x] `publishEngineeredToMavenLocal` (host-api / host-webgpu / abi-mvp / abi-cm / abi-wasi)
- [x] [`docs/maven-local.en.md`](../maven-local.en.md); exclude demo / runtime-wasmtime natives / Guest / jniLibs
- [x] CHANGELOG + README status sync

### B — abi-mvp flat render

- [x] Flat surface/render imports + `WasmtimeAbiLinker` registration (CM cube-aligned subset)
- [x] Cpu Host unit tests (multi-frame surface + render main chain); no new instrumented cases
- [x] [`render-subset.en.md`](../mapping/render-subset.en.md) abi-mvp row ❌ → ⚠️ subset; **primary acceptance remains CM cube**
- [x] CHANGELOG

### C — Optional perf (non-blocking)

- [x] [`docs/perf/p1-boundary.en.md`](../perf/p1-boundary.en.md) anchors moved to abi-mvp / CM cube; marked informal
- [x] Non-gating `AbiMvpHostBindingsTest.boundaryNoteTimingSmoke` (no ratio assertion)
- [x] CHANGELOG

## Key deliverables

- Local coordinates: [`docs/maven-local.en.md`](../maven-local.en.md)
- abi-mvp flat render: `AbiMvp` / `AbiMvpHostBindings` / `WasmtimeAbiLinker`
- Perf notes: [`docs/perf/p1-boundary.en.md`](../perf/p1-boundary.en.md)
- Out of scope: wasi-gfx, compliance marketing, external release, true CM async / WASI P3, upstream PRs, true WIT dtor overlay, JMH / formal perf contracts
