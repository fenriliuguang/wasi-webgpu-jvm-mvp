# P1 boundary cost notes (draft)

[中文](p1-boundary.md) | **English**

> experimental · Guest → Wasmtime (L1) → abi-mvp → `WasiWebGpuHost` (L2) → `CpuWasiWebGpuHost`  
> Baseline: pure Kotlin calling L2 directly (same CPU Host)  
> Android: same Guest / abi-mvp path → `DawnWasiWebGpuHost` (see `WasmtimeVectorAddInstrumentedTest`)

## How to measure

```bash
./gradlew :runtime-wasmtime:test --tests "*.WasmtimeVectorAddTest.boundaryNoteTimingSmoke"
```

The test runs a few iterations of `n=256` vector-add and prints average time for the guest path vs direct Kotlin→L2.  
**Not** a formal benchmark: includes Wasmtime engine create / module compile / instantiate; no warmup matrix or JMH.

## Qualitative conclusions (P1)

| Cost block | Notes |
|------------|-------|
| GPU / shaders | Desktop uses CPU Host simulation; Android Dawn path is instrumented tests |
| Import boundary | One JNI/host callback per abi-mvp import; vector-add call count is O(constant) |
| Memory copies | `queue_write_buffer` / `buffer_get_mapped_range` copy through Guest linear memory |
| Android native | Bionic `libwasmtime4j.so` (`runtime-wasmtime/android-natives`); isolated from desktop Maven natives |

Relative to “pure Kotlin → L2”: the Guest path is necessarily slower (engine startup + boundary round-trips). P1 targets a **semantic closed loop**, not a speed ratio.

## Follow-ups (do not block P1)

- Reuse Engine/Module; measure only steady-state `run_vector_add`
- Same-input comparison with Android Dawn instrumented tests (already have `WasmtimeVectorAddInstrumentedTest`)
- Write batch-copy conventions (fewer, larger transfers) into a formal perf contract
