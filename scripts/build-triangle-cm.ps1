# Rebuild experimental CM guest (triangle-cm).
# Requires: rustc/cargo (wasm32-unknown-unknown), wasm-tools.

# Native tools (cargo) often write progress to stderr; do not treat that as failure.
$ErrorActionPreference = "Continue"
$Root = Split-Path -Parent $PSScriptRoot
$Guest = Join-Path $Root "guest\triangle-cm"

Push-Location $Guest
try {
    cargo build --target wasm32-unknown-unknown --release
    if ($LASTEXITCODE -ne 0) { throw "cargo build failed: $LASTEXITCODE" }
    $core = Join-Path $Guest "target\wasm32-unknown-unknown\release\triangle_cm.wasm"
    $out = Join-Path $Guest "triangle_cm.wasm"
    wasm-tools component new $core -o $out
    if ($LASTEXITCODE -ne 0) { throw "wasm-tools component new failed: $LASTEXITCODE" }
    Write-Host "Wrote $out ($((Get-Item $out).Length) bytes)"
    wasm-tools component wit $out
} finally {
    Pop-Location
}
