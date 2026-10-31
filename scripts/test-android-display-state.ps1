$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'android-display-state.ps1')
$panel='DisplayDeviceInfo{"Built-in Screen": uniqueId="local:fixture", state-placeholder, type INTERNAL, FLAG_ALLOWED_TO_BE_DEFAULT_DISPLAY}'
$off=$panel.Replace('state-placeholder','state OFF, committedState OFF')
$on=$panel.Replace('state-placeholder','state ON, committedState ON')
$doze=$panel.Replace('state-placeholder','state DOZE, committedState DOZE')
$external=$off.Replace('type INTERNAL','type EXTERNAL')
$cases=@(
    @($off,'OFF'),@($on,'ON'),@($doze,'DOZE'),@($external,'Unknown'),
    @("mDisplayState=OFF`n$on",'ON'),@("$external`n$on",'ON'),
    @("$off`n$on",'Unknown'),@('mDisplayState=OFF','Unknown'),
    @($panel,'Unknown')
)
foreach($case in $cases){
    $actual=Get-InternalDefaultDisplayState $case[0]
    if($actual-cne$case[1]){throw "Expected $($case[1]), got $actual"}
}
Write-Output "$($cases.Count) display evidence parser cases passed"
