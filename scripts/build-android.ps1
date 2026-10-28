param([switch]$WithoutTests)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
New-Item -ItemType Directory -Force "$repo/artifacts", "$repo/work" | Out-Null
# Work around this Windows host's Java Unix-domain selector failure, inherited by Gradle/Kotlin workers.
if ($IsWindows) {
    $env:JAVA_TOOL_OPTIONS = "-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider -Djdk.net.unixdomain.tmpdir=$($repo.Replace('\','/'))/work -Djava.net.preferIPv4Stack=true"
}
Push-Location "$repo/android"
try {
    $tasks = @(':host-app:assembleDebug', ':remote-app:assembleDebug', ':ui-probe-app:assembleDebug', ':host-app:assembleDebugAndroidTest', ':host-app:lintDebug', ':remote-app:lintDebug', ':ui-probe-app:lintDebug')
    if (!$WithoutTests) { $tasks += @(':core-protocol:test', ':library-cache:test', ':source-adapters:testDebugUnitTest') }
    & ./gradlew.bat --no-daemon @tasks 2>&1 | Tee-Object "$repo/artifacts/android-build.txt"
    if ($LASTEXITCODE -ne 0) { throw "Android build failed ($LASTEXITCODE). See artifacts/android-build.txt" }
    Copy-Item host-app/build/outputs/apk/debug/host-app-debug.apk "$repo/artifacts/dx340-companion-0.1.0-debug.apk"
    Copy-Item remote-app/build/outputs/apk/debug/remote-app-debug.apk "$repo/artifacts/dx340-remote-0.1.0-debug.apk"
    Copy-Item host-app/build/outputs/apk/androidTest/debug/host-app-debug-androidTest.apk "$repo/artifacts/dx340-source-probe-0.1.0-debug.apk"
    Copy-Item ui-probe-app/build/outputs/apk/debug/ui-probe-app-debug.apk "$repo/artifacts/dx340-ui-research-0.1.0-debug.apk"
    Get-ChildItem "$repo/artifacts/dx340-*.apk" | ForEach-Object { "$((Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant())  $($_.Name)" } | Set-Content "$repo/artifacts/SHA256SUMS.txt"
} finally { Pop-Location }
