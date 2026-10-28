param([string]$Serial = 'd', [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe")
# Temporarily removes one already-granted companion browsing service, then restores it.
# The other service and global Accessibility flag stay enabled; no playback/volume command.
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$out = Join-Path $repo 'artifacts/pending-accessibility'
New-Item -ItemType Directory -Force $out | Out-Null
$mango = 'com.ibasso.music'; $apple = 'com.apple.android.music'
$components = @{
    $apple = 'org.dx340.remote.host/org.dx340.remote.host.AppleMusicAccessibilityService'
    $mango = 'org.dx340.remote.host/org.dx340.remote.host.MangoAccessibilityService'
}
$labels = @{$apple='DX340 Apple Music browsing'; $mango='DX340 Mango folder browsing'}
function Secure-Value([string]$Key) {
    $raw = & $Adb -s $Serial shell settings get secure $Key
    if ($LASTEXITCODE -ne 0) { throw "Cannot read $Key" }
    ($raw -join '').Trim()
}
function Service-Set {
    @((Secure-Value 'enabled_accessibility_services') -split ':' | Where-Object { $_ -and $_ -ne 'null' } | Sort-Object -Unique)
}
function Write-Services([string[]]$Services) {
    if ($Services.Count -lt 1 -or @($Services | Where-Object { $_ -notmatch '^[A-Za-z0-9_./-]+$' }).Count) { throw 'Unsafe or empty service set; no setting written' }
    & $Adb -s $Serial shell settings put secure enabled_accessibility_services ($Services -join ':')
    if ($LASTEXITCODE -ne 0) { throw 'Could not write service setting' }
}
function Restore-Service([string]$Component) {
    $current = @(Service-Set)
    if ($Component -notin $current) { Write-Services @($current + $Component | Sort-Object -Unique) }
    $watch = [Diagnostics.Stopwatch]::StartNew()
    do {
        $restored = @(Service-Set)
        if (($restored -join ':') -ceq ($original -join ':')) { break }
        Start-Sleep -Milliseconds 100
    } while ($watch.ElapsedMilliseconds -lt 5000)
    if (($restored -join ':') -cne ($original -join ':') -or (Secure-Value 'accessibility_enabled') -ne $globalBefore) { throw 'Accessibility baseline was not restored' }
}
function Assert-Bound([string]$Label, [string]$AbsentSource = '') {
    $raw = & $Adb -s $Serial shell dumpsys accessibility
    $raw | Set-Content (Join-Path $out "$Label-accessibility.txt")
    $text = $raw -join "`n"
    $match = [regex]::Match($text, 'Bound services:\{([\s\S]*?)\}\s*Enabled services:')
    if (!$match.Success -or $text -notmatch 'Crashed services:\{\}') { throw 'Unrecognized or crashed Accessibility state' }
    foreach ($source in @($apple,$mango)) {
        $present = $match.Groups[1].Value.Contains($labels[$source])
        if ($present -ne ($source -ne $AbsentSource)) { throw "Unexpected binding state for $source" }
    }
}
function Read-Report([string]$Label, [string]$Tag, [string]$Operation, [string]$Source, [string]$ExpectedError = '') {
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$Tag.json"
    if ($LASTEXITCODE -ne 0) { throw 'Missing debug report' }
    $raw | Set-Content (Join-Path $out "$Label.json")
    $report = ($raw -join "`n") | ConvertFrom-Json
    if ($report.phase -ne 'FINISHED' -or $report.operation -ne $Operation -or $report.requestedSource -ne $Source -or [string]$report.error -ne $ExpectedError) { throw "$Label returned $($report.error), expected $ExpectedError" }
    if (@($report.sourceStatesAfter).Count -ne 2 -or @($report.sourceStatesAfter | Where-Object playing).Count) { throw 'Source sessions are not both paused' }
    return $report
}
function Probe([string]$Label, [string]$Operation, [string]$Source, [string[]]$Extra = @(), [string]$ExpectedError = '') {
    $tag = [guid]::NewGuid().ToString('N')
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation $Operation --es source $Source --es reportTag $tag @Extra > (Join-Path $out "$Label-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw 'Broadcast failed' }
    Read-Report $Label $tag $Operation $Source $ExpectedError
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
    $title = if ($Source -eq $apple) { 'Songs' } else { 'Internal Storage' }
    $item = @($root.data.items | Where-Object { $_.title -eq $title -and $_.browsable })
    if ($item.Count -ne 1 -or $item[0].id -notmatch '^[A-Za-z0-9:_-]+$' -or $root.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid UI navigation token' }
    [pscustomobject]@{args=@('--es','parent',$item[0].id,'--es','revision',$root.data.revision);title=$title;caps=$root.state.capabilities}
}
$original = @(Service-Set)
$globalBefore = Secure-Value 'accessibility_enabled'
$notificationsBefore = Secure-Value 'enabled_notification_listeners'
if ($globalBefore -ne '1' -or $components[$apple] -notin $original -or $components[$mango] -notin $original -or @($original | Where-Object { $_ -notmatch '^[A-Za-z0-9_./-]+$' }).Count) { throw 'Both existing companion grants are required; no setting changed' }
$original | ConvertTo-Json | Set-Content (Join-Path $out 'services-before.json')
Assert-Bound '00-before'
$pidBefore = ((& $Adb -s $Serial shell pidof org.dx340.remote.host) -join '').Trim()
if (!$pidBefore) { throw 'Start the matching debug companion first' }
& $Adb -s $Serial shell input keyevent 224
& $Adb -s $Serial shell wm dismiss-keyguard
$before = Probe '00-before-state' 'GET_STATE' $mango
$results = @()
try {
    foreach ($source in @($apple,$mango)) {
        $label = if ($source -eq $apple) { 'apple' } else { 'mango' }
        $prepared = Prepare "$label-prepare" $source
        $component = $components[$source]
        $reduced = @($original | Where-Object { $_ -ne $component })
        $tag = [guid]::NewGuid().ToString('N')
        $broadcastArgs = @('am','broadcast','-a','org.dx340.remote.host.DEBUG_UI_PROBE','-n','org.dx340.remote.host/.DebugUiProbeReceiver','--es','operation','BROWSE','--es','source',$source,'--es','reportTag',$tag) + $prepared.args
        try {
            if (((Service-Set) -join ':') -cne ($original -join ':')) { throw 'Service baseline changed before test' }
            $watch = [Diagnostics.Stopwatch]::StartNew()
            $command = ($broadcastArgs -join ' ') + " > /dev/null & sleep 0.25; run-as org.dx340.remote.host cat files/debug-probe-$tag.json; settings put secure enabled_accessibility_services '$($reduced -join ':')'; wait"
            $pending = & $Adb -s $Serial shell $command
            $watch.Stop()
            if ($LASTEXITCODE -ne 0) { throw 'Concurrent revocation failed' }
            $pending | Set-Content (Join-Path $out "$label-before-revoke.json")
            if ((($pending -join "`n") | ConvertFrom-Json).phase -ne 'STARTED') { throw 'Browse was not pending at revocation' }
            $interrupted = Read-Report "$label-interrupted" $tag 'BROWSE' $source 'UI_PERMISSION_REQUIRED'
            if (((Service-Set) -join ':') -cne ($reduced -join ':') -or (Secure-Value 'accessibility_enabled') -ne $globalBefore) { throw 'Unexpected Accessibility configuration change' }
            Assert-Bound "$label-revoked" $source
            if ($interrupted.state.accessError) { throw 'Playback-session access was unexpectedly lost' }
            foreach ($cap in @('canPlay','canPause','canNext','canPrevious','canSeek','canReadQueue','canPlayQueueItem')) {
                if ($interrupted.state.capabilities.$cap -ne $prepared.caps.$cap) { throw "Ordinary session capability changed: $cap" }
            }
            if ($source -eq $apple) {
                if ($interrupted.state.capabilities.canBrowse -or $interrupted.state.capabilities.canPlayItem -or $interrupted.state.capabilities.canPlayNativeItem) { throw 'Unavailable Apple item path still advertised' }
                $deniedArgs = @()
            } else {
                if ($interrupted.state.capabilities.canBrowseFolders -or !$interrupted.state.capabilities.canBrowse -or $interrupted.state.capabilities.canPlayNativeItem -ne $prepared.caps.canPlayNativeItem) { throw 'Mango native/UI capabilities were not separated' }
                $native = Probe "$label-native-root-while-revoked" 'BROWSE' $source
                $folder = @($native.data.items | Where-Object id -eq 'mango-ui:root')
                if ($folder.Count -ne 1 -or $folder[0].browsable -or $folder[0].playable) { throw 'Unavailable assisted folder entry stayed actionable' }
                $deniedArgs = @('--es','parent','mango-ui:root')
            }
            $denied = Probe "$label-new-ui-request-denied" 'BROWSE' $source $deniedArgs 'UI_PERMISSION_REQUIRED'
            $queue = Probe "$label-native-queue-while-revoked" 'GET_QUEUE' $source
        } finally { Restore-Service $component }
        $recoveryWatch = [Diagnostics.Stopwatch]::StartNew()
        do {
            $restored = Probe "$label-restored-state" 'GET_STATE' $source
            $ready = if ($source -eq $apple) { $restored.state.capabilities.canBrowse } else { $restored.state.capabilities.canBrowseFolders }
            if ($ready) { break }
            Start-Sleep -Milliseconds 100
        } while ($recoveryWatch.ElapsedMilliseconds -lt 5000)
        if (!$ready) { throw 'Browsing service did not recover' }
        Assert-Bound "$label-restored"
        $stale = Probe "$label-old-token" 'BROWSE' $source $prepared.args 'STALE_LIBRARY'
        $fresh = Prepare "$label-recovery" $source
        $page = Probe "$label-fresh-page" 'BROWSE' $source $fresh.args
        if ($page.data.title -ne $fresh.title -or $page.data.items.Count -lt 1) { throw 'Fresh browse did not recover' }
        $results += [ordered]@{source=$source;pendingError=$interrupted.error;elapsedMs=$watch.ElapsedMilliseconds;newUiRequestError=$denied.error;nativeSessionCapabilitiesPreserved=$true;nativeQueueRowsWhileUiRevoked=$queue.data.items.Count;oldTokenError=$stale.error;recoveredTitle=$page.data.title;recoveredRows=$page.data.items.Count;originalGrantSetRestored=$true;bothSourcesPaused=$true}
    }
} finally {
    # Restore every originally granted companion service before validating the whole set,
    # even if an earlier per-case restore failed before reaching the second component.
    $current = @(Service-Set)
    $owned = @($components.Values | Where-Object { $_ -in $original })
    $merged = @($current + $owned | Sort-Object -Unique)
    if (($merged -join ':') -cne ($current -join ':')) { Write-Services $merged }
    foreach ($component in $components.Values) { Restore-Service $component }
    $final = Probe '99-final-state' 'GET_STATE' $mango
}
if (((& $Adb -s $Serial shell pidof org.dx340.remote.host) -join '').Trim() -ne $pidBefore) { throw 'Host process restarted' }
if ((Secure-Value 'enabled_notification_listeners') -ne $notificationsBefore) { throw 'Notification listener setting changed' }
@(Service-Set) | ConvertTo-Json | Set-Content (Join-Path $out 'services-after.json')
Assert-Bound '99-final'
[ordered]@{kind='actual-pending-browse-with-own-Accessibility-grant-revoke-restore-no-BLE';cases=$results;companionPidUnchanged=$true;globalAccessibilityFlagUnchanged=$true;notificationAccessUnchanged=$true;bothSourcesPaused=$true;controlSource=$final.state.source} | ConvertTo-Json -Depth 6 | Tee-Object (Join-Path $out 'summary.json')
