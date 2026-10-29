param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [Parameter(Mandatory=$true)][string]$FixtureApk,
    [Parameter(Mandatory=$true)][string]$ProductionApk,
    [ValidateSet('volume','browsing','handoff')][string]$Scenario = 'volume',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$fixture = (Resolve-Path -LiteralPath $FixtureApk).Path
$production = (Resolve-Path -LiteralPath $ProductionApk).Path
$captureRoot = Join-Path $repo "artifacts/$Scenario-captures"
New-Item -ItemType Directory -Force -Path $captureRoot | Out-Null
function Invoke-CaptureAdb([string[]]$CommandArguments) {
    & $Adb -s $Serial @CommandArguments
    if ($LASTEXITCODE -ne 0) { throw "Capture ADB step failed: $($CommandArguments[0])" }
}
function Save-Capture([string]$Name) {
    Invoke-CaptureAdb @('shell','screencap','-p','/sdcard/dx340-volume-capture.png') | Out-Null
    Invoke-CaptureAdb @('pull','/sdcard/dx340-volume-capture.png',(Join-Path $captureRoot $Name)) | Out-Null
}
$originalFont = (Invoke-CaptureAdb @('shell','settings','get','system','font_scale') | Out-String).Trim()
$originalNight = (Invoke-CaptureAdb @('shell','cmd','uimode','night') | Out-String).Trim()
if ($originalNight -notmatch '^Night mode: (yes|no|auto)$') { throw 'Cannot preserve current night mode; no capture install performed.' }
$nightToRestore = $Matches[1]
& "$PSScriptRoot/verify-android-signing.ps1" -Serial $Serial -Apk $fixture -Adb $Adb
& "$PSScriptRoot/verify-android-signing.ps1" -Serial $Serial -Apk $production -Adb $Adb
$record = [ordered]@{
    kind = "native-Android-rendering-of-synthetic-$Scenario-states-no-Bluetooth"
    fixtureSha256 = (Get-FileHash -LiteralPath $fixture -Algorithm SHA256).Hash.ToLowerInvariant()
    productionSha256 = (Get-FileHash -LiteralPath $production -Algorithm SHA256).Hash.ToLowerInvariant()
    originalFontScale = $originalFont; originalNightMode = $nightToRestore
    fixtureEntryPoint = "phone/test_support/${Scenario}_capture.dart"
    semanticReadiness = 'UIAutomator unavailable on this device; inspect actual pixels. No BLE or audio claim.'
    ordinaryAppRestored = $false
}
$cleanupErrors = [System.Collections.Generic.List[string]]::new()
try {
    Invoke-CaptureAdb @('install','-r',$fixture) | Out-Null
    $states = if ($Scenario -eq 'volume') { @(
        @{ name='synthetic-high-dark.png'; mode='volume-high'; night='yes'; scale='1.0' },
        @{ name='synthetic-low-light.png'; mode='volume-low'; night='no'; scale='1.0' },
        @{ name='synthetic-large-dark.png'; mode='volume-high'; night='yes'; scale='1.5' }
    ) } elseif ($Scenario -eq 'browsing') { @(
        @{ name='synthetic-library-dark.png'; mode='browsing-library'; night='yes'; scale='1.0' },
        @{ name='synthetic-queue-light.png'; mode='browsing-queue'; night='no'; scale='1.0' },
        @{ name='synthetic-library-large-dark.png'; mode='browsing-library'; night='yes'; scale='1.5' }
    ) } else { @(
        @{ name='synthetic-handoff-dark.png'; mode='handoff-ready'; night='yes'; scale='1.0' },
        @{ name='synthetic-handoff-light.png'; mode='handoff-ready'; night='no'; scale='1.0' },
        @{ name='synthetic-handoff-large-dark.png'; mode='handoff-ready'; night='yes'; scale='1.5' },
        @{ name='synthetic-handoff-pending.png'; mode='handoff-pending'; night='no'; scale='1.0' },
        @{ name='synthetic-handoff-lost.png'; mode='handoff-lost'; night='yes'; scale='1.0' }
    ) }
    foreach ($state in $states) {
        Invoke-CaptureAdb @('shell','cmd','uimode','night',$state.night) | Out-Null
        Invoke-CaptureAdb @('shell','settings','put','system','font_scale',$state.scale) | Out-Null
        Invoke-CaptureAdb @('shell','am','force-stop','org.dx340.remote.controller') | Out-Null
        Invoke-CaptureAdb @('shell','am','start','-W','-n','org.dx340.remote.controller/org.dx340.remote.dx340_phone.MainActivity','--es','captureTab',$state.mode) | Out-Null
        # The fixture waits for its first frame before scrolling to the whole action.
        Start-Sleep -Seconds 3
        Save-Capture $state.name
    }
} finally {
    try {
        if ($originalFont -eq 'null') { Invoke-CaptureAdb @('shell','settings','delete','system','font_scale') | Out-Null }
        else { Invoke-CaptureAdb @('shell','settings','put','system','font_scale',$originalFont) | Out-Null }
        Invoke-CaptureAdb @('shell','cmd','uimode','night',$nightToRestore) | Out-Null
    } catch { $cleanupErrors.Add('Display setting restoration failed.') }
    try {
        Invoke-CaptureAdb @('install','-r',$production) | Out-Null
        Invoke-CaptureAdb @('shell','am','start','-W','--activity-clear-top','-n','org.dx340.remote.controller/org.dx340.remote.dx340_phone.MainActivity') | Out-Null
        Start-Sleep -Seconds 2
        Save-Capture 'ordinary-app-restored.png'
        $record.ordinaryAppRestored = $true
    } catch { $cleanupErrors.Add('Ordinary phone app restoration/launch failed; inspect before using the device.') }
    $record.cleanupErrors = @($cleanupErrors)
    $record | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $captureRoot 'provenance.json')
    if ($cleanupErrors.Count) { throw ($cleanupErrors -join ' ') }
}
Write-Output "Synthetic captures complete; ordinary phone restored. Evidence: $captureRoot"
