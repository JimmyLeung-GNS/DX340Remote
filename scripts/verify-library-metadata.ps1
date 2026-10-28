param()
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$output = Join-Path $repo 'artifacts/library-metadata'
New-Item -ItemType Directory -Force $output | Out-Null
New-Item -ItemType Directory -Force (Join-Path $repo 'work') | Out-Null
if ($IsWindows) {
    $env:JAVA_TOOL_OPTIONS = "-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider -Djdk.net.unixdomain.tmpdir=$($repo.Replace('\','/'))/work -Djava.net.preferIPv4Stack=true"
}
Push-Location (Join-Path $repo 'android')
try {
    $report = Join-Path $output 'summary.json'
    & ./gradlew.bat --no-daemon :library-cache:test :library-cache:metadataBenchmark "-PmetadataReport=$report" 2>&1 | Tee-Object (Join-Path $output 'build.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Metadata fixture/benchmark failed. See artifacts/library-metadata/build.txt' }
    $summary = Get-Content -Raw $report | ConvertFrom-Json
    if ($summary.tracks -ne 10000 -or !$summary.componentTimingTargetMet) { throw 'Metadata benchmark did not meet its component checks' }
    $summary | Select-Object kind,tracks,pages,firstCachedComponentMs,firstUncachedComponentMs,maxEncodedPageBytes,componentTimingTargetMet
} finally { Pop-Location }
