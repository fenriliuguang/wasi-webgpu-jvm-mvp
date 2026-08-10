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
| Default (cube primary acceptance) | **sync-compat (locked)**: `requestAdapter` / `requestDevice` / `mapAsync` still wait via `CountDownLatch`; standard long-tail `*-async` may stub as result Err |
| True CM async (**A gate closed; not pursued in this repo**) | Archive [`archive-true-cm-async-dod.en.md`](../scheme/archive-true-cm-async-dod.en.md). Further true async → sister [`wasmtime-android-kt`](../../../wasmtime-android-kt); contract [`dual-runtime-track.en.md`](../scheme/dual-runtime-track.en.md) |
| WASI Preview3 | **Not** a close-out requirement here; current CM natives do not build `wasi-p3` |

Skew vs upstream (default path): WIT `async func` methods still return synchronously on sync-compat; gap rows marked ⚠️ sync-compat (not lifted after the true-cm-async A gate).

Default timeout is 30s; timeout throws `HostException.Backend` (experimental) or lifts to the matching result Err.
