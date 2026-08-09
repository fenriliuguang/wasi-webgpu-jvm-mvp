# wit-lock

[中文](README.md) | **English**

Pin the upstream subset so the Host does not drift with WIT tip.

| Field | Value |
|-------|-------|
| Package | `wasi:webgpu/webgpu` |
| Version | `0.3.0-rc.2` |
| Upstream | https://github.com/WebAssembly/wasi-webgpu |
| Tag | `v0.3.0-rc.2` |
| Imports | https://github.com/WebAssembly/wasi-webgpu/blob/v0.3.0-rc.2/wit/imports.wit |
| **Standard vendor (slice A)** | [`deps/wasi-webgpu/`](deps/wasi-webgpu/) — [`PIN.md`](deps/wasi-webgpu/PIN.md) · `webgpu.wit` · `imports.wit` · [`_inventory.json`](deps/wasi-webgpu/_inventory.json) |
| L2 scope in this repo | Still **compute + minimal surface/render**; full coverage tracked in the gap matrix |
| P1 Guest ABI | **abi-mvp** (`wasi-webgpu-mvp` core imports, **not** CM / not compliant) |
| CM slice (dual-track) | [`compute-cm/`](compute-cm/) — `experimental:webgpu-cm@0.5.0` (**still not** compliant); coexists with the standard package until Guests migrate |
| Standard ABI (slice B) | [`abi-wasi`](../abi-wasi/) `AbiWasi` — import `wasi:webgpu/webgpu@0.3.0-rc.2`; Linker registers resources + Unsupported stubs; see [`compliant-world-dual-track.en.md`](../docs/mapping/compliant-world-dual-track.en.md) |
| Phase plan | [`docs/scheme/compliant-world.en.md`](../docs/scheme/compliant-world.en.md) · gap [`docs/mapping/compliant-world-gap.en.md`](../docs/mapping/compliant-world-gap.en.md) |

## Dual-track

- **experimental**: current Guests (vector-add-cm / triangle-cm) and `abi-cm` / Linker stay on `experimental:webgpu-cm@0.5.0`.  
- **Standard package**: vendored and pinned; Linker wiring is slice **B+**; do **not** advertise compliance before matrix close-out.  
- **Upgrade**: update [`deps/wasi-webgpu/PIN.md`](deps/wasi-webgpu/PIN.md) → `python scripts/gen-wasi-webgpu-inventory.py` → `python scripts/gen-compliant-world-gap.py` → then Host / ABI.

P0/P1 do **not** generate a full wit-bindgen host. The CM slice still pins this repo’s `wit/compute-cm/world.wit`.
