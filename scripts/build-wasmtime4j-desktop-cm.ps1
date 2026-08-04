# Apply tracked CM resources patch, build desktop wasmtime4j-native, install into Maven cache jars.
# Prerequisites: rustc/cargo (1.97+), network for first clone.
param(
    [string]$Wasmtime4jTag = "v47.0.2-1.5.0"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Deps = Join-Path $Root ".deps\wasmtime4j"
$Patch = Join-Path $Root "patches\wasmtime4j-v47.0.2-1.5.0-cm-resources.patch"

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

# Workspace cargo target lives at .deps/wasmtime4j/target.
$Dll = Join-Path $Deps "target\release\wasmtime4j.dll"
if (-not (Test-Path $Dll)) {
    throw "Patched DLL not found at $Dll"
}
Write-Host "Built $Dll ($((Get-Item $Dll).Length) bytes)"

$entryPath = "natives/windows-x86_64/wasmtime4j.dll"
$jars = @()
foreach ($artifact in @("wasmtime4j-native", "wasmtime4j-jni")) {
    $found = Get-ChildItem "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\ai.tegmentum\$artifact" `
        -Recurse -Filter "$artifact-*-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch "sources|javadoc" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($found) { $jars += $found }
}
if ($jars.Count -eq 0) {
    throw "Maven wasmtime4j-native/jni jars not found; run a Gradle resolve first"
}

$jarList = ($jars | ForEach-Object { $_.FullName }) -join "`n"
python -c @"
import os, zipfile
from pathlib import Path
dll = Path(r'$Dll')
entry = r'$entryPath'
jars = [Path(p) for p in r'''$jarList'''.splitlines() if p.strip()]
for jar in jars:
    bak = Path(str(jar) + '.orig')
    if not bak.exists():
        bak.write_bytes(jar.read_bytes())
        print('backed up', bak)
    tmp = jar.with_suffix('.jar.tmp')
    with zipfile.ZipFile(jar, 'r') as zin, zipfile.ZipFile(tmp, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename == entry:
                continue
            zout.writestr(item, zin.read(item.filename))
        zout.write(dll, entry)
    os.replace(tmp, jar)
    with zipfile.ZipFile(jar, 'r') as z:
        print('installed', jar.name, z.getinfo(entry).file_size)
"@
if ($LASTEXITCODE -ne 0) { throw "failed to install DLL into jar" }
Write-Host "Re-run desktop tests (Gradle extracts natives to a fresh temp dir each process)."
