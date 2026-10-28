# Optional Mango UI-assisted mode

On 28 October 2026, after reviewing the native-interface limits, the user explicitly answered **“Allow optional Mango UI mode.”** This authorizes investigation/implementation of optional screen-on/unlocked Mango folder browsing while preserving Mango playback and existing native controls. It is a separate decision from the earlier Apple Music UI mode. App updates require revalidation; no replacement playback engine is accepted.

## Initial physical research

DX340 Android 13, Mango 4.1.37/code 240. Read-only UI research opened the ordinary Mango Activity and navigated its public library controls. ADB used observed node IDs/bounds for this research only; this is not a production AccessibilityService result or BLE test. No file was selected or played by these research taps; Apple remained playing and Mango paused in the observed sessions.

- Main screen library control: com.ibasso.music:id/main_list_iv.
- Folder detail: folder_detail_content, entry_title_tv, breadcrumb tv_content nodes, row file_name_tv and music_lib_all_music_list. Visible FLAC filenames were observed in the saved Internal Storage/Music/Tayori folder.
- File rows expose numeric file_info_index_tv; sampled folder rows expose file_info_icon_iv. These are candidate version-specific semantic discriminators, not yet a proved production model.
- Important trap: folder_detail_content retains multiple overlapping ancestor page trees. The first current page matched one research screenshot, but later own-service snapshots put the visible page second. Fixed first/last-page assumptions are invalid. A production parser must isolate and revalidate the active folder and breadcrumb path; it must never aggregate all file_name_tv nodes.
- Library navigation arrow opens a Music menu, not an ordinary parent operation. Menu nodes include internalStorgeLayout (actual spelling), sdcardLayout and allPlaylistLayout. It also contains netLayout/Local Network, which must be excluded from this Bluetooth-only feature.

Artifacts: mango-ui-initial.{xml,png}, mango-ui-library.{xml,png}, mango-ui-storage-root.xml and mango-ui-storage-picker.xml. They are research evidence only.

## Implementation requirements

Keep direct Mango albums/songs and transport controls. Add an optional folder entry with explicit screen-on/unlock limitations; isolate the Mango UI adapter from Apple and BLE transport. Use semantic node actions only in production, with exact app-version checks, current-window/breadcrumb/snapshot-scoped IDs, fresh file-label validation, bounded navigation and stale-screen errors. Only local storage choices may be exposed; never navigate Local Network, delete/settings/download controls or arbitrary remote paths. UI file selection must route through the shared source handoff and keep Mango as engine.

Verify actual service reads and selection, known FLAC/WAV files, rescan/SD and stale IDs, then both phone BLE paths. The permission decision and these ADB research captures do not prove that an app-based Mango folder feature works yet.

## Own-service implementation checkpoint, 29 October

**VERIFIED, local adapter only:** the new separate AccessibilityService read Internal Storage → Music → Tayori → 1st Live “春を待つ” and returned the nine currently visible WAV rows. It selected `02. 白日夢.wav` through SourceRegistry's observed source handoff. Mango reported 白日夢 playing and Apple Music paused. Wi-Fi was0 and Active default network was none. The user answered **“Yes, the correct song is audible”** through AMP15 phone out. This delayed reply is not an instantaneous metadata/acoustic measurement; no phone Bluetooth command was involved. Device/app versions are unchanged from the research baseline.

The service needs `flagIncludeNotImportantViews` to retain the folder layout wrappers. Without it, initial storage navigation timed out. Sibling drawingOrder distinguished the visible Music page (2, above cached root1), Tayori (3, above1/2), and the deep file page (4, stored first above cached1/2/3). Actual screenshots and returned rows agreed. Ambiguous/tied drawing order fails closed; seven synthetic parser tests include cached pages, wrong storage/menu, changed filenames, breadcrumbs and ordering. [Android drawing-order contract](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#getDrawingOrder()).

**FAILED, then under repair:** an early parser selected cached root rows after entering Music. It was corrected before any file-play request. An early storage-root reset timed out after popping one breadcrumb level; bounded, settled navigation now handles Mango's page transitions, with further physical recovery checks pending. These failures remain evidence rather than successful browse results.

Evidence in ignored artifacts/: mango-ui-stable-{internal,music,tayori,files}.json, mango-ui-stable-{tayori,files}-snapshot.json, mango-ui-stable-tayori.png, mango-ui-wav-selected.json and mango-ui-selection-{wifi,network}.txt. Source state is not acoustic proof. Full FLAC/WAV, SD, duplicate/rescan, stale-ID and both phone matrices remain open. No replacement engine, private files, network menu, volume change or destructive action was used.
