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
$apk = "$repo/artifacts/dx340-phone-0.2.0-development.apk"
if (!(Test-Path -LiteralPath $apk)) { throw 'Run scripts/build-phone.ps1 first.' }
New-Item -ItemType Directory -Force "$repo/artifacts" | Out-Null
$package = 'org.dx340.remote.controller'
Invoke-Adb @('install', '-r', $apk)
$launch = Invoke-Adb @('shell', 'am', 'start', '-W', '-n', "$package/org.dx340.remote.dx340_phone.MainActivity")
$launch | Set-Content "$repo/artifacts/flutter-android-launch.txt"
if (($launch -join "`n") -notmatch 'Status: ok') { throw 'Activity launch did not report success.' }
# A launched Activity can still fail during Flutter/native startup. Wait for real app semantics.
$ready = $false
for ($attempt = 0; $attempt -lt 3; $attempt++) {
    $dump = Invoke-Adb @('shell', 'uiautomator', 'dump', '/sdcard/dx340-phone-launch.xml')
    if (($dump -join ' ') -match 'ERROR|could not') { Start-Sleep -Seconds 1; continue }
    Invoke-Adb @('pull', '/sdcard/dx340-phone-launch.xml', "$repo/artifacts/flutter-android-launch.xml") | Out-Null
    $ui = Get-Content -LiteralPath "$repo/artifacts/flutter-android-launch.xml" -Raw
    if ($ui -match 'Player' -and $ui -match 'Queue' -and $ui -match 'Setup' -and $ui -match 'Library') { $ready = $true; break }
    Start-Sleep -Seconds 1
}
$process = Invoke-Adb @('shell', 'pidof', $package)
$inventory = @{
    serial = $Serial
    model = (Invoke-Adb @('shell', 'getprop', 'ro.product.model') -join '').Trim()
    os = (Invoke-Adb @('shell', 'getprop', 'ro.build.version.release') -join '').Trim()
    build = (Invoke-Adb @('shell', 'getprop', 'ro.build.fingerprint') -join '').Trim()
    package = $package
    process = ($process -join '').Trim()
    apkSha256 = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    result = 'VERIFIED install/Activity launch/process; screenshot requires inspection'
    semanticReadiness = $(if ($ready) { 'VERIFIED four tab labels' } else { 'FAILED automation: no usable UIAutomator root. This does not establish an app or screen-reader failure.' })
    bluetoothAcceptance = 'NOT TESTED: launch on DX340 does not provide a separate phone'
}
$inventory | ConvertTo-Json | Set-Content "$repo/artifacts/flutter-android-launch.json"
Invoke-Adb @('shell', 'screencap', '-p', '/sdcard/dx340-phone-launch.png') | Out-Null
Invoke-Adb @('pull', '/sdcard/dx340-phone-launch.png', "$repo/artifacts/flutter-android-player-dark.png") | Out-Null
Invoke-Adb @('shell', 'rm', '/sdcard/dx340-phone-launch.png', '/sdcard/dx340-phone-launch.xml') | Out-Null
Write-Output 'Shared Android Player opened. Inspect the screenshot. Physical phone BLE remains NOT TESTED.'
if (!$ready) { Write-Warning 'UIAutomator readiness was not verified; inspect screenshot and record the automation limitation separately.' }
