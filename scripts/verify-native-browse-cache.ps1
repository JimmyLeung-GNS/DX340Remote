param(
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
# Source-local read-only check. Never issues playback, volume, permission or network changes.
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$run = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8)
$out = Join-Path $repo "artifacts/native-page-cache/$run"
New-Item -ItemType Directory -Force -Path $out | Out-Null
$cases = @()
$startingVolume = $null
function Probe([string]$Label, [string]$Source = 'com.ibasso.music', [string]$Operation = 'BROWSE', [string[]]$Extra = @()) {
    if ($Source -notin @('com.ibasso.music','com.apple.android.music') -or $Operation -notin @('BROWSE','GET_STATE')) { throw 'Read-only source operation required' }
    $tag = [guid]::NewGuid().ToString('N')
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation $Operation --es source $Source --es reportTag $tag @Extra > (Join-Path $out "$Label-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw "Broadcast failed: $Label" }
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
    if ($LASTEXITCODE -ne 0) { throw "Missing result: $Label" }
    $raw | Set-Content (Join-Path $out "$Label.json")
    $r = ($raw -join "`n") | ConvertFrom-Json
    if ($r.phase -ne 'FINISHED' -or $r.requestedSource -ne $Source -or $r.operation -ne $Operation -or $r.error) { throw "Failed $Label ($($r.error))" }
    foreach ($field in @('sourceStatesBefore','sourceStatesAfter')) {
        $states = @($r.$field)
        if ($states.Count -ne 2 -or @($states | Where-Object { $_.source -in @('com.ibasso.music','com.apple.android.music') -and $_.playbackState -eq 2 -and !$_.playing }).Count -ne 2) { throw 'Both sources must remain paused; no playback command was sent' }
    }
    if ($null -ne $script:startingVolume -and $r.state.volume.current -ne $script:startingVolume) { throw 'Reported volume changed; no automatic adjustment sent' }
    return $r
}
function Page([string]$Label, [string]$Parent, [string]$Revision, [int]$Number, [bool]$Cached) {
    if ($Parent -notmatch '^[A-Za-z0-9:_-]+$' -or ($Revision -and $Revision -notmatch '^[A-Za-z0-9:_-]+$')) { throw 'Invalid opaque page identity' }
    $extra = @('--es','parent',$Parent,'--ei','page',[string]$Number)
    if ($Revision) { $extra += @('--es','revision',$Revision) }
    $r = Probe $Label -Extra $extra
    if ($null -eq $r.data.cached -or $r.data.cached -ne $Cached -or $r.data.parent -ne $Parent -or $r.data.page -ne $Number) { throw "Unexpected cache/page result: $Label" }
    $script:cases += [ordered]@{label=$Label;cached=$r.data.cached;ageMs=$r.data.cacheAgeMs;elapsedMs=$r.elapsedMs;rows=@($r.data.items).Count;nextPage=$r.data.nextPage}
    return $r
}
try {
    $wifi = ((& $Adb -s $Serial shell settings get global wifi_on) -join '').Trim()
    $network = (& $Adb -s $Serial shell dumpsys connectivity) -join "`n"
    if ($wifi -ne '0' -or $network -notmatch 'Active default network: none') { throw 'Offline precondition failed; no settings changed' }
    $network | Set-Content (Join-Path $out 'connectivity.txt')
    $before = Probe '00-before' -Operation GET_STATE
    $script:startingVolume = $before.state.volume.current
    $root = Probe '01-root'
    if ($root.data.cached -ne $false) { throw 'Root refresh must query the source' }
    $music = @($root.data.items | Where-Object { $_.title -eq 'All Music' -and $_.browsable -and !$_.uiAssisted })
    if ($music.Count -ne 1) { throw 'Expected unique native Mango All Music category' }
    $parent = $music[0].id; $revision = $root.data.revision
    $cold = Page '02-cold' $parent $revision 0 $false
    $warm = Page '03-warm' $parent $revision 0 $true
    if (($cold.data.items | ConvertTo-Json -Depth 5 -Compress) -cne ($warm.data.items | ConvertTo-Json -Depth 5 -Compress) -or $cold.data.nextPage -ne $warm.data.nextPage) { throw 'Cache changed metadata order or cursor' }
    if ($null -ne $cold.data.nextPage) {
        $next = [int]$cold.data.nextPage
        $page2 = Page '04-next-cold' $parent $revision $next $false
        $page2warm = Page '05-next-warm' $parent $revision $next $true
        if (($page2.data.items | ConvertTo-Json -Depth 5 -Compress) -cne ($page2warm.data.items | ConvertTo-Json -Depth 5 -Compress) -or $page2.data.nextPage -ne $page2warm.data.nextPage) { throw 'Cached continuation changed metadata or cursor' }
    }
    # Both production playItem validations use a null expected revision. No PLAY_ITEM here.
    $null = Page '06-fresh-validation-style-read' $parent '' 0 $false
    $null = Page '07-after-fresh' $parent $revision 0 $true
    $refreshed = Probe '08-root-refresh'
    if ($refreshed.data.cached -ne $false) { throw 'Root refresh used cached metadata' }
    $revision = $refreshed.data.revision
    $null = Page '09-after-root-refresh' $parent $revision 0 $false
    $null = Probe '10-other-source' -Source com.apple.android.music -Operation GET_STATE
    $null = Probe '11-return-mango' -Operation GET_STATE
    $null = Page '12-after-source-switch' $parent $revision 0 $false
    Start-Sleep -Milliseconds 10100
    $null = Page '13-after-expiry' $parent $revision 0 $false
    $null = Probe '14-after' -Operation GET_STATE
    [ordered]@{status='VERIFIED_SOURCE_LOCAL_METADATA_CACHE_ONLY';volume=$startingVolume;bothSourcesPaused=$true;cases=$cases;scope='Actual DX340 native browser metadata; no playback command, phone BLE or cached UI-assisted navigation acceptance'} | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $out 'summary.json')
    Get-Content (Join-Path $out 'summary.json')
} catch {
    [ordered]@{status='FAILED';reason=$_.Exception.Message;cases=$cases;scope='Read-only diagnostic stopped; no playback/volume command was sent'} | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $out 'summary.json')
    throw
} finally {
    Write-Output "Native browse cache evidence: $out"
}
