
$ErrorActionPreference = "Stop"

$source = Join-Path $PSScriptRoot "test_files"

if (-not (Test-Path $source)) {
    throw "Missing test_files folder. Generate the dummy files first."
}

adb shell "mkdir -p /sdcard/ranslab"
if ($LASTEXITCODE -ne 0) {
    throw "Could not create Android lab folder."
}

adb shell "rm -rf /sdcard/ranslab/*"
if ($LASTEXITCODE -ne 0) {
    throw "Could not clear Android lab folder."
}

adb push "$source/." /sdcard/ranslab/
if ($LASTEXITCODE -ne 0) {
    throw "Could not copy test files to Android."
}

Write-Host "Lab reset complete."
