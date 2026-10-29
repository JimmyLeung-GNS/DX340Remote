param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$out = Join-Path $repo 'artifacts/controller-management'
New-Item -ItemType Directory -Force $out | Out-Null
$tag = [guid]::NewGuid().ToString('N')
& $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation CHECK_CONTROLLER_APPROVALS --es source com.ibasso.music --es reportTag $tag > (Join-Path $out 'component-check-broadcast.txt')
if ($LASTEXITCODE -ne 0) { throw 'Controller component request failed' }
$raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
if ($LASTEXITCODE -ne 0) { throw 'Controller component report missing' }
$raw | Set-Content (Join-Path $out 'component-check.json')
$report = ($raw -join "`n") | ConvertFrom-Json
if ($report.phase -ne 'FINISHED' -or $report.error -or $report.operation -ne 'CHECK_CONTROLLER_APPROVALS' -or !$report.data.passed -or !$report.data.productionPreferencesUnchanged -or !$report.data.temporaryPreferencesDeleted) { throw "Controller component failed: $($report.error)" }
foreach ($field in @('sourceStatesBefore','sourceStatesAfter')) {
    $states = @($report.$field)
    if ($states.Count -ne 2 -or @($states | Where-Object { $_.playbackState -eq 2 -and !$_.playing }).Count -ne 2) { throw 'Both music sessions must be observed paused; no playback command was sent' }
}
$report.data | ConvertTo-Json -Depth 6
