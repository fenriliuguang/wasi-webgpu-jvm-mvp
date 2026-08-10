# Guest 标准 descriptor 真机落地 + 旋转纹理立方体（guest-descriptor-cube）

**中文** | [English](guest-descriptor-cube.en.md)

> **状态：已完成（2026-08-10）。** 切片 **A–D ✅**；归档 [`archive-guest-descriptor-cube-dod.md`](archive-guest-descriptor-cube-dod.md)。  
> 承接：合规 World A–G 归档（[`archive-compliant-world-dod.md`](archive-compliant-world-dod.md)）。  
> 组合：Natives 解锁（A ✅）→ Guest 标准 descriptor + 立方体 Demo（B ✅）→ wasi 主路径子集接线（C ✅）→ 资源生命周期加固（D ✅，仍非真 WIT dtor）。

## 一句话

在 **不推进 wasi-gfx、不宣传合规产品** 的前提下：重编 Android CM `.so` 解锁嵌套 borrow；把 experimental Guest 真机验收从顶层 helpers 迁到标准 descriptor；用 **缓慢持续旋转、带开源图片纹理的正方体** 作主 Demo；并可选把已有 L2 路径接到 wasi 轨子集、加固资源生命周期。主验收仍在 `experimental:webgpu-cm`（现行 `@0.8.0`：depth / `write-texture` / cube world）。

```text
A 重编 Android CM natives（嵌套 borrow）
  → B Guest 标准 descriptor + 旋转纹理立方体 Demo
  → C wasi 主路径子集接线（已有 L2；不扫长尾 ❌）
  → D 资源生命周期加固（真 dtor 或文档化偏差 + 保险）
```

对照：[`compliant-world-gap.md`](../mapping/compliant-world-gap.md)（19 ✅ / 16 ⚠️ / 189 ❌）· [`compliant-world-dual-track.md`](../mapping/compliant-world-dual-track.md)。

## 已定决策

| 问题 | 决定 |
|------|------|
| 本阶段范围 | 锁定 **A+B+C+D**；下表「本阶段不做」写死；Maven / `abi-mvp` render / perf **不**纳入本阶段 DoD |
| 主线顺序 | **A → B** 强序；**C** 可与 B 尾部交错但不得阻塞立方体 Demo 关门；**D** 可与 B/C 交错，不阻塞 A/B 验收 |
| 主验收轨 | 仍为 **experimental**；标准包双轨可接线子集，但 **不** 把 Guest 主 Demo 迁到 `wasi:webgpu`（C 只做 Host/Linker） |
| **B Demo 选型** | **缓慢持续旋转 + 开源图片纹理的正方体**（Guest CM → 同一 L2 → Dawn；宿主帧循环驱动）；**不用** 每帧变色三角 / 多 draw 作主验收 |
| 图片素材 | **开源许可**（优先 **CC0** / 等价公有领域；若 CC-BY 须保留署名）；**入库** `guest/.../assets/`（或等价路径）+ `ATTRIBUTION`；**禁止**运行时拉取外网图 |
| 立方体最小能力 | 允许为本 Demo **additive** 扩展 experimental WIT / L2 / Dawn：**深度附件（最小）**、**纹理上传**（`write-texture` 或 `copy-buffer-to-texture`）、UV + sampler bind、uniform MVP（`write-buffer`）、可选 index/`draw-indexed`；**不做** MSAA / 多光源 / PBR / wasi-gfx |
| 特化 API | `*storage3` / `*3` / `submit1` / `*-triangle*` 等可保留 deprecated；**真机验收路径不得再依赖**顶层 helpers（A 解锁后） |
| 缺口矩阵 | C 仅抬升「已有 experimental/L2 路径」对应的 wasi 行（❌→接线或保持 stub）；**禁止**以「扫完 191 ❌」为本阶段目标 |
| Async | 仍 **sync-compat**；真 CM async 不做 |
| 合规宣称 | 包名 / README 保持 `experimental`；C 接线后 **仍不得**宣传已合规 `wasi:webgpu` 产品 |
| 上游 | overlay / 补丁自洽；**不对** tegmentum/wasmtime4j 提 issue/PR |
| 验收形态 | 桌面单测（有 natives）+ `run-android-instrumented.ps1`（**CM cube**）+ Demo 手点可见缓慢旋转纹理立方体；每子切片 CHANGELOG / 映射文档 |

## 子切片与 DoD

### A — Android CM natives 解锁

- [x] 按 [`android-wasmtime.md`](../android-wasmtime.md) §6 / [`patches/`](../../patches/) 重跑 `scripts/build-wasmtime4j-android.ps1`，替换 `runtime-wasmtime/android-natives/jniLibs/` 中带 **递归** `cm-resources` 的 Bionic `.so`
- [x] 文档钉定：补丁集合、构建命令、`.so` 替换路径；桌面 CM natives 同源 `cm-resources` 补丁（`build-wasmtime4j-desktop-cm.ps1`，尊重 `CARGO_TARGET_DIR`）；Windows Android 交叉编译 `opt-level>=1` rustc AV → 脚本默认 `CARGO_PROFILE_RELEASE_OPT_LEVEL=0`
- [x] 冒烟：`vector-add-cm` 改走嵌套标准 descriptor；桌面 `:runtime-wasmtime:test`（WasmtimeCmVectorAddTest）绿灯；仪器两波真机回归移交手测 / 后续切片
- [x] CHANGELOG 记录 natives 重建 + Guest 嵌套路径冒烟

