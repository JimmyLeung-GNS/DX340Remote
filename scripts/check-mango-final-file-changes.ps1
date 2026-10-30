param(
    [Parameter(Mandatory=$true)][string]$ExpectedCompanionSha256,
    [string]$Serial='d',
    [string]$Adb="$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string]$Python='python'
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'mango-listening-tools.ps1')
$repo=Split-Path $PSScriptRoot -Parent
$fixture=(& $Python (Join-Path $PSScriptRoot 'prepare-library-change-fixture.py') --output-parent (Join-Path $repo 'artifacts')) -join ''
if($LASTEXITCODE -ne 0){throw 'Fixture generation failed'}
$m=Get-Content (Join-Path $fixture 'manifest.json') -Raw|ConvertFrom-Json
if($m.identity -notmatch '^[0-9a-f]{32}$' -or $m.deviceDirectory -cne ('/sdcard/Music/DX340RemoteVerification-'+$m.identity)){throw 'Unsafe owned directory'}
$remote=$m.deviceDirectory
$summary=[ordered]@{kind='final-package-physical-folder-rename-removal-validation-no-playback-no-BLE';sourceCommit=(git -C $repo rev-parse HEAD).Trim();sourceTreeDirty=[bool](git -C $repo status --porcelain);helperSha256=(Get-FileHash $PSCommandPath).Hash.ToLowerInvariant();expectedCompanionSha256=$ExpectedCompanionSha256;fixture=$fixture;checks=@();status='FAILED_OR_INTERRUPTED';cleanup='NOT_CREATED'}
$ctx=New-MangoProbeContext (Join-Path $fixture 'probes') $Serial $Adb $false 37
$created=$false
function Assert-Owned([string]$Name,[string]$Hash) {
    $actual=((& $Adb -s $Serial shell sha256sum "$remote/$Name") -split '\s+')[0]
    if($LASTEXITCODE -ne 0 -or $actual -cne $Hash){throw "Owned content differs: $Name; refusing mutation"}
}
function Hold-Page([string]$Label) {
    $page=Open-MangoLocation $ctx @('Internal Storage','Music',('DX340RemoteVerification-'+$m.identity))
    $path=Join-Path $fixture "$Label.json"
    $page|ConvertTo-Json -Depth 12|Set-Content $path
    return $path
}
function Check-Page([string]$Page,[string]$Name,[string]$Expected) {
    & (Join-Path $PSScriptRoot 'check-mango-file-validation.ps1') -PageFile $Page -FileName $Name -ExpectedCompanionSha256 $ExpectedCompanionSha256 -Expected $Expected -Serial $Serial -Adb $Adb
    if($LASTEXITCODE -ne 0){throw 'Validation subprocess failed'}
}
try {
    Assert-MangoOffline $ctx
    $null=Invoke-MangoProbe $ctx GET_STATE
    & $Adb -s $Serial shell test ! -e $remote
    if($LASTEXITCODE -ne 0){throw 'Refusing existing destination'}
    & $Adb -s $Serial shell mkdir $remote
    if($LASTEXITCODE -ne 0){throw 'Owned directory creation failed'}
    $created=$true; $summary.cleanup='PENDING'
    foreach($name in @('ownership.txt','probe-A.wav')) {
        & $Adb -s $Serial push (Join-Path $fixture $name) "$remote/$name" > (Join-Path $fixture "push-$name.txt")
        if($LASTEXITCODE -ne 0){throw 'Upload failed'}
        Assert-Owned $name $m.files.$name
    }
    $m.state='UPLOADED_FINAL_PACKAGE_TEST_PENDING_CLEANUP'
    $m|ConvertTo-Json -Depth 8|Set-Content (Join-Path $fixture 'manifest.json')
    Check-Page (Hold-Page 'unchanged-A') 'probe-A.wav' 'PASS'
    $summary.checks+='unchanged generated WAV preparation PASS'
    $held=Hold-Page 'held-A-before-rename'
    Assert-Owned 'ownership.txt' $m.files.'ownership.txt'; Assert-Owned 'probe-A.wav' $m.files.'probe-A.wav'
    & $Adb -s $Serial shell test ! -e "$remote/probe-B.wav"
    if($LASTEXITCODE -ne 0){throw 'Rename destination exists'}
    & $Adb -s $Serial shell mv "$remote/probe-A.wav" "$remote/probe-B.wav"
    if($LASTEXITCODE -ne 0){throw 'Rename failed'}
    Check-Page $held 'probe-A.wav' 'STALE_LIBRARY'
    $summary.checks+='actual rename rejects held A token after source refresh'
    $held=Hold-Page 'held-B-before-removal'
    Assert-Owned 'ownership.txt' $m.files.'ownership.txt'; Assert-Owned 'probe-B.wav' $m.files.'probe-A.wav'
    & $Adb -s $Serial shell rm "$remote/probe-B.wav"
    if($LASTEXITCODE -ne 0){throw 'Owned file removal failed'}
    Check-Page $held 'probe-B.wav' 'STALE_LIBRARY'
    $summary.checks+='actual removal rejects held B token after source refresh'
    $null=Invoke-MangoProbe $ctx GET_STATE
    $summary.status='ALL_THREE_FINAL_PACKAGE_VALIDATION_CASES_PASS'
} finally {
    if($created) {
        try {
            Assert-Owned 'ownership.txt' $m.files.'ownership.txt'
            foreach($name in @('probe-A.wav','probe-B.wav')) {
                & $Adb -s $Serial shell test -e "$remote/$name"
                if($LASTEXITCODE -eq 0) {
                    Assert-Owned $name $m.files.'probe-A.wav'
                    & $Adb -s $Serial shell rm "$remote/$name"
                    if($LASTEXITCODE -ne 0){throw 'Owned WAV cleanup failed'}
                }
            }
            & $Adb -s $Serial shell rm "$remote/ownership.txt"
            if($LASTEXITCODE -ne 0){throw 'Owned marker cleanup failed'}
            & $Adb -s $Serial shell rmdir $remote
            if($LASTEXITCODE -ne 0){throw 'Exact directory not empty; do not recurse'}
            & $Adb -s $Serial shell test ! -e $remote
            if($LASTEXITCODE -ne 0){throw 'Fixture directory remains'}
            $summary.cleanup='VERIFIED_ABSENT'
            $m.state='REMOVED_NO_FIXTURE_REMAINS'
        } catch { $summary.cleanup='FAILED_VERIFY_OWNERSHIP_AND_CLEAN_MANUALLY';$summary.cleanupError=$_.Exception.Message }
    }
    $m|ConvertTo-Json -Depth 8|Set-Content (Join-Path $fixture 'manifest.json')
    $summary.completedUtc=[DateTime]::UtcNow.ToString('o')
    $summary|ConvertTo-Json -Depth 10|Set-Content (Join-Path $fixture 'summary.json')
    Write-Output (Join-Path $fixture 'summary.json')
}
