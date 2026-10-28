param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$out = Join-Path $repo 'artifacts/assisted-interruptions'
New-Item -ItemType Directory -Force $out | Out-Null
$apple = 'com.apple.android.music'; $mango = 'com.ibasso.music'
$results = @()
function Wake-Target {
    & $Adb -s $Serial shell input keyevent 224
    & $Adb -s $Serial shell wm dismiss-keyguard
}
function Probe([string]$Label, [string]$Operation, [string]$Source, [string[]]$Extra = @(),
    [string]$ExpectedError = '', [switch]$SleepDuring) {
    # Only fixed operations, package IDs and validated opaque tokens go into Android shell commands.
    $tag = [guid]::NewGuid().ToString('N')
    $broadcastArgs = @('am','broadcast','-a','org.dx340.remote.host.DEBUG_UI_PROBE','-n','org.dx340.remote.host/.DebugUiProbeReceiver',
        '--es','operation',$Operation,'--es','source',$Source,'--es','reportTag',$tag) + $Extra
    $watch = [Diagnostics.Stopwatch]::StartNew()
    if ($SleepDuring) {
        $command = ($broadcastArgs -join ' ') + " > /dev/null & sleep 0.25; run-as org.dx340.remote.host cat files/debug-probe-$tag.json; input keyevent 223; wait"
        $pending = & $Adb -s $Serial shell $command
        if ($LASTEXITCODE -ne 0) { throw "Sleep probe failed: $Label" }
        $pending | Set-Content (Join-Path $out "$Label-before-sleep.json")
        if ((($pending -join "`n") | ConvertFrom-Json).phase -ne 'STARTED') { throw 'Browse was not pending at the sleep trigger' }
        & $Adb -s $Serial shell dumpsys power > (Join-Path $out "$Label-power.txt")
    } else {
        & $Adb -s $Serial shell @broadcastArgs > (Join-Path $out "$Label-broadcast.txt")
        if ($LASTEXITCODE -ne 0) { throw "Broadcast failed: $Label" }
    }
    $watch.Stop()
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
    if ($LASTEXITCODE -ne 0) { throw "Missing report: $Label" }
    $raw | Set-Content (Join-Path $out "$Label.json")
    if ($SleepDuring) {
        $power = Get-Content (Join-Path $out "$Label-power.txt") -Raw
        if ($power -notmatch 'mWakefulness=(Asleep|Dozing)') { throw 'Screen-off/dozing state was not observed' }
    }
    $report = ($raw -join "`n") | ConvertFrom-Json
    if ($report.phase -ne 'FINISHED' -or $report.operation -ne $Operation -or $report.requestedSource -ne $Source) { throw "Wrong/unfinished report: $Label" }
    if ([string]$report.error -ne $ExpectedError) { throw "$Label returned $($report.error), expected $ExpectedError" }
    if (@($report.sourceStatesAfter | Where-Object playing).Count) { throw 'Unexpected playback; stopped checks without sending a playback command' }
    $report | Add-Member -NotePropertyName probeElapsedMs -NotePropertyValue $watch.ElapsedMilliseconds
    return $report
}
function Token-Args($Page, $Item) {
    if ($Item.id -notmatch '^[A-Za-z0-9:_-]+$' -or $Page.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid opaque browse token' }
    return @('--es','parent',$Item.id,'--es','revision',$Page.revision)
}
function Prepare([string]$Label, [string]$Source) {
    if ($Source -eq $mango) {
        & $Adb -s $Serial shell am start -W -n com.ibasso.music/.activities.MainActivity > (Join-Path $out "$Label-launch.txt")
        Start-Sleep -Milliseconds 500
    }
    $root = Probe "$Label-root" 'BROWSE' $Source
    if ($Source -eq $mango) {
        if ($root.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid native root revision' }
        $root = Probe "$Label-home" 'BROWSE' $Source @('--es','parent','mango-ui:root','--es','revision',$root.data.revision)
    }
    $title = if ($Source -eq $apple) { 'Songs' } else { 'Internal Storage' }
    $item = @($root.data.items | Where-Object { $_.title -eq $title -and $_.browsable })
    if ($item.Count -ne 1) { throw "Missing $title entry" }
    return [pscustomobject]@{args = @(Token-Args $root.data $item[0]); title = $title}
}
Wake-Target
$before = Probe '00-before-state' 'GET_STATE' $mango
try {
    foreach ($source in @($apple,$mango)) {
        $name = if ($source -eq $apple) { 'apple' } else { 'mango' }
        foreach ($kind in @('sleep','source-change')) {
            $label = "$name-$kind"
            Wake-Target
            $prepared = Prepare "$label-prepare" $source
            if ($kind -eq 'sleep') {
                try { $interrupted = Probe "$label-interrupted" 'BROWSE' $source $prepared.args 'UI_SCREEN_UNLOCK_REQUIRED' -SleepDuring }
                finally { Wake-Target }
            } else {
                $other = if ($source -eq $apple) { $mango } else { $apple }
                $extra = $prepared.args + @('--ei','changeSourceAfterMs','200','--es','changeSourceTo',$other)
                $interrupted = Probe "$label-interrupted" 'BROWSE' $source $extra 'AUTHORIZATION_DENIED'
                if ($interrupted.interruption.to -ne $other -or $interrupted.state.source -ne $other) { throw 'No source change while request pending' }
            }
            $stale = Probe "$label-old-token" 'BROWSE' $source $prepared.args 'STALE_LIBRARY'
            $fresh = Prepare "$label-recovery" $source
            $recovered = Probe "$label-fresh-page" 'BROWSE' $source $fresh.args
            if ($recovered.data.title -ne $fresh.title -or $recovered.data.items.Count -lt 1) { throw 'Fresh browse did not recover' }
            $results += [ordered]@{
                source=$source;interruption=$kind;error=$interrupted.error;elapsedMs=$interrupted.probeElapsedMs
                staleTokenError=$stale.error;recoveredTitle=$recovered.data.title;recoveredRows=$recovered.data.items.Count
                bothSourcesPaused=$true
            }
        }
    }
} finally {
    Wake-Target
    $final = Probe '99-final-state' 'GET_STATE' $mango
}
$results | ConvertTo-Json -Depth 5 | Tee-Object (Join-Path $out 'summary.json')
