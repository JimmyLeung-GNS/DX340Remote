# Flutter phone migration checkpoint

28 October 2026. IN PROGRESS, not physical acceptance. The user selected a shared phone UI with native Bluetooth/lifecycle underneath. Actual Android package and iPhone simulator checks are running. Do not infer phone radio reliability from these builds.

Initial failed checks: first widget test lacked the foundation import, and record/map comparisons incorrectly relied on map identity; corrected. The large-text widget probe initially looked for lazy ListView content before scrolling; corrected to scroll to the real control. These were test failures, not device BLE results. Final commands/results will be appended after execution.
