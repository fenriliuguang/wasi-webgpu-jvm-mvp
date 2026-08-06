# experimental:webgpu-cm@0.3.0

**中文** | [English](README.en.md)

Component Model compute + 最小 surface/render 切片。

- 包名 **experimental** — **不得**称为合规 `wasi:webgpu`
- 方法名向 `wasi:webgpu@0.3.0-rc.2` 相关路径靠
- 句柄为 WIT `resource` + method（内部仍映射到 L2 `GpuHandle`）
- async WIT → 同步（与 L2 一致）
- bind-group helpers 仍是 vector-add 特化
- **0.2.0：** `buffer-descriptor` + `buffer-usage-flags` / `map-mode-flags`（u32 别名）；`create-buffer(descriptor)`；`map-async(mode, …)` 取代 `map-read`
- **0.3.0：** Android native-window `surface` + triangle 形 `render-pipeline` / `render-pass`
- World exports：`vector-add` → `run-vector-add`；`triangle` → `run-triangle`（Host 注入 native window）

Guest：`guest/vector-add-cm/`、`guest/triangle-cm/`  
Host 适配：`abi-cm` → `WasiWebGpuHost`  
接线：`runtime-wasmtime` 的 `runtime.cm`（`ComponentLinker` + `defineResource`）

桌面 CM 需要打补丁的 wasmtime4j native（resource 编组 / 多 resource 注册 / 实例化时重放 resources）。  
补丁入库：[`patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](../../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch)

```powershell
./scripts/build-wasmtime4j-desktop-cm.ps1
```
