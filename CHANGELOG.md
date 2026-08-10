# Changelog

All notable changes to this experimental MVP are documented here.
Package / marketing claims remain **non-compliant** `wasi:webgpu` until a full standard world is wired.

## Unreleased

### Planning — true CM async chartered (docs only)

- Charter plan [`docs/scheme/true-cm-async.md`](docs/scheme/true-cm-async.md) / EN (slices A–E; tier A = CM async DoD; P3 optional spike)
- Memo [`true-cm-async-memo.md`](docs/scheme/true-cm-async-memo.md) marked **chartered** → points at formal plan
- README / scheme / errors-async ZH/EN: plan **chartered / frozen**; default path remains sync-compat until code slices land; **no** implementation in this docs drop
- Primary acceptance unchanged: experimental CM cube

### Docs sync — engineering-handoff archive consistency

- Align ZH/EN after A–C close-out: errors-async / true-cm-async-memo wording; engineering-handoff title + landing-order marked complete; archive-guest-descriptor-cube forward pointer to handoff archive

### Engineering handoff — slice C (optional perf notes) + phase close-out

- Refresh [`docs/perf/p1-boundary.md`](docs/perf/p1-boundary.md) / EN: drop deleted vector-add anchors; point at abi-mvp flat / CM cube paths; mark informal (no JMH / ratio gate)
- Non-gating smoke: `AbiMvpHostBindingsTest.boundaryNoteTimingSmoke` (abi-mvp flat vs direct L2 averages; prints only)
- Archive [`docs/scheme/archive-engineering-handoff-dod.md`](docs/scheme/archive-engineering-handoff-dod.md); README / scheme mark engineering-handoff **complete** (A–C)

### Engineering handoff — slice B (abi-mvp flat surface/render)

- Flat `wasi-webgpu-mvp` imports for surface configure / get-current-texture-view / present / unconfigure, triangle render-pipeline, begin-pass (clear + color/depth), set-pipeline / set-bind-group / set-vertex-buffer / draw / end, create-texture-2d / texture-create-view / write-texture
- `WasmtimeAbiLinker` registers the new helpers; `AbiMvpHostBindings` mirrors CM View↔Texture pairing
- Cpu Host: handle-only render-pipeline / render-pass stubs so desktop unit tests cover the chain (not a real WGSL rasterizer)
- Tests: multi-frame surface leak + render main-chain on Cpu; **primary acceptance remains CM cube** (no new instrumented cases)
- Docs: render-subset abi-mvp row ❌ → ⚠️ subset; handoff B DoD checked

### Engineering handoff — slice A (Maven publishability, no external release)

- Pin `groupId` `io.github.fenriliuguang.wasi.webgpu.experimental` / version `0.1.0-experimental` in `gradle.properties`
- Shared [`gradle/wasi-webgpu-publishing.gradle.kts`](gradle/wasi-webgpu-publishing.gradle.kts) + root `publishEngineeredToMavenLocal` for `host-api`, `host-webgpu`, `abi-mvp`, `abi-cm`, `abi-wasi` (local `~/.m2` self-check only; **no** remote repos)
- Docs: [`docs/maven-local.md`](docs/maven-local.md) / EN — engineered set, exclusions (demo / runtime-wasmtime natives / Guest / jniLibs), natives boundary; **not** a consumer-release guide
- POM `description` stays experimental / non-compliant / no-external-release; README / scheme / handoff DoD A checked

### Planning

- **Engineering handoff: Maven publishability (no external release) / abi-mvp render / optional perf** (2026-08-10): [`docs/scheme/engineering-handoff.md`](docs/scheme/engineering-handoff.md) — **A–C complete** → [`archive-engineering-handoff-dod.md`](docs/scheme/archive-engineering-handoff-dod.md). Remains **experimental**; **no** remote upload / “published / ready for consumers” claims. Primary acceptance remains experimental CM cube
- **True CM async (chartered 2026-08-10, docs only):** [`docs/scheme/true-cm-async.md`](docs/scheme/true-cm-async.md); memo history [`true-cm-async-memo.md`](docs/scheme/true-cm-async-memo.md); sync-compat remains default until code slices

### Docs sync — archive + consistency (CM cube baseline)

- Root README ZH/EN index deduped; Guest link → `guest/cube-cm`; scheme stage table adds handoff row
- Active docs aligned to sole device acceptance = CM cube `@0.8.0`: render/threading/compute-subset, android-wasmtime ZH/EN, wit + compute-cm READMEs
- Historical plan pages (compliant-world, blockers) keep phase wording; add supersession notes where they still said `@0.7.0` / two-wave / triangle as “current”

### guest-descriptor-cube slice D — resource-lifetime hardening (still not true WIT dtor)

- Chose frame-equivalent nets over `JniComponentLinker` rep-only overlay: AbiCm `dropRep` / `releaseLifetimeSafetyNets`; clear View↔Texture on present / next acquire / unconfigure; `WasmtimeCmCube.Session` calls nets after `runCube` / `runFrameLoop` / `close`
- Cpu fake surface for desktop lifetime tests; `AbiCmHostBindingsTest` ×60 acquire/present with no Texture/View growth
- Docs: UPSTREAM §4 decision; guest-descriptor-cube D checked + [`archive-guest-descriptor-cube-dod.md`](docs/scheme/archive-guest-descriptor-cube-dod.md); README / scheme / threading sync
- **Still not true WIT dtor** (wasmtime4j `resourceTable` miss); no upstream PR; Demo `releaseAllGpuObjects` may remain as Session handoff insurance

