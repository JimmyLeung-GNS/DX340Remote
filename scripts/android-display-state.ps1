function Get-InternalDefaultDisplayState([string]$Snapshot) {
    # Inspect actual device records, not a requested/logical display or an external panel.
    $devices = @($Snapshot -split "`n" | Where-Object {
        $_ -match 'DisplayDeviceInfo\{' -and $_ -match '\btype INTERNAL\b' -and
        $_ -match '\bFLAG_ALLOWED_TO_BE_DEFAULT_DISPLAY\b'
    })
    if ($devices.Count -ne 1) { return 'Unknown' }
    $state = [regex]::Match($devices[0], ', state (\w+),')
    if (!$state.Success) { return 'Unknown' }
    return $state.Groups[1].Value
}
