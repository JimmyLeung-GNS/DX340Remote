# Continue development on this Windows PC

Confirmed by the user on 29 October 2026: keep developing in `C:\GitHub\DX340Remote` on the current Windows PC. The working branch is `codex/source-recovery-checkpoint`. Continue this checkout rather than creating a second clone or assuming a move to another Windows PC.

## Resume the existing checkout

Use PowerShell 7 (7.6.5 was verified here). Read AGENTS.md, development-handoff.md, implementation-plan.md and status.md. Check the local branch/checkpoint:

```powershell
Set-Location C:\GitHub\DX340Remote
git status --short
git branch --show-current
git log -1 --oneline
adb devices -l
```

The latest saved commit is recorded by Git and ignored artifacts/checkpoint-manifest.json. The source checkpoint may advance after the last APK build; the manifest separately identifies each APK. Native86/Flutter32 and Windows diagnostic19 test results retain their recorded software/device scope. Current packages are companion40114ff8... and shared phone09aaae78...; full hashes and verification are in the manifest and dated reports.

## Existing toolchain and signing

The local workspace already has these paths. They were checked again for this continuation update:

| Purpose | Local path/version |
|---|---|
| Java | `C:\Program Files\Android\Android Studio\jbr`, JBR 21.0.7 |
| Android SDK | `C:\Users\user\AppData\Local\Android\Sdk` |
| Pinned Flutter | `C:\GitHub\DX340Remote\work\flutter`, 3.41.4/revision `ff37bef603469fb030f2b72995ab929ccfc227f0` |
| Original development key | `C:\Users\user\.android\dx340-original-debug.keystore`, outside Git |
| Windows BLE diagnostic Python | `C:\GitHub\DX340Remote\work\ble-diagnostic\Scripts\python.exe` |

The default/global Flutter is different. For a rebuild, select the pinned tools and original key in this PowerShell process. The scripts below build/test locally; they do not install onto a device. Keep the existing ignored work/artifacts/local.properties files.

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

Only DX340 d is currently available over USB. The Windows PC's radio can perform development diagnostics here; windows-ble-diagnostics.md contains the prepared native pairing/approval/state sequence. The present operator must handle pairing dialogs and identified local approval. Current listening availability and actual separate Android/iPhone acceptance are still pending. The three-second Mango page target remains unmet. These gates are distinct from the choice of development PC.

Mac/Xcode is required later to compile/sign/validate the iPhone code. mac-continuation.md documents that platform-specific work while this Windows checkout remains the main development workspace. Full project scope/remaining evidence is in completion-audit.md and phone-reliability-runbook.md.
