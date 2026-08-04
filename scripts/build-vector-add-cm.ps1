# Rebuild experimental CM guest (vector-add-cm).
# Requires: rustc/cargo (wasm32-unknown-unknown), wasm-tools.

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Guest = Join-Path $Root "guest\vector-add-cm"

Push-Location $Guest
try {
    cargo build --target wasm32-unknown-unknown --release 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "cargo build failed: $LASTEXITCODE" }
    $core = Join-Path $Guest "target\wasm32-unknown-unknown\release\vector_add_cm.wasm"
    $out = Join-Path $Guest "vector_add_cm.wasm"
    wasm-tools component new $core -o $out
    if ($LASTEXITCODE -ne 0) { throw "wasm-tools component new failed: $LASTEXITCODE" }
    Write-Host "Wrote $out ($((Get-Item $out).Length) bytes)"
    wasm-tools component wit $out
} finally {
    Pop-Location
}
