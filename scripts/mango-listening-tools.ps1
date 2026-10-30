# Shared development helpers for source-local Mango inventory/listening, never phone BLE proof.
. (Join-Path $PSScriptRoot 'debug-probe-tools.ps1')
function New-MangoProbeContext([string]$OutputDirectory, [string]$Serial, [string]$Adb, [bool]$AllowSelection = $false, [int]$ExpectedVolume = 37) {
    New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
    [pscustomobject]@{OutputDirectory=$OutputDirectory;Serial=$Serial;Adb=$Adb;AllowSelection=$AllowSelection;ExpectedVolume=$ExpectedVolume;RequestCount=0;SelectionSent=$false}
}
function Assert-MangoOffline($Context) {
    $wifi = ((& $Context.Adb -s $Context.Serial shell settings get global wifi_on) -join '').Trim()
    $network = (& $Context.Adb -s $Context.Serial shell dumpsys connectivity) -join "`n"
    if ($wifi -ne '0' -or $network -notmatch 'Active default network: none') { throw 'Requires Wi-Fi off/no active default network; no settings changed' }
    $network | Set-Content (Join-Path $Context.OutputDirectory 'connectivity.txt')
}
function Invoke-MangoProbe($Context, [string]$Operation = 'BROWSE', [string[]]$Extra = @()) {
    if ($Operation -notin @('BROWSE','GET_STATE','PLAY_ITEM','SET_PLAYING')) { throw 'Operation not allowed' }
    if ($Operation -in @('PLAY_ITEM','SET_PLAYING') -and !$Context.AllowSelection) { throw 'Read-only context cannot control playback' }
    if ($Operation -eq 'PLAY_ITEM') {
        if ($Context.SelectionSent) { throw 'Only one deliberate PLAY_ITEM per attempt; never retry playback automatically' }
        $Context.SelectionSent=$true
    }
    if ($Operation -eq 'SET_PLAYING' -and ($Extra -join ' ') -cne '--ez playing false') { throw 'Helper allows pausing the preceding Mango clip only' }
    $Context.RequestCount++
    if ($Context.RequestCount -gt 400) { throw 'Bounded diagnostic request limit reached' }
    $tag = [guid]::NewGuid().ToString('N')
    $label = '{0:D3}-{1}' -f $Context.RequestCount,$Operation
    & $Context.Adb -s $Context.Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation $Operation --es source com.ibasso.music --es reportTag $tag @Extra > (Join-Path $Context.OutputDirectory "$label-broadcast.txt")
    if ($LASTEXITCODE -ne 0) { throw "Broadcast failed: $label" }
    $r = Read-DebugProbeReceipt -Adb $Context.Adb -Serial $Context.Serial -Tag $tag -Operation $Operation -Source com.ibasso.music -OutputFile (Join-Path $Context.OutputDirectory "$label.json")
    if ($r.phase -ne 'FINISHED' -or $r.operation -ne $Operation -or $r.requestedSource -ne 'com.ibasso.music' -or $r.error) { throw "Failed $label ($($r.error))" }
    foreach ($field in @('sourceStatesBefore','sourceStatesAfter')) {
        $states=@($r.$field)
        if ($states.Count -ne 2 -or @($states | Where-Object { $_.source -eq 'com.apple.android.music' -and $_.playbackState -eq 2 -and !$_.playing }).Count -ne 1) { throw 'Apple must stay paused and both source sessions must be visible' }
        if (!$Context.AllowSelection -and @($states | Where-Object { $_.source -eq 'com.ibasso.music' -and $_.playbackState -eq 2 -and !$_.playing }).Count -ne 1) { throw 'Inventory requires Mango paused; no playback command was sent' }
    }
    if ($r.state.volume.current -ne $Context.ExpectedVolume) { throw 'Reported output level changed; no automatic adjustment sent' }
    return $r
}
function Invoke-MangoNavigate($Context, $Page, [string]$Id, [int]$Number = 0) {
    if ($Id -notmatch '^[A-Za-z0-9:_-]+$' -or $Page.data.revision -notmatch '^[A-Za-z0-9:_-]+$') { throw 'Invalid opaque UI identity' }
    Invoke-MangoProbe $Context 'BROWSE' @('--es','parent',$Id,'--es','revision',$Page.data.revision,'--ei','page',[string]$Number)
}
function Find-MangoRow($Context, $Page, [string]$Title, [bool]$Folder) {
    $header=$Page.data.title
    for ($scroll=0; $scroll -lt 12; $scroll++) {
        if ($Page.data.title -cne $header -or !$Page.data.uiAssisted) { throw 'Folder context changed' }
        $matches=@($Page.data.items | Where-Object { $_.title -ceq $Title -and $_.browsable -eq $Folder -and ($Folder -or $_.playable) })
        if ($matches.Count -gt 1) { throw 'Ambiguous duplicate visible row labels' }
        if ($matches.Count -eq 1) { return [pscustomobject]@{Page=$Page;Row=$matches[0]} }
        if ($null -eq $Page.data.nextPage) { break }
        $Page=Invoke-MangoNavigate $Context $Page $Page.data.parent ([int]$Page.data.nextPage)
    }
    throw "Requested row not found in bounded current folder: $Title"
}
function Open-MangoLocation($Context, [string[]]$Labels) {
    if ($Labels.Count -lt 1 -or $Labels.Count -gt 8 -or $Labels[0] -notin @('Internal Storage','SD Card')) { throw 'Expected bounded source-UI storage/folder labels' }
    $native=Invoke-MangoProbe $Context
    $choice=Invoke-MangoNavigate $Context $native 'mango-ui:root'
    $entry=Find-MangoRow $Context $choice $Labels[0] $true
    $page=Invoke-MangoNavigate $Context $entry.Page $entry.Row.id
    $expected=$Labels[0]
    if ($page.data.title -cne $expected) { throw 'Unexpected storage header' }
    foreach ($label in @($Labels | Select-Object -Skip 1)) {
        $entry=Find-MangoRow $Context $page $label $true
        $page=Invoke-MangoNavigate $Context $entry.Page $entry.Row.id
        $expected+=' / '+$label
        if ($page.data.title -cne $expected) { throw 'Folder breadcrumb changed' }
    }
    return $page
}
function Read-MangoLocation($Context, [string[]]$Labels, [int]$MaxPages = 8) {
    $page=Open-MangoLocation $Context $Labels
    $expected=$Labels -join ' / '
    $observed=[Collections.Generic.Dictionary[string,object]]::new([StringComparer]::Ordinal)
    $complete=$false
    for ($index=0; $index -lt $MaxPages; $index++) {
        if ($page.data.title -cne $expected -or !$page.data.uiAssisted) { throw 'Folder context changed during inventory' }
        foreach ($row in $page.data.items) {
            $key=$row.title+"`0"+$row.browsable+"`0"+$row.playable
            $observed[$key]=[ordered]@{title=$row.title;browsable=$row.browsable;playable=$row.playable;subtitle=$row.subtitle}
        }
        if ($null -eq $page.data.nextPage) { $complete=$true;break }
        $page=Invoke-MangoNavigate $Context $page $page.data.parent ([int]$page.data.nextPage)
    }
    [pscustomobject]@{labels=$Labels;header=$expected;observedRows=@($observed.Values);reachedEnd=$complete;note='Observed source-UI row labels, not filesystem scan or stable global identity'}
}
