# Demo CM stability + frame loop (demo-cm-stability) — locked plan

[中文](demo-cm-stability.md) | **English**

> **Status: locked (main slice).** Root README status section matches this page. Continues P6: [`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md).

## One-liner

Close out P6: make the manual Demo "CM triangle" as green as the instrumented path, and upgrade the CM Guest from a one-shot draw to a host-driven frame loop.

```text
MainActivity "CM triangle" button
  → pauseSurfaceAndAwait (L2 stops frames + unconfigure)
  → CM owns the Surface: init → draw-frame loop (single reused Host)
  → finish: confirm unconfigure after present + drop → resume L2
```

## Locked decisions

| Question | Decision |
|----------|----------|
| Main slice | Close out the four P6 symptom classes + host-driven frame loop; no WIT semantic-surface changes (no records), no wasi-gfx |
| Surface ownership | L2 fully paused while CM runs (reuse `TriangleRenderer.pauseSurfaceAndAwait`); confirm unconfigure before resuming L2 |
| Host | Reuse a single `DawnWasiWebGpuHost` per Demo process (aligns with the process-level CM host registry; `WasmtimeCmTriangleAndroid.runOnce` already accepts an injected host) |
| Frame-loop shape | Stay on WIT `@0.3.0`; append exports to world `triangle` (init / draw-frame / drop three-part, additive, no bump); the host `webgpu-triangle-cm` thread calls `draw-frame` every frame. Fallback: repeated one-shot (comparison only — surface rebuild per frame is costly) |
| Acceptance | Both green: manual rapid taps + a new instrumented repeat-trigger case |

## DoD

- [ ] Manual Demo repeat triggers of the CM triangle (incl. rapid taps) without `VK_ERROR_NATIVE_WINDOW_IN_USE` / `invalid handle` / SIGSEGV
- [ ] Repeated CM instantiate in the same process is stable (process-level registry reuse or isolation fix)
- [ ] Unconfigure confirmed after CM present; L2 can resume on-screen
- [ ] Frame loop: CM Guest triangle renders continuously (host-driven); `docs/mapping/threading` updated
- [ ] Instrumented: new repeat-trigger case green; existing CM tests (triangle / vector-add) do not regress
- [ ] Docs: blockers P6 marked resolved; CHANGELOG

## Out of scope (this slice)

- wasi-gfx canvas abstraction, full compliant `wasi:webgpu` world
- More WIT records (slice B), Maven Central
- `abi-mvp` flat render imports

## Sequence

1. Host / Surface lifecycle hardening (reuse Host, confirm unconfigure, resume L2)
2. Same-process repeat instantiate fix (process-level registry)
3. New instrumented repeat-trigger case + manual rapid-tap verification
4. WIT export split (init / draw-frame / drop) + host-driven frame loop
5. Docs / CHANGELOG

## Risks

- Registry fix may disturb desktop unit tests (`forkEvery=1` gating)
- Frame-loop thread affinity (render thread vs CM calling thread; see [`docs/mapping/threading.en.md`](../mapping/threading.en.md))
- Dawn present / processEvents timing (same class as P3 Scudo risk)

## Links

- Root README: [`README.en.md`](../../README.en.md)
- Previous slice archive: [`archive-guest-onscreen-cm-dod.en.md`](archive-guest-onscreen-cm-dod.en.md)
- P6 details: [`guest-onscreen-cm-blockers.md`](guest-onscreen-cm-blockers.md)
- Render mapping: [`docs/mapping/render-subset.en.md`](../mapping/render-subset.en.md)
- Threading: [`docs/mapping/threading.en.md`](../mapping/threading.en.md)
- WIT: [`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)
