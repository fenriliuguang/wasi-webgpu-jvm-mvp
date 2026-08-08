# wasmtime4j patches

[中文](README.md) | **English**

Trackable unified diffs against upstream tag **`v47.0.2-1.5.0`**
(`ai.tegmentum:wasmtime4j` / `gradle/libs.versions.toml`).

| File | Purpose | Touched files |
|------|---------|---------------|
| [`wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](wasmtime4j-v47.0.2-1.5.0-cm-resources.patch) | Desktop + Android CM WIT resources | `component/linker.rs`, `jni/component_linker.rs` |
| [`wasmtime4j-v47.0.2-1.5.0-android.patch`](wasmtime4j-v47.0.2-1.5.0-android.patch) | Android Bionic / ART | `async_runtime.rs`, `jni/memory.rs` |

Source checkout under `.deps/wasmtime4j` is **not** committed (see root `.gitignore`).  
Patches themselves are committed; build scripts clone the tag then `git apply`.

Upstream gap notes (phase C; **do not open upstream PRs**): [`UPSTREAM.en.md`](UPSTREAM.en.md) — ConcurrentCallCodec u64, Validation, destructor gap, local overlays.

## Apply

```powershell
# Desktop CM → runtime-wasmtime/desktop-natives/ (does not mutate Gradle cache)
./scripts/build-wasmtime4j-desktop-cm.ps1

# Android .so (default: JNI 1_6 + unsigned handle checks + CM resources)
./scripts/build-wasmtime4j-android.ps1
# abi-mvp only (skip CM): ./scripts/build-wasmtime4j-android.ps1 -SkipCmResourcesPatch
```

Manual:

```bash
git clone --depth 1 --branch v47.0.2-1.5.0 \
  https://github.com/tegmentum/wasmtime4j.git .deps/wasmtime4j
git -C .deps/wasmtime4j apply --check patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch
git -C .deps/wasmtime4j apply patches/wasmtime4j-v47.0.2-1.5.0-cm-resources.patch
```

## Re-export

If you change native sources under `.deps/wasmtime4j` again:

```powershell
python ./scripts/export-wasmtime4j-patches.py
```

Then review with `git diff patches/` and commit.

## CM patch summary

1. Host callback: `Resource` ↔ `U32(rep)` marshalling (`vals_to_host_params` / `host_results_to_vals`)
2. Multiple `resource` registrations on the same interface (`allow_shadowing` + batch re-hang)
3. Process-wide resource registry so `add_registered_host_functions_to_linker` can replay onto a fresh linker (upstream `nativeInstantiateWithLinker` does not use the caller linker)
4. JNI `defineResource` interface path uses `"{ns}/{iface}"` (matches guest import `experimental:webgpu-cm/host@0.2.0`)
