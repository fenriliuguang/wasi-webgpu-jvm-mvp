# Compliant-world dual-track package identity

[中文](compliant-world-dual-track.md) | **English**

> **Status:** post B–G close-out dual-track (2026-08-09) — Linker coexistence; standard package mostly stubs; **primary acceptance / Guests remain experimental**.  
> Plan: [`compliant-world.en.md`](../scheme/compliant-world.en.md) · PIN: [`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md) · Archive: [`archive-compliant-world-dod.en.md`](../scheme/archive-compliant-world-dod.en.md)

## One-liner

Register both CM import tracks on the same `WasmtimeCmLinker`: **experimental** (current Guests and primary acceptance) and **wasi:webgpu** (standard skeleton / stubs). Dual-track is transitional; matrix close-out means method-level coverage is complete — **not** that Guests moved to the standard package, and **not** that compliance may be advertised.

## Package identity

| Track | Import interface | Module | Guest status |
|-------|------------------|--------|--------------|
| experimental (primary) | `experimental:webgpu-cm/host@0.8.0` | [`abi-cm`](../../abi-cm/) `AbiCm` | vector-add-cm / triangle-cm / cube-cm **stay here** (standard descriptors; guest-descriptor-cube B) |
| Standard (dual-track stub) | `wasi:webgpu/webgpu@0.3.0-rc.2` | [`abi-wasi`](../../abi-wasi/) `AbiWasi` | no Guest yet; resources registered; **result methods** stub → `ComponentVal.err` (slice F); other funcs throw **Unsupported**; **not yet** the primary acceptance path |

## Linker behavior

[`WasmtimeCmLinker`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmLinker.kt) `instantiate`:

1. `registerExperimentalResources` (`AbiCm.Resource.ALL`)
2. `registerWasiResources` (`AbiWasi.Resource.ALL`, 33)
3. `registerExperimentalImports` (existing L2 wiring)
4. `registerWasiImportStubs` (result methods → `ComponentVal.err`; others → `HostException.Unsupported`)

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
