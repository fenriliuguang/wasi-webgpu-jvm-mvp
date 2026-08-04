# Cross-compile wasmtime4j-native for Android and install into runtime-wasmtime/android-natives/jniLibs.
param(
    [string]$NdkVersion = "28.2.13676358",
    [string]$Wasmtime4jTag = "v47.0.2-1.5.0",
    [int]$ApiLevel = 24
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$Ndk = Join-Path $Sdk "ndk\$NdkVersion"
if (-not (Test-Path $Ndk)) {
    throw "Android NDK not found at $Ndk. Install with sdkmanager --install `"ndk;$NdkVersion`"."
}

$Deps = Join-Path $Root ".deps\wasmtime4j"
if (-not (Test-Path (Join-Path $Deps "wasmtime4j-native\Cargo.toml"))) {
    New-Item -ItemType Directory -Force -Path (Split-Path $Deps) | Out-Null
    git clone --depth 1 --branch $Wasmtime4jTag https://github.com/tegmentum/wasmtime4j.git $Deps
}

# Android ART rejects JNI_VERSION_1_8 (65544) from JNI_OnLoad; return 1_6 instead.
$AsyncRs = Join-Path $Deps "wasmtime4j-native\src\async_runtime.rs"
$asyncText = Get-Content -LiteralPath $AsyncRs -Raw
if ($asyncText -notmatch 'cfg\(target_os = "android"\)') {
    $patched = $asyncText -replace `
        '(?ms)(cache_jvm\(Arc::new\(vm\)\);\s*debug!\("JNI_OnLoad: JavaVM cached for async CompletableFuture bridge"\);\s*)jni::sys::JNI_VERSION_1_8', `
        @'
$1// Android ART only accepts JNI_VERSION_1_2 / 1_4 / 1_6 (not 1_8 = 65544).
    #[cfg(target_os = "android")]
    {
        jni::sys::JNI_VERSION_1_6
    }
    #[cfg(not(target_os = "android"))]
    {
        jni::sys::JNI_VERSION_1_8
    }
'@
    if ($patched -eq $asyncText) {
        throw "Failed to patch JNI_OnLoad in $AsyncRs for Android JNI_VERSION_1_6"
    }
    Set-Content -LiteralPath $AsyncRs -Value $patched -NoNewline
    Write-Host "Patched JNI_OnLoad to return JNI_VERSION_1_6 on Android"
}

# Android MTE/TBI tagged pointers look negative as signed jlong; use unsigned low-page checks.
$MemoryRs = Join-Path $Deps "wasmtime4j-native\src\jni\memory.rs"
$memoryText = Get-Content -LiteralPath $MemoryRs -Raw
if ($memoryText -match 'if memory_ptr < 0x1000 \|\| memory_ptr == -1 \{') {
    $memoryPatched = $memoryText.Replace(
        'if memory_ptr < 0x1000 || memory_ptr == -1 {',
        'if memory_ptr == 0 || memory_ptr == -1 || (memory_ptr as u64) < 0x1000 {'
    ).Replace(
        'if table_ptr < 0x1000 || table_ptr == -1 {',
        'if table_ptr == 0 || table_ptr == -1 || (table_ptr as u64) < 0x1000 {'
    )
    if ($memoryPatched -eq $memoryText) {
        throw "Failed to patch signed jlong handle checks in $MemoryRs"
    }
    Set-Content -LiteralPath $MemoryRs -Value $memoryPatched -NoNewline
    Write-Host "Patched memory.rs handle checks for Android MTE/TBI pointers"
}

$Out = Join-Path $Root "runtime-wasmtime\android-natives\jniLibs"
New-Item -ItemType Directory -Force -Path $Out | Out-Null

$Stubs = Join-Path $Root "runtime-wasmtime\android-natives\link-stubs"
New-Item -ItemType Directory -Force -Path $Stubs | Out-Null
# Bionic has no libpthread; rustc still passes -lpthread on unix targets.
Set-Content -Path (Join-Path $Stubs "libpthread.so") -Value "INPUT(-lc)`n" -NoNewline -Encoding ascii

$env:ANDROID_NDK_HOME = $Ndk
$env:ANDROID_NDK_ROOT = $Ndk
# wasmtime 47.x needs rustc >= 1.94 (see Cargo.lock / cranelift MSRV).
if (-not $env:RUSTUP_TOOLCHAIN) {
    $env:RUSTUP_TOOLCHAIN = "1.97.1"
}
$env:CARGO_TARGET_AARCH64_LINUX_ANDROID_RUSTFLAGS = "-Lnative=$Stubs"
$env:CARGO_TARGET_X86_64_LINUX_ANDROID_RUSTFLAGS = "-Lnative=$Stubs"

Push-Location (Join-Path $Deps "wasmtime4j-native")
try {
    # Match Maven wasmtime4j-native defaults (excludes wasi-nn). jni-bindings alone does not compile.
    cargo ndk -t arm64-v8a -t x86_64 -o $Out --platform $ApiLevel -- `
        build --release --locked
} finally {
    Pop-Location
}

Write-Host "Installed Android natives under $Out"
Get-ChildItem -Recurse $Out -Filter "*.so" | ForEach-Object {
    $strip = Join-Path $Ndk "toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-strip.exe"
    if (Test-Path $strip) {
        & $strip --strip-unneeded $_.FullName
    }
    "{0} ({1:N1} MB)" -f $_.FullName, ($_.Length / 1MB)
}
