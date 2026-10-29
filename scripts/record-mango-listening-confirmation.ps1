param(
    [Parameter(Mandatory=$true)][string]$RunDirectory,
    [Parameter(Mandatory=$true)][ValidateSet('Confirmed','Failed','Stopped')][string]$Result,
    [Parameter(Mandatory=$true)][ValidateLength(1,500)][string]$Statement
)
# Record only an actual human answer. No device command and no inferred audio result.
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
$allowed=[IO.Path]::GetFullPath((Join-Path $repo 'artifacts/mango-listening'))+[IO.Path]::DirectorySeparatorChar
$run=(Resolve-Path -LiteralPath $RunDirectory).Path
if (!$run.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)) { throw 'Attempt must be inside this repository Mango listening directory' }
$file=Join-Path $run 'summary.json'
$report=Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
if ($report.kind -ne 'actual-offline-Mango-own-adapter-selection-no-BLE' -or $report.status -ne 'AWAITING_HUMAN_LISTENING_CONFIRMATION' -or $report.audibleConfirmation) { throw 'Only a pending actual Mango selection may receive a listening result' }
if ($Result -eq 'Confirmed' -and (!$report.selectionSent -or !$report.observedCorrectPlayingState -or $report.fileHeader.format -ne $report.candidate.formatLabel)) { throw 'Matching selection/state/container evidence required' }
$report.audibleConfirmation=[ordered]@{result=$Result;answer=$Statement;recordedUtc=[DateTime]::UtcNow.ToString('o');reportedBy='human listener';scope='Delayed human report separate from observed state. No phone BLE, output-rate/bit-perfect or quality measurement.'}
$report.status=switch($Result) { 'Confirmed' {'VERIFIED_LOCAL_SELECTION_AND_USER_REPORTED_AUDIO'} 'Failed' {'FAILED_USER_LISTENING'} 'Stopped' {'STOPPED_USER_LISTENING'} }
$report | ConvertTo-Json -Depth 9 | Set-Content -LiteralPath $file
Write-Output "Recorded Mango listener result for selection $($report.sequence): $Result"
