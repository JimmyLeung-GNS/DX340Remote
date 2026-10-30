param(
    [string]$Serial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
# Actual source adapters with SYNTHETIC storage invalidation. Never mounts,
# unmounts, edits files, plays music, changes volume or touches controller grants.
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/debug-probe-tools.ps1"
$run='storage-'+(Get-Date -Format yyyyMMdd-HHmmss)+'-'+[guid]::NewGuid().ToString('N').Substring(0,5)
$out=Join-Path $repo "artifacts/storage-boundary-probes/$run"
New-Item -ItemType Directory -Path $out -Force|Out-Null
$script:sequence=0
$mango='com.ibasso.music';$apple='com.apple.android.music'
function Probe([string]$Label,[string]$Operation,[string]$Source,[string[]]$Extra=@(),[string]$ExpectedError='') {
    $script:sequence++
    $tag="$run-$($script:sequence)"
    & $Adb -s $Serial shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation $Operation --es source $Source --es reportTag $tag @Extra *> "$out/$Label-broadcast.txt"
    if($LASTEXITCODE-ne0){throw "Broadcast failed: $Label"}
    $r=Read-DebugProbeReceipt -Adb $Adb -Serial $Serial -Tag $tag -Operation $Operation -Source $Source -OutputFile "$out/$Label.json" -TimeoutMs 12000
    if([string]$r.error-cne$ExpectedError){throw "$Label returned $($r.error), expected $ExpectedError"}
    if(@($r.sourceStatesAfter|Where-Object playing).Count){throw "Unexpected playing source after $Label; no playback command was sent"}
    return $r
}
function Browse([string]$Label,[string]$Source,$Parent=$null,[string[]]$Extra=@()) {
    $probeArguments=@()
    if($Parent){$probeArguments=@('--es','parent',[string]$Parent.id,'--es','revision',[string]$Parent.revision)}
    (Probe $Label BROWSE $Source ($probeArguments+$Extra)).data
}
function Child($Data,[string]$Title) {
    $row=@($Data.items|Where-Object {$_.title-ceq$Title -and $_.browsable})
    if($row.Count-ne1){throw "Expected one browsable $Title"}
    [pscustomobject]@{id=$row[0].id;revision=$Data.revision}
}
$before=Probe '00-before' GET_STATE $mango
if($before.sourceStatesAfter.Count-ne2){throw 'Both source sessions must be observable and paused'}
$initialVolume=$before.state.volume.current
$completed=$false
try {
    $monitor=Probe '01-monitor' STORAGE_DIAGNOSTIC $mango
    if(!$monitor.data.active){throw 'Storage monitor is unavailable'}
    $root=Browse '02-native-root' $mango
    $albums=Browse '03-native-albums' $mango (Child $root 'Album')
    $album=@($albums.items|Where-Object browsable|Select-Object -First 1)
    if($album.Count-ne1){throw 'A native Mango album is required'}
    $nativeParent=[pscustomobject]@{id=$album[0].id;revision=$albums.revision}
    $native=Browse '04-native-tracks' $mango $nativeParent
    $nativeTrack=@($native.items|Where-Object playable|Select-Object -First 1)
    if($nativeTrack.Count-ne1){throw 'A verified native Mango item is required'}
    $null=Probe '05-native-proof-before' DESCRIBE_ITEM $mango @('--es','item',[string]$nativeTrack[0].id)
    $queue=Probe '06-apple-queue-before' GET_QUEUE $apple
    if($queue.data.total-le50){throw 'Apple public queue above50 required for continuation check'}

    $folder=Browse '07-folder-root' $mango ([pscustomobject]@{id='mango-ui:root';revision=$root.revision})
    $folder=Browse '08-folder-internal' $mango (Child $folder 'Internal Storage')
    $folder=Browse '09-folder-music' $mango (Child $folder 'Music')
    for($depth=0;$depth-lt8;$depth++){
        $file=@($folder.items|Where-Object playable|Select-Object -First 1)
        if($file.Count){break}
        $next=@($folder.items|Where-Object browsable|Select-Object -First 1)
        if(!$next.Count){throw 'No existing Mango folder/file exposed on this page'}
        $folder=Browse "10-folder-depth-$depth" $mango ([pscustomobject]@{id=$next[0].id;revision=$folder.revision})
    }
    if(!$file.Count){throw 'No file within bounded folder depth'}
    $mangoHeld=@('--es','item',[string]$file[0].id,'--es','revision',[string]$folder.revision)
    $null=Probe '11-folder-proof-before' VALIDATE_MANGO_UI_ITEM $mango $mangoHeld
    $downloaded=Browse '12-apple-downloaded' $apple
    $appleAlbums=Browse '13-apple-albums' $apple (Child $downloaded 'Albums')
    $first=@($appleAlbums.items|Where-Object browsable|Select-Object -First 1)
    if(!$first.Count){throw 'A downloaded Apple album is required'}
    $appleTracks=Browse '14-apple-tracks' $apple ([pscustomobject]@{id=$first[0].id;revision=$appleAlbums.revision})
    $song=@($appleTracks.items|Where-Object playable|Select-Object -First 1)
    if(!$song.Count){throw 'A downloaded Apple song is required'}
    $appleHeld=@('--es','item',[string]$song[0].id,'--es','revision',[string]$appleTracks.revision)
    $null=Probe '15-apple-proof-before' VALIDATE_APPLE_UI_ITEM $apple $appleHeld
    $boundary=Probe '16-synthetic-boundary' INVALIDATE_STORAGE_LIBRARY $apple
    if(!$boundary.data.bothSourceEpochsRetired-or !$boundary.data.before.appleUi.hasFrame-or !$boundary.data.before.mangoUi.hasFrame-or
        $boundary.data.after.appleUi.hasFrame-or $boundary.data.after.mangoUi.hasFrame-or $boundary.data.after.appleUi.offlineContext-or
        $boundary.data.after.callbackEvents-ne$boundary.data.before.callbackEvents){throw 'Synthetic boundary did not retire both real UI/native contexts'}
    $null=Probe '17-stale-apple-item' VALIDATE_APPLE_UI_ITEM $apple $appleHeld 'STALE_LIBRARY'
    $null=Probe '18-stale-mango-item' VALIDATE_MANGO_UI_ITEM $mango $mangoHeld 'STALE_LIBRARY'
    $null=Probe '19-stale-native-proof' DESCRIBE_ITEM $mango @('--es','item',[string]$nativeTrack[0].id) 'EXPIRED_ITEM'
    $null=Probe '20-stale-native-page' BROWSE $mango @('--es','parent',[string]$nativeParent.id,'--es','revision',[string]$nativeParent.revision) 'STALE_LIBRARY'
    $null=Probe '21-stale-queue-continuation' GET_QUEUE $apple @('--ei','offset','50','--es','revision',[string]$queue.data.revision) 'STALE_LIBRARY'
    $freshRoot=Browse '22-fresh-native-root' $mango
    $freshAlbums=Browse '23-fresh-native-albums' $mango (Child $freshRoot 'Album')
    $freshFirst=@($freshAlbums.items|Where-Object browsable|Select-Object -First 1)
    $freshTracks=Browse '24-fresh-native-tracks' $mango ([pscustomobject]@{id=$freshFirst[0].id;revision=$freshAlbums.revision})
    if(!@($freshTracks.items|Where-Object playable).Count){throw 'Native recovery has no playable metadata'}
    $null=Browse '25-fresh-folder-root' $mango ([pscustomobject]@{id='mango-ui:root';revision=$freshRoot.revision})
    $null=Browse '26-fresh-apple-root' $apple
    $pending=@()
    foreach($case in @(@('mango-ui',$mango,'mango-ui:playlists',100),@('apple-ui',$apple,$null,100),@('native',$mango,$null,0))){
        & $Adb -s $Serial shell input keyevent KEYCODE_HOME|Out-Null
        $probeArguments=@('--ei','changeStorageAfterMs',[string]$case[3])
        if($case[2]){$probeArguments+=@('--es','parent',[string]$case[2])}
        $r=Probe "27-pending-$($case[0])" BROWSE $case[1] $probeArguments 'STALE_LIBRARY'
        if($r.interruption.kind-cne'synthetic-storage-boundary-no-SD-event'){throw 'Browse completed before synthetic interruption; no pending-case pass'}
        $null=Browse "28-recovery-$($case[0])" $case[1]
        $pending+=$case[0]
    }
    $completed=$true
} finally {
    $final=Probe '99-final-mango' GET_STATE $mango
    if($final.state.volume.current-ne$initialVolume-or$final.state.volume.verified){throw 'Final output state changed unexpectedly'}
}
[ordered]@{kind='actual-source-adapters-with-synthetic-storage-boundaries-no-SD-event-no-BLE-no-playback';passed=$completed;
    nativeProofAndPageRejected=$true;bothUiFramesRetired=$true;heldUiItemsRejected=$true;oldQueueRevisionRejected=$true;
    freshNativeAndAssistedRecovery=$true;pendingBrowseCases=$pending;bothSourcesPaused=$true;volume=$initialVolume;
    volumeVerified=$false;selectedSource=$mango;requests=$script:sequence;physicalSdRemoval='NOT TESTED';rawEvidence=$out
}|ConvertTo-Json -Depth 5|Tee-Object "$out/summary.json"
