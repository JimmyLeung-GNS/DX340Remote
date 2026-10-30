# Software-only receipt orchestration regressions; no device or Bluetooth access.
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'debug-probe-tools.ps1')
$out=Join-Path (Split-Path $PSScriptRoot -Parent) ('work/probe-receipt-tests-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $out|Out-Null
function Test-AdbReceipt {
    if (($args -join ' ') -cne '-s test shell run-as org.dx340.remote.host cat files/debug-probe-unit.json') { throw 'Only exact receipt reads allowed; never send playback' }
    $script:reads++
    $global:LASTEXITCODE=0
    if ($script:responses.Count -gt 1) { $value=$script:responses[0];$script:responses=@($script:responses|Select-Object -Skip 1) } else { $value=$script:responses[0] }
    if ($value -ceq 'READ_FAILURE') { $global:LASTEXITCODE=1;return }
    $value
}
function Read-TestReceipt([string[]]$Responses,[int]$TimeoutMs=1500) {
    $script:responses=$Responses;$script:reads=0
    Read-DebugProbeReceipt -Adb Test-AdbReceipt -Serial test -Tag unit -Operation PLAY_ITEM -Source com.ibasso.music -OutputFile (Join-Path $out 'receipt.json') -TimeoutMs $TimeoutMs
}
function Expect-Error([scriptblock]$Action,[string]$Message) {
    try { &$Action|Out-Null } catch { if ($_.Exception.Message -like "*$Message*") { return };throw }
    throw "Expected failure: $Message"
}
$started='{"phase":"STARTED","operation":"PLAY_ITEM"}'
$finished='{"phase":"FINISHED","operation":"PLAY_ITEM","requestedSource":"com.ibasso.music","error":null}'
$r=Read-TestReceipt @($started,'{"phase":','READ_FAILURE',$finished)
if($r.phase -cne 'FINISHED' -or $script:reads -ne 4){throw 'Did not await same job through partial/missing reads'}
Expect-Error {Read-TestReceipt @($started) 1} 'outcome unknown'
Expect-Error {Read-TestReceipt @('{"phase":"FINISHED","operation":"GET_STATE","requestedSource":"com.ibasso.music"}')} 'Unrecognized diagnostic receipt'
Expect-Error {Read-TestReceipt @('{"phase":"FINISHED","operation":"PLAY_ITEM","requestedSource":"com.apple.android.music"}')} 'source mismatch'
$r=Read-TestReceipt @('{"phase":"FINISHED","operation":"PLAY_ITEM","requestedSource":"com.ibasso.music","error":"DEBUG_PROBE_TIMEOUT"}')
if($r.error -cne 'DEBUG_PROBE_TIMEOUT' -or $script:reads -ne 1){throw 'Device error must be preserved without retry'}
'PASS:5 software receipt cases; no playback/device/Bluetooth operation'
