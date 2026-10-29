param(
    [Parameter(Mandatory=$true)][ValidateRange(1,20)][int]$Sequence,
    [int]$ExpectedVolume = 37,
    [string]$Serial = 'd',
    [string]$PlanPath,
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
# Deliberate ONE-selection hardware experiment, never a batch playback loop.
# Human listening is recorded separately; this helper cannot assert audible success.
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
if (!$PlanPath) { $PlanPath = Join-Path $repo 'artifacts/apple-listening-plan.json' }
$plan = Get-Content -LiteralPath $PlanPath -Raw | ConvertFrom-Json
$entry = @($plan.selections | Where-Object sequence -eq $Sequence)
if ($entry.Count -ne 1 -or $entry[0].source -ne 'com.apple.android.music') { throw 'Expected exactly one Apple listening candidate' }
$candidate = $entry[0]
$run = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$out = Join-Path $repo "artifacts/apple-listening/$('{0:D2}' -f $Sequence)-$run"
New-Item -ItemType Directory -Force -Path $out | Out-Null
$candidate | ConvertTo-Json | Set-Content (Join-Path $out 'candidate.json')
function Offline {
    $wifi = ((& $Adb -s $Serial shell settings get global wifi_on) -join '').Trim()
    $network = (& $Adb -s $Serial shell dumpsys connectivity) -join "`n"
    if ($wifi -ne '0' -or $network -notmatch 'Active default network: none') { throw 'Offline precondition failed; no network settings changed' }
    $network | Set-Content (Join-Path $out 'connectivity.txt')
}
function Probe([string]$Label, [string]$Operation='BROWSE', [string[]]$Extra=@()) {
    if ($Operation -notin @('BROWSE','GET_STATE','GET_QUEUE','SET_PLAYING','PLAY_ITEM')) { throw 'Operation not allowed' }
    $tag = [guid]::NewGuid().ToString('N')
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation $Operation --es source com.apple.android.music --es reportTag $tag @Extra > (Join-Path $out "$Label-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw "Broadcast failed: $Label" }
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
    if ($LASTEXITCODE -ne 0) { throw "Missing report: $Label" }
    $raw | Set-Content (Join-Path $out "$Label.json")
    $r = ($raw -join "`n") | ConvertFrom-Json
    if ($r.phase -ne 'FINISHED' -or $r.operation -ne $Operation -or $r.requestedSource -ne 'com.apple.android.music' -or $r.error) { throw "Failed $Label ($($r.error))" }
    if ($r.state.volume.current -ne $ExpectedVolume) { throw 'Output level changed; no automatic adjustment sent' }
    return $r
}
function Navigate([string]$Label, $Page, [string]$Id) {
    if ($Id -notmatch '^[A-Za-z0-9:_-]+$' -or $Page.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid opaque identity' }
    Probe $Label 'BROWSE' @('--es','parent',$Id,'--es','revision',$Page.data.revision)
}
function Find-Row([string]$Label, $Page, [string]$Title, [bool]$Playable, [string]$Subtitle='') {
    $expectedHeader = $Page.data.title.Trim()
    for ($n=0; $n -le 12; $n++) {
        if ($Page.data.title.Trim() -cne $expectedHeader) { throw 'Collection/page header changed during navigation' }
        $rows = @($Page.data.items | Where-Object { $_.title.Trim() -ceq $Title.Trim() -and (($_.playable -eq $true) -eq $Playable) -and ($Playable -or $_.browsable) -and (!$Subtitle -or $_.subtitle -ceq $Subtitle) })
        if ($rows.Count -gt 1) { throw 'Ambiguous current row; no selection sent' }
        if ($rows.Count -eq 1) { return @{page=$Page;row=$rows[0]} }
        if ($null -eq $Page.data.nextPage) { break }
        if ($Page.data.parent -notmatch '^[A-Za-z0-9:_-]+$' -or $Page.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid continuation' }
        $Page = Probe "$Label-scroll-$n" 'BROWSE' @('--es','parent',$Page.data.parent,'--es','revision',$Page.data.revision,'--ei','page',[string]$Page.data.nextPage)
    }
    throw "Candidate no longer found: $Label"
}
$installedPath = ((& $Adb -s $Serial shell pm path org.dx340.remote.host) -join '').Trim() -replace '^package:',''
if ($installedPath -notmatch '^/data/app/[A-Za-z0-9_~+./=-]+/base\.apk$') { throw 'Cannot identify installed companion' }
$installedCopy = Join-Path $out 'installed-companion.apk'
& $Adb -s $Serial pull $installedPath $installedCopy | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Installed companion provenance missing' }
$summary = [ordered]@{kind='actual-offline-Apple-own-adapter-selection-no-BLE';sequence=$Sequence;candidate=$candidate;sourceCommit=(git -C $repo rev-parse HEAD).Trim();sourceTreeDirty=[bool](git -C $repo status --porcelain);installedCompanionSha256=(Get-FileHash -LiteralPath $installedCopy).Hash.ToLowerInvariant();helperSha256=(Get-FileHash -LiteralPath $PSCommandPath).Hash.ToLowerInvariant();startedUtc=[DateTime]::UtcNow.ToString('o');artifactDirectory=$out;selectionSent=$false;observedCorrectPlayingState=$false;audibleConfirmation=$null;status='FAILED_OR_INTERRUPTED'}
try {
    Offline
    $before = Probe '00-before' 'GET_STATE'
    if (@($before.sourceStatesAfter | Where-Object { $_.source -eq 'com.ibasso.music' -and $_.playbackState -eq 2 -and !$_.playing }).Count -ne 1) { throw 'Mango must already be paused' }
    # Pause only the preceding Apple clip before browsing the next deliberate selection.
    if ($before.state.playing) { $null = Probe '01-pause-preceding-clip' 'SET_PLAYING' @('--ez','playing','false') }
    $root = Probe '02-downloaded'
    if ($root.data.title -ne 'Downloaded') { throw 'Downloaded root required' }
    $category = Find-Row '03-category' $root $candidate.collectionKind $false
    $collections = Navigate '04-collections' $category.page $category.row.id
    $collection = Find-Row '05-collection' $collections $candidate.collection $false
    $tracks = Navigate '06-tracks' $collection.page $collection.row.id
    if ($tracks.data.title.Trim() -cne $candidate.collection.Trim()) { throw 'Collection header changed' }
    $track = Find-Row '07-track' $tracks $candidate.trackTitle $true $candidate.trackArtistLabel
    $queue = @(); $offset=0; $revision=$null
    do {
        $extra = @('--ei','offset',[string]$offset)
        if ($revision) { if ($revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid queue revision' }; $extra += @('--es','revision',$revision) }
        $q = Probe "08-queue-$offset" 'GET_QUEUE' $extra
        if ($q.data.offset -ne $offset -or $q.data.total -gt 10000 -or ($revision -and $revision -ne $q.data.revision)) { throw 'Queue changed or incomplete' }
        $revision=$q.data.revision; $queue += @($q.data.items); $next=$q.data.nextOffset
        if ($null -ne $next) { if ($next -le $offset -or $next -gt 10000) { throw 'Invalid queue continuation' }; $offset=$next }
    } while ($null -ne $next)
    if ($queue.Count -ne $q.data.total) { throw 'Incomplete queue baseline' }
    $summary.completeQueueCountBefore=$queue.Count
    $summary.titleAbsentFromCompleteQueueBefore=($candidate.trackTitle -cnotin $queue.title)
    Offline
    if ($track.row.id -notmatch '^[A-Za-z0-9:_-]+$' -or $track.page.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid current track identity' }
    $summary.selectionSent=$true
    $selected = Probe '09-select-once' 'PLAY_ITEM' @('--es','item',$track.row.id,'--es','revision',$track.page.data.revision)
    for ($poll=0; $poll -lt 4; $poll++) {
        $seen = Probe "10-observation-$poll" 'GET_STATE'
        if ($seen.state.title -ceq $candidate.trackTitle -and $seen.state.playing -eq $true) { break }
        Start-Sleep -Milliseconds 500
    }
    if ($seen.state.title -cne $candidate.trackTitle -or !$seen.state.playing -or @($seen.sourceStatesAfter | Where-Object { $_.source -eq 'com.ibasso.music' -and $_.playbackState -eq 2 -and !$_.playing }).Count -ne 1) { throw 'Requested Apple title/playing and Mango-paused state not observed; no playback retry' }
    $summary.observedCorrectPlayingState=$true
    $summary.observedTitle=$seen.state.title; $summary.observedArtist=$seen.state.artist
    $summary.observedUtc=[DateTime]::UtcNow.ToString('o'); $summary.status='AWAITING_HUMAN_LISTENING_CONFIRMATION'
    Offline
} catch {
    $summary.failure = $_.Exception.Message
    throw
} finally {
    $summary | ConvertTo-Json -Depth 7 | Set-Content (Join-Path $out 'summary.json')
}
$summary | ConvertTo-Json -Depth 7
