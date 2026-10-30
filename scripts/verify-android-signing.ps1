param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [Parameter(Mandatory=$true)][string]$Apk,
    [string]$Package = 'org.dx340.remote.controller',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string]$Sdk = "$env:LOCALAPPDATA/Android/Sdk"
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$candidate = (Resolve-Path -LiteralPath $Apk).Path
if ($Package -notmatch '^[a-zA-Z][a-zA-Z0-9_.]+$') { throw 'Invalid Android package name.' }
$buildTools = Get-ChildItem -LiteralPath "$Sdk/build-tools" -Directory |
    Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (!$buildTools) { throw 'Android SDK build-tools are required to verify signing.' }
$java = if ($env:JAVA_HOME) { "$env:JAVA_HOME/bin/java.exe" } else { (Get-Command java -ErrorAction Stop).Source }
$signer = Join-Path $buildTools.FullName 'lib/apksigner.jar'
$aapt = Join-Path $buildTools.FullName 'aapt2.exe'
$badging = & $aapt dump badging $candidate 2>&1
if ($LASTEXITCODE -ne 0 -or ($badging -join "`n") -notmatch "(?m)^package: name='$([regex]::Escape($Package))'") {
    throw "Candidate APK does not identify package $Package."
}
function Read-Signers([string]$Path) {
    $output = & $java -jar $signer verify --print-certs $Path 2>&1
    if ($LASTEXITCODE -ne 0) { throw "APK signature verification failed for $Path" }
    $digests = @($output | ForEach-Object {
        if ($_ -match '^Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]{64})$') { $Matches[1].ToLowerInvariant() }
    } | Sort-Object -Unique)
    if (!$digests.Count) { throw "No APK signing certificate was found for $Path" }
    return ($digests -join ',')
}
$candidateSigners = Read-Signers $candidate
$paths = & $Adb -s $Serial shell pm path $Package 2>&1
$pathExitCode = $LASTEXITCODE
$base = @($paths | Where-Object { $_ -match '^package:/data/app/.+/base\.apk$' })
if (!$base.Count) {
    if (($paths -join '').Trim()) { throw 'Unrecognized installed-package path; inspect manually.' }
    # Android16 returns1 with empty output for an absent package. Confirm the
    # device/package manager is responding before calling this a new install.
    if ($pathExitCode -notin @(0,1)) { throw 'Unable to inspect installed app; verify the selected ADB device.' }
    $listed = & $Adb -s $Serial shell pm list packages $Package 2>&1
    if ($LASTEXITCODE -ne 0 -or ($listed -join '').Trim()) { throw 'Package absence was not verified; inspect the selected device before installing.' }
    New-Item -ItemType Directory -Force "$repo/artifacts" | Out-Null
    [ordered]@{
        package = $Package
        installed = $false
        candidateSha256 = (Get-FileHash -LiteralPath $candidate -Algorithm SHA256).Hash.ToLowerInvariant()
        installedSignerSha256 = $null
        candidateSignerSha256 = $candidateSigners
        sameSigner = $null
        note = 'Package absence confirmed by a successful package-manager query; candidate signature verified. No install, data reset or signing-key migration.'
    } | ConvertTo-Json | Set-Content "$repo/artifacts/android-upgrade-signing.json"
    Write-Output "No installed $Package; candidate signature verified for a new install."
    return
}
if ($pathExitCode -ne 0) { throw 'Unable to inspect installed app; verify the selected ADB device.' }
if ($base.Count -ne 1) { throw 'Ambiguous installed base APK; inspect manually.' }
New-Item -ItemType Directory -Force "$repo/artifacts" | Out-Null
$installed = "$repo/artifacts/upgrade-installed-$Package.apk"
& $Adb -s $Serial pull ($base[0] -replace '^package:', '') $installed | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Unable to read installed APK signing identity.' }
$installedSigners = Read-Signers $installed
$signersMatch = $candidateSigners -eq $installedSigners
[ordered]@{
    package = $Package
    candidateSha256 = (Get-FileHash -LiteralPath $candidate -Algorithm SHA256).Hash.ToLowerInvariant()
    installedSignerSha256 = $installedSigners
    candidateSignerSha256 = $candidateSigners
    sameSigner = $signersMatch
    note = 'Read-only development-key comparison; no install, uninstall, data reset or signing-key migration.'
} | ConvertTo-Json | Set-Content "$repo/artifacts/android-upgrade-signing.json"
if (!$signersMatch) { throw 'Signing identity differs from the installed app. Restore the original development key before upgrading; installed data was preserved. See artifacts/android-upgrade-signing.json.' }
Write-Output 'Installed and candidate APK signing certificates match.'
