# Demo CM stability + frame loop (demo-cm-stability) — complete

[中文](demo-cm-stability.md) | **English**

> **Status: complete (2026-08-07).** DoD archive: [`archive-demo-cm-stability-dod.en.md`](archive-demo-cm-stability-dod.en.md).  
> Continues P6: [`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md).  
> **Device regression**: D1–D6 closed on V2458A (2026-08-08) → [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) (ZH).

## One-liner

Close out P6: make the manual Demo "CM triangle" as green as the instrumented path, and upgrade the CM Guest from a one-shot draw to a host-driven frame loop.

```text
MainActivity "CM triangle" button
  → pauseSurfaceAndAwait (L2 stops frames + Host teardown)
  → CM owns the Surface: init → draw-frame loop (reused Host + Session)
  → finish: drop-triangle → releaseAllGpuObjects → resume L2
```

## Locked decisions

| Question | Decision |
|----------|----------|
| Main slice | Close out the four P6 symptom classes + host-driven frame loop; no WIT semantic-surface changes (no records), no wasi-gfx |
| Surface ownership | L2 fully paused while CM runs (`pauseSurfaceAndAwait` → `teardownGpu`); then `resumeSurfaceAndAwait` |
| Host / Session (Demo) | Reuse Host + Session; each press `releaseAllGpuObjects` (keep Instance/linker) |
| Frame-loop shape | Stay on WIT `@0.3.0`; append `init-triangle` / `draw-frame` / `drop-triangle` (additive, no bump); host `webgpu-triangle-cm` drives the loop. Keep `run-triangle` for instrumented one-shot |
| Same-process repeat | Demo and instrumented reuse Session; frame cleanup via `releaseFrameResources` |
| Acceptance | Manual tap gating + instrumented repeat-trigger case |

## DoD

See archive [`archive-demo-cm-stability-dod.en.md`](archive-demo-cm-stability-dod.en.md) (all checked).

## Out of scope (this slice)

- wasi-gfx canvas abstraction, full compliant `wasi:webgpu` world
- More WIT records (slice B), Maven Central
- `abi-mvp` flat render imports

## Sequence (done)

1. ~~Host / Surface lifecycle hardening~~ — `b5e6212`
2. ~~Same-process repeat instantiate (Session reuse)~~ — `654896a`
3. ~~Instrumented repeat-trigger case~~ — `110944d`
4. ~~WIT three-part + host frame loop~~ — `841b55c`
5. ~~Docs / CHANGELOG~~ — this wrap-up

## Links

- DoD archive: [`archive-demo-cm-stability-dod.en.md`](archive-demo-cm-stability-dod.en.md)
- Device regression blockers: [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) (ZH)
- Root README: [`README.en.md`](../../README.en.md)
- Previous slice archive: [`archive-guest-onscreen-cm-dod.en.md`](archive-guest-onscreen-cm-dod.en.md)
- P6 details: [`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)
- Threading: [`docs/mapping/threading.en.md`](../mapping/threading.en.md)
- WIT: [`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)
