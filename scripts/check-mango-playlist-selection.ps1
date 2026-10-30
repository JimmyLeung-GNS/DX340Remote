param(
    [Parameter(Mandatory=$true)][ValidateSet('白日夢','風のたより')][string]$Track,
    [Parameter(Mandatory=$true)][string]$ExpectedCompanionSha256,
    [string]$Serial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
$out=Join-Path $repo ('artifacts/mango-playlist-selection-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
$ctx=New-MangoProbeContext $out $Serial $Adb $true 37
$summary=[ordered]@{kind='actual-source-local-playlist-production-selection-no-BLE';sourceCommit=(git -C $repo rev-parse HEAD).Trim();sourceTreeDirty=[bool](git -C $repo status --porcelain);track=$Track;expectedCompanionSha256=$ExpectedCompanionSha256;selectionSent=$false;observedCorrectPlayingState=$false;humanAudio=$null;status='FAILED_OR_INTERRUPTED'}
try {
    if ($ExpectedCompanionSha256 -notmatch '^[a-f0-9]{64}$') { throw 'Expected exact installed APK hash' }
    $path=((& $Adb -s $Serial shell pm path org.dx340.remote.host)-replace '^package:','').Trim()
    & $Adb -s $Serial pull $path (Join-Path $out 'installed.apk') > (Join-Path $out 'pull.txt')
    if ($LASTEXITCODE -ne 0 -or (Get-FileHash (Join-Path $out 'installed.apk')).Hash.ToLowerInvariant() -cne $ExpectedCompanionSha256) { throw 'Installed companion differs' }
    Assert-MangoOffline $ctx
    $before=Invoke-MangoProbe $ctx 'GET_STATE'
    if (@($before.sourceStatesAfter | Where-Object playing).Count) { throw 'Both players must be paused before selection' }
    $root=Invoke-MangoProbe $ctx
    $list=Invoke-MangoNavigate $ctx $root 'mango-ui:playlists'
    $playlist=@($list.data.items | Where-Object { $_.title -ceq 'DX340 Remote verification' -and $_.browsable -and !$_.playable })
    if ($playlist.Count -ne 1) { throw 'Expected unique authorized fixture' }
    $detail=Invoke-MangoNavigate $ctx $list $playlist[0].id
    if ($detail.data.title -cne 'DX340 Remote verification' -or !$detail.data.uiAssisted) { throw 'Playlist context changed' }
    $row=@($detail.data.items|Where-Object {$_.title -ceq $Track -and $_.subtitle -ceq 'tayori' -and $_.playable -and !$_.browsable})
    if($row.Count -ne 1){throw 'Expected unique fresh track row'}
    $summary.status='PLAY_ITEM_ABOUT_TO_SEND'
    $summary|ConvertTo-Json -Depth 8|Set-Content (Join-Path $out 'summary.json')
    $null=Invoke-MangoProbe $ctx 'PLAY_ITEM' @('--es','item',$row[0].id,'--es','revision',$detail.data.revision)
    $summary.selectionSent=$true
    $matching=0
    for($poll=0;$poll -lt 8;$poll++) {
        $seen=Invoke-MangoProbe $ctx 'GET_STATE'
        $summary.lastObservedState=$seen.state
        if($seen.state.playing -and $seen.state.title -ceq $Track -and $seen.state.artist -ceq 'tayori'){$matching++}else{$matching=0}
        if($matching -ge 2){break}
        Start-Sleep -Milliseconds 300
    }
    if($matching -lt 2){throw 'Expected source state did not settle; audio unverified'}
    $summary.observedCorrectPlayingState=$true
    $summary.status='AWAITING_SEPARATE_HUMAN_AUDIO_CONFIRMATION'
} finally {
    $summary.requestCount=$ctx.RequestCount
    $summary.completedUtc=[DateTime]::UtcNow.ToString('o')
    $summary|ConvertTo-Json -Depth 8|Set-Content (Join-Path $out 'summary.json')
    Write-Output (Join-Path $out 'summary.json')
}
