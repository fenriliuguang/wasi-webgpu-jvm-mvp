# Rebuild experimental CM guest (cube-cm).
# Requires: rustc/cargo (wasm32-unknown-unknown), wasm-tools.
# Prefer rustc 1.97.1 when available; pass --ignore-rust-version for wit-bindgen MSRV.

$ErrorActionPreference = "Continue"
$Root = Split-Path -Parent $PSScriptRoot
$Guest = Join-Path $Root "guest\cube-cm"
$env:Path = "$env:USERPROFILE\.cargo\bin;$env:Path"

$cargo = "cargo"
if (Get-Command "rustup" -ErrorAction SilentlyContinue) {
    $ver = & rustup run 1.97.1 rustc --version 2>$null
    if ($LASTEXITCODE -eq 0 -and $ver) {
        $env:RUSTUP_TOOLCHAIN = "1.97.1"
        Write-Host "Using RUSTUP_TOOLCHAIN=1.97.1"
    }
}

Push-Location $Guest
try {
    & $cargo build --target wasm32-unknown-unknown --release --ignore-rust-version
    if ($LASTEXITCODE -ne 0) { throw "cargo build failed: $LASTEXITCODE" }
    $meta = cargo metadata --no-deps --format-version 1 | ConvertFrom-Json
    $core = Join-Path $meta.target_directory "wasm32-unknown-unknown\release\cube_cm.wasm"
    if (-not (Test-Path $core)) { throw "missing core wasm: $core" }
    $out = Join-Path $Guest "cube_cm.wasm"
    wasm-tools component new $core -o $out
    if ($LASTEXITCODE -ne 0) { throw "wasm-tools component new failed: $LASTEXITCODE" }
    Write-Host "Wrote $out ($((Get-Item $out).Length) bytes)"
    wasm-tools component wit $out
} finally {
    Pop-Location
}
