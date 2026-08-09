# Desktop natives (CM-patched wasmtime4j)

**English** (Chinese context: root README / `patches/README.md`)

Local build output for **CM resources**–patched desktop `wasmtime4j` natives.
Used by `:runtime-wasmtime` when present; **not** committed (see root `.gitignore`).

Without these files, desktop abi-mvp tests still run against Maven `wasmtime4j-native`.
CM tests (`WasmtimeCmCubeTest`) **skip** until you rebuild.

## Layout

```text
desktop-natives/
  windows-x86_64/wasmtime4j.dll
  linux-x86_64/libwasmtime4j.so
  linux-aarch64/libwasmtime4j.so
  darwin-aarch64/libwasmtime4j.dylib
  README.md
```

Jar resource paths match upstream Maven layout: `natives/<platform>/<lib>`.

## Rebuild

Requires: Rust **1.97+**, network for first `.deps/wasmtime4j` clone.

```powershell
./scripts/build-wasmtime4j-desktop-cm.ps1
```

Applies [`patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](../../patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch)
and copies the release binary into this directory. Does **not** mutate `~/.gradle/caches`.

Then:

```bash
./gradlew :runtime-wasmtime:test
```

Force CM tests when the binary is already present (Gradle sets this automatically):

```bash
./gradlew :runtime-wasmtime:test
```

CM tests **skip** if `desktop-natives/<platform>/` has no matching library.