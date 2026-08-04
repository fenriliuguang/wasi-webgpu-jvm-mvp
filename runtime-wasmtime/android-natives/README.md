# Android natives for wasmtime4j

Prebuilt `libwasmtime4j.so` for Android (Bionic), used via `jniLibs` + `System.loadLibrary("wasmtime4j")`.

Desktop JVM continues to use Maven `wasmtime4j-native` (glibc / Darwin / Windows). **Do not** ship those desktop binaries as Android `jniLibs`.

## Layout

```text
android-natives/
  jniLibs/arm64-v8a/libwasmtime4j.so
  jniLibs/x86_64/libwasmtime4j.so
  link-stubs/libpthread.so   # linker script: INPUT(-lc) for Bionic
  README.md
```

## Rebuild

Requires: Rust **≥ 1.94** (1.97+ recommended), `cargo-ndk`, Android NDK, targets `aarch64-linux-android` / `x86_64-linux-android`.

```powershell
./scripts/build-wasmtime4j-android.ps1
```

Version pin matches `gradle/libs.versions.toml` (`wasmtime4j`). Source checkout lives under `.deps/wasmtime4j` (gitignored).

The build script `git apply`s tracked [`patches/wasmtime4j-v47.0.2-1.5.0-android.patch`](../../patches/wasmtime4j-v47.0.2-1.5.0-android.patch):

1. `JNI_OnLoad` → `JNI_VERSION_1_6` (ART rejects `1_8` / `65544`)
2. `memory.rs` handle checks use unsigned compare (MTE/TBI tagged pointers)

Progress, pitfalls, and Java-side Validation shim: [`docs/android-wasmtime.md`](../../docs/android-wasmtime.md).
