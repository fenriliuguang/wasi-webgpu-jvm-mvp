# Render / Surface 子集映射（experimental）

**中文** | [English](render-subset.en.md)

本表覆盖红三角上屏路径所需的 L2 / WIT 方法。仍为 **experimental**；**不是**合规 `wasi:webgpu` / wasi-gfx。

对照实现：[`TriangleRenderer`](../../android-demo/src/main/java/io/github/fenriliuguang/wasi/webgpu/demo/onscreen/TriangleRenderer.kt) → `WasiWebGpuHost` → Dawn。
Guest 上屏：[`guest/triangle-cm`](../../guest/triangle-cm) → [`WasmtimeCmTriangle`](../../runtime-wasmtime/src/main/kotlin/io/github/fenriliuguang/wasi/webgpu/experimental/runtime/cm/WasmtimeCmTriangle.kt) → abi-cm → 同一 L2 → Dawn。

| WIT / 语义 | L2 (`WasiWebGpuHost`) | Dawn | 备注 |
|------------|----------------------|------|------|
| `create-surface-from-native-window` | `instanceCreateSurfaceFromAndroidNativeWindow` | `GPUInstance.createSurface(AndroidNativeWindow)` | Android only；Cpu Host → Unsupported |
| `surface.configure` | `surfaceConfigure` | `getCapabilities` + `configure` | 返回选用的 texture format |
| `surface.get-current-texture-view` | `surfaceGetCurrentTexture` + `textureCreateView` | `getCurrentTexture` + `createView` | L2 暴露 status；CM 封装且非 Success 时抛 Validation |
| `surface.present` | `surfacePresent` | `present` | |
| `surface.unconfigure` | `surfaceUnconfigure` | `unconfigure` | |
| `device.create-render-pipeline-triangle` | `deviceCreateRenderPipelineTriangle` | empty layout + TriangleList | 要求 `vs_main` / `fs_main` |
| `command-encoder.begin-render-pass-clear` | `commandEncoderBeginRenderPassClear` | `beginRenderPass` Clear/Store | 单 color attachment |
| `render-pass-encoder.set-pipeline` | `renderPassSetPipeline` | `setPipeline` | |
| `render-pass-encoder.draw` | `renderPassDraw` | `draw` | |
| `render-pass-encoder.end` | `renderPassEnd` | `end` | |

## Guest CM 上屏路径（已通）

```text
guest/triangle-cm（triangle_cm.wasm，world triangle）
  export run-triangle(window-handle: u64, width: u32, height: u32)
  → WasmtimeCmTriangle（L1）/ WasmtimeCmTriangleAndroid（assets 加载）
  → Wasmtime ComponentLinker + abi-cm（上表 imports）
  → 同一 WasiWebGpuHost → Dawn → SurfaceView
```

- **Window**：Host 侧注入 native window（`Surface` → ANativeWindow 指针，按 u64 传）；Guest 只持 `surface` resource，不创建 window
- **Guest 内部序列**（`guest/triangle-cm/src/lib.rs`）：`create-surface-from-native-window` → `configure` → `create-render-pipeline-triangle` → `get-current-texture-view` → `begin-render-pass-clear` → `set-pipeline` / `draw(3)` / `end` → `submit1` → `present` → `unconfigure`；shader 与 L2 `TriangleRenderer` 保持一致
- **验收形态**：单次 draw；仪器测试 `WasmtimeCmTriangleInstrumentedTest` 绿灯（vivo V2458A / Mali）
- **桌面**：无 Android Surface 时 `CpuWasiWebGpuHost` → Unsupported，相关单测 skip（与 CM compute 门控一致）
- **u64 注意**：`window-handle` 高位可超 `Long.MAX_VALUE`；wasmtime4j `ConcurrentCallCodec` 须按无符号解析（android-demo 覆盖，见 [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)）；Demo 手点稳定性遗留见 [`../scheme/guest-onscreen-cm-blockers.md`](../scheme/guest-onscreen-cm-blockers.md) P6

## 明确不做（本切片）

| 区域 | 状态 |
|------|------|
| Guest CM 上屏（triangle-cm 单次 draw） | ✅ 已通（见上节） |
| wasi-gfx 上屏 | ❌ |
| 通用 render-pipeline descriptor / MSAA / depth | ❌ |
| Canvas / 多 window 抽象 | ❌ |
| `abi-mvp` 扁平 render import | ❌（P1 仍仅 compute） |

## 线程

见 [`threading.md`](threading.md)：同一 Host 上 surface 与 submit 同渲染线程。
