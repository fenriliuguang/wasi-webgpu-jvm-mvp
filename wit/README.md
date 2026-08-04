# wit-lock

**中文** | [English](README.en.md)

钉死上游子集，避免 Host 随 WIT tip 漂移。

| 字段 | 值 |
|------|----|
| 包 | `wasi:webgpu/webgpu` |
| 版本 | `0.3.0-rc.2` |
| 上游 | https://github.com/WebAssembly/wasi-webgpu |
| imports 摘要 | https://github.com/WebAssembly/wasi-webgpu/blob/main/imports.md |
| 本仓 L2 范围 | **compute 子集**（见 [`docs/mapping/compute-subset.md`](../docs/mapping/compute-subset.md) / [EN](../docs/mapping/compute-subset.en.md)） |
| P1 Guest ABI | **abi-mvp**（`wasi-webgpu-mvp` core imports，**非** CM / 非合规） |
| CM 切片 | [`compute-cm/`](compute-cm/) — `experimental:webgpu-cm@0.1.0`（typed lists/strings + WIT resources；**仍非**合规 wasi:webgpu） |

P0/P1 **不** vendoring 完整上游 WIT，也不生成完整 wit-bindgen host。  
CM 切片钉定的是本仓 `wit/compute-cm/world.wit`（方法名向 wasi:webgpu compute 靠）。  
升级时：先更新本文件与映射表，再改 `WasiWebGpuHost` / abi-mvp / abi-cm。
