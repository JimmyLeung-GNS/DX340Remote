param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$run = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$out = Join-Path $repo "artifacts/apple-browse-inventory/$run"
New-Item -ItemType Directory -Force $out | Out-Null
$wifi = ((& $Adb -s $Serial shell settings get global wifi_on) -join '').Trim()
$network = (& $Adb -s $Serial shell dumpsys connectivity) -join "`n"
if ($wifi -ne '0' -or $network -notmatch 'Active default network: none') { throw 'Offline inventory requires Wi-Fi off and no active default network; no settings were changed' }
$wifi | Set-Content (Join-Path $out 'wifi.txt')
$network | Set-Content (Join-Path $out 'connectivity.txt')
function Probe([string]$Label, [string]$Operation = 'BROWSE', [string[]]$Extra = @()) {
    if ($Operation -notin @('BROWSE','GET_STATE')) { throw 'Read-only inventory operation required' }
    $tag = [guid]::NewGuid().ToString('N')
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation $Operation --es source com.apple.android.music --es reportTag $tag @Extra > (Join-Path $out "$Label-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw "Broadcast failed: $Label" }
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
    if ($LASTEXITCODE -ne 0) { throw "Missing report: $Label" }
    $raw | Set-Content (Join-Path $out "$Label.json")
    $report = ($raw -join "`n") | ConvertFrom-Json
    if ($report.phase -ne 'FINISHED' -or $report.operation -ne $Operation -or $report.requestedSource -ne 'com.apple.android.music' -or $report.error) { throw "Unexpected report: $Label ($($report.error))" }
    foreach ($field in @('sourceStatesBefore','sourceStatesAfter')) {
        $states = @($report.$field)
        if ($states.Count -ne 2 -or @($states | Where-Object { $_.source -in @('com.ibasso.music','com.apple.android.music') -and $_.playbackState -eq 2 -and !$_.playing }).Count -ne 2) { throw 'Both source sessions must be observed paused; no playback command was sent' }
    }
    return $report
}
function Navigate([string]$Label, $Current, [string]$Item) {
    if ($Item -notmatch '^[A-Za-z0-9:_-]+$' -or $Current.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid opaque navigation identity' }
    Probe $Label 'BROWSE' @('--es','parent',$Item,'--es','revision',$Current.data.revision)
}
function Category([string]$Label, [string]$Title) {
    $root = Probe "$Label-root"
    if ($root.data.title -ne 'Downloaded') { throw 'Expected verified Downloaded root' }
    $entry = @($root.data.items | Where-Object { $_.title -eq $Title -and $_.browsable })
    if ($entry.Count -ne 1) { throw "Missing $Title category" }
    Navigate "$Label-page" $root $entry[0].id
}
function Scroll([string]$Label, $Current) {
    if ($null -eq $Current.data.nextPage -or $Current.data.parent -notmatch '^[A-Za-z0-9:_-]+$' -or $Current.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid page identity' }
    Probe $Label 'BROWSE' @('--es','parent',$Current.data.parent,'--es','revision',$Current.data.revision,'--ei','page',[string]$Current.data.nextPage)
}
$collections = @()
& $Adb -s $Serial shell input keyevent 224
& $Adb -s $Serial shell wm dismiss-keyguard
try {
    foreach ($category in @('Albums','Playlists')) {
        $visited = @{}
        $count = if ($category -eq 'Albums') { 5 } else { 2 }
        foreach ($index in 1..$count) {
            $label = "$category-$index"
            $list = Category "$label-list" $category
            $entry = @()
            foreach ($scroll in 0..10) {
                if ($list.data.title -ne $category) { throw 'Unexpected collection category' }
                $entry = @($list.data.items | Where-Object { $_.browsable -and !$visited.ContainsKey($_.title) } | Select-Object -First 1)
                if ($entry.Count -eq 1) { break }
                if ($null -eq $list.data.nextPage) { break }
                $list = Scroll "$label-list-scroll-$scroll" $list
            }
            if ($entry.Count -ne 1) { throw "Not enough distinct observed $category for this inventory; no playback was attempted" }
            $name = $entry[0].title
            $tracks = Navigate "$label-tracks-0" $list $entry[0].id
            $observed = @{}
            foreach ($scroll in 0..7) {
                # Favourite Songs has decorative trailing NBSPs in its actual header.
                # Normalize only this display comparison, preserving action IDs/revisions.
                if ($tracks.data.title.Trim() -ne $name.Trim()) { throw 'Unexpected collection header' }
                foreach ($item in $tracks.data.items) {
                    if ($item.playable -and !$item.browsable) { $observed[$item.title+'|'+$item.subtitle] = [ordered]@{title=$item.title;subtitle=$item.subtitle} }
                }
                if ($observed.Count -ge 4 -or $null -eq $tracks.data.nextPage) { break }
                $tracks = Scroll "$label-tracks-scroll-$scroll" $tracks
            }
            $collections += [ordered]@{category=$category;name=$name;observedPlayableDownloadedRowLabels=$observed.Count;trackLabels=@($observed.Values)}
            $visited[$name] = $true
            ConvertTo-Json -InputObject $collections -Depth 6 | Set-Content (Join-Path $out 'collections.json')
        }
    }
} finally { $final = Probe '99-final-paused-state' 'GET_STATE' }
$albums = @($collections | Where-Object category -eq 'Albums')
$playlists = @($collections | Where-Object category -eq 'Playlists')
$distinctLabels = @($collections | ForEach-Object { $_.trackLabels } | ForEach-Object { $_.title+'|'+$_.subtitle } | Sort-Object -Unique)
[ordered]@{kind='actual-offline-Apple-UI-browse-inventory-no-selection-no-BLE';albums=$albums.Count;playlists=$playlists.Count;observedAlbumRowLabels=($albums.observedPlayableDownloadedRowLabels | Measure-Object -Sum).Sum;observedPlaylistRowLabels=($playlists.observedPlayableDownloadedRowLabels | Measure-Object -Sum).Sum;distinctTitleArtistLabels=$distinctLabels.Count;bothSourcesPaused=$true;note='Visible downloaded-row labels only; not complete collection sizes, stable track identities, playback or listening evidence.';artifactDirectory=$out} | ConvertTo-Json -Depth 5 | Tee-Object (Join-Path $out 'summary.json')
