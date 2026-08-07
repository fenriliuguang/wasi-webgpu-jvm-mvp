# Guest CM on-screen DoD archive (complete)

[中文](archive-guest-onscreen-cm-dod.md) | **English**

> Completed acceptance checklist moved out of the root README.  
> Later stages: root [`README.en.md`](../../README.en.md); P6 closed in [`archive-demo-cm-stability-dod.en.md`](archive-demo-cm-stability-dod.en.md).

Archive covers work through: `07ec669` instrumented-green fix (u64 JSON + vivo Scenario) + `47c342d` DoD check-off and docs wrap-up (2026-08-06). Original plan page: [`guest-onscreen-cm.en.md`](guest-onscreen-cm.en.md).

## DoD

- [x] `guest/triangle-cm` (or equivalent world export) + prebuilt `.wasm`; reuse triangle-shaped helpers such as `create-render-pipeline-triangle`
- [x] Host injects native window; Guest does not create windows
- [x] Android instrumented: CM Guest → Dawn on-screen triangle (needs CM-patched Bionic `.so`)
- [x] Docs: Guest path in `docs/mapping/render-subset`; root README / plan-page DoD checked off
- [x] Desktop: surface-related unit tests skip without an Android Surface (same CM gating)

## Key deliverables

- Guest: [`guest/triangle-cm`](../../guest/triangle-cm) (world `triangle`, export `run-triangle`) + prebuilt `triangle_cm.wasm`
- L1 / Android entries: `WasmtimeCmTriangle` + `WasmtimeCmTriangleAndroid` (Host injects the native window; Guest only holds `surface`)
- Instrumented green: `WasmtimeCmTriangleInstrumentedTest` (2026-08-06, vivo V2458A / Mali; one-shot draw acceptance)
- Fixes: P2 `ConcurrentCallCodec` unsigned-u64 parse (android-demo overlay); P5 vivo `ActivityScenario` intent mismatch → `ActivityLifecycleMonitorRegistry`
- Docs: Guest path in [`docs/mapping/render-subset.en.md`](../mapping/render-subset.en.md) (bilingual); pitfall log [`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)
- Leftover: ~~P6 manual Demo-button stability~~ → closed by [`demo-cm-stability.en.md`](demo-cm-stability.en.md) / [`archive-demo-cm-stability-dod.en.md`](archive-demo-cm-stability-dod.en.md) (2026-08-07)
