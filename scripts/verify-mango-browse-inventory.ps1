param(
    [string]$Serial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string[]]$ArtistLabels=@(),
    [int]$ExpectedVolume=37,
    [ValidateRange(1,4)][int]$AlbumsPerArtist=2
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
$run=[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8)
$out=Join-Path $repo "artifacts/mango-browse-inventory/$run"
$probe=New-MangoProbeContext $out $Serial $Adb $false $ExpectedVolume
$collections=@()
try {
    Assert-MangoOffline $probe
    $null=Invoke-MangoProbe $probe 'GET_STATE'
    & $Adb -s $Serial shell input keyevent 224
    & $Adb -s $Serial shell wm dismiss-keyguard
    $music=Read-MangoLocation $probe @('Internal Storage','Music')
    if (!$ArtistLabels.Count) { $ArtistLabels=@($music.observedRows | Where-Object browsable | Select-Object -First 5 -ExpandProperty title) }
    foreach ($artist in $ArtistLabels) {
        if (@($music.observedRows | Where-Object { $_.title -ceq $artist -and $_.browsable }).Count -ne 1) { throw 'Requested artist folder not observed uniquely under Music' }
        $artistPage=Read-MangoLocation $probe @('Internal Storage','Music',$artist)
        $albums=@($artistPage.observedRows | Where-Object browsable | Sort-Object title | Select-Object -First $AlbumsPerArtist)
        foreach ($album in $albums) {
            $folder=Read-MangoLocation $probe @('Internal Storage','Music',$artist,$album.title)
            $files=@($folder.observedRows | Where-Object { $_.playable -and !$_.browsable -and $_.title -match '\.(flac|wav)$' } | ForEach-Object { [ordered]@{fileLabel=$_.title;formatLabel=([IO.Path]::GetExtension($_.title).TrimStart('.').ToUpperInvariant())} })
            $collections += [ordered]@{artistFolder=$artist;albumFolder=$album.title;locationLabels=$folder.labels;header=$folder.header;fileLabels=$files;reachedEnd=$folder.reachedEnd;status='NOT TESTED playback - observed filename extensions only'}
            ConvertTo-Json -InputObject $collections -Depth 7 | Set-Content (Join-Path $out 'collections.json')
        }
    }
    Assert-MangoOffline $probe
    $null=Invoke-MangoProbe $probe 'GET_STATE'
    [ordered]@{status='VERIFIED_READ_ONLY_SOURCE_UI_INVENTORY';source='com.ibasso.music';collections=$collections.Count;observedFileLabels=@($collections | ForEach-Object { $_.fileLabels }).Count;formats=@($collections | ForEach-Object { $_.fileLabels | ForEach-Object { $_.formatLabel } } | Sort-Object -Unique);bothSourcesPaused=$true;volume=$ExpectedVolume;artifactDirectory=$out;scope='Filename-extension metadata only, no file-content/codec/playback/audio/phone BLE proof'} | ConvertTo-Json -Depth 5 | Tee-Object (Join-Path $out 'summary.json')
} catch {
    [ordered]@{status='FAILED_READ_ONLY_INVENTORY';reason=$_.Exception.Message;completedCollections=$collections.Count;artifactDirectory=$out} | ConvertTo-Json | Set-Content (Join-Path $out 'summary.json')
    throw
} finally { Write-Output "Mango inventory evidence: $out" }
