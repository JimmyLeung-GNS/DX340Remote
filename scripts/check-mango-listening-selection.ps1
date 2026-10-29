param(
    [Parameter(Mandatory=$true)][ValidateRange(1,20)][int]$Sequence,
    [int]$ExpectedVolume=37,
    [string]$Serial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string]$Python='python',
    [string]$PlanPath
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
if (!$PlanPath) { $PlanPath=Join-Path $repo 'artifacts/mango-listening-plan.json' }
$plan=Get-Content -LiteralPath $PlanPath -Raw | ConvertFrom-Json
$entries=@($plan.selections | Where-Object sequence -eq $Sequence)
if ($entries.Count -ne 1 -or $entries[0].source -ne 'com.ibasso.music' -or $entries[0].mode -ne 'ui-assisted') { throw 'Expected one observed Mango candidate' }
$candidate=$entries[0]
$run=[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8)
$out=Join-Path $repo "artifacts/mango-listening/$('{0:D2}' -f $Sequence)-$run"
$probe=New-MangoProbeContext $out $Serial $Adb $true $ExpectedVolume
$candidateFile=Join-Path $out 'candidate.json'
$candidate | ConvertTo-Json -Depth 7 | Set-Content -LiteralPath $candidateFile
$summary=[ordered]@{kind='actual-offline-Mango-own-adapter-selection-no-BLE';sequence=$Sequence;candidate=$candidate;sourceCommit=(git -C $repo rev-parse HEAD).Trim();sourceTreeDirty=[bool](git -C $repo status --porcelain);startedUtc=[DateTime]::UtcNow.ToString('o');artifactDirectory=$out;selectionSent=$false;observedCorrectPlayingState=$false;audibleConfirmation=$null;status='FAILED_OR_INTERRUPTED'}
try {
    $installedPath=((& $Adb -s $Serial shell pm path org.dx340.remote.host) -join '').Trim() -replace '^package:',''
    if ($installedPath -notmatch '^/data/app/[A-Za-z0-9_~+./=-]+/base\.apk$') { throw 'Cannot identify installed companion' }
    $installedCopy=Join-Path $out 'installed-companion.apk'
    & $Adb -s $Serial pull $installedPath $installedCopy | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Installed companion provenance missing' }
    $summary.installedCompanionSha256=(Get-FileHash -LiteralPath $installedCopy -Algorithm SHA256).Hash.ToLowerInvariant()
    $summary.helperSha256=(Get-FileHash -LiteralPath $PSCommandPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $summary.commonHelperSha256=(Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'mango-listening-tools.ps1') -Algorithm SHA256).Hash.ToLowerInvariant()
    $summary.headerInspectorSha256=(Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'inspect-mango-public-header.py') -Algorithm SHA256).Hash.ToLowerInvariant()
    $summary.headerParserSha256=(Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'mango_header_format.py') -Algorithm SHA256).Hash.ToLowerInvariant()
    Assert-MangoOffline $probe
    $before=Invoke-MangoProbe $probe 'GET_STATE'
    if ($before.state.playing) {
        $paused=Invoke-MangoProbe $probe 'SET_PLAYING' @('--ez','playing','false')
        if ($paused.state.playing) { throw 'Preceding Mango clip did not pause' }
    }
    & $Adb -s $Serial shell input keyevent 224
    & $Adb -s $Serial shell wm dismiss-keyguard
    $page=Open-MangoLocation $probe $candidate.locationLabels
    $track=Find-MangoRow $probe $page $candidate.fileLabel $false
    if ($track.Page.data.title -cne ($candidate.locationLabels -join ' / ')) { throw 'Exact candidate folder not observed' }
    $headerFile=Join-Path $out 'public-file-header.json'
    & $Python -X utf8 (Join-Path $PSScriptRoot 'inspect-mango-public-header.py') --candidate $candidateFile --output $headerFile --adb $Adb --serial $Serial > (Join-Path $out 'public-header-command.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Candidate container header could not be verified; no playback was sent' }
    $summary.fileHeader=Get-Content -LiteralPath $headerFile -Raw | ConvertFrom-Json
    # A file's embedded title can legitimately differ from its filename.
    $expectedTitle=if ($summary.fileHeader.titleTag) { $summary.fileHeader.titleTag } else { $candidate.expectedTitle }
    $summary.effectiveExpectedTitle=$expectedTitle
    $summary.expectedTitleBasis=if ($summary.fileHeader.titleTag) { 'Complete embedded title tag within bounded public header' } else { 'Filename-derived hypothesis; no complete title tag in prefix' }
    Assert-MangoOffline $probe
    if ($track.Row.id -notmatch '^[A-Za-z0-9:_-]+$' -or $track.Page.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid fresh UI selection identity' }
    $null=Invoke-MangoProbe $probe 'PLAY_ITEM' @('--es','item',$track.Row.id,'--es','revision',$track.Page.data.revision)
    for ($poll=0; $poll -lt 4; $poll++) {
        $seen=Invoke-MangoProbe $probe 'GET_STATE'
        if ($seen.state.playing -and $seen.state.title -ceq $expectedTitle) { break }
        Start-Sleep -Milliseconds 500
    }
    $summary.observedTitle=$seen.state.title; $summary.observedArtist=$seen.state.artist; $summary.observedAlbum=$seen.state.album
    if (!$seen.state.playing -or $seen.state.title -cne $expectedTitle -or @($seen.sourceStatesAfter | Where-Object { $_.source -eq 'com.ibasso.music' -and $_.playbackState -eq 3 -and $_.playing }).Count -ne 1) { throw 'Requested title/playing state not observed; preserve attempt, no playback retry' }
    $summary.observedCorrectPlayingState=$true
    $summary.observedUtc=[DateTime]::UtcNow.ToString('o')
    $summary.status='AWAITING_HUMAN_LISTENING_CONFIRMATION'
    Assert-MangoOffline $probe
} catch {
    $summary.failure=$_.Exception.Message
    throw
} finally {
    $summary.selectionSent=$probe.SelectionSent
    $summary | ConvertTo-Json -Depth 9 | Set-Content (Join-Path $out 'summary.json')
    Write-Output "Mango listening attempt: $out"
}
$summary | ConvertTo-Json -Depth 9
