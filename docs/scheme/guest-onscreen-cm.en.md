# Guest CM on-screen (triangle-cm) — locked plan

[中文](guest-onscreen-cm.md) | **English**

> **Status: complete (instrumented green 2026-08-06, all DoD checked).** Root README DoD matches this page; leftovers in blockers P6 (out of slice).

## One-liner

Reuse existing L2 surface/render + `experimental:webgpu-cm@0.3.0` imports; add a Guest that draws a red triangle via Wasmtime CM.

```text
Guest triangle-cm.wasm
  → Wasmtime ComponentLinker + abi-cm
  → same WasiWebGpuHost / Dawn
  → Android SurfaceView
```

## Locked decisions

| Question | Decision |
|----------|----------|
| Main slice | A: Guest CM on-screen (not B records / not C wasi-gfx) |
| Acceptance shape | **One-shot / low-frequency draw first** (easier instrumented green); frame loop later if needed |
| Kotlin demo | **Keep both**: `TriangleRenderer` (L2) plus CM Guest path / instrumented test |
| Window | Kotlin injects native window; Guest **only holds** a `surface` resource |
| WIT version | Prefer stay on `@0.3.0` when only adding world export / Guest assets |

## DoD

- [x] `guest/triangle-cm` (or equivalent world export) + prebuilt `.wasm`; reuse triangle-shaped helpers
- [x] Host injects native window; Guest does not create windows
- [x] Android instrumented: CM Guest → Dawn on-screen triangle (needs CM-patched Bionic `.so`)
- [x] Docs: Guest path in `docs/mapping/render-subset`; check off root README / this DoD
- [x] Desktop: skip surface-related unit tests without Android Surface (same CM gating)

## Out of scope (this slice)

- wasi-gfx canvas abstraction
- General render-pipeline descriptors / MSAA / depth
- `abi-mvp` flat render imports
- Full compliant `wasi:webgpu` world, Maven Central

## Sequence

1. ~~Guest WIT world export (`run-triangle`) + rebuild~~ — done: `guest/triangle-cm` + `triangle_cm.wasm`
2. ~~Demo / tests: CM instantiate → one-shot draw~~ — wired: `WasmtimeCmTriangle` + Demo button + instrumented skeleton (desktop skips / CpuHost Unsupported without Surface)
3. ~~Instrumented green + bilingual docs index~~ — green (2026-08-06, vivo V2458A / Mali); `render-subset` Guest path added in both languages
4. ~~CHANGELOG~~ — added (Unreleased: Guest CM on-screen)

## Risks

Frame-loop vs CM thread affinity; Surface lifetime vs Guest resource drop; instrumented-test flakiness.

## Links

- Root README: [`README.en.md`](../../README.en.md)  
- Baseline archive: [`archive-baseline-dod.en.md`](archive-baseline-dod.en.md)  
- Render mapping: [`docs/mapping/render-subset.en.md`](../mapping/render-subset.en.md)  
- Threading: [`docs/mapping/threading.en.md`](../mapping/threading.en.md)  
- WIT: [`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)
