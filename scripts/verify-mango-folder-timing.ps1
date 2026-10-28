param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [ValidateSet('Internal Storage','Playlist')][string]$ExpectedStart = 'Internal Storage'
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$run = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$out = Join-Path $repo "artifacts/mango-folder-timing/$run"
New-Item -ItemType Directory -Force $out | Out-Null
function Probe([string]$Label, [string]$Operation, [string[]]$Extra = @()) {
    $tag = [guid]::NewGuid().ToString('N')
    $watch = [Diagnostics.Stopwatch]::StartNew()
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation $Operation --es source com.ibasso.music --es reportTag $tag @Extra > (Join-Path $out "$Label-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw "Broadcast failed: $Label" }
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
    $watch.Stop()
    if ($LASTEXITCODE -ne 0) { throw "Report read failed: $Label" }
    $raw | Set-Content (Join-Path $out "$Label.json")
    $report = ($raw -join "`n") | ConvertFrom-Json
    if ($report.phase -ne 'FINISHED' -or $report.operation -ne $Operation -or $report.requestedSource -ne 'com.ibasso.music' -or $report.error) { throw "Unexpected report: $Label ($($report.error))" }
    if (@($report.sourceStatesBefore | Where-Object playing).Count -or @($report.sourceStatesAfter | Where-Object playing).Count) { throw 'Both source sessions must be paused; no playback command was sent' }
    if ($null -eq $report.elapsedMs) { throw 'This check requires the current debug APK with monotonic probe timing' }
    $report | Add-Member -NotePropertyName adbRoundTripMs -NotePropertyValue $watch.ElapsedMilliseconds
    return $report
}
# Prepare the desired starting screen manually or in a separately observed research setup.
# This script never taps coordinates or changes playlists, permissions, playback or volume.
$before = Probe '01-start' 'SNAPSHOT'
$headers = @($before.data.nodes | Where-Object { $_.id -eq 'com.ibasso.music:id/entry_title_tv' })
if ($headers.Count -ne 1 -or $headers[0].text -ne $ExpectedStart) { throw "Prepare the expected $ExpectedStart screen first" }
if ($ExpectedStart -eq 'Playlist') {
    if (@($before.data.nodes | Where-Object id -eq 'com.ibasso.music:id/playlist_view').Count -ne 1) { throw 'Playlist layout not observed' }
} elseif (@($before.data.nodes | Where-Object id -eq 'com.ibasso.music:id/folder_detail_content').Count -ne 1 -or @($before.data.nodes | Where-Object id -eq 'com.ibasso.music:id/tv_content').Count) { throw 'Expected storage root, not a nested or unknown folder' }
$native = Probe '02-native-root' 'BROWSE'
$assistedHome = Probe '03-assisted-home' 'BROWSE' @('--es','parent','mango-ui:root','--es','revision',$native.data.revision)
$entry = @($assistedHome.data.items | Where-Object { $_.title -eq 'Internal Storage' -and $_.browsable })
if ($entry.Count -ne 1 -or $entry[0].id -notmatch '^[A-Za-z0-9:_-]+$' -or $assistedHome.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid storage token' }
$page = Probe '04-storage' 'BROWSE' @('--es','parent',$entry[0].id,'--es','revision',$assistedHome.data.revision)
if ($page.data.title -ne 'Internal Storage' -or !$page.data.uiAssisted -or $page.data.items.Count -lt 1) { throw 'Unexpected storage result' }
$after = Probe '05-finished' 'SNAPSHOT'
[ordered]@{kind='source-local-own-adapter-timing-no-BLE';serial=$Serial;start=$ExpectedStart;title=$page.data.title;rows=$page.data.items.Count;probeElapsedMs=$page.elapsedMs;adbRoundTripMs=$page.adbRoundTripMs;bothSourcesPaused=$true;navigation=$after.data.navigation;note='Probe timer includes debug receiver/source dispatch. ADB timer includes broadcast/report read. Neither is phone/BLE timing.'} | ConvertTo-Json -Depth 9 | Tee-Object (Join-Path $out 'summary.json')
