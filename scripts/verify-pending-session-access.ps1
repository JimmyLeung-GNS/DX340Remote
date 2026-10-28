param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
# Development test: revokes only this companion's initially granted notification listener.
# Restores that grant in finally, verifies the complete original listener set, never plays music.
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$out = Join-Path $repo 'artifacts/pending-session-access'
New-Item -ItemType Directory -Force $out | Out-Null
$component = 'org.dx340.remote.host/org.dx340.remote.host.SessionListener'
$mango = 'com.ibasso.music'; $apple = 'com.apple.android.music'
function Listener-Set {
    $raw = & $Adb -s $Serial shell settings get secure enabled_notification_listeners
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read listener settings' }
    @((($raw -join '').Trim() -split ':') | Where-Object { $_ -and $_ -ne 'null' } | Sort-Object -Unique)
}
function Restore-Listener([string[]]$Original) {
    & $Adb -s $Serial shell cmd notification allow_listener $component > (Join-Path $out 'restore-command.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Own playback-session access could not be restored' }
    $watch = [Diagnostics.Stopwatch]::StartNew()
    do {
        $current = @(Listener-Set)
        if (($current -join ':') -ceq ($Original -join ':')) { return }
        Start-Sleep -Milliseconds 100
    } while ($watch.ElapsedMilliseconds -lt 5000)
    throw 'Listener settings differ after restoration; inspect before continuing'
}
function Read-Report([string]$Label, [string]$Tag, [string]$Operation, [string]$Source, [string]$ExpectedError = '') {
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$Tag.json"
    if ($LASTEXITCODE -ne 0) { throw "Missing report: $Label" }
    $raw | Set-Content (Join-Path $out "$Label.json")
    $report = ($raw -join "`n") | ConvertFrom-Json
    if ($report.phase -ne 'FINISHED' -or $report.operation -ne $Operation -or $report.requestedSource -ne $Source) { throw "Wrong/unfinished report: $Label" }
    if ([string]$report.error -ne $ExpectedError) { throw "$Label returned $($report.error), expected $ExpectedError" }
    return $report
}
function Probe([string]$Label, [string]$Operation, [string]$Source, [string[]]$Extra = @(), [string]$ExpectedError = '') {
    $tag = [guid]::NewGuid().ToString('N')
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation $Operation --es source $Source --es reportTag $tag @Extra > (Join-Path $out "$Label-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw "Broadcast failed: $Label" }
    Read-Report $Label $tag $Operation $Source $ExpectedError
}
function Assert-Paused($Report) {
    if (@($Report.sourceStatesAfter).Count -ne 2 -or @($Report.sourceStatesAfter | Where-Object playing).Count) { throw 'Both source sessions must be present and paused' }
}
function Prepare([string]$Label, [string]$Source) {
    if ($Source -eq $mango) {
        & $Adb -s $Serial shell am start -W -n com.ibasso.music/.activities.MainActivity > (Join-Path $out "$Label-launch.txt")
        Start-Sleep -Milliseconds 500
    }
    $root = Probe "$Label-root" 'BROWSE' $Source
    if ($Source -eq $mango) {
        if ($root.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid root revision' }
        $root = Probe "$Label-home" 'BROWSE' $Source @('--es','parent','mango-ui:root','--es','revision',$root.data.revision)
    }
    Assert-Paused $root
    $title = if ($Source -eq $apple) { 'Songs' } else { 'Internal Storage' }
    $item = @($root.data.items | Where-Object { $_.title -eq $title -and $_.browsable })
    if ($item.Count -ne 1 -or $item[0].id -notmatch '^[A-Za-z0-9:_-]+$' -or $root.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Missing or invalid browse token' }
    [pscustomobject]@{args=@('--es','parent',$item[0].id,'--es','revision',$root.data.revision);title=$title;epoch=$root.state.sourceEpoch}
}
$originalListeners = @(Listener-Set)
if ($component -notin $originalListeners) { throw 'Own listener is not initially granted; no setting changed' }
$originalListeners | ConvertTo-Json | Set-Content (Join-Path $out 'listeners-before.json')
$pidBefore = ((& $Adb -s $Serial shell pidof org.dx340.remote.host) -join '').Trim()
if (!$pidBefore) { throw 'Start the matching debug companion first' }
& $Adb -s $Serial shell input keyevent 224
& $Adb -s $Serial shell wm dismiss-keyguard
Assert-Paused (Probe '00-before-state' 'GET_STATE' $mango)
$results = @()
try {
    foreach ($source in @($apple,$mango)) {
        $label = if ($source -eq $apple) { 'apple' } else { 'mango' }
        $prepared = Prepare "$label-prepare" $source
        $tag = [guid]::NewGuid().ToString('N')
        $broadcastArgs = @('am','broadcast','-a','org.dx340.remote.host.DEBUG_UI_PROBE','-n','org.dx340.remote.host/.DebugUiProbeReceiver','--es','operation','BROWSE','--es','source',$source,'--es','reportTag',$tag) + $prepared.args
        $watch = [Diagnostics.Stopwatch]::StartNew()
        try {
            # Fixed package/component and validated opaque tokens only; no arbitrary shell text.
            $command = ($broadcastArgs -join ' ') + " > /dev/null & sleep 0.25; run-as org.dx340.remote.host cat files/debug-probe-$tag.json; cmd notification disallow_listener $component > /dev/null; wait"
            $pending = & $Adb -s $Serial shell $command
            $watch.Stop()
            if ($LASTEXITCODE -ne 0) { throw 'Concurrent revocation command failed' }
            $pending | Set-Content (Join-Path $out "$label-before-revocation.json")
            if ((($pending -join "`n") | ConvertFrom-Json).phase -ne 'STARTED') { throw 'Browse was not pending at revocation' }
            $interrupted = Read-Report "$label-interrupted" $tag 'BROWSE' $source 'AUTHORIZATION_DENIED'
            $revoked = @(Listener-Set)
            $revoked | ConvertTo-Json | Set-Content (Join-Path $out "$label-listeners-revoked.json")
            if ($component -in $revoked -or ($revoked -join ':') -cne (($originalListeners | Where-Object { $_ -ne $component }) -join ':')) { throw 'Unexpected listener state during revocation' }
            if ($interrupted.state.accessError -ne 'NOTIFICATION_ACCESS_REQUIRED' -or $interrupted.state.sourceEpoch -eq $prepared.epoch -or @($interrupted.state.capabilities.PSObject.Properties | Where-Object { $_.Name.StartsWith('can') -and $_.Value -eq $true }).Count) { throw 'Access loss/capability/epoch invalidation failed' }
            if ($interrupted.data -or $interrupted.state.title -ne 'No active track' -or @($interrupted.sourceStatesAfter).Count) { throw 'Source data remained exposed while access was revoked' }
            $denied = Probe "$label-new-request-denied" 'BROWSE' $source @() 'PERMISSION_REVOKED'
            & $Adb -s $Serial shell dumpsys media_session > (Join-Path $out "$label-media-session-revoked.txt")
        } finally { Restore-Listener $originalListeners }
        $recoveryWatch = [Diagnostics.Stopwatch]::StartNew()
        do {
            $restored = Probe "$label-restored-state" 'GET_STATE' $source
            if (!$restored.state.accessError -and @($restored.sourceStatesAfter).Count -eq 2) { break }
            Start-Sleep -Milliseconds 100
        } while ($recoveryWatch.ElapsedMilliseconds -lt 5000)
        if ($restored.state.accessError) { throw 'Source access did not automatically recover' }
        Assert-Paused $restored
        $stale = Probe "$label-old-token" 'BROWSE' $source $prepared.args 'STALE_LIBRARY'
        $fresh = Prepare "$label-recovery" $source
        $page = Probe "$label-fresh-page" 'BROWSE' $source $fresh.args
        if ($page.data.title -ne $fresh.title -or $page.data.items.Count -lt 1) { throw 'Fresh assisted browse did not recover' }
        $queue = Probe "$label-native-queue-recovered" 'GET_QUEUE' $source
        Assert-Paused $queue
        $results += [ordered]@{source=$source;pendingError=$interrupted.error;elapsedMs=$watch.ElapsedMilliseconds;newRequestError=$denied.error;capabilitiesDisabled=$true;sourceMetadataCleared=$true;sourceEpochChanged=$true;oldTokenError=$stale.error;recoveredTitle=$page.data.title;recoveredRows=$page.data.items.Count;recoveredQueueRows=$queue.data.items.Count;listenerSetRestored=$true;bothSourcesPausedBeforeAndAfter=$true}
    }
} finally {
    Restore-Listener $originalListeners
    $final = Probe '99-final-state' 'GET_STATE' $mango
}
Assert-Paused $final
$pidAfter = ((& $Adb -s $Serial shell pidof org.dx340.remote.host) -join '').Trim()
if ($pidBefore -ne $pidAfter) { throw 'Companion process restarted during access checks' }
[ordered]@{kind='actual-pending-source-browse-with-own-session-access-revoke-restore-no-BLE';cases=$results;companionPidUnchanged=$true;listenerSetRestored=$true;bothSourcesPaused=$true;controlSource=$final.state.source} | ConvertTo-Json -Depth 6 | Tee-Object (Join-Path $out 'summary.json')
