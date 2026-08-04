# guest/vector-add

**中文** | [English](README.en.md)

Experimental **abi-mvp** Guest（core wasm imports），**不是**标准 Component Model / 合规 `wasi:webgpu`。

## 重建

```bash
wasm-tools parse guest/vector-add/vector_add.wat -o guest/vector-add/vector_add.wasm
wasm-tools validate guest/vector-add/vector_add.wasm
```

仓库已提交预编译 `vector_add.wasm`。修改 `.wat` 后请重新 parse 并一并提交。

## 导出

- `run_vector_add(ptr_a, ptr_b, ptr_out, n) -> i32`：`n` 为 `f32` 元素个数；缓冲区为 little-endian。
- `memory`：线性内存；WGSL 与 `"main"` 入口名嵌入 data section（须与 `VectorAddScenario.SHADER` 字节一致）。
