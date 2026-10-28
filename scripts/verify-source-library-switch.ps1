param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [ValidateRange(1,30)][int]$Rounds = 10,
    [switch]$StartFromHome
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$run = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$out = Join-Path $repo "artifacts/source-library-switch/$run"
New-Item -ItemType Directory -Force $out | Out-Null
$rows = @()
function Probe([string]$Label, [string]$Operation, [string]$Source, [string[]]$Extra = @()) {
    $tag = [guid]::NewGuid().ToString('N')
    $watch = [Diagnostics.Stopwatch]::StartNew()
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation $Operation --es source $Source --es reportTag $tag @Extra > (Join-Path $out "$Label-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw "Broadcast failed: $Label" }
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
    $watch.Stop()
    if ($LASTEXITCODE -ne 0) { throw "Report read failed: $Label" }
    $raw | Set-Content (Join-Path $out "$Label.json")
    $report = ($raw -join "`n") | ConvertFrom-Json
    if ($report.phase -ne 'FINISHED' -or $report.operation -ne $Operation -or $report.requestedSource -ne $Source) { throw "Wrong/unfinished report: $Label" }
    foreach ($field in @('sourceStatesBefore','sourceStatesAfter')) {
        $states = @($report.$field)
        if ($states.Count -ne 2 -or @($states | Where-Object { $_.source -in @('com.ibasso.music','com.apple.android.music') -and $_.playbackState -eq 2 -and !$_.playing }).Count -ne 2) { throw 'Both source sessions must be observed paused; no playback command was sent' }
    }
    if ($null -eq $report.elapsedMs) { throw 'Current debug APK with monotonic probe timing is required' }
    $report | Add-Member -NotePropertyName adbRoundTripMs -NotePropertyValue $watch.ElapsedMilliseconds
    return $report
}
function Sample([string]$Label, [string]$Source, [string[]]$Extra = @(), [string]$ExpectedTitle = '') {
    $report = Probe $Label 'BROWSE' $Source $Extra
    $valid = !$report.error -and (!$ExpectedTitle -or $report.data.title -eq $ExpectedTitle)
    $script:rows += [ordered]@{case=$Label;source=$Source;passed=$valid;error=$report.error;title=$report.data.title;rows=$report.data.items.Count;probeElapsedMs=$report.elapsedMs;adbRoundTripMs=$report.adbRoundTripMs}
    ConvertTo-Json -InputObject $script:rows -Depth 5 | Set-Content (Join-Path $out 'samples.json')
    return $report
}
$mango = 'com.ibasso.music'; $apple = 'com.apple.android.music'
# Wake for this source-navigation test; do not alter the device timeout or launch apps
# manually between rounds. The actual adapters must obtain their own foreground window.
& $Adb -s $Serial shell input keyevent 224
& $Adb -s $Serial shell wm dismiss-keyguard
try {
    $before = Probe '00-paused-state' 'GET_STATE' $mango
    if ($before.error) { throw "Source state unavailable: $($before.error)" }
    foreach ($round in 1..$Rounds) {
        if ($StartFromHome) { & $Adb -s $Serial shell input keyevent 3 }
        $appleRoot = Sample "$round-apple-root" $apple @() 'Downloaded'
        if ($appleRoot.error) { $capture = Probe "$round-apple-failed-snapshot" 'SNAPSHOT' $apple }
        $native = Sample "$round-mango-native" $mango
        if ($native.error) { continue }
        $assistedHome = Sample "$round-mango-home" $mango @('--es','parent','mango-ui:root','--es','revision',$native.data.revision) 'Mango local folders'
        if ($assistedHome.error) { continue }
        $entry = @($assistedHome.data.items | Where-Object { $_.title -eq 'Internal Storage' -and $_.browsable })
        if ($entry.Count -ne 1 -or $entry[0].id -notmatch '^[A-Za-z0-9:_-]+$' -or $assistedHome.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid storage identity' }
        if ($StartFromHome) { & $Adb -s $Serial shell input keyevent 3 }
        $storage = Sample "$round-mango-storage" $mango @('--es','parent',$entry[0].id,'--es','revision',$assistedHome.data.revision) 'Internal Storage'
        if ($storage.error) { $capture = Probe "$round-mango-failed-snapshot" 'SNAPSHOT' $mango }
    }
} finally { $final = Probe '99-final-paused-state' 'GET_STATE' $mango }
$failed = @($rows | Where-Object { !$_.passed })
[ordered]@{kind='actual-source-library-switch-no-BLE-no-playback';startFromHome=[bool]$StartFromHome;rounds=$Rounds;samples=$rows.Count;failedSamples=$failed.Count;allPassed=($failed.Count -eq 0 -and $rows.Count -eq 4*$Rounds);bothSourcesPaused=$true;controlSource=$final.state.source;artifactDirectory=$out} | ConvertTo-Json -Depth 5 | Tee-Object (Join-Path $out 'summary.json')
if ($failed.Count -or $rows.Count -ne 4*$Rounds) { throw 'Source library switch check recorded failures; inspect samples.json' }
