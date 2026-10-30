param([string]$Serial='d', [string]$Adb='', [string]$Python='', [string]$ControllerSerial='')
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
Set-Location $repo
if (!$Python) { $Python=(Get-Command python -ErrorAction SilentlyContinue).Source }
if (!$Python) { $Python=(Get-Command python3 -ErrorAction SilentlyContinue).Source }
if (!$Python) { throw 'Python3.9+ is required; pass its actual path with -Python' }
& $Python -c 'import sys; assert sys.version_info >= (3, 9), "Python3.9+ required"'
if ($LASTEXITCODE) { throw 'Selected Python is unavailable or older than3.9' }
if ((& git status --porcelain)) { throw 'Commit the source/docs first; checkpoint must be reviewable and clean' }
$head=(& git rev-parse HEAD).Trim()
$branch=(& git branch --show-current).Trim()
$previous=Get-Content (Join-Path $repo 'artifacts/checkpoint-manifest.json') -Raw | ConvertFrom-Json
if ($branch -ne 'codex/source-recovery-checkpoint') { throw 'Unexpected checkpoint branch' }
$out=Join-Path $repo ('artifacts/device-checkpoint-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'-'+$head.Substring(0,7))
if (Test-Path $out) { throw 'Checkpoint directory exists; do not overwrite' }
New-Item -ItemType Directory $out | Out-Null
& git bundle create "$out/dx340-checkpoint.bundle" $branch
if ($LASTEXITCODE) { throw 'Bundle creation failed' }
Copy-Item docs/device-continuation.md "$out/START_HERE.md"
Copy-Item scripts/verify-device-checkpoint.py "$out/verify-checkpoint.py"
$packages=@()
function Copy-VerifiedInstalledBackup([string]$Role, [string]$Destination) {
    $record=@($previous.preservedInstalledPackages | Where-Object { $_.role -eq $Role })
    if ($record.Count -ne 1) { throw "No recorded installed backup for $Role; do not substitute older bytes" }
    $backup=Join-Path $repo ('artifacts/'+$record[0].file)
    if ((Get-FileHash $backup).Hash.ToLowerInvariant() -ne $record[0].sha256) { throw "Installed backup hash mismatch for $Role" }
    Copy-Item -LiteralPath $backup -Destination $Destination
}
if (!$Adb) { $Adb=(Get-Command adb -ErrorAction SilentlyContinue).Source }
$online=$false
if ($Adb) { $model=((& $Adb -s $Serial shell getprop ro.product.model 2>$null) -join '').Trim(); $online=$LASTEXITCODE -eq 0 -and $model -eq 'DX340' }
foreach ($role in @(@('installed-companion','org.dx340.remote.host'),@('installed-shared-phone','org.dx340.remote.controller'))) {
    $file=$role[0]+'.apk'
    if ($online) {
        $path=((& $Adb -s $Serial shell pm path $role[1] | Select-Object -First 1) -replace '^package:','').Trim()
        if ($path -notmatch '^/data/app/[A-Za-z0-9_~+./=-]+/base\.apk$') { throw 'Unexpected installed APK path' }
        & $Adb -s $Serial pull $path "$out/$file" > "$out/$($role[0])-pull.txt"
        if ($LASTEXITCODE) { throw 'Installed APK preservation failed' }
        $scope='Fresh installed DX340 bytes; earlier listening/rendering reports keep their recorded source/package scope'
    } else {
        Copy-VerifiedInstalledBackup $role[0] "$out/$file"
        $scope='Previously verified installed backup; DX340 unavailable during export, current device state not reverified'
    }
    $packages += [ordered]@{role=$role[0];file=$file;sha256=(Get-FileHash "$out/$file").Hash.ToLowerInvariant();scope=$scope}
}
if ($ControllerSerial) {
    if (!$Adb) { throw 'ADB is required to preserve a separate controller phone' }
    $model=((& $Adb -s $ControllerSerial shell getprop ro.product.model 2>$null) -join '').Trim()
    if ($LASTEXITCODE -ne 0 -or !$model) {
        $file='installed-android-controller-phone.apk'
        Copy-VerifiedInstalledBackup 'installed-android-controller-phone' "$out/$file"
        $packages += [ordered]@{role='installed-android-controller-phone';file=$file;sha256=(Get-FileHash "$out/$file").Hash.ToLowerInvariant();scope='Previously pulled and hash-verified separate controller backup; device unavailable at export. Current device state not reverified. No bond, private app data or signing key exported.'}
    } else {
    if ($model -eq 'DX340') { throw 'Expected a separate controller phone' }
    $path=((& $Adb -s $ControllerSerial shell pm path org.dx340.remote.controller | Select-Object -First 1) -replace '^package:','').Trim()
    if ($path -notmatch '^/data/app/[A-Za-z0-9_~+./=-]+/base\.apk$') { throw 'Unexpected controller APK path' }
    $file='installed-android-controller-phone.apk'
    & $Adb -s $ControllerSerial pull $path "$out/$file" > "$out/controller-phone-pull.txt"
    if ($LASTEXITCODE) { throw 'Controller phone APK preservation failed' }
    $packages += [ordered]@{role='installed-android-controller-phone';file=$file;sha256=(Get-FileHash "$out/$file").Hash.ToLowerInvariant();deviceModel=$model;scope='Fresh separate Android controller bytes; inspect the dated phone report for source/build and physical verification scope. No OS bond, private app data or signing key exported.'}
    }
}
foreach ($file in @('dx340-companion-0.1.0-debug.apk','dx340-phone-0.2.0-development.apk')) {
    $record=@($previous.candidatePackages | Where-Object { $_.file -eq $file })
    $digest=(Get-FileHash (Join-Path $repo "artifacts/$file")).Hash.ToLowerInvariant()
    if ($record.Count -ne 1 -or $record[0].sha256 -ne $digest) { throw 'Candidate package differs from the prior build record' }
    Copy-Item (Join-Path $repo "artifacts/$file") (Join-Path $out ('candidate-'+$file))
    $matchesInstalled=@($packages | Where-Object { $_.role -like 'installed-*' -and $_.sha256 -eq $digest }).Count -gt 0
    $packages += [ordered]@{role=if($matchesInstalled){'build-output-matches-installed'}else{'uninstalled-candidate'};file='candidate-'+$file;sha256=$digest;buildSourceCommit=$record[0].sourceCommit;matchesInstalledPackage=$matchesInstalled;scope=if($matchesInstalled){'Build output matches preserved installed bytes; earlier evidence keeps its original scope'}else{'Existing build candidate not installed by this export; match prior source/evidence before testing'}}
}
$tracked=& git ls-files
if (@($tracked | Where-Object { $_ -match '(?i)\.(keystore|jks|p12|pfx|mobileprovision|apk)$' }).Count) { throw 'Signing/package file unexpectedly tracked; do not package' }
$files=@(Get-ChildItem $out -File | Sort-Object Name | ForEach-Object { [ordered]@{file=$_.Name;sha256=(Get-FileHash $_.FullName).Hash.ToLowerInvariant()} })
[ordered]@{formatVersion=1;createdUtc=[DateTime]::UtcNow.ToString('o');sourceCommit=$head;sourceBranch=$branch;files=$files;packages=$packages;pcAccess=[ordered]@{preference='Keep PC development access; no revocation tests unless requested';perIdentityEvidence=$previous.pcDevelopmentAccess;note='A saved grant belongs to that specific radio identity. Inspect current handoff evidence and verify/enroll another PC separately.'};physicalTests='Physical phone/signing/background/handoff/Watch acceptance remains pending; read current reports for hosted Mac results';signing='Private Android key remains outside this archive; public certificate24f48aa3... matches installed packages. Apple signing is separate.';github='This is a portable local checkpoint; no push/publication performed'} | ConvertTo-Json -Depth 8 | Set-Content "$out/checkpoint.json"
$clone=Join-Path $repo ('work/device-checkpoint-verify-'+[guid]::NewGuid().ToString('N').Substring(0,8))
& $Python "$out/verify-checkpoint.py" --folder $out --clone-to $clone > "$out/verification.txt"
if ($LASTEXITCODE) { throw 'Portable checkpoint verification failed' }
$zip=$out+'.zip'
Compress-Archive -Path "$out/*" -DestinationPath $zip -CompressionLevel Optimal
[ordered]@{sourceCommit=$head;folder=$out;zip=$zip;zipSha256=(Get-FileHash $zip).Hash.ToLowerInvariant();verifiedClone=$clone} | ConvertTo-Json | Tee-Object (Join-Path $repo 'artifacts/latest-device-checkpoint.json')
