# Continue on the next Windows PC

Latest per-identity update: on30 October JIMMYAH was manually approved and a separate saved-grant BLE state/duplicate-read connection passed; it remains approved and disconnected. This does not approve DESKTOP-T9ICUE4 or another radio. Preserve the earlier export's recorded revoked state as history, then read development-handoff.md and test-results/2026-10-30-resumed-mac-provenance.md for the actual current identity/state. The exporter now uses an explicitly selectable Python3.9+ executable and carries per-identity evidence from its local manifest instead of hardcoding that every PC is revoked. Build outputs that match installed bytes are labelled accordingly.

On JIMMYAH, the matching original Android key is at C:\Users\JimmyLeung\.android\debug.keystore, outside Git. The C:\Users\user path below belongs to the previous exporting PC. The public certificate is the same24f48aa3... identity; inspect the actual destination instead of copying either path blindly. The key is never included in an export.

30 October2026: the user is moving development to **another Windows PC**. This supersedes the earlier instruction to stay on DESKTOP-T9ICUE4. The source checkout being exported is `C:\GitHub\DX340Remote`, branch `codex/source-recovery-checkpoint`. The new PC's path/user/toolchain must be inventoried rather than copied from these old paths.

The ZIP contains a Git source bundle, integrity manifest/verifier, exact installed companion/controller APK backups and separately labelled uninstalled candidates. `checkpoint.json` identifies the exact source commit and every package hash/build scope. The source bundle excludes signing keys, SDKs, ignored logs and build caches. Earlier physical evidence stays in the dated tracked reports; missing raw artifacts are not recreated as proof.

## Import without overwriting another checkout

Copy and extract the ZIP on the new PC. In its extracted folder, with Git and Python3.9+ available:

```powershell
python .\verify-checkpoint.py --clone-to C:\GitHub\DX340Remote
Set-Location C:\GitHub\DX340Remote
git status --short
git log -1 --oneline
```

The verifier checks SHA256s, refuses path traversal/duplicate files, clones only into a new directory, compares HEAD with the manifest and verifies the real Git bundle. If that directory already exists, choose a new path; nothing is reset or overwritten. Do not simply clone GitHub and assume it contains this latest local checkpoint. No push/publication is performed by packaging.

Read AGENTS.md, CODEX_GOAL_PROMPT.txt, docs/development-handoff.md, docs/implementation-plan.md and docs/status.md. Use docs/windows-continuation.md as the prior toolchain/build recipe, substituting the new PC's actual Java/SDK paths. Keep pinned Flutter3.41.4/revisionff37bef603469fb030f2b72995ab929ccfc227f0 in the new checkout's ignored work/flutter rather than changing a global Flutter installation. Run actual builds on that host; source import is not a build pass.

The original Android development key remains outside Git at `C:\Users\user\.android\dx340-original-debug.keystore` on the old PC. Its certificate is `24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d`. Transfer it separately through your secure method if the new PC will build Android in-place upgrades; select it with process-local DX340_DEBUG_KEYSTORE. It is **not in the ZIP**. Provided APKs already have the matching signature and can be installed without rebuilding. Never uninstall an existing app to bypass a signer mismatch. Apple device signing uses the Mac's Apple account/team separately.

## Preserve device and permission state

Installed DX340 baseline: companion04846d60... and ordinary shared phone23c536e7.... Native120/Flutter46/diagnostic23 software evidence and earlier scoped Apple/Mango listening results remain separate from current phone acceptance. Candidates5e1d6e0c.../2d42728f... were built from096601a and remain uninstalled. Do not attach old listening acceptance to a new candidate. Match all full hashes against the manifest.

Latest user preference is to keep PC controller access for development. **Actual last verified state differs:** the old PC's authenticated OS bond remains, but its companion approval was intentionally revoked before that latest preference change; restoration has not yet been verified. Do not claim the PC is approved, run another revoke test on it, or remove its OS bond. When restoration is needed, prepare a live identified request and let the owner grant it locally; then check saved authorization. This is independent of source transfer.

A different Windows PC has a different Bluetooth identity. Existing DX340 bonds/approvals do not transfer by copying the repository or Android signing key. Use `scripts/verify-windows-pairing-state.py` on the new PC to compare actual adapter identities. Enroll the new PC separately when needed and preserve other identities. PC diagnostics are development-only; each real phone still needs its own BLE/offline/source/reliability acceptance. Keep PC diagnostic connections disconnected during the first iPhone test on the installed single-controller baseline while retaining any saved approval.

Both sources were last observed paused at37, output-volume proof false, both optional browsing services bound and HiBy stopped. Verify fresh state before testing; do not infer audio from metadata or compile success. Original App approval/revocation and failed state reads are in docs/test-results/2026-10-30-paired-ble-boundary.md.

## Mac/iPhone preparation included

The new `scripts/mac-iphone-tests.py` provides preflight, isolated simulator verification, signed ordinary-device build and explicit device install/launch commands. Read docs/mac-iphone-quickstart.md. It was tested here at software-guard/non-Mac scope only; it has **not** run a Mac build, signed an iPhone app or passed physical BLE. Supported Mac/Xcode, a real iPhone and its native trust/signing/Bluetooth confirmations are still required. Full Android/iPhone/handoff/reliability/Watch scope remains in completion-audit.md and the phone/controller runbooks.

## Prompt for the next session

```text
/goal Continue DX340Remote from the verified device-switch checkpoint. Read AGENTS.md, CODEX_GOAL_PROMPT.txt, docs/development-handoff.md, docs/implementation-plan.md, docs/status.md and docs/device-continuation.md first. Verify the imported HEAD against checkpoint.json and inspect this new Windows PC's actual toolchain/devices. Preserve all verified evidence and unfinished physical tests separately. Keep PC controller access for development; its last verified companion grant was revoked, so verify/restore it through identified owner approval rather than assuming it exists. Prepare/run the included Mac/iPhone workflow when the Mac and iPhone are available. Continue the full project without treating builds, simulator results or PC diagnostics as physical phone/audio acceptance.
```