### guest-descriptor-cube slice C — wasi primary-path subset wiring

- `WasmtimeCmLinker`: after experimental imports, `registerWasiImports` wires ~33 `PRIMARY_PATH` wasi:webgpu methods onto the same `AbiCmHostBindings` / GpuHandle space; stubs skip wired names
- Shared `CmDescriptorParsers` for experimental + wasi descriptor parsing; `WasiResultCodec.ok()` / `ok(ComponentVal)` for result-returning success paths
- Unit: `WasiResultCodecTest` ok paths + `WasiPrimaryPathWiringTest` (no wasi Guest); primary acceptance remains experimental `cube-cm`
- Docs: dual-track + gap rows (`write-texture-with-copy` / render `set-bind-group` / `queue.submit` etc.); guest-descriptor-cube C checked
- Wiring ≠ compliance product; no wasi-track cube Guest obligation

### Cube-only acceptance baseline

- Remove vector-add / triangle Guest demos (`guest/vector-add*`, `guest/triangle-cm`), Demo UI buttons, L1 runtime entrypoints, and instrumented tests
- Demo / instrumented acceptance is **CM cube only** (`WasmtimeCmCubeInstrumentedTest`); `run-android-instrumented.ps1` no longer multi-wave
- `releaseFrameResources` sweeps encoders only (not Guest-owned Texture/View) so cube depth/albedo survive across frames; uniform buffer size 256 for Dawn alignment

### guest-descriptor-cube slice B — rotating textured cube + depth / write-texture

- `experimental:webgpu-cm` **0.7.0 → 0.8.0**: `queue.write-texture`, render-pass `set-bind-group`, `depth-stencil-state` / pass attachment, `world cube`
- L2 + Dawn + Cpu stubs; AbiCm / WasmtimeCmLinker parsers; fix `GpuTextureFormat.RGBA8_UNORM` → `0x16` (alpha05)
- New `guest/cube-cm/` (CC0 procedural checkerboard + ATTRIBUTION); migrate `triangle-cm` to standard descriptors; bump `vector-add-cm` / triangle wasm to `@0.8.0`
- Runtime `WasmtimeCmCube` + Android Demo `CubeCmOneShot` button; instrumented `WasmtimeCmCubeInstrumentedTest`; `run-android-instrumented.ps1` wave3 (separate process from triangle)
- Docs: render-subset + guest-descriptor-cube B checked

### guest-descriptor-cube slice A — Android CM natives unlock

- Rebuilt `runtime-wasmtime/android-natives/jniLibs` with recursive `cm-resources` (+ android) patches via `scripts/build-wasmtime4j-android.ps1`
- Desktop CM natives: `build-wasmtime4j-desktop-cm.ps1` honors `CARGO_TARGET_DIR`; Android script fails hard on cargo ndk errors; Windows cross-compile defaults `CARGO_PROFILE_RELEASE_OPT_LEVEL=0` (rustc AV at opt≥1)
- Smoke: `vector-add-cm` migrates to nested standard descriptors (`create-bind-group` / pipeline-layout / `create-compute-pipeline` / `queue.submit`); desktop `WasmtimeCmVectorAddTest` green
- Docs: [`guest-descriptor-cube.md`](docs/scheme/guest-descriptor-cube.md) A checked; [`android-wasmtime.md`](docs/android-wasmtime.md) §6

### Planning (prior)

- **Guest standard descriptors on device + rotating textured cube** (2026-08-09→10): [`docs/scheme/guest-descriptor-cube.md`](docs/scheme/guest-descriptor-cube.md) / archive [`archive-guest-descriptor-cube-dod.md`](docs/scheme/archive-guest-descriptor-cube-dod.md) — slices A–D complete. Handed off Maven / `abi-mvp` render / perf → [`engineering-handoff.md`](docs/scheme/engineering-handoff.md)

### Docs sync (pre-cube; superseded)

- Cross-doc consistency pass at compliant-world close-out: primary acceptance was experimental `@0.7.0`; matrix close-out ≠ compliance marketing. **Superseded** by guest-descriptor-cube `@0.8.0` + CM-cube-only acceptance + 2026-08-10 docs sync above

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

### Planning (historical)

- **Compliant wasi:webgpu world (no gfx) was docs-locked** (2026-08-09) then **completed A–G** the same day: [`docs/scheme/compliant-world.md`](docs/scheme/compliant-world.md) / [EN](docs/scheme/compliant-world.en.md); archive [`archive-compliant-world-dod.md`](docs/scheme/archive-compliant-world-dod.md). **Current status:** next phase **docs-locked** as guest-descriptor-cube (see ### Planning above); **no** wasi-gfx / Maven / `abi-mvp` render / perf; still **no** compliance-product marketing; primary acceptance remains `experimental:webgpu-cm@0.7.0`
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
