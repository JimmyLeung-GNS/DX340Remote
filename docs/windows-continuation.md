# Windows build workflow and device-switch checkpoint

On30 October2026 the user explicitly chose **another Windows PC** and requested the latest checkpoint be pushed to GitHub. Read device-continuation.md first. The paths/tools below describe the exporting DESKTOP-T9ICUE4 checkout at `C:\GitHub\DX340Remote`; inventory the new PC and substitute its actual paths. Branch: `codex/source-recovery-checkpoint`. Earlier29 October stay-on-this-PC wording is superseded.

## Resume the existing checkout

Use PowerShell 7 (7.6.5 was verified here). Read AGENTS.md, development-handoff.md, implementation-plan.md and status.md. Check the local branch/checkpoint:

```powershell
Set-Location C:\GitHub\DX340Remote
git status --short
git branch --show-current
git log -1 --oneline
adb devices -l
```

Current GitHub resumption: source096601a on DESKTOP-T9ICUE4 builds with native120 JVM tests/lint, Flutter analysis46 tests/release/lint, and23 Windows protocol software tests. New local candidate APKs5e1d6e0c.../2d42728f... are not installed. DX340 retains companion04846d60... and ordinary phone23c536e7..., freshly pulled/hash-checked and saved separately. Only DX340 is available. This PC initially reported unpaired/protectionNONE. One protected unapproved-denial session passed on installed04846d60, but later INFO/CCCD failures and bond disappearance remain unresolved. Only this PC test pairing was removed; native re-pairing confirmation is pending. Keep JIMMYAH authenticated results and both hosted iOS04b04aa results at their recorded scope. See test-results/2026-10-29-github-checkpoint-resume.md for the final resumption result.

Earlier code c24be4c: Android build/lint103 JVM tests; Flutter analysis42 tests/release/lint and shared iOS CI40857287517 pass. Installed companion469ce971.../phoneb12f83e4.... Twenty Apple downloaded selections across five albums/two playlists reached matching source state and separate human audible confirmation;16 outside the full public queue. Twenty-four attempts preserve four rejections, and the run spans pre/post-fix builds, so clean final-build20 and both-phone T07 acceptance remain open. A collapsing Back-button fingerprint bug was corrected while preserving row/header/window/live-action validation. Four final pending-interruption/token/recovery cases pass. Persistent Library/Queue playback bar has scoped Android/iPhone rendering ship reviews; Android upper content under SystemUI tooltip excluded. Both sources paused37, volume grant cleared by earlier restart, both browsing services enabled/bound, HiBy stopped, PC approval absent/bond retained. Intermittent PC failures, full Mango20/format/SD, physical phones/background/latency/audio-quality/battery/walking/handoff and Watch remain incomplete. Read test-results/2026-10-29-apple-listening-matrix.md and current status before device work. Primary workspace C:\GitHub\DX340Remote, branch codex/source-recovery-checkpoint; local manifests/bundles can lag source.

Latest native-cache continuation: installed companion04846d60.../native108 tests, phone unchanged. Run scripts/verify-native-browse-cache.ps1 for read-only native cached/fresh/refresh/source-switch/expiry checks; both sources must be paused. PLAY_ITEM always revalidates fresh. Read test-results/2026-10-29-native-page-cache.md before device work. Earlier listening/UI evidence above retains its package scope.

Mango listening continuation:20 local WAV/FLAC samples are confirmed across five album folders on the same04846d60... host;21 attempts retain1 diagnostic title mismatch, and19/20 selected page3 rows. Both sources paused37. Read test-results/2026-10-29-mango-listening-matrix.md and its exact one-selection/recording commands before starting another listening test. No answer is pending.

## Existing toolchain and signing

Read-only Mango native coverage can be repeated with `scripts/verify-mango-native-inventory.ps1 -Serial d -Adb (Get-Command adb).Source` while both players are paused37/offline. Current22 Album rows expose635 entries; All Music exposes500, independently confirmed at Mango's public browser boundary. This is metadata coverage, not native Album playback or phone T08. See test-results/2026-10-29-mango-native-inventory.md. Its Android instrumentation probe restarts the host/bindings and is separate from the ordinary read-only script; use the documented backup/restoration sequence if rerunning it.

