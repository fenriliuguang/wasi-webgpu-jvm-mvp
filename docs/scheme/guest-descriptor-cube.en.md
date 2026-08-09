# Guest standard descriptors on device + rotating textured cube (guest-descriptor-cube)

[中文](guest-descriptor-cube.md) | **English**

> **Status: in progress (2026-08-09).** Slices **A ✅ / B ✅**; C–D not started.  
> Continues from: compliant-world A–G archive ([`archive-compliant-world-dod.en.md`](archive-compliant-world-dod.en.md)).  
> Composition: natives unlock (A ✅) → Guest standard descriptors + cube demo (B ✅) → wasi primary-path subset wiring (C) → resource-lifetime hardening (D).

## One-liner

Without **wasi-gfx** and without **compliance-product marketing**: rebuild the Android CM `.so` to unlock nested borrow; move experimental Guest device acceptance off top-level helpers onto standard descriptors; use a **slowly, continuously rotating cube with an open-licensed image texture** as the main demo; optionally wire existing L2 paths onto a wasi-track subset and harden resource lifetime. Primary acceptance stays on `experimental:webgpu-cm` (currently `@0.8.0`: depth / `write-texture` / cube world).

```text
A Rebuild Android CM natives (nested borrow)
  → B Guest standard descriptors + rotating textured cube demo
  → C wasi primary-path subset wiring (existing L2; do not clear long-tail ❌)
  → D Resource-lifetime hardening (true dtor or documented gap + safety nets)
```

Refs: [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md) (16 ✅ / 17 ⚠️ / 191 ❌) · [`compliant-world-dual-track.en.md`](../mapping/compliant-world-dual-track.en.md).

## Decisions

| Topic | Decision |
|-------|----------|
| Phase scope | Lock **A+B+C+D**; out-of-scope table is hard; Maven / `abi-mvp` render / perf are **not** in this phase’s DoD |
| Order | **A → B** strict; **C** may interleave after B’s tail but must not block cube demo close-out; **D** may interleave, non-blocking for A/B acceptance |
| Primary track | Still **experimental**; wasi dual-track may wire a subset, but the main Demo Guest does **not** move to `wasi:webgpu` (C is Host/Linker only) |
| **B demo choice** | **Slow continuous rotation + open-licensed image-textured cube** (Guest CM → same L2 → Dawn; host-driven frame loop); **not** per-frame color triangle / multi-draw as primary acceptance |
| Image asset | **Open license** (prefer **CC0** / public-domain equivalent; CC-BY requires attribution); **vendored** under `guest/.../assets/` (or equivalent) + `ATTRIBUTION`; **no** runtime download |
| Cube minimum surface | Allow **additive** experimental WIT / L2 / Dawn for this demo: **minimal depth**, **texture upload** (`write-texture` or `copy-buffer-to-texture`), UV + sampler bind, MVP uniform (`write-buffer`), optional index/`draw-indexed`; **no** MSAA / multi-light / PBR / wasi-gfx |
| Specialized APIs | Keep `*storage3` / `*3` / `submit1` / `*-triangle*` as deprecated if useful; **device acceptance must not depend** on top-level helpers after A |
| Gap matrix | C only lifts wasi rows that already have experimental/L2 paths; **do not** treat “clear all 191 ❌” as a goal |
| Async | Stay **sync-compat**; no true CM async |
| Compliance claims | Package / README stay `experimental`; wiring in C still **must not** claim a compliant `wasi:webgpu` product |
| Upstream | Overlay/patches self-contained; **no** issues/PRs to tegmentum/wasmtime4j |
| Acceptance | Desktop unit tests (with natives) + `run-android-instrumented.ps1` two waves no-regression + hand-tap demo shows slow rotating textured cube; per-slice CHANGELOG / mapping docs |

## Slices & DoD

### A — Android CM natives unlock

- [x] Rebuild via [`android-wasmtime.en.md`](../android-wasmtime.en.md) §6 / [`patches/`](../../patches/) (`scripts/build-wasmtime4j-android.ps1`); replace Bionic `.so` under `runtime-wasmtime/android-natives/jniLibs/` with **recursive** `cm-resources`
- [x] Document patch set, build command, `.so` paths; desktop CM natives share `cm-resources` (`build-wasmtime4j-desktop-cm.ps1`, honors `CARGO_TARGET_DIR`); Windows Android cross-compile rustc AV at `opt-level>=1` → script defaults `CARGO_PROFILE_RELEASE_OPT_LEVEL=0`
- [x] Smoke: `vector-add-cm` on nested standard descriptors; desktop `:runtime-wasmtime:test` (WasmtimeCmVectorAddTest) green; device two-wave instrumented regression deferred to hand-check / later slices
- [x] CHANGELOG notes natives rebuild + Guest nested-path smoke

