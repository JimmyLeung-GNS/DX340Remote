# Development verification on GitHub

The user authorized public publication at https://github.com/JimmyLeung-GNS/DX340Remote. This enables a development-only Apple build path without access to the user's Mac. Everyday music control remains direct BLE; no CI service participates in it.

.github/workflows/apple-verification.yml uses GitHub's standard macos-latest runner only when the repository is public. GitHub documents standard hosted runners as free for public repositories. [Runner reference](https://docs.github.com/en/actions/reference/runners/github-hosted-runners). No larger/paid runner, subscription, signing account, provisioning profile or secret is configured.

The workflow pins official checkout/upload actions to resolved release commit hashes, grants read-only repository permissions and disables persisted checkout credentials. It selects an available iPhone simulator, records Xcode/macOS/runtime inventory, runs shared Swift tests, builds the native app unsigned, installs/launches it, captures processes and a screenshot, and saves small evidence artifacts for3 days. No app-store upload or signed iPhone package is produced.

The Debug build defines DEBUG explicitly so launch arguments select the actual native screen for capture. The script captures all4 disconnected screens, Player dark and largest-text dark after fresh launch. Native Vision recognition waits for the expected heading and4 tab labels before accepting each image; it does not test taps or a Bluetooth connection. Inspect actual run conclusion, logs and pixels before claiming a pass.

Final source412b87a run40699505189 passed Swift3 tests/build/launch/captures. The independent reviewer scored3 listed rendering fixes resolved, without whole-product acceptance. See test-results/2026-10-28-ios-simulator.md. A queued run, generated project, simulator success or BLE API availability does not verify physical iPhone/DX340 Bluetooth, authorization, background operation or audio. Exact iPhone OS/build compatibility and signing still require the real device/Mac.

The workflow runs on relevant source pushes/PRs and can be manually dispatched once present on the default branch. If Actions is unavailable/disabled, retain scripts/build-apple.sh as the user-run Mac path; do not change account/billing settings automatically. Actual run results belong in docs/status.md and test-results/.
