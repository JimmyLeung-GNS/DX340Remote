param(
    [Parameter(Mandatory=$true)][string]$PageFile,
    [Parameter(Mandatory=$true)][string]$FileName,
    [Parameter(Mandatory=$true)][string]$ExpectedCompanionSha256,
    [ValidateSet('PASS','STALE_LIBRARY','EXPIRED_ITEM')][string]$Expected='PASS',
    [string]$Serial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
$out=Join-Path $repo ('artifacts/mango-file-validation-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,6))
$ctx=New-MangoProbeContext $out $Serial $Adb $false 37
$summary=[ordered]@{kind='source-local-production-folder-refresh-validation-only-no-playback-no-BLE';sourceCommit=(git -C $repo rev-parse HEAD).Trim();sourceTreeDirty=[bool](git -C $repo status --porcelain);pageSha256=(Get-FileHash -LiteralPath $PageFile).Hash.ToLowerInvariant();fileName=$FileName;expected=$Expected;expectedCompanionSha256=$ExpectedCompanionSha256;status='FAILED_OR_INTERRUPTED'}
try {
    if($ExpectedCompanionSha256 -notmatch '^[a-f0-9]{64}$'){throw 'Expected exact installed APK hash'}
    $path=((& $Adb -s $Serial shell pm path org.dx340.remote.host)-replace '^package:','').Trim()
    & $Adb -s $Serial pull $path (Join-Path $out 'installed.apk') > (Join-Path $out 'pull.txt')
    if($LASTEXITCODE -ne 0 -or (Get-FileHash (Join-Path $out 'installed.apk')).Hash.ToLowerInvariant() -cne $ExpectedCompanionSha256){throw 'Installed companion differs'}
    Assert-MangoOffline $ctx
    $null=Invoke-MangoProbe $ctx GET_STATE
    $page=Get-Content -LiteralPath $PageFile -Raw|ConvertFrom-Json
    $row=@($page.data.items|Where-Object{$_.title -ceq $FileName -and $_.playable -and !$_.browsable})
    if($page.requestedSource -cne 'com.ibasso.music' -or !$page.data.uiAssisted -or $row.Count -ne 1 -or $row[0].id -notmatch '^mango-ui:[A-Za-z0-9:_-]+$' -or $page.data.revision -notmatch '^[A-Za-z0-9:_-]+$'){throw 'Expected exact held assisted-file proof'}
    $tag='file_validation_'+[guid]::NewGuid().ToString('N').Substring(0,12)
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation VALIDATE_MANGO_UI_ITEM_FRESH --es source com.ibasso.music --es item $row[0].id --es revision $page.data.revision --es reportTag $tag > (Join-Path $out 'validation-broadcast.txt')
    $raw=& $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
    $raw|Set-Content (Join-Path $out 'validation.json')
    $result=($raw -join "`n")|ConvertFrom-Json
    $summary.observedError=$result.error
    $summary.elapsedMs=$result.elapsedMs
    if($result.phase -cne 'FINISHED' -or !$result.data.validationOnly -or $result.data.dispatchedPlayback){throw 'Not a completed validation-only result'}
    if(($Expected -ceq 'PASS' -and $result.error) -or ($Expected -cne 'PASS' -and $result.error -cne $Expected)){throw "Unexpected result: $($result.error)"}
    foreach($states in @($result.sourceStatesBefore,$result.sourceStatesAfter)) {
        if(@($states).Count -ne 2 -or @($states|Where-Object playing).Count){throw 'Both source sessions must remain paused'}
    }
    if($result.state.volume.current -ne 37){throw 'Output level changed'}
    $null=Invoke-MangoProbe $ctx GET_STATE
    $summary.status='PASS_WITHIN_VALIDATION_ONLY_SCOPE'
} finally {
    $summary.completedUtc=[DateTime]::UtcNow.ToString('o')
    $summary|ConvertTo-Json -Depth 8|Set-Content (Join-Path $out 'summary.json')
    Write-Output (Join-Path $out 'summary.json')
}
