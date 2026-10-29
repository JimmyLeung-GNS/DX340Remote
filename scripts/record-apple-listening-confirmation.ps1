param(
    [Parameter(Mandatory=$true)][string]$RunDirectory,
    [Parameter(Mandatory=$true)][ValidateSet('Confirmed','Failed','Stopped')][string]$Result,
    [Parameter(Mandatory=$true)][ValidateLength(1,500)][string]$Statement
)
# Record an actual listener's answer only. Never derive it from a compile, write or state.
# This helper sends no device command and cannot measure audio.
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$allowed = [IO.Path]::GetFullPath((Join-Path $repo 'artifacts/apple-listening')) + [IO.Path]::DirectorySeparatorChar
$run = (Resolve-Path -LiteralPath $RunDirectory).Path
if (!$run.StartsWith($allowed, [StringComparison]::OrdinalIgnoreCase)) { throw 'Result must stay inside this repository listening-artifact directory' }
$file = Join-Path $run 'summary.json'
$report = Get-Content -LiteralPath $file -Raw | ConvertFrom-Json
if ($report.kind -ne 'actual-offline-Apple-own-adapter-selection-no-BLE' -or $report.status -ne 'AWAITING_HUMAN_LISTENING_CONFIRMATION' -or $report.audibleConfirmation) { throw 'Only a pending actual selection may receive a new listening result' }
if ($Result -eq 'Confirmed' -and (!$report.selectionSent -or !$report.observedCorrectPlayingState)) { throw 'Source selection/state evidence must exist before combining it with listening confirmation' }
$report.audibleConfirmation = [ordered]@{
    result = $Result; answer = $Statement; recordedUtc = [DateTime]::UtcNow.ToString('o')
    reportedBy = 'human listener'; scope = 'Delayed human report; observed state timestamp remains separate. No BLE/audio measurement claim.'
}
$report.status = switch ($Result) {
    'Confirmed' { 'VERIFIED_LOCAL_SELECTION_AND_USER_REPORTED_AUDIO' }
    'Failed' { 'FAILED_USER_LISTENING' }
    'Stopped' { 'STOPPED_USER_LISTENING' }
}
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $file
Write-Output "Recorded listener result for selection $($report.sequence): $Result"
