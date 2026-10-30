# Automatic handoff when switching devices

Standing user instruction,30 October2026 (Asia/Hong_Kong): whenever the user stops development to switch devices, save the complete project checkpoint, commit/push it, and supply a continuation prompt automatically. This is authorized for the existing JimmyLeung-GNS/DX340Remote repository. It applies to Windows, Mac, iPhone and Android destinations. The implementation goal stays paused until the user asks to continue.

## Before ending the session

1. Stop feature development. Safely finish or stop running commands/agents and record their real outcomes. Do not start extra feature work or tests merely to delay the stop.
2. Update status.md, development-handoff.md and affected evidence/runbooks: completed work, current branch and source revisions, built versus installed packages/hashes, commands/results/failures, actual device state, unknowns, remaining requirements and exact next steps. Preserve user decisions, including shared Flutter UI/native Bluetooth, Mango playback and accepted optional UI modes. Never promote simulated or source-local results to physical phone/audio acceptance.
3. Review intentional changes and exclude signing keys, credentials, private data and unrelated edits. Commit and push the active handoff branch to the existing authorized remote; verify its exact SHA. Never force-push over another device's work. A clean tree still needs remote-checkpoint verification. State an actual push failure plainly and retain local changes/recovery files.
4. Refresh the existing local bundle/manifest when available. Clearly distinguish tracked GitHub documentation/source from ignored artifacts, packages, raw logs and signing material. List anything that must be transferred separately. Transfer signing material privately, preserving the original certificate; do not publish it or replace the installed application's signer silently.
5. Return the repository link, branch, verified commit, separate transfer requirements and the prompt below, inserting the actual checkpoint. Mark the goal paused where the environment supports it. Do not call the unfinished system complete.

## Destination preparation

| Destination | Resume checks |
|---|---|
| Windows | Reuse or clone the latest handoff branch; inspect local changes, JDK/Android SDK/pinned Flutter, private signing material and connected devices. Use windows-continuation.md, adapting the previous PC's paths. |
| Mac | Inspect the checkout, pinned Flutter, actual macOS/Xcode, signing/team and simulator/physical iPhone availability. Use mac-continuation.md. Do not treat hosted CI as a physical iPhone pass. |
| iPhone or Android | Inspect which repository/execution environment is actually available. A controller phone or chat device is not proof of a desktop build environment. Use an available Windows/Mac build host or existing CI where authorized, and perform only supported device tests. Keep unavailable builds/signing/hardware checks explicitly unverified. |

Do not assume `main` is current. On an existing checkout, inspect uncommitted changes before fetching/switching/updating; preserve newer work. No absolute Windows path is required on other devices. Re-inventory device/OS/app versions and authorization state when testing resumes; saved listening reports are historical evidence, not fresh confirmations.

## Continuation prompt

Replace the checkpoint placeholder with the verified pushed commit in the outgoing reply:

```text
Continue development of DX340 Bluetooth Remote from repository
https://github.com/JimmyLeung-GNS/DX340Remote
Branch: codex/source-recovery-checkpoint
Checkpoint: <verified pushed commit>

Before changing code, read AGENTS.md, CODEX_GOAL_PROMPT.txt,
docs/development-handoff.md, docs/device-switch-handoff.md,
docs/implementation-plan.md and docs/status.md in the current checkout.
Inspect the branch, local changes, remote checkpoint, host OS/toolchain,
signing access and connected hardware. Preserve any newer work and adapt
the platform continuation guide to this environment; do not assume the old
Windows paths or that a phone can perform desktop/Xcode builds.

Resume the unfinished implementation and appropriate testing. Preserve the
shared Flutter phone UI with native Kotlin/Swift Bluetooth, Bluetooth-only
control, existing Mango playback, Apple Music-managed downloads and the
accepted optional screen-on/unlocked browsing modes. Keep source-local audio,
simulations, builds and physical phone Bluetooth evidence separate. Do not
claim unverified hardware features work. Read the current handoff test report
and physical runbook before installing or testing controller handoff.

Complete independent work when hardware or signing is unavailable and record
precise next steps. Keep signing keys out of Git. Whenever I stop to switch
devices, automatically update the handoff/evidence, commit and push the
intended changes, verify the remote checkpoint, and give me a fresh continuation
prompt plus any separate transfer requirements.
```
