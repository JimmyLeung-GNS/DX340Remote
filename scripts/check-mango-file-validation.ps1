param(
    [Parameter(Mandatory=$true)][string]$PageFile,
    [Parameter(Mandatory=$true)][string]$FileName,
    [Parameter(Mandatory=$true)][string]$ExpectedCompanionSha256,
    [ValidateSet('PASS','STALE_LIBRARY','EXPIRED_ITEM','SAFE_HOME_INTERRUPTION','AUTHORIZATION_DENIED')][string]$Expected='PASS',
    [ValidateSet('None','Home','SourceChange')][string]$Interrupt='None',
    [string]$Serial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
$out=Join-Path $repo ('artifacts/mango-file-validation-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,6))
$ctx=New-MangoProbeContext $out $Serial $Adb $false 37
$summary=[ordered]@{kind='source-local-production-folder-refresh-validation-only-no-playback-no-BLE';sourceCommit=(git -C $repo rev-parse HEAD).Trim();sourceTreeDirty=[bool](git -C $repo status --porcelain);pageSha256=(Get-FileHash -LiteralPath $PageFile).Hash.ToLowerInvariant();fileName=$FileName;expected=$Expected;expectedCompanionSha256=$ExpectedCompanionSha256;status='FAILED_OR_INTERRUPTED'}
$summary.helperSha256=(Get-FileHash -LiteralPath $PSCommandPath).Hash.ToLowerInvariant()
$summary.interruption=$Interrupt
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
    $summary.reportTag=$tag
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation VALIDATE_MANGO_UI_ITEM_FRESH --es source com.ibasso.music --es item $row[0].id --es revision $page.data.revision --es reportTag $tag > (Join-Path $out 'validation-broadcast.txt')
    if($Interrupt -cne 'None') {
        $summary.interruptionStartedUtc=[DateTime]::UtcNow.ToString('o')
        if($Interrupt -ceq 'Home') { & $Adb -s $Serial shell input keyevent 3 }
        else {
            $switchTag='source_change_'+[guid]::NewGuid().ToString('N').Substring(0,12)
            & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation GET_STATE --es source com.apple.android.music --es reportTag $switchTag > (Join-Path $out 'source-change-broadcast.txt')
            & $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$switchTag.json"|Set-Content (Join-Path $out 'source-change.json')
        }
        if($LASTEXITCODE -ne 0){throw 'Interruption action failed'}
    }
    $deadline=[DateTime]::UtcNow.AddSeconds(27)
    $observationFailures=0
    $result=$null
    do {
        $raw=& $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$tag.json"
        if($LASTEXITCODE -ne 0){$observationFailures++}
        else {
            try {$observed=($raw -join "`n")|ConvertFrom-Json -ErrorAction Stop} catch {$observed=$null;$observationFailures++}
            if($null -ne $observed) {
                if($observed.operation -cne 'VALIDATE_MANGO_UI_ITEM_FRESH' -or $observed.phase -cnotin @('STARTED','FINISHED')){throw 'Unrecognized diagnostic job state'}
                $result=$observed
                $raw|Set-Content (Join-Path $out 'validation.json')
                if($result.phase -ceq 'FINISHED'){break}
            }
        }
        Start-Sleep -Milliseconds 150
    } while([DateTime]::UtcNow -lt $deadline)
    $summary.observationFailures=$observationFailures
    $summary.observedError=$result.error
    $summary.elapsedMs=$result.elapsedMs
    if($result.phase -cne 'FINISHED' -or !$result.data.validationOnly -or $result.data.dispatchedPlayback){throw 'Not a completed validation-only result'}
    $expectedErrors=if($Expected -ceq 'SAFE_HOME_INTERRUPTION'){@('UI_FOREGROUND_REQUIRED','UI_UNEXPECTED_SCREEN','UI_ACTION_INTERRUPTED')}elseif($Expected -ceq 'PASS'){@()}else{@($Expected)}
    if(($Expected -ceq 'PASS' -and $result.error) -or ($Expected -cne 'PASS' -and $result.error -cnotin $expectedErrors)){throw "Unexpected result: $($result.error)"}
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