Current resumption host is DESKTOP-T9ICUE4. GitHub checkpoint096601a was fetched and fast-forwarded here; its preceding build/audio/handoff reports include work on JIMMYAH. Those results retain their original host/package scope. The paths below were verified for this session:

| Purpose | Local path/version |
|---|---|
| Java | `C:\Program Files\Android\Android Studio\jbr`, JBR21.0.7 |
| Android SDK | `C:\Users\user\AppData\Local\Android\Sdk` |
| Pinned Flutter | `C:\GitHub\DX340Remote\work\flutter`, 3.41.4/revision `ff37bef603469fb030f2b72995ab929ccfc227f0` |
| Original development key | `C:\Users\user\.android\dx340-original-debug.keystore`, outside Git; both candidate signing preflights match the installed certificate |
| Windows BLE diagnostic Python | `C:\GitHub\DX340Remote\work\ble-diagnostic\Scripts\python.exe` |

JIMMYAH's recorded checkpoint used Microsoft JDK21.0.9, SDK under `C:\Users\JimmyLeung\AppData\Local\Android\Sdk`, pinned Flutter under `C:\Users\JimmyLeung\AppData\Local\Flutter` and its external `.android\debug.keystore`. Keep that inventory with its reports; it is not this PC's toolchain. Global Java/Flutter/key settings are unchanged by the process-local commands below.

Select the pinned tools and original key in this PowerShell process. The scripts below build/test locally; they do not install onto a device. Keep the existing ignored work/artifacts/local.properties files.

```powershell
Set-Location C:\GitHub\DX340Remote
$savedJavaForDx340 = $env:JAVA_HOME
$savedSdkForDx340 = $env:ANDROID_HOME
$savedPathForDx340 = $env:PATH
$savedKeyForDx340 = $env:DX340_DEBUG_KEYSTORE
$savedJavaOptionsForDx340 = $env:JAVA_TOOL_OPTIONS
$savedErrorActionForDx340 = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Stop'
    $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
    $env:ANDROID_HOME = 'C:\Users\user\AppData\Local\Android\Sdk'
    $env:PATH = "$PWD\work\flutter\bin;$env:PATH"
    $env:DX340_DEBUG_KEYSTORE = 'C:\Users\user\.android\dx340-original-debug.keystore'
    ./scripts/build-android.ps1
    ./scripts/build-phone.ps1
} finally {
    $env:JAVA_HOME = $savedJavaForDx340
    $env:ANDROID_HOME = $savedSdkForDx340
    $env:PATH = $savedPathForDx340
    $env:DX340_DEBUG_KEYSTORE = $savedKeyForDx340
    $env:JAVA_TOOL_OPTIONS = $savedJavaOptionsForDx340
    $ErrorActionPreference = $savedErrorActionForDx340
}
```

Use installation.md's matching-certificate preflight before any later install-r. Preserve app data and existing keys. The original key-transfer problem is resolved; use the path above for future updates.

## Local checkpoint and remaining physical steps

The branch contains the committed source/docs. Ignored artifacts/dx340-checkpoint.bundle and checkpoint-manifest.json are refreshed together and verified by a separate local clone. They are a recovery copy for this PC; no development relocation is required. APKs, logs, tools and the signing key are separate from the Git bundle. Rebuild and verify the manifest after code/package changes rather than treating an old package as current source.

Only DX340 d is currently available over USB. Earlier JIMMYAH native pairing/approved sessions/fresh revoked denial retain their recorded scope; intermittent failures/live-revocation timing remain open. DESKTOP-T9ICUE4 started this resumption unpaired, so its own native consent/protocol results must be recorded separately. Current native re-pairing confirmation is pending in this chat; direct routine desktop/DX340 testing is authorized. The computer-use helper cannot target the ShellExperienceHost picker; matching-code/security confirmation remains manual. Last Android dump lists JIMMYAH only, and targeted WinRT cleanup verified this PC pairedAfter=false. Separate Android/iPhone, two-controller handoff and full source/latency/reliability acceptance remain pending. See test-results/2026-10-29-authenticated-windows.md, test-results/2026-10-29-github-checkpoint-resume.md and windows-ble-diagnostics.md.

Mac/Xcode is required later to compile/sign/validate the iPhone code. mac-continuation.md documents that platform-specific work while this Windows checkout remains the main development workspace. Full project scope/remaining evidence is in completion-audit.md and phone-reliability-runbook.md.
