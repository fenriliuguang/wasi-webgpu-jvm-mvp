# Upstream contribution notes (wasmtime4j)

[中文](UPSTREAM.md) | **English**

Tracked patches against **`v47.0.2-1.5.0`** (`ai.tegmentum:wasmtime4j`).
This file is a **contribution brief**, not a submitted PR. Open issues/PRs against
[tegmentum/wasmtime4j](https://github.com/tegmentum/wasmtime4j) only when ready to maintain them upstream.

## Patches

| Patch | Problem | Suggested upstream change |
|-------|---------|---------------------------|
| [`wasmtime4j-v47.0.2-1.5.0-android.patch`](wasmtime4j-v47.0.2-1.5.0-android.patch) | ART rejects `JNI_VERSION_1_8` from `JNI_OnLoad` | Return `JNI_VERSION_1_6` (or detect Android / use max supported) |
| same android patch | Signed `jlong` compare treats MTE/TBI-tagged pointers as corrupt | Compare handles as `u64` (`memory_ptr as u64 < 0x1000`, same for tables) |
| [`wasmtime4j-v47.0.2-1.5.0-cm-resources.patch`](wasmtime4j-v47.0.2-1.5.0-cm-resources.patch) | CM host callbacks need `Resource` ↔ `U32(rep)`; multi-resource registration; fresh linker on instantiate | Resource marshalling helpers; `allow_shadowing` / batch re-define; process-level registry replay for `nativeInstantiateWithLinker` |

## Reproduce (Android)

```powershell
./scripts/build-wasmtime4j-android.ps1
# APK jniLibs: runtime-wasmtime/android-natives/jniLibs/
```

## Reproduce (desktop CM)

```powershell
./scripts/build-wasmtime4j-desktop-cm.ps1
# → runtime-wasmtime/desktop-natives/<platform>/
./gradlew :runtime-wasmtime:test
```

## Re-export after editing `.deps/wasmtime4j`

```powershell
python ./scripts/export-wasmtime4j-patches.py
```

## Related Java-side Android shim (not a Rust patch)

`android-demo` ships a relaxed `ai.tegmentum.wasmtime4j.util.Validation` that allows high-bit-set
handles (ARM64 TBI/PAC signed `long`). Prefer documenting or fixing upstream validation if
native pointers are intentionally bit-cast to `jlong`.
