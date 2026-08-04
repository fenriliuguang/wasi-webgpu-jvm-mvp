# guest/vector-add

[中文](README.md) | **English**

Experimental **abi-mvp** Guest (core wasm imports), **not** standard Component Model / compliant `wasi:webgpu`.

## Rebuild

```bash
wasm-tools parse guest/vector-add/vector_add.wat -o guest/vector-add/vector_add.wasm
wasm-tools validate guest/vector-add/vector_add.wasm
```

Prebuilt `vector_add.wasm` is committed. After editing `.wat`, re-parse and commit both.

## Exports

- `run_vector_add(ptr_a, ptr_b, ptr_out, n) -> i32`: `n` is `f32` element count; buffers are little-endian.
- `memory`: linear memory; WGSL and the `"main"` entry name are embedded in the data section (must match `VectorAddScenario.SHADER` bytes).
