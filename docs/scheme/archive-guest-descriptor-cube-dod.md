# Guest 标准 descriptor + 旋转纹理立方体 DoD 归档（已完成）

**中文** | [English](archive-guest-descriptor-cube-dod.en.md)

> 自根 README / 计划页迁出的**已完成**验收清单归档。  
> 原计划页：[`guest-descriptor-cube.md`](guest-descriptor-cube.md)。承接：[`archive-compliant-world-dod.md`](archive-compliant-world-dod.md)。

归档对应：切片 A（Android CM natives）→ B（标准 descriptor + cube Demo）→ C（wasi PRIMARY_PATH 接线）→ D（生命周期加固，**仍非真 WIT dtor**），2026-08-10。

**重要：** wasi 子集接线 ≠ 可对外宣传「合规 wasi:webgpu 产品」。主验收仍为 `experimental:webgpu-cm` **CM cube**。

## DoD

### A — Android CM natives 解锁

- [x] 重编 Bionic `.so`（递归 `cm-resources` + android 补丁）
- [x] 文档 / 脚本钉定；桌面 CM natives 同源补丁
- [x] 嵌套标准 descriptor 冒烟

### B — Guest 标准 descriptor + 旋转纹理立方体

- [x] 验收路径去掉 deprecated helpers
- [x] `guest/cube-cm/` 缓慢旋转纹理正方体；depth / `write-texture` / MVP；`@0.8.0`
- [x] 仪器 `WasmtimeCmCubeInstrumentedTest` + Demo `CubeCmOneShot`

### C — wasi 主路径子集接线

- [x] `PRIMARY_PATH` ~33 → 同一 `AbiCmHostBindings`；其余 stub
- [x] 双轨 / 缺口文档；主验收 Guest 仍 experimental

### D — 资源生命周期加固

- [x] 选强化帧等价保险 + 文档化偏差；**不做** `JniComponentLinker` rep-only overlay / 上游 PR
- [x] `releaseLifetimeSafetyNets` + Cpu ×60 帧无 swapchain 句柄累积；Demo `releaseAllGpuObjects` 可留作交接保险
- [x] **仍非真 WIT dtor**（见 [`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) §4）

## 关键交付物

- 真机验收基准：CM cube（vector-add / triangle 示例已移除）
- 生命周期：[`patches/UPSTREAM.md`](../../patches/UPSTREAM.md) §4；`AbiCmHostBindings` / `WasmtimeCmCube.Session`
- 未做（阶段排除）：wasi-gfx、合规宣传、真 CM async、上游 PR、Maven、`abi-mvp` render、perf、真 dtor overlay
