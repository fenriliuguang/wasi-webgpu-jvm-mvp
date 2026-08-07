# Demo CM stability + frame loop DoD archive (complete)

[中文](archive-demo-cm-stability-dod.md) | **English**

> Completed acceptance checklist moved out of the root README / plan page.  
> Original plan: [`demo-cm-stability.en.md`](demo-cm-stability.en.md). Continues P6: [`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md).

Archive covers work through: `b5e6212` (Host / L2 resume) → `654896a` (Session) → `110944d` (instrumented repeat) → `841b55c` (three-part frame loop) + this docs wrap-up (2026-08-07).

## DoD

- [x] Manual Demo repeat CM triangle (pause → frame loop → resume; button disabled for the span) without must-hit `VK_ERROR_NATIVE_WINDOW_IN_USE` / `invalid handle` (rapid taps gated by disable)
- [x] Same-process repeated CM: reuse `WasmtimeCmTriangle.Session` (linker/instance); no back-to-back recreate
- [x] After CM `drop-triangle` (Guest unconfigure), L2 `resumeSurfaceAndAwait` can resume on-screen
- [x] Frame loop: `init-triangle` / `draw-frame` / `drop-triangle` (`@0.3.0` additive); `docs/mapping/threading` updated for CM
- [x] Instrumented: new `cmGuestRepeatTriangleReusesSession`; existing one-shot / vector-add paths kept (device re-run: `scripts/run-android-instrumented.ps1`)
- [x] Docs: blockers P6 marked resolved; CHANGELOG; this archive

## Key deliverables

- Demo: `TriangleRenderer.resumeSurfaceAndAwait`; `TriangleCmOneShot` reuses Host + Session + `runFrameLoopAndAwait`
- L1: `WasmtimeCmTriangle.Session` (`runTriangle` / `initTriangle` / `drawFrame` / `dropTriangle` / `runFrameLoop`)
- WIT / Guest: world `triangle` additive three-part exports; prebuilt `triangle_cm.wasm`
- Instrumented: `WasmtimeCmTriangleInstrumentedTest.cmGuestRepeatTriangleReusesSession`
- Threading: [`docs/mapping/threading.en.md`](../mapping/threading.en.md) CM Guest frame-loop contract