### B — Guest standard descriptors + rotating textured cube

- [x] **Migrate:** `vector-add-cm` / `triangle-cm` to standard descriptors; drop deprecated helpers from acceptance paths
- [x] **New demo Guest** `guest/cube-cm/`: cube **continuously rotating** slowly about Y; all faces sample the same open-licensed texture
- [x] **Asset:** original CC0 64×64 checkerboard (procedural in Guest) + [`ATTRIBUTION.md`](../../guest/cube-cm/ATTRIBUTION.md); offline-reproducible
- [x] **Minimal Host/WIT adds:** `@0.8.0` depth-stencil, `write-texture`, render-pass `set-bind-group`, sampler+texture bind, per-frame MVP `write-buffer`; Surface stays **Host-injected**; host frame loop `CubeCmOneShot`
- [x] Instrumented: `WasmtimeCmCubeInstrumentedTest` + `run-android-instrumented.ps1` **wave3** (separate process from triangle; no back-to-back)
- [x] Update [`render-subset.en.md`](../mapping/render-subset.en.md) + CHANGELOG; `experimental:webgpu-cm` **0.7.0 → 0.8.0**

### C — wasi primary-path subset wiring

- [ ] On `WasmtimeCmLinker`, wire **existing** experimental/L2 primary-chain methods to `wasi:webgpu/webgpu@0.3.0-rc.2` (adapter/device/queue/buffer/compute+render subset); leave the rest Unsupported / result stubs
- [ ] Update [`compliant-world-dual-track.en.md`](../mapping/compliant-world-dual-track.en.md) + matching gap rows (this subset only); **primary acceptance Guest stays experimental**
- [ ] Document: wiring ≠ compliance product; no obligation for a wasi-track cube Guest
- [ ] Regression: experimental Guests / instrumented tests must not break due to wasi registration

### D — Resource-lifetime hardening

- [ ] Under [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md), advance one (or both): (1) rep-only destructor overlay / maintainable in-repo hook; (2) strengthen View↔Texture `tryDrop` + frame/Session safety nets and **document** the gap vs true WIT dtors
- [ ] No handle-leak symptoms across many rotating cube frames (reuse D2/D3/D6 habits: shared Session, `releaseAllGpuObjects` may remain as handoff insurance)
- [ ] **No** upstream PR; docs-only close-out is allowed if DoD states “still not true dtor”

## Out of scope

| ID | Item |
|----|------|
| — | wasi-gfx / canvas / multi-window (on-screen stays Host-injected) |
| — | Marketing a compliant `wasi:webgpu` product (matrix/wiring ≠ compliance claim) |
| — | True CM async / WASI Preview3 async runtime (stay sync-compat) |
| — | Issues/PRs against tegmentum/wasmtime4j (in-repo overlay) |
| — | Maven Central / publishing |
| — | `abi-mvp` flat render imports |
| — | Optional perf ([`docs/perf/`](../perf/)) — non-blocking, not in this phase DoD |
| — | Clearing all 191 gap ❌ rows / implementing query-set·render-bundle·features/limits long tail |
| — | Moving the main Demo Guest onto the standard-package import (C is Host/Linker subset only) |
| — | MSAA, multi-light, PBR, runtime texture download, proprietary / unclear-license assets |

## Landing order

1. **A** Rebuild and pin Android CM `.so` + smoke  
2. **B** Guest standard-descriptor migration + open-licensed rotating textured cube + mapping/instrumented  
3. **C** wasi primary-path subset wiring + dual-track/gap docs (may finish after B close-out)  
4. **D** Lifetime hardening or documented deviation  
5. Docs close-out: check all DoD → `archive-guest-descriptor-cube-dod.md` (create then); root README / scheme / CHANGELOG  

## Links

- Root README: [`README.en.md`](../../README.en.md)  
- Scheme index: [`docs/scheme/README.en.md`](README.en.md)  
- Prior archive: [`archive-compliant-world-dod.en.md`](archive-compliant-world-dod.en.md)  
- Gap / dual-track: [`compliant-world-gap.en.md`](../mapping/compliant-world-gap.en.md) · [`compliant-world-dual-track.en.md`](../mapping/compliant-world-dual-track.en.md)  
- Compute / Render: [`compute-subset.en.md`](../mapping/compute-subset.en.md) · [`render-subset.en.md`](../mapping/render-subset.en.md)  
- Android natives: [`docs/android-wasmtime.en.md`](../android-wasmtime.en.md) · [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
- WIT: [`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit) · [`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)  
