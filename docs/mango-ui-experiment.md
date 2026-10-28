# Optional Mango UI-assisted mode

On 28 October 2026, after reviewing the native-interface limits, the user explicitly answered **“Allow optional Mango UI mode.”** This authorizes investigation/implementation of optional screen-on/unlocked Mango folder browsing while preserving Mango playback and existing native controls. It is a separate decision from the earlier Apple Music UI mode. App updates require revalidation; no replacement playback engine is accepted.

## Initial physical research

DX340 Android 13, Mango 4.1.37/code 240. Read-only UI research opened the ordinary Mango Activity and navigated its public library controls. ADB used observed node IDs/bounds for this research only; this is not a production AccessibilityService result or BLE test. No file was selected or played by these research taps; Apple remained playing and Mango paused in the observed sessions.

- Main screen library control: com.ibasso.music:id/main_list_iv.
- Folder detail: folder_detail_content, entry_title_tv, breadcrumb tv_content nodes, row file_name_tv and music_lib_all_music_list. Visible FLAC filenames were observed in the saved Internal Storage/Music/Tayori folder.
- File rows expose numeric file_info_index_tv; sampled folder rows expose file_info_icon_iv. These are candidate version-specific semantic discriminators, not yet a proved production model.
- Important trap: folder_detail_content retains multiple overlapping ancestor page trees. The first current page matched the visible screenshot, while hidden cached pages also appeared in UIAutomator. A production parser must isolate and revalidate the active folder and breadcrumb path; it must never aggregate all file_name_tv nodes.
- Library navigation arrow opens a Music menu, not an ordinary parent operation. Menu nodes include internalStorgeLayout (actual spelling), sdcardLayout and allPlaylistLayout. It also contains netLayout/Local Network, which must be excluded from this Bluetooth-only feature.

Artifacts: mango-ui-initial.{xml,png}, mango-ui-library.{xml,png}, mango-ui-storage-root.xml and mango-ui-storage-picker.xml. They are research evidence only.

## Implementation requirements

Keep direct Mango albums/songs and transport controls. Add an optional folder entry with explicit screen-on/unlock limitations; isolate the Mango UI adapter from Apple and BLE transport. Use semantic node actions only in production, with exact app-version checks, current-window/breadcrumb/snapshot-scoped IDs, fresh file-label validation, bounded navigation and stale-screen errors. Only local storage choices may be exposed; never navigate Local Network, delete/settings/download controls or arbitrary remote paths. UI file selection must route through the shared source handoff and keep Mango as engine.

Verify actual service reads and selection, known FLAC/WAV files, rescan/SD and stale IDs, then both phone BLE paths. The permission decision and these ADB research captures do not prove that an app-based Mango folder feature works yet.
