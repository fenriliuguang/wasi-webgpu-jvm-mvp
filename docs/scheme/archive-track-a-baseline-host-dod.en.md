# Track A mainline DoD archive (complete)

[中文](archive-track-a-baseline-host-dod.md) | **English**

> Completed acceptance checklist moved out of the root README / plan page.  
> Plan page: [`track-a-baseline-host.en.md`](track-a-baseline-host.en.md). Continues from: [`archive-true-cm-async-dod.en.md`](archive-true-cm-async-dod.en.md) + [`dual-runtime-track.en.md`](dual-runtime-track.en.md).

Archive covers slices A (CI / repeat cube instrumented / lifetime checklist) → B (formal Host surface), 2026-08-15.

**Important:** Still **experimental**; local Publishing **≠** external release; async **locked sync-compat**; primary acceptance remains CM cube. **Not** a true-async / compliance-product phase.

## DoD

### A — Baseline care (reliability)

- [x] CI JVM job adds `:abi-cm:test` / `:abi-wasi:test`; android-assemble adds `publishEngineeredToMavenLocal` (fail red; no remote upload)
- [x] Instrumented `WasmtimeCmCubeInstrumentedTest` same-Session `runCube` ×3 (D6); still `releaseAllGpuObjects` after; first-launch RESUMED wait hardened
- [x] Cpu: `AbiCmHostBindingsTest` multi-frame + formal beginRenderPass/`queueSubmit(list)` + two-step acquire do not accumulate handles
- [x] Care checklist pinned (plan page + [`dual-runtime-track.en.md`](dual-runtime-track.en.md) short section)
- [x] CHANGELOG + root README status sync

### B — Host follow for Track B (formal surface)

- [x] KDoc + [`render-subset.en.md`](../mapping/render-subset.en.md): Track B should move to `commandEncoderBeginRenderPass(descriptor)`, `queueSubmit(list)`, `surfaceGetCurrentTexture` + `textureCreateView`; clear helper / `queueSubmit1` stay deprecated compatibility window
- [x] abi-cm / abi-mvp Cpu formal `beginRenderPass` + `queueSubmit(list)` minimal clear→finish→submit tests
- [x] texture / view lifetime short contract (post-present `tryDrop` / frame-pair release) documented
- [x] This round did **not** add adapter `features` / `limits` / `info`, `deviceDestroy`, `on-submitted-work-done`
- [x] Local `publishEngineeredToMavenLocal`; CHANGELOG; **no** Track B repo edits

## Key deliverables

- CI: [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)
- Instrumented: `cmGuestCubeSameSessionRepeatRunCube` + `scripts/run-android-instrumented.ps1`
- Formal surface: `WasiWebGpuHost` / `AbiCmHostBindings` / `AbiMvpHostBindings` KDoc; AbiCm two-step `surfaceGetCurrentTexture`
- Mapping: [`render-subset.en.md`](../mapping/render-subset.en.md) “Track B formal Host surface” section
- Out of scope: true CM async, silent Track B L1 swap, compliance marketing / external release, true WIT dtor overlay, device-less Dawn JVM suite, natives/Guest artifact packaging, gap-matrix long tail, wasi-gfx
