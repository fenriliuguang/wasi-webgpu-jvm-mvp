# Semantic hardening & engineering debt (semantic-hardening) — complete

[中文](semantic-hardening.md) | **English**

> **Status: complete (2026-08-09).** DoD archive: [`archive-semantic-hardening-dod.en.md`](archive-semantic-hardening-dod.en.md).  
> Continues: device stability D1–D6 closed ([`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) ZH).  
> Packages: semantic hardening (A+B) → engineering debt (C+D) → Demo deepening (E).

## One-liner

Without wasi-gfx or a full compliant world, push the CM semantic surface toward standard descriptors and resource destructors, clear upstream / instrumented peripheral debt, and accept with a richer Guest demo.

```text
A WIT records (render / pipeline, …)
  → B Guest/Host resource destructor wiring
  → C Upstream gap notes in-repo (no upstream PRs)
  → D D7 Studio instrumented vs script alignment
  → E Richer Guest demo (still experimental WIT)
```

## Locked decisions

| Question | Decision |
|----------|----------|
| Phase scope | Lock **A+B+C+D+E** as the current phase; **F/G/H/I** explicitly out; **J** (perf) optional, non-blocking |
| Main order | **A** (records) then **B** (destructors); **C/D** may interleave; **E** after A (and B if needed) |
| **E choice** | **Vertex buffer** (Guest `create-buffer` + `write-buffer` + `set-vertex-buffer` + `@location(0)`); not per-frame color / multi-draw as primary acceptance |
| WIT version | **Bump** `experimental:webgpu-cm` when records change import shape (expect `0.3.0` → `0.4.0`); pure additive tweaks may stay unbumped (same convention as prior slice) |
| Specialized APIs | Keep helpers such as `create-render-pipeline-triangle` or mark deprecated; new path prefers records/descriptors |
| Destructor policy | WIT resource drop → Host `drop*`; reduce **semantic** reliance on `releaseFrameResources` / `releaseAllGpuObjects` (Demo may keep settle as belt-and-suspenders) |
| Upstream (C) | In-repo notes + overlay strategy in [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md); **do not** open upstream issues/PRs |
| Instrumented (D) | Close D7: align Studio `*InstrumentedTest` with `scripts/run-android-instrumented.ps1`, or document a single recommended entry |
| Acceptance | Desktop unit tests (with natives) + Android instrumented green + Demo taps must not regress D1–D6; docs / CHANGELOG per sub-slice |

## Sub-slices & DoD

### A — WIT records expansion

- [x] `experimental:webgpu-cm` **0.4.0**: `vertex-attribute` / `vertex-buffer-layout` + `set-vertex-buffer` + `create-render-pipeline-triangle-buffers` (follow buffer `0.2.0` precedent)
- [x] L2 + Dawn + Cpu stub + `abi-cm` + WasmtimeCmLinker wired; keep old `create-render-pipeline-triangle`
- [x] `docs/mapping/render-subset` updated; Guest wasm rebuilt for `@0.4.0` (still uses old triangle helper)
- [x] Guest switches to buffers API (**E** wired); instrumented device re-check OK (2026-08-08, V2458A, two-wave script)

### B — Guest resource destructor wiring

- [x] Frame-equivalent drop: AbiCm tracks View↔Texture pairs; `tryDrop` on present / next acquire (Texture is not a WIT resource)
- [x] Idempotent Host `tryDrop`; `HandleTable.tryDrop`
- [x] Docs: paired release vs still-needed `releaseFrameResources` (encoder orphans) / Demo `releaseAllGpuObjects` (Surface/Device; true WIT dtor still blocked by wasmtime4j `resourceTable`)
- [x] Instrumented CM triangle×N (shared Session + `releaseAllGpuObjects`) device re-check (D2/D3/D6); Demo hand-tap CM×N + L2 resume still nice-to-have
- [x] (Optional note) wasmtime4j destructor miss → `host.drop(rep)` documented in UPSTREAM §4 (notes only; no upstream submit)

### C — Upstream gap notes (no upstream PRs)

- [x] `ConcurrentCallCodec` unsigned-u64 (plus Validation / destructor / native patches) recorded in-repo: [`patches/UPSTREAM.en.md`](../../patches/UPSTREAM.en.md)
- [x] Overlay / filtered-jar strategy documented and self-contained; **do not** open upstream issues/PRs for this project

### D — D7 instrumented peripheral

- [x] **Single recommended entry**: `scripts/run-android-instrumented.ps1` (two `am instrument` waves + `force-stop` between; CM vector-add and CM triangle must not back-to-back in one process)
- [x] Blockers D7 marked “documented bypass formalized”; Studio / `:connectedDebugAndroidTest` may still UTP `Process crashed` (see `docs/android-wasmtime` §7)

### E — Richer Guest demo (locked: vertex buffer)

- [x] Guest uploads float32x2 vertices (`VERTEX \| COPY_DST`), `set-vertex-buffer(0, …)` then `draw(3)`; shader reads `@location(0)`
- [x] Uses `create-render-pipeline-triangle-buffers` + records; Host still keeps old `create-render-pipeline-triangle` (contrast)
- [x] Instrumented device re-check (2026-08-08, V2458A); desktop CpuHost still Unsupported / skip; Demo hand-tap nice-to-have

## Out of scope (this phase)

| ID | Item |
|----|------|
| F | wasi-gfx / canvas abstraction |
| G | Full compliant `wasi:webgpu` world |
| H | Maven Central / publishing |
| I | `abi-mvp` flat render imports |
| — | Multi-window, full MSAA/depth (unless minimally required by A records) |

## Sequence

1. **A** WIT records + L2/abi-cm/Guest wiring + mapping docs  
2. **B** destructor → Host drop; stability regression  
3. **C** / **D** engineering debt (may interleave with 1–2)  
4. **E** Guest demo deepening + instrumented / manual acceptance  
5. Docs wrap-up: check DoD here → [`archive-semantic-hardening-dod.en.md`](archive-semantic-hardening-dod.en.md); root README / scheme / CHANGELOG

## Links

- Root README: [`README.en.md`](../../README.en.md)  
- Scheme index: [`docs/scheme/README.en.md`](README.en.md)  
- Prior blockers: [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) (ZH)  
- Render mapping: [`docs/mapping/render-subset.en.md`](../mapping/render-subset.en.md)  
- Upstream brief: [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
- WIT: [`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)  
