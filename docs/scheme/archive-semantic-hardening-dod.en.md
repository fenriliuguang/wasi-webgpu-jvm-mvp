# Semantic hardening & engineering debt DoD archive (complete)

[中文](archive-semantic-hardening-dod.md) | **English**

> Completed acceptance checklist moved out of the root README / plan page.  
> Original plan: [`semantic-hardening.en.md`](semantic-hardening.en.md). Continues device regression: [`demo-cm-stability-blockers.md`](demo-cm-stability-blockers.md) (ZH).

Archive covers work through: `66bee92` (lock A–E) → `904c36e` (A / 0.4.0 records) → `61da76d` (E vertex buffer) → `1c53c14` (B frame pair drop) → `71ecc18` / `c22c408` (C upstream notes) → `f477c2d` (D7 two-wave script) + this docs wrap-up (2026-08-09).

## DoD

### A — WIT records

- [x] `experimental:webgpu-cm` **0.4.0**: `vertex-attribute` / `vertex-buffer-layout` + `set-vertex-buffer` + `create-render-pipeline-triangle-buffers`
- [x] L2 + Dawn + Cpu stub + `abi-cm` + WasmtimeCmLinker; keep old `create-render-pipeline-triangle`
- [x] `docs/mapping/render-subset`; Guest wasm `@0.4.0`

### B — Resource destructors (frame-equivalent)

- [x] AbiCm View↔Texture pairs; `tryDrop` on present / next acquire
- [x] Idempotent Host / `HandleTable.tryDrop`; true WIT dtor still blocked by wasmtime4j `resourceTable` (see UPSTREAM)
- [x] Instrumented CM triangle×N + `releaseAllGpuObjects` device re-check (D2/D3/D6)

### C — Upstream gap notes

- [x] [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md): ConcurrentCallCodec u64, Validation, destructor, native patches, overlay; **do not** open upstream issues/PRs

### D — D7 instrumented peripheral

- [x] Single recommended entry: `scripts/run-android-instrumented.ps1` (two waves + force-stop between)
- [x] Blockers D7 “documented bypass formalized”; Studio UTP may still Process crashed

### E — Guest vertex buffer

- [x] float32x2 + `create-render-pipeline-triangle-buffers` + `set-vertex-buffer` + `@location(0)`
- [x] Instrumented device re-check (2026-08-08, V2458A); desktop CpuHost Unsupported / skip

## Key deliverables

- WIT / Guest: `experimental:webgpu-cm@0.4.0`; `triangle_cm.wasm` on buffers path
- L2 / abi-cm: records wiring + frame View↔Texture `tryDrop`
- Upstream: long-term in-repo notes in [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)
- Instrumented: two-wave `am instrument` script as sole recommended entry
- Mapping: [`docs/mapping/render-subset.en.md`](../mapping/render-subset.en.md)
