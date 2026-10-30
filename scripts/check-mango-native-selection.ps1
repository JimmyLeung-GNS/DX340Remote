param(
    [Parameter(Mandatory=$true)][string]$PlanPath,
    [Parameter(Mandatory=$true)][ValidateRange(1,20)][int]$Sequence,
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [int]$ExpectedVolume = 37,
    [string]$ExpectedCompanionSha256 = '7129c3073935faf5f1c7744623e254891ab7048d79f1e90eab83176fe4b49d3b'
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
$plan=Get-Content -LiteralPath $PlanPath -Raw | ConvertFrom-Json
$choices=@($plan.selections | Where-Object sequence -eq $Sequence)
if ($choices.Count -ne 1 -or $choices[0].mode -ne 'native' -or $choices[0].source -ne 'com.ibasso.music') { throw 'Expected one native candidate' }
$candidate=$choices[0]
if ($candidate.id -notmatch '^[A-Za-z0-9_]+$' -or $candidate.parent -notmatch '^[A-Za-z0-9_]+$' -or $candidate.page -notin 0..10000) { throw 'Invalid native identity/page' }
$run=[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8)
$out=Join-Path $repo "artifacts/mango-native-matrix/$('{0:D2}' -f $Sequence)-$run"
$probe=New-MangoProbeContext $out $Serial $Adb $true $ExpectedVolume
$summary=[ordered]@{kind='actual-offline-native-Mango-production-selection-no-BLE';sequence=$Sequence;candidate=$candidate;sourceCommit=(git -C $repo rev-parse HEAD).Trim();sourceTreeDirty=[bool](git -C $repo status --porcelain);startedUtc=[DateTime]::UtcNow.ToString('o');artifactDirectory=$out;selectionSent=$false;observedCorrectPlayingState=$false;humanAudio=$null;status='FAILED_OR_INTERRUPTED'}
$summary.planSha256=(Get-FileHash -LiteralPath $PlanPath).Hash.ToLowerInvariant()
$summary.helperSha256=(Get-FileHash -LiteralPath $PSCommandPath).Hash.ToLowerInvariant()
$summary.commonHelperSha256=(Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')).Hash.ToLowerInvariant()
try {
    $installedPath=((& $Adb -s $Serial shell pm path org.dx340.remote.host) -join '').Trim() -replace '^package:',''
    if ($installedPath -notmatch '^/data/app/[A-Za-z0-9_~+./=-]+/base\.apk$') { throw 'Installed package identity missing' }
    & $Adb -s $Serial shell sha256sum $installedPath > (Join-Path $out 'installed-package-digest.txt')
    if ($LASTEXITCODE) { throw 'Cannot hash installed package' }
    $actualDigest=((Get-Content (Join-Path $out 'installed-package-digest.txt') -Raw) -split '\s+')[0]
    if ($actualDigest -cne $ExpectedCompanionSha256) { throw 'Installed companion differs from this test build' }
    $summary.installedCompanionSha256=$actualDigest
    Assert-MangoOffline $probe
    $before=Invoke-MangoProbe $probe 'GET_STATE'
    if (@($before.sourceStatesAfter | Where-Object playing).Count) { throw 'Pause both sources before each selection; no playback was sent' }
    if (!$before.state.capabilities.canPlayNativeItem) { throw 'Corrected native scheme requires local confirmation first' }
    $root=Invoke-MangoProbe $probe
    if ($root.data.uiAssisted -or $root.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Expected current native source root' }
    if ($candidate.parent -cne '__ALL_MUSIC__') {
        $albums=Invoke-MangoProbe $probe 'BROWSE' @('--es','parent','__ALL_ALBUM__','--es','revision',$root.data.revision,'--ei','page','0')
        $album=@($albums.data.items | Where-Object { $_.id -ceq $candidate.parent -and $_.browsable -and $_.title -ceq $candidate.albumLabel })
        if ($album.Count -ne 1) { throw 'Observed album identity changed' }
    }
    $page=Invoke-MangoProbe $probe 'BROWSE' @('--es','parent',$candidate.parent,'--es','revision',$root.data.revision,'--ei','page',([string]$candidate.page))
    if ($page.data.uiAssisted -or $page.data.parent -cne $candidate.parent -or $page.data.page -ne $candidate.page -or $page.data.revision -cne $root.data.revision) { throw 'Native page context changed' }
    $row=@($page.data.items | Where-Object { $_.id -ceq $candidate.id -and $_.title -ceq $candidate.displayTitle -and $_.subtitle -ceq $candidate.expectedArtist -and $_.playable -and !$_.browsable -and !$_.uiAssisted })
    if ($row.Count -ne 1) { throw 'Expected unique fresh native playable candidate' }
    $summary.status='PLAY_ITEM_ABOUT_TO_SEND'
    $summary | ConvertTo-Json -Depth 10 | Set-Content (Join-Path $out 'summary.json')
    $null=Invoke-MangoProbe $probe 'PLAY_ITEM' @('--es','item',$row[0].id,'--es','revision',$page.data.revision)
    $matchingReads=0
    for ($poll=0; $poll -lt 8; $poll++) {
        $seen=Invoke-MangoProbe $probe 'GET_STATE'
        $summary.lastObservedState=$seen.state
        if ($seen.state.playing -and $seen.state.title -ceq $candidate.expectedTitle -and $seen.state.artist -ceq $candidate.expectedArtist) { $matchingReads++ } else { $matchingReads=0 }
        if ($matchingReads -ge 2) { break }
        Start-Sleep -Milliseconds 500
    }
    if ($matchingReads -lt 2) { throw 'Selected native title/artist did not settle correctly; preserve failure without retry' }
    $summary.observedCorrectPlayingState=$true
    $summary.observedUtc=[DateTime]::UtcNow.ToString('o')
    $summary.status='AWAITING_HUMAN_AUDIO_CONFIRMATION'
    Assert-MangoOffline $probe
} catch {
    $summary.failurePhase=$summary.status
    $summary.status='FAILED_SELECTION_OR_OBSERVATION'
    $summary.failure=$_.Exception.Message
    # A failed post-dispatch test is stopped, never automatically played again.
    if ($probe.SelectionSent) {
        try { $paused=Invoke-MangoProbe $probe 'SET_PLAYING' @('--ez','playing','false'); $summary.failurePaused=!$paused.state.playing }
        catch { $summary.failurePauseError=$_.Exception.Message }
    }
    throw
} finally {
    $summary.selectionSent=$probe.SelectionSent
    $summary | ConvertTo-Json -Depth 10 | Set-Content (Join-Path $out 'summary.json')
    [ordered]@{attemptDirectory=$out;sequence=$Sequence;status=$summary.status} | ConvertTo-Json | Set-Content (Join-Path $repo 'artifacts/latest-native-mango-attempt.json')
    Write-Output "Native Mango attempt: $out"
}
$summary | ConvertTo-Json -Depth 10
