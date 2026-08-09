# Changelog

All notable changes to this experimental MVP are documented here.
Package / marketing claims remain **non-compliant** `wasi:webgpu` until a full standard world is wired.

## Unreleased

### Compliant-world slice G — long-tail close-out

- Gap matrix: remaining ❌ rows marked explicit Unsupported (wasi stub / long-tail); no dangling “missing”
- DoD archived: [`docs/scheme/archive-compliant-world-dod.md`](docs/scheme/archive-compliant-world-dod.md); plan A–G complete; **still no** compliance-product marketing

### Compliant-world slice F — result / error-kind lift

- `HostErrorMapping` + WIT error-kind mirrors (`WasiWebGpuError.kt`); wasi result stubs return `ComponentVal.err` via `WasiResultCodec` / `AbiWasiResults`
- experimental track unchanged (throw → trap); async remains sync-compat
- Docs: [`errors-async.md`](docs/mapping/errors-async.md); gap F rows; plan DoD F checked

### Compliant-world slice E — generic render (no gfx)

- `experimental:webgpu-cm` **0.6.0 → 0.7.0**: `create-render-pipeline(descriptor)` / `begin-render-pass(descriptor)`; deprecate `*-triangle*` / `begin-render-pass-clear`
- L2 + Dawn (+ Cpu Unsupported); AbiCm / WasmtimeCmLinker parsers; helpers delegate to generic path
- `abi-mvp` flat `device_create_compute_pipeline`: wrap guest BGL → PipelineLayout (slice D layout change; keeps old vector-add wasm)
- `triangle-cm` package bump; device Guest still uses top-level helpers until Android `.so` rebuild (nested borrow)
- Docs: render-subset + gap; plan DoD E checked; instrumented two waves OK (vivo)

### Compliant-world slice D — texture / sampler / pipeline-layout

- `experimental:webgpu-cm` **0.5.0 → 0.6.0**: `create-texture` / `create-sampler` / `create-pipeline-layout` / `texture.create-view`; BGL/BG sampler·texture entries; `compute-pipeline.layout` → pipeline-layout
- L2 + Dawn + Cpu stubs; AbiCm / WasmtimeCmLinker parsers; Cpu unit test for create/view/sampler/pipeline-layout + sampler/texture bind-group
- Gap matrix D primary paths ✅; attribute/label rows explicit Unsupported
- Guests rebuilt for package bump (vector-add still uses top-level helpers for nested borrow)

### Compliant-world slice C — compute de-specialize

- `experimental:webgpu-cm` **0.4.0 → 0.5.0**: standard `create-bind-group-layout` / `create-bind-group` / `create-compute-pipeline(descriptor)` / `queue.submit(list)`; keep deprecated `*storage3` / `*3` / `submit1` / `create-compute-pipeline-bgl`
- Host + Linker parsers wired; `vector-add-cm` uses layout descriptor + top-level resource helpers (nested borrow-in-record/list needs rebuilt `.so`)
- `cm-resources` patch: recursive `Resource`→`U32(rep)` inside list/record/option/… (apply + rebuild natives to unlock full descriptor Guest)
- Device: `run-android-instrumented.ps1` two waves OK (vivo); triangle Guest package bump only
- Docs: compute-subset + wit/compute-cm READMEs; plan DoD C checked

### Compliant-world slice B — dual-track Linker

- New `:abi-wasi` module: `AbiWasi` constants for `wasi:webgpu/webgpu@0.3.0-rc.2` (33 resources / 224 funcs; `scripts/gen-abi-wasi-constants.py`)
- `WasmtimeCmLinker`: register experimental + wasi resources; wasi funcs → `HostException.Unsupported` stubs; old Guests unchanged
- Docs: [`docs/mapping/compliant-world-dual-track.md`](docs/mapping/compliant-world-dual-track.md); plan DoD B checked

### Compliant-world slice A — upstream pin + gap matrix

- Vendored `wasi:webgpu@0.3.0-rc.2` at [`wit/deps/wasi-webgpu/`](wit/deps/wasi-webgpu/) ([`PIN.md`](wit/deps/wasi-webgpu/PIN.md), `webgpu.wit`, `imports.wit`, `_inventory.json`)
- Method-level gap matrix (224 rows): [`docs/mapping/compliant-world-gap.md`](docs/mapping/compliant-world-gap.md); regen via `scripts/gen-wasi-webgpu-inventory.py` + `scripts/gen-compliant-world-gap.py`
- wit-lock dual-track notes; plan DoD A checked; still **no** Host/ABI/Guest wiring (slice B+)