### B — Guest 标准 descriptor + 旋转纹理立方体

- [x] **迁移：** `vector-add-cm` / `triangle-cm` 改走标准 descriptor（`create-bind-group` / pipeline / `queue.submit(list)` / 通用 render 等）；去掉对 deprecated helpers 的验收依赖
- [x] **新 Demo Guest** `guest/cube-cm/`：缓慢绕 Y 轴**持续旋转**的正方体；六面采样同一开源纹理
- [x] **素材：** 项目原创 CC0 64×64 棋盘格（Guest 内过程生成）+ [`ATTRIBUTION.md`](../../guest/cube-cm/ATTRIBUTION.md)；可离线复现
- [x] **Host/WIT 最小增量：** `@0.8.0` depth-stencil、`write-texture`、render-pass `set-bind-group`、sampler+texture bind、MVP uniform 每帧 `write-buffer`；Surface 仍 **Host 注入**；宿主帧循环 `CubeCmOneShot`
- [x] 仪器：`WasmtimeCmCubeInstrumentedTest` + `run-android-instrumented.ps1` **wave3**（与 triangle 分进程；勿同进程背靠背）
- [x] 更新 [`render-subset.md`](../mapping/render-subset.md) / EN + CHANGELOG；`experimental:webgpu-cm` **0.7.0 → 0.8.0**

### C — wasi 主路径子集接线

- [x] 在 `WasmtimeCmLinker` 上将 **已有** experimental/L2 主链方法接到 `wasi:webgpu/webgpu@0.3.0-rc.2`（adapter/device/queue/buffer/compute+render+texture 子集；`PRIMARY_PATH` ~33）；未接线者保持 Unsupported / result stub
- [x] 更新 [`compliant-world-dual-track.md`](../mapping/compliant-world-dual-track.md) + 缺口矩阵对应行（仅本子集）；**主验收 Guest 仍 experimental**
- [x] 文档写明：接线 ≠ 合规产品；无 wasi 轨立方体 Guest 义务
- [x] 回归：experimental Guest / 仪器不因 wasi 注册变化而破坏

### D — 资源生命周期加固

- [x] 在 [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) 约束下选 **(2)**：强化 View↔Texture `tryDrop` + 帧/Session 释放保险并 **明确文档化**与真 WIT dtor 的偏差；**(1) `JniComponentLinker` rep-only overlay 不做**（jni 私有路径维护成本高）
- [x] 多帧：`AbiCmHostBindingsTest` Cpu fake surface ×60 帧无 Texture/View 累积；`releaseLifetimeSafetyNets` 挂在 `WasmtimeCmCube.Session`；Demo 仍可 `releaseAllGpuObjects` 交接（D2/D3/D6）
- [x] **不对**上游提 PR；DoD 写明 **仍非真 WIT dtor**（`dropRep` 仅占位入口）

## 本阶段不做

| ID | 项 |
|----|-----|
| — | wasi-gfx / canvas / 多 window 抽象（上屏继续 Host 注入） |
| — | 宣传已合规 `wasi:webgpu` 产品（矩阵/接线齐套 ≠ 合规宣称） |
| — | 真 CM async / WASI Preview3 异步运行时（保持 sync-compat） |
| — | 对 tegmentum/wasmtime4j 提 issue/PR（本仓 overlay 自洽） |
| — | Maven Central / 发包 |
| — | `abi-mvp` 扁平 render import |
| — | 可选 perf（[`docs/perf/`](../perf/)）不阻塞、不纳入本阶段 DoD |
| — | 扫完缺口矩阵 191 ❌ / 实现 query-set·render-bundle·features/limits 等长尾 |
| — | 将主 Demo Guest 迁到标准包 import（C 只做 Host/Linker 子集） |
| — | MSAA、多光源、PBR、运行时下载纹理、专有/不明许可素材 |

## 落地顺序

1. **A** 重编并钉定 Android CM `.so` + 冒烟  
2. **B** Guest 标准 descriptor 迁移 + 开源纹理旋转立方体 + 映射/仪器  
3. **C** wasi 主路径子集接线 + 双轨/缺口文档（不阻塞 B 关门亦可后置收口）  
4. **D** 生命周期加固或偏差文档化  
5. 文档收口：本页 DoD 全勾 → [`archive-guest-descriptor-cube-dod.md`](archive-guest-descriptor-cube-dod.md)；根 README / scheme / CHANGELOG  

## 链接

- 根 README：[`README.md`](../../README.md)  
- 方案索引：[`docs/scheme/README.md`](README.md)  
- 上阶段归档：[`archive-compliant-world-dod.md`](archive-compliant-world-dod.md)  
- 缺口 / 双轨：[`compliant-world-gap.md`](../mapping/compliant-world-gap.md) · [`compliant-world-dual-track.md`](../mapping/compliant-world-dual-track.md)  
- Compute / Render：[`compute-subset.md`](../mapping/compute-subset.md) · [`render-subset.md`](../mapping/render-subset.md)  
- Android natives：[`docs/android-wasmtime.md`](../android-wasmtime.md) · [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md)  
- WIT：[`wit/compute-cm/world.wit`](../../wit/compute-cm/world.wit) · [`wit/deps/wasi-webgpu/PIN.md`](../../wit/deps/wasi-webgpu/PIN.md)  
