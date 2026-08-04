# Reliable Android instrumented tests via adb (bypasses AGP UTP).
# Use when Studio/Gradle :connectedDebugAndroidTest reports "Process crashed"
# while `am instrument` is green — common on vivo when UTP reinstall races.
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

$runner = "io.github.fenriliuguang.wasi.webgpu.demo.test/androidx.test.runner.AndroidJUnitRunner"
$args = @("shell", "am", "instrument", "-w", "-r")
if ($Class) {
    $args += @("-e", "class", $Class)
}
$args += $runner

Write-Host "Running: adb $($args -join ' ')"
& $adb @args
exit $LASTEXITCODE
