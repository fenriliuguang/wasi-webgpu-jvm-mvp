# experimental:webgpu-cm@0.8.0

**中文** | [English](README.en.md)

Component Model compute + 最小 surface/render 切片。

- 包名 **experimental** — **不得**称为合规 `wasi:webgpu`
- 方法名向 `wasi:webgpu@0.3.0-rc.2` 相关路径靠
- 句柄为 WIT `resource` + method（内部仍映射到 L2 `GpuHandle`）
- async WIT → 同步（与 L2 一致）
- **0.2.0：** `buffer-descriptor` + `buffer-usage-flags` / `map-mode-flags`（u32 别名）；`create-buffer(descriptor)`；`map-async(mode, …)` 取代 `map-read`
- **0.3.0：** Android native-window `surface` + triangle 形 `render-pipeline` / `render-pass`
- **0.4.0：** `vertex-attribute` / `vertex-buffer-layout`；`set-vertex-buffer`；`create-render-pipeline-triangle-buffers`
- **0.5.0（slice C）：** 标准形 `create-bind-group-layout` / `create-bind-group` / `create-compute-pipeline(descriptor)` / `queue.submit(list)`；保留 `*storage3` / `*3` / `submit1` / `create-compute-pipeline-bgl`（deprecated）
- **0.6.0（slice D）：** `create-texture` / `create-sampler` / `create-pipeline-layout` / `texture.create-view`；BGL/BG sampler·texture 条目；`compute-pipeline.layout` → pipeline-layout
- **0.7.0（slice E）：** `create-render-pipeline(descriptor)` / `begin-render-pass(descriptor)`；`*-triangle*` / `begin-render-pass-clear` deprecated
- **0.8.0（guest-descriptor-cube B）：** `write-texture`；render-pass `set-bind-group`；depth-stencil；`world cube`
- World exports（WIT 仍保留历史 world）：`vector-add` / `triangle` / `cube`；**现行入库 Guest 仅 cube** → `run-cube` / `init-cube` / `draw-frame` / `drop-cube`（Host 注入 native window）

Guest（现行）：[`guest/cube-cm/`](../../guest/cube-cm/)（标准 descriptor；嵌套 borrow 需递归 `cm-resources` 补丁 natives）。历史 vector-add-cm / triangle-cm 示例已移除。  
Host 适配：`abi-cm` → `WasiWebGpuHost`  
接线：`runtime-wasmtime` 的 `runtime.cm`（`ComponentLinker` + `defineResource`）

桌面 CM 需要打补丁的 wasmtime4j native（resource 编组 / 多 resource 注册 / 实例化时重放 resources）。  
补丁入库：[`patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](../../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch)

```powershell
./scripts/build-wasmtime4j-desktop-cm.ps1
```
