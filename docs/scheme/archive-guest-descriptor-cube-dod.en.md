# Guest standard descriptors + rotating textured cube DoD archive (complete)

[中文](archive-guest-descriptor-cube-dod.md) | **English**

> Completed acceptance checklist archived from the root README / plan page.  
> Plan: [`guest-descriptor-cube.en.md`](guest-descriptor-cube.en.md). Continues: [`archive-compliant-world-dod.en.md`](archive-compliant-world-dod.en.md).

Archive covers slices A (Android CM natives) → B (standard descriptors + cube Demo) → C (wasi PRIMARY_PATH wiring) → D (lifetime hardening, **still not true WIT dtor**), 2026-08-10.

**Important:** wasi subset wiring ≠ marketing a compliant `wasi:webgpu` product. Primary acceptance remains experimental `experimental:webgpu-cm` **CM cube**.

## DoD

### A — Android CM natives unlock

- [x] Rebuilt Bionic `.so` (recursive `cm-resources` + android patches)
- [x] Docs / scripts pinned; desktop CM natives same patch set
- [x] Nested standard-descriptor smoke

### B — Guest standard descriptors + rotating textured cube

- [x] Acceptance path no longer depends on deprecated helpers
- [x] `guest/cube-cm/` slow rotating textured cube; depth / `write-texture` / MVP; `@0.8.0`
- [x] Instrumented `WasmtimeCmCubeInstrumentedTest` + Demo `CubeCmOneShot`

### C — wasi primary-path subset wiring

- [x] `PRIMARY_PATH` ~33 → same `AbiCmHostBindings`; rest stubbed
- [x] Dual-track / gap docs; primary Guest stays experimental

### D — Resource-lifetime hardening

- [x] Chose strengthened frame-equivalent nets + documented gap; **no** `JniComponentLinker` rep-only overlay / upstream PR
- [x] `releaseLifetimeSafetyNets` + Cpu ×60 frames with no swapchain handle growth; Demo `releaseAllGpuObjects` may remain as handoff insurance
- [x] **Still not true WIT dtor** (see [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §4)

## Key deliverables

- Device acceptance baseline: CM cube (vector-add / triangle demos removed)
- Lifetime: [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md) §4; `AbiCmHostBindings` / `WasmtimeCmCube.Session`
- Out of scope for this phase: wasi-gfx, compliance marketing, true CM async, upstream PRs, true dtor overlay  
- Later completed: Maven publishability / `abi-mvp` render / optional perf → [`archive-engineering-handoff-dod.en.md`](archive-engineering-handoff-dod.en.md)
