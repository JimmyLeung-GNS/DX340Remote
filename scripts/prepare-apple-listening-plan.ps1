param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string]$InventoryDirectory
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$wifi = ((& $Adb -s $Serial shell settings get global wifi_on) -join '').Trim()
$network = (& $Adb -s $Serial shell dumpsys connectivity) -join "`n"
if ($wifi -ne '0' -or $network -notmatch 'Active default network: none') { throw 'Offline preparation requires Wi-Fi off and no active default network; no settings were changed' }
if (!$InventoryDirectory) {
    $latest = Get-ChildItem (Join-Path $repo 'artifacts/apple-browse-inventory') -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (!$latest) { throw 'Run verify-apple-browse-inventory.ps1 first' }
    $InventoryDirectory = $latest.FullName
}
$collections = Get-Content -Raw (Join-Path $InventoryDirectory 'collections.json') | ConvertFrom-Json
if (@($collections | Where-Object category -eq 'Albums').Count -ne 5 -or @($collections | Where-Object category -eq 'Playlists').Count -ne 2) { throw 'Expected five observed albums and two playlists' }
$queue = @(); $offset = 0; $revision = $null
do {
    $tag = [guid]::NewGuid().ToString('N')
    $arguments = @('am','broadcast','-a','org.dx340.remote.host.DEBUG_UI_PROBE','-n','org.dx340.remote.host/.DebugUiProbeReceiver','--es','operation','GET_QUEUE','--es','source','com.apple.android.music','--es','reportTag',$tag,'--ei','offset',[string]$offset)
    if ($revision) {
        if ($revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid queue revision' }
        $arguments += @('--es','revision',$revision)
    }
    & $Adb -s $Serial shell @arguments > (Join-Path $InventoryDirectory "listening-queue-$offset-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw 'Queue request failed' }
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
    if ($LASTEXITCODE -ne 0) { throw 'Queue report missing' }
    $raw | Set-Content (Join-Path $InventoryDirectory "listening-queue-$offset.json")
    $report = ($raw -join "`n") | ConvertFrom-Json
    if ($report.phase -ne 'FINISHED' -or $report.error -or $report.operation -ne 'GET_QUEUE' -or $report.requestedSource -ne 'com.apple.android.music' -or $report.data.offset -ne $offset -or $report.data.total -gt 10000) { throw 'Invalid queue report' }
    foreach ($field in @('sourceStatesBefore','sourceStatesAfter')) {
        $states = @($report.$field)
        if ($states.Count -ne 2 -or @($states | Where-Object { $_.source -in @('com.ibasso.music','com.apple.android.music') -and $_.playbackState -eq 2 -and !$_.playing }).Count -ne 2) { throw 'Both source sessions must be observed paused; no playback command was sent' }
    }
    if ($revision -and $report.data.revision -ne $revision) { throw 'Queue changed during preparation' }
    $revision = $report.data.revision; $queue += @($report.data.items)
    $next = $report.data.nextOffset
    if ($null -ne $next) {
        if ($next -le $offset -or $next -gt 10000) { throw 'Invalid queue continuation' }
        $offset = $next
    }
} while ($null -ne $next)
if ($queue.Count -ne $report.data.total) { throw 'Complete queue did not match reported total' }
$used = @{}; $plan = @(); $playlistIndex = 0
foreach ($collection in $collections) {
    $take = 3
    if ($collection.category -eq 'Playlists') { $playlistIndex++; if ($playlistIndex -eq 2) { $take = 2 } }
    $chosen = @($collection.trackLabels | Sort-Object title,subtitle | Where-Object { !$used.ContainsKey($_.title+'|'+$_.subtitle) } | Select-Object -First $take)
    if ($chosen.Count -ne $take) { throw 'Not enough distinct observed labels for this matrix; inspect the collection before preparing another plan' }
    foreach ($track in $chosen) {
        $used[$track.title+'|'+$track.subtitle] = $true
        $plan += [ordered]@{sequence=$plan.Count+1;source='com.apple.android.music';mode='ui-assisted';collectionKind=$collection.category;collection=$collection.name;trackTitle=$track.title;trackArtistLabel=$track.subtitle;titleAbsentFromCompleteQueueNow=($track.title -notin $queue.title);status='NOT TESTED - candidate metadata only';audibleConfirmation=$null}
    }
}
if ($plan.Count -ne 20 -or !@($plan | Where-Object { $_.titleAbsentFromCompleteQueueNow }).Count) { throw 'Plan needs twenty candidates including one outside the complete queue' }
[ordered]@{kind='pending-listening-plan-no-playback';generatedUtc=[DateTime]::UtcNow.ToString('o');sourceCommit=(git -C $repo rev-parse HEAD).Trim();inventoryDirectory=$InventoryDirectory;completeQueueCount=$queue.Count;queueRevision=$revision;selections=$plan;note='Re-browse and revalidate fresh opaque IDs/revisions and outside-queue status at each actual selection. Labels do not prove playback or stable identity.'} | ConvertTo-Json -Depth 7 | Set-Content (Join-Path $repo 'artifacts/apple-listening-plan.json')
[ordered]@{candidates=$plan.Count;albumCollections=5;playlistCollections=2;distinctTitleArtistLabels=$used.Count;completeQueueCount=$queue.Count;outsideQueueCandidateLabels=@($plan | Where-Object { $_.titleAbsentFromCompleteQueueNow }).Count;playedThisCheck=0} | ConvertTo-Json | Tee-Object (Join-Path $repo 'artifacts/apple-listening-plan-summary.json')
