param(
    [Parameter(Mandatory=$true)][string]$AttemptDirectory,
    [Parameter(Mandatory=$true)][ValidateSet('Correct','Incorrect')][string]$Result,
    [Parameter(Mandatory=$true)][string]$UserQuote,
    [string]$Serial = 'd',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [int]$ExpectedVolume = 37
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
$folder=(Resolve-Path -LiteralPath $AttemptDirectory).Path
$allowedRoot=[IO.Path]::GetFullPath((Join-Path $repo 'artifacts/mango-native-matrix'))+[IO.Path]::DirectorySeparatorChar
if (!$folder.StartsWith($allowedRoot,[StringComparison]::OrdinalIgnoreCase)) { throw 'Attempt must be under this repository native matrix artifacts' }
$summaryPath=Join-Path $folder 'summary.json'
$summary=Get-Content -LiteralPath $summaryPath -Raw | ConvertFrom-Json -AsHashtable
if ($summary.status -ne 'AWAITING_HUMAN_AUDIO_CONFIRMATION' -or !$summary.observedCorrectPlayingState -or $summary.humanAudio) { throw 'Expected one pending observed selection; do not overwrite results' }
if ([string]::IsNullOrWhiteSpace($UserQuote)) { throw 'Actual user answer required; never infer listening from state' }
$summary['humanAudio']=$UserQuote
$summary['humanResult']=$Result
$summary['confirmedUtc']=[DateTime]::UtcNow.ToString('o')
$summary['status']=if($Result -eq 'Correct'){'USER_CONFIRMED_AUDIBLE'}else{'USER_REPORTED_INCORRECT'}
# Preserve the answer before cleanup; a pause failure cannot erase it.
$summary | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $summaryPath
$probe=New-MangoProbeContext (Join-Path $folder 'after-confirmation') $Serial $Adb $true $ExpectedVolume
try {
    $before=Invoke-MangoProbe $probe 'GET_STATE'
    $summary['confirmationObservedState']=$before.state
    $paused=Invoke-MangoProbe $probe 'SET_PLAYING' @('--ez','playing','false')
    $summary['pausedAfterConfirmation']=!$paused.state.playing
    if (!$summary['pausedAfterConfirmation']) { throw 'Pause not observed' }
    Assert-MangoOffline $probe
} finally { $summary | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $summaryPath }
Write-Output "Recorded actual human answer and paused: $folder"
