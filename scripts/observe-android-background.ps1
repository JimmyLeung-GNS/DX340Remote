param(
    [Parameter(Mandatory=$true)][string]$PhoneSerial,
    [string]$DxSerial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [ValidateRange(1800,7200)][int]$DurationSeconds=1800,
    [ValidateRange(15,60)][int]$SampleSeconds=30,
    [ValidateSet('Any','Asleep','Dozing')][string]$ExpectedDxWakefulness='Any',
    [ValidateSet('Any','OFF')][string]$ExpectedDxDisplayState='Any',
    [string]$StopFile
)
# Read-only USB-powered observation. Connect/authorize the ordinary phone app,
# put it in the background and set the intended screen state before running.
# No app launch, BLE command, radio/permission change, playback or volume action.
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'android-display-state.ps1')
$out=Join-Path $repo ('artifacts/android-background-hold/'+(Get-Date -Format yyyyMMdd-HHmmss)+'-'+[guid]::NewGuid().ToString('N').Substring(0,5))
New-Item -ItemType Directory -Path $out -Force|Out-Null
function Read-Adb([string]$Serial,[string[]]$Arguments){
    $value=& $Adb -s $Serial @Arguments 2>&1
    if($LASTEXITCODE-ne0){throw "Device observation failed ($($Arguments[0])); do not restart the experiment solely from a monitoring failure"}
    $value-join"`n"
}
$phonePid=Read-Adb $PhoneSerial @('shell','pidof','org.dx340.remote.controller')
$hostPid=Read-Adb $DxSerial @('shell','pidof','org.dx340.remote.host')
if(!$phonePid-or!$hostPid){throw 'Both existing app processes are required'}
$clock=[Diagnostics.Stopwatch]::StartNew()
$seen=[Collections.Generic.HashSet[string]]::new()
$samples=@();$failure=$null;$operatorStopped=$false
$startingVolume=$null;$startingRoute=$null
function Sample {
    $i=$script:samples.Count
    $tag='{0:D3}'-f$i
    $phone=Read-Adb $PhoneSerial @('shell','dumpsys','activity','services','org.dx340.remote.controller')
    $hostServiceSnapshot=Read-Adb $DxSerial @('shell','dumpsys','activity','services','org.dx340.remote.host')
    $media=Read-Adb $DxSerial @('shell','dumpsys','media_session')
    $power=Read-Adb $DxSerial @('shell','dumpsys','power')
    $display=Read-Adb $DxSerial @('shell','dumpsys','display')
    $phonePower=Read-Adb $PhoneSerial @('shell','dumpsys','power')
    $phoneActivities=Read-Adb $PhoneSerial @('shell','dumpsys','activity','activities')
    $audio=Read-Adb $DxSerial @('shell','dumpsys','audio')
    $gatt=Read-Adb $PhoneSerial @('logcat','-d','-v','threadtime','BluetoothGatt:D','DX340RemoteClient:I','*:S')
    $hostEvents=Read-Adb $DxSerial @('logcat','-d','-v','threadtime','DX340Remote:I','*:S')
    $phone|Set-Content "$out/$tag-phone-service.txt";$hostServiceSnapshot|Set-Content "$out/$tag-host-service.txt"
    $media|Set-Content "$out/$tag-media.txt";$power|Set-Content "$out/$tag-dx-power.txt"
    $display|Set-Content "$out/$tag-dx-display.txt"
    $phonePower|Set-Content "$out/$tag-phone-power.txt";$hostEvents|Set-Content "$out/$tag-host-events.txt"
    $phoneActivities|Set-Content "$out/$tag-phone-activities.txt"
    $audio|Set-Content "$out/$tag-dx-audio.txt"
    $gatt|Set-Content "$out/$tag-phone-gatt.txt"
    foreach($line in ($gatt-split"`n")){if($seen.Add($line)){$line|Add-Content "$out/unique-gatt-lines.txt"}}
    $pNow=Read-Adb $PhoneSerial @('shell','pidof','org.dx340.remote.controller')
    $hNow=Read-Adb $DxSerial @('shell','pidof','org.dx340.remote.host')
    $states=@();foreach($package in @('com.ibasso.music','com.apple.android.music')){
        $match=[regex]::Match($media,'(?s)package='+[regex]::Escape($package)+'.*?state=PlaybackState \{state=(\d+)')
        if(!$match.Success){throw 'Both source states must remain observable'}
        $states+=[int]$match.Groups[1].Value
    }
    $volume=[regex]::Match($audio,'(?s)- STREAM_MUSIC:.*?streamVolume:(\d+)')
    $route=[regex]::Match($audio,'(?s)- STREAM_MUSIC:.*?Devices: ([^\r\n]+)')
    if(!$volume.Success-or!$route.Success){throw 'Music output observation unavailable'}
    $level=[int]$volume.Groups[1].Value;$output=$route.Groups[1].Value.Trim()
    $phoneWakefulness=[regex]::Match($phonePower,'mWakefulness=(\w+)').Groups[1].Value
    $dxWakefulness=[regex]::Match($power,'mWakefulness=(\w+)').Groups[1].Value
    $dxDisplayState=Get-InternalDefaultDisplayState $display
    $resumed=[regex]::Match($phoneActivities,'(?m)^\s*(?:topResumedActivity|mResumedActivity)=(.+)$').Groups[1].Value
    $phoneTop=[regex]::Match($resumed,'u\d+\s+([A-Za-z0-9_.$]+/[A-Za-z0-9_.$]+)').Groups[1].Value
    $phoneAppBackground=if($phoneTop){!$phoneTop.StartsWith('org.dx340.remote.controller/')}else{$phoneWakefulness-in@('Asleep','Dozing')}
    if($null-eq$script:startingVolume){$script:startingVolume=$level;$script:startingRoute=$output}
    $row=[ordered]@{at=(Get-Date -Format o);elapsedSeconds=[math]::Round($clock.Elapsed.TotalSeconds,3);
        phoneProcessUnchanged=($pNow-ceq$phonePid);hostProcessUnchanged=($hNow-ceq$hostPid);
        phoneForegroundService=($phone-match'isForeground=true');hostForegroundService=($hostServiceSnapshot-match'isForeground=true');
        sourceStates=$states;musicVolume=$level;musicRoute=$output;outputUnchanged=($level-eq$script:startingVolume-and$output-ceq$script:startingRoute);
        dxWakefulness=$dxWakefulness;expectedDxWakefulness=$ExpectedDxWakefulness;
        dxWakefulnessMatches=($ExpectedDxWakefulness-eq'Any'-or$dxWakefulness-ceq$ExpectedDxWakefulness);
        dxDisplayState=$dxDisplayState;expectedDxDisplayState=$ExpectedDxDisplayState;
        dxDisplayMatches=($ExpectedDxDisplayState-eq'Any'-or$dxDisplayState-ceq$ExpectedDxDisplayState);
        phoneWakefulness=$phoneWakefulness;phoneTopActivity=$phoneTop;phoneAppBackground=$phoneAppBackground}
    $script:samples+=,$row
    $row|ConvertTo-Json -Compress|Add-Content "$out/samples.jsonl"
    $row|ConvertTo-Json -Compress|Write-Output
    if(!$row.dxWakefulnessMatches){throw 'Observed DX340 wakefulness differs from the required hold state; do not infer a screen-off pass'}
    if(!$row.dxDisplayMatches){throw 'Observed internal DX340 display differs from the required hold state; do not infer a display-off pass'}
    if(!$row.phoneAppBackground-or!$row.phoneProcessUnchanged-or!$row.hostProcessUnchanged-or!$row.phoneForegroundService-or!$row.hostForegroundService-or!$row.outputUnchanged-or@($states|Where-Object {$_-ne2}).Count){throw 'Observed background/process/service/source/output condition failed; inspect this run, do not infer a continuous pass'}
}
try {
    do {
        if($StopFile-and(Test-Path -LiteralPath $StopFile)){$operatorStopped=$true;$failure='Stopped by operator; no completed observation claim';break}
        Sample
        $remaining=$DurationSeconds-$clock.Elapsed.TotalSeconds
        if($remaining-le0){break}
        Start-Sleep -Milliseconds ([int]([math]::Min($SampleSeconds,$remaining)*1000))
    } while($true)
} catch {$failure=$_.Exception.Message;throw}
finally {
    [ordered]@{kind='physical-USB-powered-Android-background-observation-no-BLE-commands';
        requestedSeconds=$DurationSeconds;elapsedSeconds=[math]::Round($clock.Elapsed.TotalSeconds,3);samples=$samples.Count;
        expectedDxWakefulness=$ExpectedDxWakefulness;
        expectedDxDisplayState=$ExpectedDxDisplayState;
        observationComplete=(!$operatorStopped-and$null-eq$failure-and$clock.Elapsed.TotalSeconds-ge$DurationSeconds);failure=$failure;operatorStopped=$operatorStopped;
        postReturnBluetoothRead='NOT TESTED; foreground the ordinary app and verify fresh reads separately';
        limits='No audio, unlocked/unplugged reliability, range, battery, radio continuity or full Gate A/T05 pass follows from service/process observations alone';
        rawEvidence=$out}|ConvertTo-Json -Depth 5|Set-Content "$out/summary.json"
}
