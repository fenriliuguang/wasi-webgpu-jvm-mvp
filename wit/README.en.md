# wit-lock

[中文](README.md) | **English**

Pin the upstream subset so the Host does not drift with WIT tip.

| Field | Value |
|-------|-------|
| Package | `wasi:webgpu/webgpu` |
| Version | `0.3.0-rc.2` |
| Upstream | https://github.com/WebAssembly/wasi-webgpu |
| Imports summary | https://github.com/WebAssembly/wasi-webgpu/blob/main/imports.md |
| L2 scope in this repo | **compute subset** (see [`docs/mapping/compute-subset.en.md`](../docs/mapping/compute-subset.en.md)) |
| P1 Guest ABI | **abi-mvp** (`wasi-webgpu-mvp` core imports, **not** CM / not compliant) |
| CM slice | [`compute-cm/`](compute-cm/) — `experimental:webgpu-cm@0.3.0` (typed lists/strings + WIT resources + buffer-descriptor + surface/render; **still not** compliant wasi:webgpu) |

P0/P1 do **not** vendor the full upstream WIT, and do not generate a full wit-bindgen host.  
The CM slice pins this repo’s `wit/compute-cm/world.wit` (method names lean toward wasi:webgpu compute).  
When upgrading: update this file and the mapping tables first, then change `WasiWebGpuHost` / abi-mvp / abi-cm.