### Planning

- **Compliant wasi:webgpu world (no gfx) locked** (2026-08-09): [`docs/scheme/compliant-world.md`](docs/scheme/compliant-world.md) / [EN](docs/scheme/compliant-world.en.md); slices A–G; **no** wasi-gfx / Maven / `abi-mvp` render / perf
- **Semantic hardening complete** (2026-08-09): A–E archived; see [`docs/scheme/archive-semantic-hardening-dod.md`](docs/scheme/archive-semantic-hardening-dod.md)
- Prior locked slices archived (baseline / Guest CM on-screen / Demo CM stability + frame loop / device stability regression)

### Semantic hardening — DoD complete 2026-08-09

- Archived A–E DoD: [`docs/scheme/archive-semantic-hardening-dod.md`](docs/scheme/archive-semantic-hardening-dod.md)
- Root README / scheme: phase marked done; next phase later docs-locked as compliant-world (see ### Planning above)

### Semantic hardening — device instrumented re-check + D7

- V2458A：`scripts/run-android-instrumented.ps1` 两波全绿（compute → CM triangle；波间 force-stop）
- Triangle 仪器：async `startActivity`（勿 `startActivitySync`）；类内共享 Host+Session + `releaseAllGpuObjects`（D2/D3/D6）
- D7：脚本定为唯一推荐入口（Studio UTP 仍可能 Process crashed）

### Semantic hardening slice C — upstream gap notes (in-repo only)

- Expanded [`patches/UPSTREAM.md`](patches/UPSTREAM.md) / EN: ConcurrentCallCodec unsigned-u64, Validation TBI, CM destructor→rep gap, native patch index, overlay strategy
- Hard rule: **do not** open issues/PRs against tegmentum/wasmtime4j from this project; overlays stay long-term

### Semantic hardening slice B (partial) — frame View↔Texture drop

- L2: `tryDrop` / `HandleTable.tryDrop` (idempotent)
- AbiCm: track View↔Texture from `getCurrentTextureView`; `tryDrop` pair on present / next acquire; `releaseFrameResources` remains encoder/orphan sweep
- True WIT destructors still blocked by wasmtime4j `resourceTable` (optional C follow-up)

### Semantic hardening slice E — Guest vertex-buffer triangle

- `guest/triangle-cm`: upload float32x2 verts (`VERTEX|COPY_DST`), `create-render-pipeline-triangle-buffers` + `set-vertex-buffer`; shader `@location(0)` (same coords/color as L2)
- Rebuilt `triangle_cm.wasm` (imports buffers path from `@0.4.0`)
- Device instrumented re-check OK (V2458A); Demo hand-tap still nice-to-have

### Semantic hardening slice A (partial) — `experimental:webgpu-cm` 0.3.0 → 0.4.0

- WIT: `vertex-attribute` / `vertex-buffer-layout` records; `vertex-format` / `vertex-step-mode` aliases; `create-render-pipeline-triangle-buffers`; `render-pass-encoder.set-vertex-buffer`
- L2 / Dawn / Cpu / abi-cm / WasmtimeCmLinker wired; keep `create-render-pipeline-triangle` (`vertex_index`) for contrast
- Rebuilt guests against `@0.4.0`; E migrates triangle Guest to buffers API
- Docs: render-subset + wit READMEs; E locked to vertex buffer in `semantic-hardening`

### Demo CM device stability regression — complete 2026-08-08 (V2458A)

- D2/D3: `close()` closes GPU objects; `releaseAllGpuObjects()` clears handle table (keep Instance) so CM can reuse Session without `WINDOW_IN_USE`
- D5: `releaseFrameResources()` after present; AbiCm `getCurrentTextureView`/`present` hooks (Guest WIT destructors unwired)
- D6: Demo keeps Host+Session across presses; recreate Session only on trap
- D1: render-path `gpuLock` (encode/submit/present/drop vs `processEvents`); deferred first frame; Vulkan + Fifo
- Docs: blockers D1–D6 closed; README / scheme stage unlocked for later work

### Planning (historical)

- Semantic hardening complete unlocked the next phase; **compliant-world (no gfx) was later docs-locked** — see Unreleased ### Planning above
- Prior locked slices archived (baseline / Guest CM on-screen / Demo CM stability + frame loop / device stability regression)

### Demo CM stability + frame loop — DoD complete 2026-08-07

- Demo: pause L2 → CM frame loop → resume L2; reuse one CM `DawnWasiWebGpuHost` + `WasmtimeCmTriangle.Session` (avoids process-global linker recreate / `invalid handle`)
- WIT `@0.3.0` additive: `init-triangle` / `draw-frame` / `drop-triangle` (keep `run-triangle` for one-shot); rebuilt `triangle_cm.wasm`
- L1: `Session.runFrameLoop`; android-demo `TriangleCmOneShot.runFrameLoopAndAwait` (~60 frames) + `TriangleRenderer.resumeSurfaceAndAwait`
- Instrumented: `cmGuestRepeatTriangleReusesSession` (same-process Host+Session ×3); existing one-shot path kept
- Docs: `docs/mapping/threading` CM frame-loop contract; blockers P6 closed; DoD archive `docs/scheme/archive-demo-cm-stability-dod.md`

### Guest CM on-screen (triangle-cm) — DoD complete 2026-08-06

- `guest/triangle-cm` + prebuilt `triangle_cm.wasm`: world `triangle` exports `run-triangle(window-handle: u64, width: u32, height: u32)`; Guest only holds `surface`, Host injects the Android native window
- `WasmtimeCmTriangle` (L1) + android-demo `WasmtimeCmTriangleAndroid` / "CM 三角" button: Wasmtime ComponentLinker + abi-cm → same L2 → Dawn one-shot red triangle
- Instrumented green: `WasmtimeCmTriangleInstrumentedTest` (vivo V2458A / Mali); desktop CpuHost → Unsupported, surface unit tests skip (same CM gating)
- Fixes: wasmtime4j `ConcurrentCallCodec` unsigned-u64 window-handle parse (android-demo overlay, `patches/UPSTREAM.md`); vivo `ActivityScenario` intent mismatch → `ActivityLifecycleMonitorRegistry`; L2 skipped under androidx.test Instrumentation
- Docs: Guest path in `docs/mapping/render-subset`; pitfall log `docs/scheme/guest-onscreen-cm-blockers.md`

### Surface/render lift (L2 + WIT)

- L2: `WasiWebGpuHost` surface/render minimal API; `DawnWasiWebGpuHost` implements; `CpuWasiWebGpuHost` → Unsupported
- `experimental:webgpu-cm` **0.2.0 → 0.3.0**: surface + render-pipeline / render-pass helpers (Android native window)
- `TriangleRenderer` draws via L2 Host (not direct androidx.webgpu); still no Guest/wasi-gfx on-screen
- Docs: `docs/mapping/render-subset.md`; threading contract for surface/submit

### On-screen demo (Kotlin)

- `android-demo`: `SurfaceView` + `TriangleRenderer` red triangle (now L2 Host→Dawn)

### Semantic expansion (CM buffer records/flags)

- `experimental:webgpu-cm` **0.1.0 → 0.2.0**: `buffer-descriptor`, `buffer-usage-flags` / `map-mode-flags` (u32 aliases)
- `create-buffer(descriptor)`; `map-async(mode, …)` replaces `map-read`
- Docs: dropped alternate-runtime mentions from roadmap / out-of-scope lists

## 0.1.0-experimental

### Features (DoD complete)

- **P0:** `WasiWebGpuHost` + Dawn / CPU Host compute subset; Android instrumented vector-add
- **P1:** `abi-mvp` Guest → Wasmtime → same L2 (desktop + Android Bionic `libwasmtime4j.so`)
- **CM slice:** `experimental:webgpu-cm` WIT resources + `abi-cm` + desktop/Android CM Guest path

### Delivery harden (this release slice)

- Desktop CM natives install to `runtime-wasmtime/desktop-natives/` (no Gradle cache mutation)
- CM unit tests skip when patched desktop natives are absent (CI-friendly)
- GitHub Actions: JVM unit tests + `:android-demo:assembleDebug`
- Patch upstream notes: `patches/UPSTREAM.md`

### Explicitly out of scope

- Maven Central / GitHub Packages publishing
- Guest / wasi-gfx on-screen; full wasi:webgpu compliance
- Opening upstream PRs to tegmentum/wasmtime4j (notes only)
