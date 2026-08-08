# Semantic hardening & engineering debt (semantic-hardening) — current phase

[中文](semantic-hardening.md) | **English**

> **Status: in progress (locked as current phase, 2026-08-08).**  
> Continues: device stability D1–D6 closed ([`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) ZH).  
> Packages: semantic hardening (A+B) → engineering debt (C+D) → Demo deepening (E).

## One-liner

Without wasi-gfx or a full compliant world, push the CM semantic surface toward standard descriptors and resource destructors, clear upstream / instrumented peripheral debt, and accept with a richer Guest demo.

```text
A WIT records (render / pipeline, …)
  → B Guest/Host resource destructor wiring
  → C Upstream wasmtime4j patch contribution prep
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
| Upstream (C) | Brief in [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md); phase goal is submittable notes + keep in-repo overlay; upstream PR merge **not** required |
| Instrumented (D) | Close D7: align Studio `*InstrumentedTest` with `scripts/run-android-instrumented.ps1`, or document a single recommended entry |
| Acceptance | Desktop unit tests (with natives) + Android instrumented green + Demo taps must not regress D1–D6; docs / CHANGELOG per sub-slice |

## Sub-slices & DoD

### A — WIT records expansion

- [x] `experimental:webgpu-cm` **0.4.0**: `vertex-attribute` / `vertex-buffer-layout` + `set-vertex-buffer` + `create-render-pipeline-triangle-buffers` (follow buffer `0.2.0` precedent)
- [x] L2 + Dawn + Cpu stub + `abi-cm` + WasmtimeCmLinker wired; keep old `create-render-pipeline-triangle`
- [x] `docs/mapping/render-subset` updated; Guest wasm rebuilt for `@0.4.0` (still uses old triangle helper)
- [x] Guest switches to buffers API (**E** wired); instrumented device re-check still pending

### B — Guest resource destructor wiring

- [x] Frame-equivalent drop: AbiCm tracks View↔Texture pairs; `tryDrop` on present / next acquire (Texture is not a WIT resource)
- [x] Idempotent Host `tryDrop`; `HandleTable.tryDrop`
- [x] Docs: paired release vs still-needed `releaseFrameResources` (encoder orphans) / Demo `releaseAllGpuObjects` (Surface/Device; true WIT dtor still blocked by wasmtime4j `resourceTable`)
- [ ] Demo CM×N + L2 resume device re-check (blockers D2/D3/D5/D6)
- [ ] (Optional) wasmtime4j destructor miss → `host.drop(rep)` patch — may fold into C

### C — Upstream contribution prep

- [ ] Package `ConcurrentCallCodec` unsigned-u64 (and related UPSTREAM rows) as an outward-facing brief / patch note
- [ ] Keep or smoothly migrate in-repo overlay / filtered-jar strategy; refresh `patches/UPSTREAM.md` status

### D — D7 instrumented peripheral

- [ ] Studio `*InstrumentedTest` matches script behavior, **or** README / `docs/android-wasmtime` names one recommended entry with a known-failure link for Studio
- [ ] Mark blockers D7 closed or “documented bypass formalized”

### E — Richer Guest demo (locked: vertex buffer)

- [x] Guest uploads float32x2 vertices (`VERTEX \| COPY_DST`), `set-vertex-buffer(0, …)` then `draw(3)`; shader reads `@location(0)`
- [x] Uses `create-render-pipeline-triangle-buffers` + records; Host still keeps old `create-render-pipeline-triangle` (contrast)
- [ ] Demo / instrumented device re-check (needs device); desktop CpuHost still Unsupported / skip

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
5. Docs wrap-up: check DoD here → archive page; root README / scheme / CHANGELOG

## Links

- Root README: [`README.en.md`](../../README.en.md)  
- Scheme index: [`docs/scheme/README.en.md`](README.en.md)  
- Prior blockers: [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) (ZH)  
- Render mapping: [`docs/mapping/render-subset.en.md`](../mapping/render-subset.en.md)  
- Upstream brief: [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
- WIT: [`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit)  
