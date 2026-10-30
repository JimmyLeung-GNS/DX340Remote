param([string]$Serial='d', [string]$Adb='')
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
Set-Location $repo
if ((& git status --porcelain)) { throw 'Commit the source/docs first; checkpoint must be reviewable and clean' }
$head=(& git rev-parse HEAD).Trim()
$branch=(& git branch --show-current).Trim()
if ($branch -ne 'codex/source-recovery-checkpoint') { throw 'Unexpected checkpoint branch' }
$out=Join-Path $repo ('artifacts/device-checkpoint-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'-'+$head.Substring(0,7))
if (Test-Path $out) { throw 'Checkpoint directory exists; do not overwrite' }
New-Item -ItemType Directory $out | Out-Null
& git bundle create "$out/dx340-checkpoint.bundle" $branch
if ($LASTEXITCODE) { throw 'Bundle creation failed' }
Copy-Item docs/device-continuation.md "$out/START_HERE.md"
Copy-Item scripts/verify-device-checkpoint.py "$out/verify-checkpoint.py"
$packages=@()
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
        $saved=if ($role[1] -eq 'org.dx340.remote.host') { 'verified-installed-companion-04846d60.apk' } else { 'verified-installed-phone-23c536e7.apk' }
        Copy-Item (Join-Path $repo "artifacts/resumed-096601a/$saved") "$out/$file"
        $scope='Previously verified installed backup; DX340 unavailable during export, current device state not reverified'
    }
    $packages += [ordered]@{role=$role[0];file=$file;sha256=(Get-FileHash "$out/$file").Hash.ToLowerInvariant();scope=$scope}
}
foreach ($file in @('dx340-companion-0.1.0-debug.apk','dx340-phone-0.2.0-development.apk')) {
    $previous=Get-Content (Join-Path $repo 'artifacts/checkpoint-manifest.json') -Raw | ConvertFrom-Json
    $record=@($previous.candidatePackages | Where-Object { $_.file -eq $file })
    $digest=(Get-FileHash (Join-Path $repo "artifacts/$file")).Hash.ToLowerInvariant()
    if ($record.Count -ne 1 -or $record[0].sha256 -ne $digest) { throw 'Candidate package differs from the prior build record' }
    Copy-Item (Join-Path $repo "artifacts/$file") (Join-Path $out ('candidate-'+$file))
    $packages += [ordered]@{role='uninstalled-candidate';file='candidate-'+$file;sha256=$digest;buildSourceCommit=$record[0].sourceCommit;scope='Existing local build candidate, not installed; match prior build-source record before testing'}
}
$tracked=& git ls-files
if (@($tracked | Where-Object { $_ -match '(?i)\.(keystore|jks|p12|pfx|mobileprovision|apk)$' }).Count) { throw 'Signing/package file unexpectedly tracked; do not package' }
$files=@(Get-ChildItem $out -File | Sort-Object Name | ForEach-Object { [ordered]@{file=$_.Name;sha256=(Get-FileHash $_.FullName).Hash.ToLowerInvariant()} })
[ordered]@{formatVersion=1;createdUtc=[DateTime]::UtcNow.ToString('o');sourceCommit=$head;sourceBranch=$branch;files=$files;packages=$packages;pcAccess='Latest user requests keeping PC controller access. OS bond retained; companion grant was last verified revoked, restoration not yet verified.';physicalTests='iPhone/Android-phone/Mac signing/background/handoff/Watch acceptance pending; Mac runner preparation is not a passed Mac build';signing='Private Android key remains outside this archive; public certificate24f48aa3... matches installed packages. Apple signing is separate.';github='This is a portable local checkpoint; no push/publication performed'} | ConvertTo-Json -Depth 8 | Set-Content "$out/checkpoint.json"
$python=Join-Path $repo 'work/ble-diagnostic/Scripts/python.exe'
$clone=Join-Path $repo ('work/device-checkpoint-verify-'+[guid]::NewGuid().ToString('N').Substring(0,8))
& $python "$out/verify-checkpoint.py" --folder $out --clone-to $clone > "$out/verification.txt"
if ($LASTEXITCODE) { throw 'Portable checkpoint verification failed' }
$zip=$out+'.zip'
Compress-Archive -Path "$out/*" -DestinationPath $zip -CompressionLevel Optimal
[ordered]@{sourceCommit=$head;folder=$out;zip=$zip;zipSha256=(Get-FileHash $zip).Hash.ToLowerInvariant();verifiedClone=$clone} | ConvertTo-Json | Tee-Object (Join-Path $repo 'artifacts/latest-device-checkpoint.json')
