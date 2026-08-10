# 本地 Maven 坐标（可发布化自检，不对外发布）

**中文** | [English](maven-local.en.md)

> **experimental** · 本页只描述仓内工程化坐标与本地 `publishToMavenLocal` 自检。  
> **不对外发布**；**不**构成「可供外部依赖 / 已发布」宣称；**不**是合规 `wasi:webgpu` 产品。

## 坐标

| 属性 | 值 |
|------|-----|
| `groupId` | `io.github.fenriliuguang.wasi.webgpu.experimental`（与包名根一致） |
| `version` | `0.1.0-experimental`（见根 `gradle.properties`：`wasi.webgpu.version`） |
| `artifactId` | 与 Gradle 模块名相同 |

工程化模块（可本地自检）：

| 模块 | 坐标 |
|------|------|
| `host-api` | `…experimental:host-api:0.1.0-experimental` |
| `host-webgpu` | `…experimental:host-webgpu:0.1.0-experimental` |
| `abi-mvp` | `…experimental:abi-mvp:0.1.0-experimental` |
| `abi-cm` | `…experimental:abi-cm:0.1.0-experimental` |
| `abi-wasi` | `…experimental:abi-wasi:0.1.0-experimental` |

**明确不纳入**本套工程化 Publishing：

- `android-demo`（应用 / 仪器壳）
- `runtime-wasmtime`（依赖自建 / 补丁 natives，见下方边界）
- Guest wasm（`guest/`）
- 预编译 Bionic `libwasmtime4j.so` / 全量 `jniLibs`、桌面 `desktop-natives/`

## 本地自检

```bash
./gradlew publishEngineeredToMavenLocal
```

等价于对上表五模块执行 `publishToMavenLocal`。仅写入本机 `~/.m2`；**无**远端仓库配置。

共享脚本：[`gradle/wasi-webgpu-publishing.gradle.kts`](../gradle/wasi-webgpu-publishing.gradle.kts)。

## 与 natives 边界

本地坐标**不**携带可用的 Android CM / 桌面 CM-patched `.so`。若在本仓外拼装 runtime：

- Android Bionic / jniLibs：按 [`android-wasmtime.md`](android-wasmtime.md) 与 [`runtime-wasmtime/android-natives/README.md`](../runtime-wasmtime/android-natives/README.md) **自建并打包**
- 桌面 CM：按 [`runtime-wasmtime/desktop-natives/README.md`](../runtime-wasmtime/desktop-natives/README.md) 放置补丁库

工程化模块之间的 `project(...)` 依赖在 POM 中会解析为上述 Maven 坐标；natives 仍须按上列文档单独处理。

## 不做

- Maven Central / Sonatype / 任何远端上传
- 「已发布 / 可供消费」文档或营销口径
- 将 demo、Guest、预编译 `.so` 标成主工件
