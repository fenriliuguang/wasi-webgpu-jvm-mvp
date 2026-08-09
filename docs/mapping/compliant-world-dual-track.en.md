# Compliant-world dual-track package identity

[中文](compliant-world-dual-track.md) | **English**

> **Status:** guest-descriptor-cube slice C (2026-08-10) — Linker coexistence; standard-package **primary-path subset wired**, long-tail still stubs; **primary acceptance / Guests remain experimental**.  
> Plan: [`compliant-world.en.md`](../scheme/compliant-world.en.md) · Next: [`guest-descriptor-cube.en.md`](../scheme/guest-descriptor-cube.en.md) · PIN: [`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md) · Archive: [`archive-compliant-world-dod.en.md`](../scheme/archive-compliant-world-dod.en.md)

## One-liner

Register both CM import tracks on the same `WasmtimeCmLinker`: **experimental** (current Guests and primary acceptance) and **wasi:webgpu** (standard package; slice C wires existing L2 primary path onto Host, rest stubs). Dual-track is transitional; wiring ≠ Guest migration, and **not** compliance-product marketing.

## Package identity

| Track | Import interface | Module | Guest status |
|-------|------------------|--------|--------------|
| experimental (primary) | `experimental:webgpu-cm/host@0.8.0` | [`abi-cm`](../../abi-cm/) `AbiCm` | **cube-cm** stays here (standard descriptors; guest-descriptor-cube B) |
| Standard (dual-track) | `wasi:webgpu/webgpu@0.3.0-rc.2` | [`abi-wasi`](../../abi-wasi/) `AbiWasi` | no Guest yet; resources registered; **PRIMARY_PATH** (~33) → `AbiCmHostBindings`; unwired results → `ComponentVal.err`; others **Unsupported**; **not** the primary acceptance path |

## Linker behavior

[`WasmtimeCmLinker`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmLinker.kt) `instantiate`:

1. `registerExperimentalResources` (`AbiCm.Resource.ALL`)
2. `registerWasiResources` (`AbiWasi.Resource.ALL`, 33)
3. `registerExperimentalImports` (existing L2 wiring)
4. `registerWasiImports` (slice C: adapter/device/queue/buffer/compute+render+texture primary path → same `AbiCmHostBindings`)
5. `registerWasiImportStubs` (**skips** already-wired names; other results → `ComponentVal.err`; non-results → `HostException.Unsupported`)

Old Guests only resolve experimental paths and are unaffected by wasi registration. Wiring ≠ compliance product; **no** obligation for a wasi-track cube Guest.

## Resource name map (excerpt)

| experimental (`AbiCm`) | wasi (`AbiWasi`) | Notes |
|------------------------|------------------|-------|
| `adapter` | `gpu-adapter` | |
| `device` | `gpu-device` | |
| `queue` | `gpu-queue` | |
| `buffer` | `gpu-buffer` | |
| `shader-module` | `gpu-shader-module` | |
| `bind-group-layout` | `gpu-bind-group-layout` | |
| `bind-group` | `gpu-bind-group` | |
| `compute-pipeline` | `gpu-compute-pipeline` | |
| `render-pipeline` | `gpu-render-pipeline` | |
| `command-encoder` | `gpu-command-encoder` | |
| `compute-pass-encoder` | `gpu-compute-pass-encoder` | |
| `render-pass-encoder` | `gpu-render-pass-encoder` | |
| `command-buffer` | `gpu-command-buffer` | |
| `texture-view` | `gpu-texture-view` | |
| `surface` | `gpu-canvas-context` (approx.) | Host inject; **not** wasi-gfx |
| (none) | `gpu` | standard root resource |
| (none) | `gpu-texture` / `gpu-sampler` / … | slices D/G |

Full method-level gaps: [`compliant-world-gap.en.md`](compliant-world-gap.en.md).

## Regen

```text
python scripts/gen-wasi-webgpu-inventory.py
python scripts/gen-abi-wasi-constants.py
python scripts/gen-compliant-world-gap.py
```

## Links

- Gap matrix: [`compliant-world-gap.en.md`](compliant-world-gap.en.md)  
- wit-lock: [`wit/README.en.md`](../../wit/README.en.md)  
