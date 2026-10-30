param(
    [Parameter(Mandatory=$true)][string]$ExpectedCompanionSha256,
    [string]$Serial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string]$Playlist='DX340 Remote pagination verification',
    [ValidateRange(1,500)][int]$ExpectedTracks=30
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
$out=Join-Path $repo ('artifacts/mango-playlist-pagination-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
$ctx=New-MangoProbeContext $out $Serial $Adb $false 37
$summary=[ordered]@{kind='actual-source-local-read-only-playlist-pagination-no-BLE';sourceCommit=(git -C $repo rev-parse HEAD).Trim();sourceTreeDirty=[bool](git -C $repo status --porcelain);expectedCompanionSha256=$ExpectedCompanionSha256;playlist=$Playlist;expectedTracks=$ExpectedTracks;status='FAILED_OR_INTERRUPTED';pages=@();rows=@()}
$summary.helperSha256=(Get-FileHash $PSCommandPath).Hash.ToLowerInvariant()
$summary.commonHelperSha256=(Get-FileHash (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')).Hash.ToLowerInvariant()
$seen=@{}
try {
    if($ExpectedCompanionSha256 -notmatch '^[a-f0-9]{64}$'){throw 'Expected exact installed APK hash'}
    $path=((& $Adb -s $Serial shell pm path org.dx340.remote.host)-replace '^package:','').Trim()
    & $Adb -s $Serial pull $path (Join-Path $out 'installed.apk') > (Join-Path $out 'pull.txt')
    if($LASTEXITCODE -ne 0 -or (Get-FileHash (Join-Path $out 'installed.apk')).Hash.ToLowerInvariant() -cne $ExpectedCompanionSha256){throw 'Installed companion differs'}
    Assert-MangoOffline $ctx
    $null=Invoke-MangoProbe $ctx GET_STATE
    $list=Invoke-MangoProbe $ctx BROWSE @('--es','parent','mango-ui:playlists')
    $entry=@($list.data.items|Where-Object{$_.title -ceq $Playlist -and $_.browsable -and !$_.playable})
    if($entry.Count -ne 1){throw 'Expected unique existing fixture; no test-data editing is performed'}
    $page=Invoke-MangoNavigate $ctx $list $entry[0].id
    $ended=$false
    for($number=0;$number -lt 20;$number++) {
        if(!$page.data.uiAssisted -or $page.data.title -cne $Playlist -or $page.data.page -ne $number){throw 'Playlist context/page changed'}
        $tag='pagination_'+[guid]::NewGuid().ToString('N').Substring(0,10)
        & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation SNAPSHOT --es source com.ibasso.music --es reportTag $tag > (Join-Path $out "snapshot-$number-broadcast.txt")
        $raw=& $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
        $raw|Set-Content (Join-Path $out "snapshot-$number.json")
        $snapshot=($raw -join "`n")|ConvertFrom-Json
        if($snapshot.phase -cne 'FINISHED' -or $snapshot.error -or @($snapshot.sourceStatesAfter|Where-Object playing).Count -or $snapshot.state.volume.current -ne 37){throw 'Expected paused settled snapshot'}
        $nodes=$snapshot.data.nodes
        if(@($nodes|Where-Object{$_.id -like '*/item_name_tv' -and $_.text -ceq $Playlist}).Count -ne 1 -or @($nodes|Where-Object{$_.id -like '*/music_count_tv' -and $_.text -ceq "total $ExpectedTracks songs"}).Count -ne 1){throw 'Live title/count missing or changed'}
        $positions=@($nodes|Where-Object{$_.id -like '*/music_item_position'})
        $pageRows=@()
        foreach($position in $positions) {
            $rowPath=$position.path.Substring(0,$position.path.LastIndexOf('.'))+'.'
            $title=@($nodes|Where-Object{$_.path.StartsWith($rowPath,[StringComparison]::Ordinal) -and $_.id -like '*/music_item_title'})
            $artist=@($nodes|Where-Object{$_.path.StartsWith($rowPath,[StringComparison]::Ordinal) -and $_.id -like '*/music_item_artist'})
            $index=0
            if(![int]::TryParse($position.text,[ref]$index) -or $index -notin 1..$ExpectedTracks -or $title.Count -gt 1 -or $artist.Count -gt 1){throw 'Ambiguous source track position'}
            # Captured boundary rows may expose a position but not a full title/artist.
            # They are excluded by the production parser and cannot be selection actions.
            if($title.Count -eq 0 -or $artist.Count -eq 0){continue}
            $record=[ordered]@{position=$index;title=$title[0].text;artist=$artist[0].text}
            if($seen.ContainsKey($index) -and ($seen[$index].title -cne $record.title -or $seen[$index].artist -cne $record.artist)){throw 'Track identity changed across visible pages'}
            $seen[$index]=$record; $pageRows+=$record
        }
        if($pageRows.Count -ne @($page.data.items).Count){throw 'Complete source positions do not match returned rows'}
        for($i=0;$i -lt $pageRows.Count;$i++) {
            if($pageRows[$i].title -cne $page.data.items[$i].title -or $pageRows[$i].artist -cne $page.data.items[$i].subtitle -or !$page.data.items[$i].playable -or $page.data.items[$i].browsable){throw 'Returned row differs from complete source identity'}
        }
        $summary.pages+=([ordered]@{page=$number;positions=@($pageRows.position);nextPage=$page.data.nextPage})
        $page|ConvertTo-Json -Depth 12|Set-Content (Join-Path $out "page-$number.json")
        if($null -eq $page.data.nextPage){$ended=$true;break}
        $page=Invoke-MangoNavigate $ctx $page $page.data.parent ([int]$page.data.nextPage)
    }
    if(!$ended -or $seen.Count -ne $ExpectedTracks -or @(1..$ExpectedTracks|Where-Object{!$seen.ContainsKey($_)}).Count){throw 'Did not expose every expected track and explicit end'}
    $null=Invoke-MangoProbe $ctx GET_STATE
    $summary.status='ALL_POSITIONS_AND_END_VERIFIED_READ_ONLY'
} finally {
    $summary.rows=@($seen.Values|Sort-Object {[int]$_.position})
    $summary.requestCount=$ctx.RequestCount
    $summary.completedUtc=[DateTime]::UtcNow.ToString('o')
    $summary|ConvertTo-Json -Depth 10|Set-Content (Join-Path $out 'summary.json')
    Write-Output (Join-Path $out 'summary.json')
}
