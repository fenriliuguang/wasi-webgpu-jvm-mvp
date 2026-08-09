# Rebuild experimental CM guest (triangle-cm).
# Requires: rustc/cargo (wasm32-unknown-unknown), wasm-tools.
# On rustc 1.86 + wit-bindgen 0.55: pass --ignore-rust-version (crate MSRV 1.87).

$ErrorActionPreference = "Continue"
$Root = Split-Path -Parent $PSScriptRoot
$Guest = Join-Path $Root "guest\triangle-cm"
$env:Path = "$env:USERPROFILE\.cargo\bin;$env:Path"

Push-Location $Guest
try {
    cargo build --target wasm32-unknown-unknown --release --ignore-rust-version
    if ($LASTEXITCODE -ne 0) { throw "cargo build failed: $LASTEXITCODE" }
    $meta = cargo metadata --no-deps --format-version 1 | ConvertFrom-Json
    $core = Join-Path $meta.target_directory "wasm32-unknown-unknown\release\triangle_cm.wasm"
    if (-not (Test-Path $core)) { throw "missing core wasm: $core" }
    $out = Join-Path $Guest "triangle_cm.wasm"
    wasm-tools component new $core -o $out
    if ($LASTEXITCODE -ne 0) { throw "wasm-tools component new failed: $LASTEXITCODE" }
    Write-Host "Wrote $out ($((Get-Item $out).Length) bytes)"
    wasm-tools component wit $out
} finally {
    Pop-Location
}
