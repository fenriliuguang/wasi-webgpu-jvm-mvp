# guest/vector-add-cm

[中文](README.md) | **English**

Experimental **Component Model** Guest for vector-add.

- Imports `experimental:webgpu-cm/host@0.7.0` (WIT resources + methods; pinned in `wit/`)
- **Not** compliant `wasi:webgpu`
- Export: `run-vector-add(a, b) -> result<list<f32>, string>`
- **guest-descriptor-cube A:** standard descriptors with nested borrow (`create-bind-group` / `pipeline-layout` / `create-compute-pipeline` / `queue.submit(list)`); needs recursive `cm-resources`–patched natives

## Rebuild

Requires Rust (`wasm32-unknown-unknown`) + `wasm-tools` + wit-bindgen **0.55.0**.

```powershell
./scripts/build-vector-add-cm.ps1
```

Or manually:

```bash
cd guest/vector-add-cm
cargo build --target wasm32-unknown-unknown --release
wasm-tools component new target/wasm32-unknown-unknown/release/vector_add_cm.wasm \
  -o vector_add_cm.wasm
```

Committed `vector_add_cm.wasm` is what desktop tests and Android assets load
(`android-demo` syncs it to `assets/guest/vector_add_cm.wasm`).
