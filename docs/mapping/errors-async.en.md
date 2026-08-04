# Errors & async notes (P0)

[中文](errors-async.md) | **English**

## Error strategy

| Layer | P0 behavior |
|-------|-------------|
| Handle errors | `HostException.InvalidHandle` |
| Out-of-subset capability | `HostException.Unsupported` |
| Dawn / validation failure | `HostException.Backend` / `Validation` |
| WIT `result<_, E>` | **Not lifted**; map in P1+ |

Spec validation and Dawn validation may disagree: prefer recording Dawn messages and track them in the mapping deviation column.

## Async

| Phase | Strategy |
|-------|----------|
| P0 | Sync MVP: `requestAdapter` / `requestDevice` / `mapAsync` wait internally via `CountDownLatch` |
| P1 | Keep sync or expose futures depending on Runtime capability |
| WASI 0.3 / CM async | Interface reserved; not implemented in P0 |

Default timeout is 30s; timeout throws `HostException.Backend`.
