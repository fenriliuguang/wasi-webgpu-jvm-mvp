# Guest CM on-screen (triangle-cm) — locked plan

[中文](guest-onscreen-cm.md) | **English**

> **Status: locked (main slice A).** Root README current DoD matches this page.

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

- [ ] `guest/triangle-cm` (or equivalent world export) + prebuilt `.wasm`; reuse triangle-shaped helpers
- [ ] Host injects native window; Guest does not create windows
- [ ] Android instrumented: CM Guest → Dawn on-screen triangle (needs CM-patched Bionic `.so`)
- [ ] Docs: Guest path in `docs/mapping/render-subset`; check off root README / this DoD
- [ ] Desktop: skip surface-related unit tests without Android Surface (same CM gating)

## Out of scope (this slice)

- wasi-gfx canvas abstraction
- General render-pipeline descriptors / MSAA / depth
- `abi-mvp` flat render imports
- Full compliant `wasi:webgpu` world, Maven Central

## Sequence

1. Guest WIT world export (e.g. `run-triangle` / `draw-triangle-frame`) + rebuild  
2. Demo / tests: CM instantiate → one-shot draw (frame loop optional later)  
3. Instrumented green + bilingual docs index  
4. CHANGELOG

## Risks

Frame-loop vs CM thread affinity; Surface lifetime vs Guest resource drop; instrumented-test flakiness.

## Links

- Root README: [`README.en.md`](../../README.en.md)  
- Baseline archive: [`archive-baseline-dod.en.md`](archive-baseline-dod.en.md)  
- Render mapping: [`docs/mapping/render-subset.en.md`](../mapping/render-subset.en.md)  
- Threading: [`docs/mapping/threading.en.md`](../mapping/threading.en.md)  
- WIT: [`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)
