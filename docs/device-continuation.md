# Continue on the next Windows PC

Latest per-identity update: on30 October JIMMYAH was manually approved and a separate saved-grant BLE state/duplicate-read connection passed; it remains approved and disconnected. This does not approve DESKTOP-T9ICUE4 or another radio. Preserve the earlier export's recorded revoked state as history, then read development-handoff.md and test-results/2026-10-30-resumed-mac-provenance.md for the actual current identity/state. The exporter now uses an explicitly selectable Python3.9+ executable and carries per-identity evidence from its local manifest instead of hardcoding that every PC is revoked. Build outputs that match installed bytes are labelled accordingly.

On JIMMYAH, the matching original Android key is at C:\Users\JimmyLeung\.android\debug.keystore, outside Git. The C:\Users\user path below belongs to the previous exporting PC. The public certificate is the same24f48aa3... identity; inspect the actual destination instead of copying either path blindly. The key is never included in an export.

30 October2026: the user is moving development to **another Windows PC**. This supersedes the earlier instruction to stay on DESKTOP-T9ICUE4. The source checkout being exported is `C:\GitHub\DX340Remote`, branch `codex/source-recovery-checkpoint`. The new PC's path/user/toolchain must be inventoried rather than copied from these old paths.

The ZIP contains a Git source bundle, integrity manifest/verifier, exact installed companion/controller APK backups and separately labelled build outputs. An output matching installed bytes is labelled accordingly; another candidate remains unverified. `checkpoint.json` identifies the exact source commit and every package hash/build scope. The source bundle excludes signing keys, SDKs, ignored logs and build caches. Earlier physical evidence stays in the dated tracked reports; missing raw artifacts are not recreated as proof.

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

Match the actual installed packages and exact full hashes against the latest `checkpoint.json`, then read the top of status.md/development-handoff.md. At the30 October pagination checkpoint, companion068c39ec.../source5491be7 is installed with native147 build/lint, all30 playlist positions across six pages and one owner-confirmed later-page track after a retained metadata delay. The temporary fixture is removed, original files verified. Earlier popup checks belong to46c49572...; its preceding two playlist audio confirmations belong to d2c24a52.../source9e8afb0; native20 belongs to7129c307..., older folder20 to04846d60.... Ordinary shared phone23c536e7... is unchanged and has DX340-only launch/rendering evidence. Do not attach earlier listening results to a new package or infer a separate phone pass.

Keep PC controller access for development. JIMMYAH's identified local approval and separate saved-grant state/duplicate-read connection were verified on30 October; its approval preference remains preserved after subsequent upgrades, with no revocation or bond changes. A post-upgrade saved preference is not a fresh radio reconnect test on that build. Earlier revoked-state reports remain historical. Do not run another PC revoke test or remove its bond. If a different PC needs access, prepare its identified request and let the owner approve locally; source transfer does not confer consent.

A different Windows PC has a different Bluetooth identity. Existing DX340 bonds/approvals do not transfer by copying the repository or Android signing key. Use `scripts/verify-windows-pairing-state.py` on the new PC to compare actual adapter identities. Enroll the new PC separately when needed and preserve other identities. PC diagnostics are development-only; each real phone still needs its own BLE/offline/source/reliability acceptance. Keep PC diagnostics disconnected during the first phone test while retaining saved approval. Explicit two-controller handoff is implemented but physical two-central acceptance remains pending; read controller-handoff-runbook.md.

Both sources were last observed paused at37, output-volume proof false, both optional browsing services bound and HiBy stopped. Verify fresh state before testing; do not infer audio from metadata or compile success. Original App approval/revocation and failed state reads are in docs/test-results/2026-10-30-paired-ble-boundary.md.

## Mac/iPhone preparation included

`scripts/mac-iphone-tests.py` provides preflight, isolated simulator verification, signed ordinary-device build and explicit device install/launch commands. Read docs/mac-iphone-quickstart.md. Hosted Mac run41026103457 at a89043ce passed18 preparation/checkpoint guards and actual native/shared simulator builds, launches, captures and restoration (Swift5/shared46 tests). Its preceding cold-Flutter failure is retained. Hosted CI has not signed/installed the owner's iPhone or passed physical BLE. Supported owner Mac/Xcode, actual iPhone inventory, team signing and native trust/Bluetooth confirmations remain required. Full Android/iPhone/handoff/reliability/Watch scope remains in completion-audit.md and the phone/controller runbooks.

## Prompt for the next session

```text
/goal Continue DX340Remote from the verified device-switch checkpoint. Read AGENTS.md, CODEX_GOAL_PROMPT.txt, docs/development-handoff.md, docs/implementation-plan.md, docs/status.md and docs/device-continuation.md first. Verify the imported HEAD against checkpoint.json and inspect this new Windows PC's actual toolchain/devices. Preserve verified evidence and unfinished physical tests separately. Keep PC controller access for development: JIMMYAH's owner-restored approval is preserved, but another radio needs its own identified enrollment. Preserve bonds and do not run PC revocation tests. Match installed package hashes and keep historical audio/simulator evidence within its recorded scope. Run the included owner-Mac/signing/iPhone workflow when available. Continue the full project without treating builds, simulator results or PC diagnostics as physical phone/audio acceptance.
```
