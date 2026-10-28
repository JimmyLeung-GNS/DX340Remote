param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [ValidateRange(1,10)][int]$Cycles = 2,
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$outDir = Join-Path $repo ('artifacts/handoff-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force $outDir | Out-Null
$sequence = 0
$steps = [System.Collections.Generic.List[object]]::new()
function Invoke-Probe([string]$Operation, [string]$Source, [bool]$Playing = $false) {
    $script:sequence++
    $previousTime = 0L
    $previousRaw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat files/apple-ui-live-test.json 2>$null
    if ($LASTEXITCODE -eq 0) {
        try { $previousTime = (($previousRaw -join "`n") | ConvertFrom-Json).time } catch { }
    }
    $arguments = @('-s',$Serial,'shell','am','broadcast','-a','org.dx340.remote.host.DEBUG_UI_PROBE',
        '-n','org.dx340.remote.host/.DebugUiProbeReceiver','--es','operation',$Operation,'--es','source',$Source)
    if ($Operation -eq 'SET_PLAYING') { $arguments += @('--ez','playing',$Playing.ToString().ToLowerInvariant()) }
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $broadcast = & $Adb @arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($broadcast -join "`n") }
    $raw = & $Adb -s $Serial shell run-as org.dx340.remote.host cat files/apple-ui-live-test.json
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read actual debug probe report' }
    $timer.Stop()
    $raw | Set-Content (Join-Path $outDir ("{0:D2}-{1}.json" -f $script:sequence,$Operation))
    $report = ($raw -join "`n") | ConvertFrom-Json
    if ($report.time -le $previousTime -or $report.operation -ne $Operation -or $report.state.source -ne $Source) {
        throw 'Debug report is stale or does not correspond to the requested operation/source'
    }
    $script:steps.Add(@{operation=$Operation;source=$Source;playing=$Playing;elapsedAdbMs=$timer.ElapsedMilliseconds;report=$report})
    if ($report.error) { throw "$Operation $Source returned $($report.error)" }
    return $report
}
$original = $null
$passed = $false
$failure = $null
$restoreFailure = $null
try {
    $before = Invoke-Probe 'GET_STATE' 'com.apple.android.music'
    if ($before.sourceStatesAfter.Count -ne 2) { throw 'Open both installed players first; both existing source sessions are required' }
    $active = @($before.sourceStatesAfter | Where-Object playing)
    if ($active.Count -ne 1) { throw 'Exactly one source must already be playing before this test' }
    $original = $active[0].source
    for ($cycle=0; $cycle -lt $Cycles; $cycle++) {
        foreach ($source in @('com.ibasso.music','com.apple.android.music')) {
            foreach ($repeat in 1..2) {
                $result = Invoke-Probe 'SET_PLAYING' $source $true
                $target = @($result.sourceStatesAfter | Where-Object source -eq $source)
                $other = @($result.sourceStatesAfter | Where-Object source -ne $source)
                if ($target.Count -ne 1 -or !$target[0].playing -or $other.Count -ne 1 -or $other[0].playbackState -ne 2) {
                    throw 'Target was not observed playing with the previous source paused'
                }
                Write-Output "Cycle $($cycle+1), $source request ${repeat}: target playing / other paused"
            }
        }
    }
    $passed = $true
} catch { $failure = $_.Exception.Message }
finally {
    if ($original) {
        try { $null = Invoke-Probe 'SET_PLAYING' $original $true }
        catch { $restoreFailure = $_.Exception.Message }
    }
    @{kind='physical-source-handoff-observations-no-ble-no-audio-judgment';passed=$passed;error=$failure;
        restoredOriginalSource=$original;restoreError=$restoreFailure;steps=$steps} |
        ConvertTo-Json -Depth 30 | Set-Content (Join-Path $outDir 'summary.json')
}
Write-Output "Evidence: $outDir/summary.json"
if ($failure -or $restoreFailure) { throw "Handoff check failed: $failure; restore: $restoreFailure" }
