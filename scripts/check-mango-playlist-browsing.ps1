param(
    [Parameter(Mandatory=$true)][string]$ExpectedCompanionSha256,
    [string]$Serial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string]$Playlist='DX340 Remote verification'
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
$out=Join-Path $repo ('artifacts/mango-playlist-check-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
$ctx=New-MangoProbeContext $out $Serial $Adb $false 37
$summary=[ordered]@{kind='actual-source-local-read-only-playlist-navigation-no-BLE';sourceCommit=(git -C $repo rev-parse HEAD).Trim();sourceTreeDirty=[bool](git -C $repo status --porcelain);expectedCompanionSha256=$ExpectedCompanionSha256;playlist=$Playlist;status='FAILED_OR_INTERRUPTED';checks=@()}
try {
    if ($ExpectedCompanionSha256 -notmatch '^[a-f0-9]{64}$') { throw 'Expected exact installed APK hash' }
    $path=((& $Adb -s $Serial shell pm path org.dx340.remote.host)-replace '^package:','').Trim()
    & $Adb -s $Serial pull $path (Join-Path $out 'installed.apk') > (Join-Path $out 'pull.txt')
    if ($LASTEXITCODE -ne 0 -or (Get-FileHash (Join-Path $out 'installed.apk')).Hash.ToLowerInvariant() -cne $ExpectedCompanionSha256) { throw 'Installed companion differs' }
    Assert-MangoOffline $ctx
    $null=Invoke-MangoProbe $ctx 'GET_STATE'
    $root=Invoke-MangoProbe $ctx
    $entry=@($root.data.items | Where-Object { $_.id -ceq 'mango-ui:playlists' -and $_.browsable -and !$_.playable -and $_.uiAssisted })
    if ($entry.Count -ne 1) { throw 'Missing separate assisted-playlists entry' }
    $list=Invoke-MangoNavigate $ctx $root $entry[0].id
    if (!$list.data.uiAssisted -or $list.data.title -cne 'Mango playlists') { throw 'Unexpected playlist list' }
    $named=@($list.data.items | Where-Object { $_.title -ceq $Playlist -and $_.browsable -and !$_.playable })
    if ($named.Count -ne 1 -or $list.data.items.Count -ne 1) { throw 'Fixture list differs; do not mutate it automatically' }
    $summary.checks+='separate root and existing playlist list'
    $detail=Invoke-MangoNavigate $ctx $list $named[0].id
    if ($detail.data.title -cne $Playlist -or @($detail.data.items).Count -ne 2 -or @($detail.data.items | Where-Object { !$_.playable -or $_.browsable }).Count -or ($detail.data.items.title -join '|') -cne '白日夢|風のたより') { throw 'Fixture tracks differ' }
    $summary.checks+='two exact playable rows; no editing/play-all/menu controls'
    $summary.observedTracks=@($detail.data.items.title)
    $back=Invoke-MangoNavigate $ctx $detail $detail.data.backId
    if ($back.data.title -cne 'Mango playlists') { throw 'Detail back failed' }
    $summary.checks+='detail back returns existing playlists'
    $fresh=Invoke-MangoNavigate $ctx $back 'mango-ui:playlists'
    if ($fresh.data.revision -ceq $list.data.revision) { throw 'Refresh did not renew snapshot' }
    $rejected=$false
    try { $null=Invoke-MangoNavigate $ctx $list $named[0].id } catch {
        $file=Join-Path $out ('{0:D3}-BROWSE.json' -f $ctx.RequestCount)
        $failure=Get-Content $file -Raw|ConvertFrom-Json
        if ($failure.error -cne 'STALE_LIBRARY' -or $failure.phase -cne 'FINISHED') { throw }
        $rejected=$true
    }
    if (!$rejected) { throw 'Expired playlist action unexpectedly accepted' }
    $summary.checks+='old revision rejected after refresh'
    $null=Invoke-MangoProbe $ctx 'GET_STATE'
    $summary.status='PASS_WITHIN_READ_ONLY_SCOPE'
} finally {
    $summary.requestCount=$ctx.RequestCount
    $summary.completedUtc=[DateTime]::UtcNow.ToString('o')
    $summary|ConvertTo-Json -Depth 8|Set-Content (Join-Path $out 'summary.json')
    Write-Output (Join-Path $out 'summary.json')
}
