param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [ValidateSet('com.ibasso.music','com.apple.android.music')][string]$RestoreSource = 'com.ibasso.music'
)
# Requires the running matching debug companion and an existing public Apple queue.
# No BLE, instrumentation, playback, volume or permission changes. Raw reports stay ignored.
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$out = Join-Path $repo 'artifacts/queue-pagination-probes'
New-Item -ItemType Directory -Force $out | Out-Null
function Read-Probe([string]$Label, [string]$Operation, [string]$Source,
    [string[]]$Extra = @(), [string]$ExpectedError = '') {
    $oldRaw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat files/apple-ui-live-test.json 2>&1
    if ($LASTEXITCODE -ne 0) {
        if (($oldRaw -join "`n") -notmatch 'No such file or directory') { throw 'Cannot read previous probe marker' }
        $oldTime = 0 # A fresh debug install need not have a previous report.
    } else { $oldTime = (($oldRaw -join "`n") | ConvertFrom-Json).time }
    $arguments = @('-s',$Serial,'shell','am','broadcast','-a','org.dx340.remote.host.DEBUG_UI_PROBE',
        '-n','org.dx340.remote.host/.DebugUiProbeReceiver','--es','operation',$Operation,'--es','source',$Source) + $Extra
    & $Adb @arguments *> (Join-Path $out "$Label-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw "Broadcast failed: $Label" }
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat files/apple-ui-live-test.json
    if ($LASTEXITCODE -ne 0) { throw "Report read failed: $Label" }
    $path = Join-Path $out "$Label.json"
    $raw | Set-Content $path
    $report = ($raw -join "`n") | ConvertFrom-Json
    if ($report.time -le $oldTime -or $report.operation -ne $Operation -or $report.state.source -ne $Source) {
        throw "Stale or wrong-source report: $Label"
    }
    if ([string]$report.error -ne $ExpectedError) { throw "$Label returned $($report.error), expected $ExpectedError" }
    return $report
}
$before=Read-Probe '00-before-state' 'GET_STATE' $RestoreSource
if (@($before.sourceStatesAfter | Where-Object playing).Count) {throw 'Pause both source apps before this read-only check; no playback action was sent'}
$apple='com.apple.android.music'
$pages=@(); $items=@(); $offset=0; $revision=''
try {
    $baseline=Read-Probe '01-complete-queue-baseline' 'CHECK_APPLE_QUEUE_BASELINE' $apple @('--es','candidate','queue-pagination-fixture-missing')
    if ($baseline.data.count -ne $baseline.data.queue.Count) {throw 'Incomplete baseline'}
    do {
        $extra=if ($offset -eq 0) { @() } else { @('--ei','offset',"$offset",'--es','revision',$revision) }
        $page=Read-Probe ('02-page-{0:D5}' -f $offset) 'GET_QUEUE' $apple $extra
        if ($page.data.revision -notmatch '^[a-f0-9]{64}$') {throw 'Unexpected queue revision encoding'}
        if ($offset -eq 0) {$revision=$page.data.revision}
        if ($page.data.offset -ne $offset -or $page.data.revision -ne $revision -or $page.data.items.Count -gt 50 -or $page.data.total -ne $baseline.data.count) {throw 'Invalid queue window'}
        $bytes=[Text.Encoding]::UTF8.GetByteCount(($page.data | ConvertTo-Json -Depth 10 -Compress))
        if ($bytes -gt 49152) {throw 'Oversized page'}
        $items+=@($page.data.items)
        $pages+=[ordered]@{offset=$offset;rows=$page.data.items.Count;nextOffset=$page.data.nextOffset;total=$page.data.total;serializedBytes=$bytes}
        if ($null -eq $page.data.nextOffset) {break}
        if ($page.data.nextOffset -ne $offset+$page.data.items.Count -or $page.data.nextOffset -le $offset -or $page.data.nextOffset -gt 10000) {throw 'Nonadvancing continuation'}
        $offset=$page.data.nextOffset
    } while ($true)
    if ($items.Count -ne $baseline.data.count) {throw 'Queue count mismatch'}
    for ($index=0;$index -lt $items.Count;$index++) {
        if ($items[$index].id -cne $baseline.data.queue[$index].id -or $items[$index].title -cne $baseline.data.queue[$index].title) {throw "Queue differs from full public baseline at $index"}
    }
    $stale=Read-Probe '03-stale-revision-rejected' 'GET_QUEUE' $apple @('--ei','offset','50','--es','revision','old-fixture-revision') 'STALE_LIBRARY'
    $missing=Read-Probe '04-missing-revision-rejected' 'GET_QUEUE' $apple @('--ei','offset','50') 'STALE_LIBRARY'
    $invalid=Read-Probe '05-invalid-offset-rejected' 'GET_QUEUE' $apple @('--ei','offset','-1','--es','revision',$revision) 'INVALID_ARGUMENT'
    $refreshed=Read-Probe '06-refresh-from-start' 'GET_QUEUE' $apple
    if ($refreshed.data.offset -ne 0 -or $refreshed.data.revision -ne $revision) {throw 'Queue refresh failed'}
} finally {$final=Read-Probe '07-control-target-restored' 'GET_STATE' $RestoreSource}
if (@($final.sourceStatesAfter | Where-Object playing).Count) {throw 'Unexpected playback'}
[ordered]@{kind='actual-DX340-source-queue-pages-no-BLE-no-playback';queueCount=$items.Count;pages=$pages;exactOrderAndTitlesMatchCompletePublicQueue=$true;staleRevisionError=$stale.error;missingRevisionError=$missing.error;invalidOffsetError=$invalid.error;refreshFromStart=$true;bothSourcesPaused=$true;controlSource=$final.state.source} | ConvertTo-Json -Depth 6 | Tee-Object (Join-Path $out 'summary.json')
