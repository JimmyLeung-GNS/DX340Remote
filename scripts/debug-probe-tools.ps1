# Read one nonce-scoped job. Never issues a broadcast or retries a playback command.
function Read-DebugProbeReceipt {
    param(
        [string]$Adb, [string]$Serial, [string]$Tag,
        [string]$Operation, [string]$Source, [string]$OutputFile,
        [ValidateRange(1,35000)][int]$TimeoutMs=35000
    )
    if ($Tag -cnotmatch '^[A-Za-z0-9_-]{1,48}$') { throw 'Invalid report tag' }
    $watch=[Diagnostics.Stopwatch]::StartNew()
    do {
        $raw=& $Adb -s $Serial shell run-as org.dx340.remote.host cat "files/debug-probe-$Tag.json"
        $observed=$null
        if ($LASTEXITCODE -eq 0) {
            try { $observed=($raw -join "`n")|ConvertFrom-Json -ErrorAction Stop } catch { }
        }
        if ($null -ne $observed) {
            if ($observed.operation -cne $Operation -or $observed.phase -cnotin @('STARTED','FINISHED')) { throw 'Unrecognized diagnostic receipt' }
            $raw|Set-Content -LiteralPath $OutputFile
            if ($observed.phase -ceq 'FINISHED') {
                if ($observed.requestedSource -cne $Source) { throw 'Receipt source mismatch' }
                return $observed
            }
        }
        if ($watch.ElapsedMilliseconds -ge $TimeoutMs) { break }
        Start-Sleep -Milliseconds 150
    } while ($true)
    throw 'Diagnostic receipt not finished; playback outcome unknown, never resend automatically'
}
