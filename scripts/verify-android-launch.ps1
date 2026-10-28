param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
function Invoke-Adb([string[]]$Arguments) {
    $result = & $Adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($result -join "`n") }
    return $result
}
New-Item -ItemType Directory -Force "$repo/artifacts" | Out-Null
$package = 'org.dx340.remote.controller'
# This installs only the phone remote; the DX340 companion and music apps are separate.
Invoke-Adb @('install', '-r', "$repo/artifacts/dx340-remote-0.1.0-debug.apk")
$launch = Invoke-Adb @('shell', 'am', 'start', '-W', '-n', "$package/.MainActivity")
$launch | Set-Content "$repo/artifacts/android-remote-launch.txt"
if (($launch -join "`n") -notmatch 'Status: ok') { throw 'Android did not report a successful Activity launch.' }
$process = Invoke-Adb @('shell', 'pidof', $package)
if (!$process) { throw 'Remote process was not found after Activity launch.' }
$inventory = @{
    serial = $Serial
    model = (Invoke-Adb @('shell', 'getprop', 'ro.product.model') -join '').Trim()
    os = (Invoke-Adb @('shell', 'getprop', 'ro.build.version.release') -join '').Trim()
    build = (Invoke-Adb @('shell', 'getprop', 'ro.build.fingerprint') -join '').Trim()
    package = $package
    process = ($process -join '').Trim()
    result = 'Activity launch reported ok and process observed; inspect screenshot for UI'
    bluetoothAcceptance = 'NOT TESTED by this launch check'
}
$inventory | ConvertTo-Json | Set-Content "$repo/artifacts/android-remote-launch.json"
Invoke-Adb @('shell', 'screencap', '-p', '/sdcard/dx340-remote-launch.png')
Invoke-Adb @('pull', '/sdcard/dx340-remote-launch.png', "$repo/artifacts/android-remote-launch.png")
Invoke-Adb @('shell', 'rm', '/sdcard/dx340-remote-launch.png')
Write-Output 'Remote launched. Inspect artifacts/android-remote-launch.png. A DX340-only launch is not a separate-phone BLE test.'
