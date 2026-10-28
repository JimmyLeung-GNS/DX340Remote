param([switch]$WithoutTests)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$flutter = (Get-Command flutter -ErrorAction SilentlyContinue).Source
if (!$flutter) { $flutter = "$env:LOCALAPPDATA/Flutter/bin/flutter.bat" }
if (!(Test-Path -LiteralPath $flutter)) { throw 'Install Flutter 3.41.4 and put flutter on PATH.' }
New-Item -ItemType Directory -Force "$repo/artifacts", "$repo/work" | Out-Null
& $flutter --version *> "$repo/artifacts/flutter-windows-toolchain.txt"
if ($LASTEXITCODE -ne 0) { throw 'Flutter SDK bootstrap failed.' }
$version = & $flutter --version --machine | ConvertFrom-Json
if ($version.frameworkVersion -ne '3.41.4' -or $version.frameworkRevision -ne 'ff37bef603469fb030f2b72995ab929ccfc227f0') {
    throw "Expected pinned Flutter 3.41.4 revision ff37bef603469fb030f2b72995ab929ccfc227f0; found $($version.frameworkVersion) / $($version.frameworkRevision)"
}
if ($IsWindows) {
    $env:JAVA_TOOL_OPTIONS = "-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider -Djdk.net.unixdomain.tmpdir=$($repo.Replace('\','/'))/work -Djava.net.preferIPv4Stack=true"
}
Push-Location "$repo/phone"
try {
    & $flutter pub get --enforce-lockfile
    if ($LASTEXITCODE -ne 0) { throw 'Flutter dependency resolution failed.' }
    & $flutter analyze 2>&1 | Tee-Object "$repo/artifacts/flutter-analyze.txt"
    if ($LASTEXITCODE -ne 0) { throw 'Flutter analysis failed.' }
    if (!$WithoutTests) {
        & $flutter test --reporter expanded 2>&1 | Tee-Object "$repo/artifacts/flutter-tests.txt"
        if ($LASTEXITCODE -ne 0) { throw 'Flutter tests failed.' }
    }
    & $flutter build apk --release 2>&1 | Tee-Object "$repo/artifacts/flutter-android-build.txt"
    if ($LASTEXITCODE -ne 0) { throw 'Flutter Android build failed.' }
    # Flutter 3.41 writes an unescaped Windows drive colon to generated properties;
    # normalize only the ignored local file before Android lint parses it.
    if ($IsWindows) {
        (Get-Content android/local.properties) -replace '^((?:sdk.dir|flutter.sdk)=[A-Za-z]):', '$1\:' | Set-Content android/local.properties
    }
    Push-Location android
    try {
        & ./gradlew.bat --no-daemon :app:lintRelease 2>&1 | Tee-Object "$repo/artifacts/flutter-android-lint.txt"
        if ($LASTEXITCODE -ne 0) { throw 'Flutter Android lint failed.' }
    } finally { Pop-Location }
    Copy-Item build/app/outputs/flutter-apk/app-release.apk "$repo/artifacts/dx340-phone-0.2.0-development.apk"
    Get-FileHash "$repo/artifacts/dx340-phone-0.2.0-development.apk" -Algorithm SHA256 | Format-List | Out-File "$repo/artifacts/flutter-apk-sha256.txt"
} finally { Pop-Location }
