# Errors & async notes (slice F)

[中文](errors-async.md) | **English**

## Error strategy

| Layer | Behavior |
|-------|----------|
| Handle errors | `HostException.InvalidHandle` |
| Out-of-subset capability | `HostException.Unsupported` |
| Dawn / validation failure | `HostException.Backend` / `Validation` |
| WIT `result<_, E>` | **Lifted on wasi:webgpu track** (below); experimental track still throw→trap |

Spec validation and Dawn validation may disagree: prefer recording Dawn messages and track them in the mapping deviation column.

## Dual-track

| Track | How Host failures reach the Guest |
|-------|-----------------------------------|
| `experimental:webgpu-cm` | Throw `HostException` → CM host callback **trap** (existing Guests unchanged) |
| `wasi:webgpu@0.3.0-rc.2` | Result-returning methods: `HostErrorMapping` + `WasiResultCodec` → `ComponentVal.err(record{kind,message})`; non-result methods still throw `Unsupported` stubs |

Code: `host-api` → `HostErrorMapping` / `WasiWebGpuError.kt`; `runtime-wasmtime` → `WasiResultCodec`; `abi-wasi` → `AbiWasiResults.BY_FUNC`.

## HostException → error-kind (heuristic)

| HostException | `gpu-error-kind` | Typical method-level kind |
|---------------|------------------|---------------------------|
| `Validation` / `InvalidHandle` | `validation-error` | `type-error` / `range-error` / pipeline `validation` |
| `Unsupported` / `Backend` | `internal-error` | `operation-error` / pipeline `internal`; Backend on `map-async` → `abort-error` |
| Single-kind methods | (same) | `unmap`→`abort-error`; `set-bind-group`→`range-error`; `create-query-set`→`type-error` |

Unwired wasi **result** stubs: map `Unsupported` into that method’s Err shape (Guest gets `result` Err, not a trap).

## Async

| Phase | Strategy |
|-------|----------|
| Current (still after engineering-handoff archive) | **sync-compat**: `requestAdapter` / `requestDevice` / `mapAsync` still wait via `CountDownLatch`; standard `*-async` methods may stub as result Err / later wiring stays sync |
| True CM async / WASI Preview3 | **Out of scope** now; memo [`true-cm-async-memo.en.md`](../scheme/true-cm-async-memo.en.md) (**not chartered**; handoff archived) |

Skew vs upstream: WIT `async func` methods still return synchronously here; gap rows marked ⚠️ sync-compat.

Default timeout is 30s; timeout throws `HostException.Backend` (experimental) or later lifts to the matching result Err.
