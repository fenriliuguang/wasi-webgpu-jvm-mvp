# Apply tracked CM resources patch, build desktop wasmtime4j-native,
# install into runtime-wasmtime/desktop-natives/<platform>/ (does NOT mutate Gradle cache).
# Prerequisites: rustc/cargo (1.97+), network for first clone.
param(
    [string]$Wasmtime4jTag = "v47.0.2-1.5.0"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Deps = Join-Path $Root ".deps\wasmtime4j"
$Patch = Join-Path $Root "patches\wasmtime4j-v47.0.2-1.5.0-cm-resources.patch"
$OutRoot = Join-Path $Root "runtime-wasmtime\desktop-natives"

if (-not (Test-Path $Patch)) {
    throw "Missing tracked patch: $Patch"
}

if (-not (Test-Path (Join-Path $Deps "wasmtime4j-native\Cargo.toml"))) {
    New-Item -ItemType Directory -Force -Path (Split-Path $Deps) | Out-Null
    git clone --depth 1 --branch $Wasmtime4jTag https://github.com/tegmentum/wasmtime4j.git $Deps
}

# Fresh apply from clean tag tree (idempotent rebuilds).
git -C $Deps checkout -- .
git -C $Deps apply --check -- "$Patch"
if ($LASTEXITCODE -ne 0) { throw "git apply --check failed for $Patch" }
git -C $Deps apply -- "$Patch"
if ($LASTEXITCODE -ne 0) { throw "git apply failed for $Patch" }
Write-Host "Applied $Patch"

if (-not $env:RUSTUP_TOOLCHAIN) {
    $env:RUSTUP_TOOLCHAIN = "1.97.1"
}

Push-Location (Join-Path $Deps "wasmtime4j-native")
try {
    # cargo writes progress to stderr; do not let PS ErrorActionPreference treat that as failure.
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & cargo build --release --features "jni-bindings,component-model,wasi"
    $cargoExit = $LASTEXITCODE
    $ErrorActionPreference = $prevEap
    if ($cargoExit -ne 0) { throw "cargo build failed: $cargoExit" }
} finally {
    Pop-Location
}

# Prefer CARGO_TARGET_DIR when set (e.g. CI / sandbox); else workspace .deps/wasmtime4j/target.
$os = [System.Runtime.InteropServices.RuntimeInformation]::OSDescription
$arch = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString()
$TargetRoot = if ($env:CARGO_TARGET_DIR) { $env:CARGO_TARGET_DIR } else { Join-Path $Deps "target" }

$platform = $null
$libName = $null
$built = $null
if ($IsWindows -or $env:OS -eq "Windows_NT") {
    if ($arch -match "X64|Amd64") {
        $platform = "windows-x86_64"
        $libName = "wasmtime4j.dll"
        $built = Join-Path $TargetRoot "release\wasmtime4j.dll"
    }
} elseif ($IsMacOS) {
    if ($arch -match "Arm64") {
        $platform = "darwin-aarch64"
        $libName = "libwasmtime4j.dylib"
        $built = Join-Path $TargetRoot "release\libwasmtime4j.dylib"
    }
} else {
    # Linux
    if ($arch -match "Arm64") {
        $platform = "linux-aarch64"
        $libName = "libwasmtime4j.so"
        $built = Join-Path $TargetRoot "release\libwasmtime4j.so"
    } else {
        $platform = "linux-x86_64"
        $libName = "libwasmtime4j.so"
        $built = Join-Path $TargetRoot "release\libwasmtime4j.so"
    }
}

if (-not $platform -or -not $built) {
    throw "Unsupported host platform (os=$os arch=$arch); add a desktop-natives layout entry"
}
if (-not (Test-Path $built)) {
    throw "Patched native not found at $built"
}

$destDir = Join-Path $OutRoot $platform
New-Item -ItemType Directory -Force -Path $destDir | Out-Null
$dest = Join-Path $destDir $libName
Copy-Item -Force -Path $built -Destination $dest
Write-Host "Installed $dest ($((Get-Item $dest).Length) bytes)"
Write-Host "Gradle will pack this into a patched wasmtime4j-native jar for tests (no Maven cache mutation)."
Write-Host "Re-run: ./gradlew :runtime-wasmtime:test"
