# Reliable Android instrumented tests via adb (bypasses AGP UTP).
# Use when Studio/Gradle :connectedDebugAndroidTest reports "Process crashed"
# while `am instrument` is green — common on vivo when UTP reinstall races.
#
# Default run uses three am-instrument waves with force-stop between them:
# wasmtime4j CM host callbacks are process-global (D6) — closing one ComponentLinker
# (vector-add / triangle / cube) can trap the next instantiate in the same process.
# Do NOT run triangle+cube back-to-back in one process.
param(
    [string]$Class = "",
    [switch]$SkipAssemble
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $Sdk "platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb not found at $adb" }

$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk

if (-not $SkipAssemble) {
    Push-Location $Root
    try {
        ./gradlew :android-demo:assembleDebug :android-demo:assembleDebugAndroidTest
        if ($LASTEXITCODE -ne 0) { throw "assemble failed: $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
}

$apk = Join-Path $Root "android-demo\build\outputs\apk\debug\android-demo-debug.apk"
$testApk = Join-Path $Root "android-demo\build\outputs\apk\androidTest\debug\android-demo-debug-androidTest.apk"
$tsApk = Join-Path $Root ".deps\androidx-test-apks\test-services-1.5.0.apk"

if (-not (Test-Path $apk)) { throw "missing $apk" }
if (-not (Test-Path $testApk)) { throw "missing $testApk" }

# Ensure test-services (Studio UTP / logcat helpers need it on some OEMs).
$tsPath = & $adb shell pm path androidx.test.services 2>$null
if (-not $tsPath) {
    if (-not (Test-Path $tsApk)) {
        $ver = "1.5.0"
        $out = Split-Path $tsApk
        New-Item -ItemType Directory -Force -Path $out | Out-Null
        $url = "https://dl.google.com/dl/android/maven2/androidx/test/services/test-services/$ver/test-services-$ver.apk"
        Write-Host "Downloading $url"
        Invoke-WebRequest -Uri $url -OutFile $tsApk
    }
    & $adb install -r -t -g --force-queryable $tsApk
    if ($LASTEXITCODE -ne 0) { throw "failed to install test-services" }
}

& $adb install -r -t -g --force-queryable $apk
if ($LASTEXITCODE -ne 0) { throw "failed to install app apk (accept USB install on device if prompted)" }
& $adb install -r -t -g --force-queryable $testApk
if ($LASTEXITCODE -ne 0) { throw "failed to install test apk" }

# Keep screen on — vivo may background the activity and leave Surface invalid.
& $adb shell input keyevent KEYCODE_WAKEUP 2>$null
& $adb shell svc power stayon true 2>$null

$runner = "io.github.fenriliuguang.wasi.webgpu.demo.test/androidx.test.runner.AndroidJUnitRunner"
$pkg = "io.github.fenriliuguang.wasi.webgpu.demo"
$appId = "io.github.fenriliuguang.wasi.webgpu.demo"

function Invoke-Instrument([string]$classFilter, [string]$label) {
    & $adb shell am force-stop $appId 2>$null | Out-Null
    Start-Sleep -Milliseconds 400
    $argList = @("shell", "am", "instrument", "-w", "-r")
    if ($classFilter) {
        $argList += @("-e", "class", $classFilter)
    }
    $argList += $runner
    Write-Host "=== $label ==="
    Write-Host "Running: adb $($argList -join ' ')"
    # Capture output: adb often exits 0 even when tests fail; parse the summary.
    $output = & $adb @argList 2>&1 | ForEach-Object { "$_" }
    $output | ForEach-Object { Write-Host $_ }
    $adbCode = [int]$LASTEXITCODE
    $text = $output -join "`n"
    if ($text -match "FAILURES!!!") { return 1 }
    if ($text -match "Process crashed") { return 1 }
    if ($adbCode -ne 0) { return $adbCode }
    if ($text -match "OK \(\d+ tests?\)") { return 0 }
    return 1
}

if ($Class) {
    exit (Invoke-Instrument $Class "custom class filter")
}

# Wave 1: compute paths (CM vector-add closes its linker at the end).
$wave1 = @(
    "$pkg.VectorAddInstrumentedTest",
    "$pkg.WasmtimeCmVectorAddInstrumentedTest",
    "$pkg.WasmtimeVectorAddInstrumentedTest"
) -join ","
# Wave 2: CM triangle (needs a fresh process after any prior CM linker.close).
$wave2 = "$pkg.WasmtimeCmTriangleInstrumentedTest"
# Wave 3: CM cube (separate process from triangle — shared draw-frame name / CM registry).
$wave3 = "$pkg.WasmtimeCmCubeInstrumentedTest"

$code1 = Invoke-Instrument $wave1 "wave1 compute"
if ($code1 -ne 0) {
    Write-Host "wave1 failed with exit code $code1"
    exit $code1
}
$code2 = Invoke-Instrument $wave2 "wave2 CM triangle"
if ($code2 -ne 0) {
    Write-Host "wave2 failed with exit code $code2"
    exit $code2
}
$code3 = Invoke-Instrument $wave3 "wave3 CM cube"
if ($code3 -ne 0) {
    Write-Host "wave3 failed with exit code $code3"
} else {
    Write-Host "All instrumented waves OK"
}
exit $code3
