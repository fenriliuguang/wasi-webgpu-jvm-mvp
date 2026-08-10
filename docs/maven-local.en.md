# Local Maven coordinates (publishability self-check; no external release)

[中文](maven-local.md) | **English**

> **experimental** · This page only documents in-repo coordinates and local `publishToMavenLocal` self-checks.  
> **No external release**; this is **not** a “published / ready for consumers” claim; **not** a compliant `wasi:webgpu` product.

## Coordinates

| Property | Value |
|----------|-------|
| `groupId` | `io.github.fenriliuguang.wasi.webgpu.experimental` (matches package root) |
| `version` | `0.1.0-experimental` (root `gradle.properties`: `wasi.webgpu.version`) |
| `artifactId` | Same as the Gradle module name |

Engineered modules (local self-check):

| Module | Coordinates |
|--------|-------------|
| `host-api` | `…experimental:host-api:0.1.0-experimental` |
| `host-webgpu` | `…experimental:host-webgpu:0.1.0-experimental` |
| `abi-mvp` | `…experimental:abi-mvp:0.1.0-experimental` |
| `abi-cm` | `…experimental:abi-cm:0.1.0-experimental` |
| `abi-wasi` | `…experimental:abi-wasi:0.1.0-experimental` |

**Explicitly excluded** from this Publishing set:

- `android-demo` (app / instrumented shell)
- `runtime-wasmtime` (self-built / patched natives; see boundary below)
- Guest wasm (`guest/`)
- Prebuilt Bionic `libwasmtime4j.so` / full `jniLibs`, desktop `desktop-natives/`

## Local self-check

```bash
./gradlew publishEngineeredToMavenLocal
```

Runs `publishToMavenLocal` for the five modules above. Writes to the machine `~/.m2` only; **no** remote repository is configured.

Shared script: [`gradle/wasi-webgpu-publishing.gradle.kts`](../gradle/wasi-webgpu-publishing.gradle.kts).

## Natives boundary

Local coordinates **do not** ship usable Android CM / desktop CM-patched `.so` files. If assembling a runtime outside this repo:

- Android Bionic / jniLibs: build and package per [`android-wasmtime.en.md`](android-wasmtime.en.md) and [`runtime-wasmtime/android-natives/README.md`](../runtime-wasmtime/android-natives/README.md)
- Desktop CM: place patched libs per [`runtime-wasmtime/desktop-natives/README.md`](../runtime-wasmtime/desktop-natives/README.md)

Inter-module `project(...)` deps resolve to the Maven coordinates above in the POM; natives remain a separate concern.

## Out of scope

- Maven Central / Sonatype / any remote upload
- “Published / ready for consumers” docs or marketing
- Treating demo, Guest, or prebuilt `.so` as primary artifacts
