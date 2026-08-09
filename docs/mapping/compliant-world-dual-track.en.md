# Compliant-world dual-track package identity (slice B)

[中文](compliant-world-dual-track.md) | **English**

> **Status:** slice B (2026-08-09) — Linker coexistence; standard-package funcs mostly stubs.  
> Plan: [`compliant-world.en.md`](../scheme/compliant-world.en.md) · PIN: [`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)

## One-liner

Register both CM import tracks on the same `WasmtimeCmLinker`: **experimental** (current Guests) and **wasi:webgpu** (standard skeleton). Dual-track is transitional; after matrix close-out the standard package is the primary acceptance path.

## Package identity

| Track | Import interface | Module | Guest status |
|-------|------------------|--------|--------------|
| experimental | `experimental:webgpu-cm/host@0.4.0` | [`abi-cm`](../../abi-cm/) `AbiCm` | vector-add-cm / triangle-cm **stay here** |
| Standard | `wasi:webgpu/webgpu@0.3.0-rc.2` | [`abi-wasi`](../../abi-wasi/) `AbiWasi` | no Guest yet; resources registered, funcs **Unsupported stubs** (wire in C+) |

## Linker behavior

[`WasmtimeCmLinker`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmLinker.kt) `instantiate`:

1. `registerExperimentalResources` (`AbiCm.Resource.ALL`)
2. `registerWasiResources` (`AbiWasi.Resource.ALL`, 33)
3. `registerExperimentalImports` (existing L2 wiring)
4. `registerWasiImportStubs` (`AbiWasi.Func.ALL` → `HostException.Unsupported`)

Old Guests only resolve experimental paths and are unaffected by wasi stubs.

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
