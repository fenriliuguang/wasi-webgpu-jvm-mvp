# Cross-compile wasmtime4j-native for Android and install into runtime-wasmtime/android-natives/jniLibs.
# Applies tracked patches/wasmtime4j-v47.0.2-1.5.0-android.patch (JNI 1_6 + unsigned handle checks).
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
$Patch = Join-Path $Root "patches\wasmtime4j-v47.0.2-1.5.0-android.patch"
if (-not (Test-Path $Patch)) {
    throw "Missing tracked patch: $Patch"
}

if (-not (Test-Path (Join-Path $Deps "wasmtime4j-native\Cargo.toml"))) {
    New-Item -ItemType Directory -Force -Path (Split-Path $Deps) | Out-Null
    git clone --depth 1 --branch $Wasmtime4jTag https://github.com/tegmentum/wasmtime4j.git $Deps
}

git -C $Deps checkout -- .
git -C $Deps apply --check -- "$Patch"
if ($LASTEXITCODE -ne 0) { throw "git apply --check failed for $Patch" }
git -C $Deps apply -- "$Patch"
if ($LASTEXITCODE -ne 0) { throw "git apply failed for $Patch" }
Write-Host "Applied $Patch"

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
