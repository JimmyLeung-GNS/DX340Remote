param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [int]$ExpectedVolume = 37,
    [ValidateRange(1,100)][int]$MaxAlbums = 100
)
# Actual source metadata only. Never sends playback, volume or settings operations.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo = Split-Path $PSScriptRoot -Parent
$run = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$out = Join-Path $repo "artifacts/mango-native-inventory/$run"
$probe = New-MangoProbeContext $out $Serial $Adb $false $ExpectedVolume
$groups = [Collections.Generic.List[object]]::new()
$revision = $null
function Read-NativePages([string]$Parent) {
    if ($Parent -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid native parent identity' }
    $rows = [Collections.Generic.List[object]]::new()
    $ids = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    $pages = [Collections.Generic.List[object]]::new()
    for ($number = 0; $number -lt 100; $number++) {
        $r = Invoke-MangoProbe $probe 'BROWSE' @('--es','parent',$Parent,'--es','revision',$revision,'--ei','page',[string]$number)
        $page = $r.data
        if ($page.parent -cne $Parent -or $page.revision -cne $revision -or $page.page -ne $number -or $page.uiAssisted) { throw 'Native page identity or source revision changed' }
        foreach ($row in $page.items) {
            if (!$row.id -or !$ids.Add([string]$row.id) -or $row.uiAssisted) { throw 'Duplicate or invalid native row identity within parent' }
            $rows.Add($row)
        }
        $pages.Add([ordered]@{page=$number;rows=@($page.items).Count;nextPage=$page.nextPage;cached=$page.cached;elapsedMs=$r.elapsedMs})
        if ($null -eq $page.nextPage) {
            return [pscustomobject]@{parent=$Parent;items=@($rows.ToArray());pages=@($pages.ToArray());complete=$true}
        }
        if ($page.nextPage -ne ($number + 1) -or @($page.items).Count -eq 0) { throw 'Invalid or non-advancing native continuation' }
    }
    throw 'Native page bound reached; inventory incomplete'
}
try {
    Assert-MangoOffline $probe
    $null = Invoke-MangoProbe $probe 'GET_STATE'
    $root = Invoke-MangoProbe $probe
    $revision = $root.data.revision
    if ($revision -notmatch '^[A-Za-z0-9:_-]+$' -or $null -ne $root.data.nextPage) { throw 'Unexpected native root revision or continuation' }
    $nativeCategories = @($root.data.items | Where-Object { !$_.uiAssisted })
    $albumCategory = @($nativeCategories | Where-Object { $_.id -ceq '__ALL_ALBUM__' -and $_.browsable })
    $musicCategory = @($nativeCategories | Where-Object { $_.id -ceq '__ALL_MUSIC__' -and $_.browsable })
    if ($albumCategory.Count -ne 1 -or $musicCategory.Count -ne 1) { throw 'Expected unique native Album/All Music categories' }
    $albums = Read-NativePages $albumCategory[0].id
    if ($albums.items.Count -gt $MaxAlbums) { throw 'Album bound exceeded; no truncated inventory pass' }
    foreach ($album in $albums.items) {
        if (!$album.browsable -or $album.playable -or $album.subtitle -notmatch '^\d+$') { throw 'Unexpected native album row/count' }
        $tracks = Read-NativePages $album.id
        if (@($tracks.items | Where-Object { !$_.playable -or $_.browsable }).Count -or $tracks.items.Count -ne [int]$album.subtitle) { throw 'Native album contents disagree with source-reported count/row type' }
        $groups.Add([ordered]@{id=$album.id;title=$album.title;sourceReportedTracks=[int]$album.subtitle;observedTracks=$tracks.items.Count;pages=$tracks.pages;items=$tracks.items;playbackStatus='NOT TESTED'})
        ConvertTo-Json -InputObject @($groups.ToArray()) -Depth 10 | Set-Content (Join-Path $out 'albums.json')
    }
    $allMusic = Read-NativePages $musicCategory[0].id
    if (@($allMusic.items | Where-Object { !$_.playable -or $_.browsable }).Count) { throw 'Unexpected All Music row type' }
    $allMusic | ConvertTo-Json -Depth 10 | Set-Content (Join-Path $out 'all-music.json')
    $albumTrackCount = 0
    foreach ($group in $groups) { $albumTrackCount += [int]$group.observedTracks }
    # Source categories need not cover the same set. Record a discrepancy rather than
    # declaring All Music complete for the collection or inventing missing rows/IDs.
    $countsMatch = $albumTrackCount -eq $allMusic.items.Count
    Assert-MangoOffline $probe
    $null = Invoke-MangoProbe $probe 'GET_STATE'
    [ordered]@{status='VERIFIED_SOURCE_LOCAL_NATIVE_METADATA_ONLY';revision=$revision;nativeRootCategories=$nativeCategories.title;albums=$groups.Count;albumTrackCount=$albumTrackCount;allMusicTracks=$allMusic.items.Count;albumAllMusicCountsEqual=$countsMatch;allMusicFinding=$(if ($countsMatch) { 'Counts agree; cross-category item identity is not proved' } else { 'SOURCE_CATEGORY_COUNT_DIFFERENCE; All Music must not be treated as a complete collection' });allMusicPages=$allMusic.pages;requests=$probe.RequestCount;bothSourcesPaused=$true;volume=$ExpectedVolume;scope='Complete bounded native Album metadata and observed All Music response for this source snapshot; no unique-file, playback, playlist, filesystem/codec, phone BLE or audio acceptance';artifactDirectory=$out} | ConvertTo-Json -Depth 8 | Tee-Object (Join-Path $out 'summary.json')
} catch {
    [ordered]@{status='FAILED_NATIVE_METADATA_INVENTORY';reason=$_.Exception.Message;completedAlbums=$groups.Count;requests=$probe.RequestCount;scope='Read-only diagnostic; no playback/volume/settings operation sent';artifactDirectory=$out} | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $out 'summary.json')
    throw
} finally { Write-Output "Native Mango inventory evidence: $out" }
