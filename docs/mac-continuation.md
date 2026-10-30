# Mac build and iPhone validation

30 October2026: the primary development checkout is moving to another Windows PC. Read device-continuation.md for the GitHub/portable source checkpoint, signing-key boundary and exact new-session prompt. The Mac remains the Xcode/signing/physical-iPhone execution host.

For a new checkout, use the published handoff branch and compare HEAD with the outgoing prompt:

```sh
git clone --branch codex/source-recovery-checkpoint https://github.com/JimmyLeung-GNS/DX340Remote.git DX340Remote
cd DX340Remote
git rev-parse HEAD
git status --short
```

Preserve local changes before updating an existing checkout. General automatic handoff rules are in device-switch-handoff.md; this guide and device-continuation.md describe the current platform-specific workflow.

The complete staged commands are in mac-iphone-quickstart.md. Start with:

```sh
python3 scripts/mac-iphone-tests.py preflight
```

The runner inventories actual Mac/Xcode/SDK/Flutter versions and device identifiers, then supports an explicitly selected simulator, signed ordinary shared-device build, and explicit install/launch. It creates committed-source worktrees and separate evidence folders, preserves previous output, checks simulator/fixture/identity/signature boundaries and uses bounded command lifetimes. It does not sign in, change global tooling, automatically provision accounts or pretend a simulator is a physical phone.

Current preparation verification is Windows software scope: five runner guards and four checkpoint-integrity checks pass; running preflight on Windows refuses Apple execution with NEEDS_MACOS_XCODE. No current Mac build, signed iPhone installation or phone BLE acceptance is claimed. Historical hosted Apple workflows at04b04aa retain their unsigned simulator scope. The reported iPhone OS27.2 still needs exact model/build inventory and actual Xcode compatibility validation.

Use the ordinary shared Flutter app for physical tests; the native reference remains in apple/. Both use the same bundle ID, so the simulator runner tests the reference first and shared ordinary app last. Native screenshot fixtures must not be installed as the physical remote. Complete phone-reliability-runbook.md/controller-handoff-runbook.md and record source observations separately from human audio confirmation.

Installed DX340 baseline is04846d60.../ordinary phone23c536e7...; current Windows candidates5e1d6e0c.../2d42728f... remain uninstalled. Earlier twenty Mango folder confirmations, mixed-build Apple confirmations and native Album635/All Music500 metadata remain separately scoped. Current full-native build evidence is120 tests, Flutter46 and diagnostic23, not the older86/32/19 records. Native metadata caching is now wired into SourceRegistry; earlier standalone-index wording describes a preceding implementation.

Latest user preference is to keep PC controller access. Last verified state was an authenticated retained OS bond with companion approval revoked; restoration has not been verified after that preference changed. Keep any restored grant, disconnect its live transport during first iPhone enrollment on the installed single-controller host, and preserve each controller's separate identity. Do not make another PC or key-transfer request an unnecessary dependency for Mac/iPhone preparation. Android private keys remain outside Git/archive; Apple signing uses the selected Mac team separately.

Full P0-P9/T01-T18 acceptance, native Album/playlist/SD/rescan/volume/audio-quality/background/range/battery/walking/two-controller and later Watch remain open. The same checkpoint contains the Windows source/debug tools and runbooks; synthetic/build success cannot fill those physical rows.