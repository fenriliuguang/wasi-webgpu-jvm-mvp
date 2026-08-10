# Boundary cost notes (informal)

[中文](p1-boundary.md) | **English**

> experimental · **not a formal benchmark** (no JMH / no speed-ratio / no FPS gate)  
> Current reproducible paths (vector-add instrumented tests and Guest demos removed):
>
> - Desktop: abi-mvp flat surface/render → `CpuWasiWebGpuHost` (see timing smoke below)  
> - Desktop CM: `:runtime-wasmtime:test` (`WasmtimeCmCubeTest`; skips without `desktop-natives`)  
> - Android: `WasmtimeCmCubeInstrumentedTest` (CM cube → Dawn; device acceptance baseline)

## How to measure (desktop smoke)

```bash
./gradlew :abi-mvp:test --tests "*.AbiMvpHostBindingsTest.boundaryNoteTimingSmoke"
```

A few iterations print averages for:

1. **abi-mvp flat path**: `AbiMvpHostBindings` → the same `CpuWasiWebGpuHost` (surface configure / get-view / present + triangle pass subset)  
2. **Pure Kotlin→L2**: equivalent L2 calls on the same Cpu Host  

**Not** a formal benchmark: includes Host create and handle churn; no warmup matrix / JMH; the only pass criterion is that both paths complete — **no** timing-ratio assertion.

For the CM cube device path (still informal perf), use:

```powershell
./scripts/run-android-instrumented.ps1
```

## Qualitative conclusions

| Cost block | Notes |
|------------|-------|
| GPU / shaders | Desktop Cpu Host is handle-level stubs only; Dawn cost shows on the Android CM cube instrumented path |
| Import boundary | Each abi-mvp flat import is one host forward; vs direct L2 this is usually a thin wrapper |
| CM / Wasmtime | Engine create, module compile, instantiate, CM host-callback round-trips (desktop CM smoke / device instrumented) |
| Memory copies | `queue_write_buffer` / `queue_write_texture` / mapped-range copy via Guest or Host buffers |
| Android native | Bionic `libwasmtime4j.so` (`runtime-wasmtime/android-natives`); isolated from desktop Maven / `desktop-natives` |

Relative to “pure Kotlin → L2”: any Wasmtime/CM Guest path is necessarily slower. These notes support **semantic / engineering boundary understanding**, not a speed-ratio target.

## Historical anchors (obsolete)

Removed with vector-add Guest / instrumented tests — do not cite:

- `WasmtimeVectorAddTest.boundaryNoteTimingSmoke`
- `WasmtimeVectorAddInstrumentedTest`

## Follow-ups (non-blocking)

- Reuse Engine/Module; measure steady-state Guest exports if an abi-mvp Guest returns  
- Same-resolution comparison with Android CM cube instrumented runs (still informal)  
- Formal perf contract / JMH: **out of scope** (engineering-handoff exclusions)  
