# DX340 BLE protocol v1 — diagnostic beta

No audio is sent over this service. All JSON is UTF-8. Commands operate installed DX340 source apps through public interfaces. Service UUID: `e7e01000-4a76-4b53-9c8f-c7a12b00d340`. INFO ends `1001`, RX ends `1002`, TX ends `1003` with the same suffix. Full constants live in Kotlin Wire and Swift Wire.

## GATT and authorization

INFO is read; RX is write-with-response; TX is indicate with CCC `00002902-0000-1000-8000-00805f9b34fb`. INFO/RX/CCC require encrypted authenticated (MITM) access through the Android Bluetooth stack. TX sends confirmed indications. Both peers serialize operations. Discovery advertises only service UUID. Default ATT payload is 20 bytes; host uses MTU minus 3; iPhone uses maximumWriteValueLength(for: .withResponse). Each capacity is capped at 512, never assumed.

Delayed results/state replies recheck current bond, local approval and authenticated session before sending. Disconnect clears pending outbound frames and subscriptions immediately; an indication already submitted to the OS cannot be recalled. Stopping the host invalidates its authenticated session before source/transport teardown. Source mutations recheck closed/permission/source/approval guards at final dispatch. These software rules do not replace physical revocation/security tests.

Host indications are serialized through a bounded confirmed-frame queue. Each submitted frame has a10-second Handler-scheduled completion deadline; sleep/main-thread scheduling can delay wall-clock expiry. Missing/error completion, immediate submit failure, overflow, unsubscribe or disconnect clears queues and the logical authenticated peer before requesting OS disconnection. Successful completions cancel their deadlines; stale scheduled timers cannot close a new transfer. Reconnection never replays old frames or playback. Android completions have no frame ID, so delayed same-peer callbacks across reconnect and all actual timing/radio behavior still require physical tests.

Open a 60-second pairing window on DX340. Accept OS pairing and visibly approve the bonded controller in the companion. HELLO returns protocol/host epoch only. AUTHORIZE returns source metadata only after approval. Approval refers to the OS bonded identity, using Android's resolved bond address; a display name or an unbonded address is never sufficient. Maximum eight approvals. Revocation denies subsequent messages and disconnects the controlling peer. One peer is allowed; a second controller must wait for disconnect. Automatic phone/Watch handoff is not implemented. Actual MITM/security and identity resolution across iPhone address changes are NOT TESTED. Do not claim production authentication from these API flags alone. No custom cipher, reusable PIN or application secret is sent.

## Frames

Each characteristic value is at least 9 bytes: 8-byte header and nonempty payload. Big endian header: magic byte `44`, version `01`, 16-bit message ID, 16-bit zero-based chunk index, 16-bit chunk count. Chunks are strictly ordered per ID. Completed messages contain at most 65,536 bytes; four incomplete messages per connection; 10-second monotonic expiry. Clear assemblies and queued controls on disconnect. Malformed/out-of-order/oversized fragments are rejected; ATT errors are distinct from JSON errors. Message IDs wrap independently of request IDs. Shared UTF-8 fixtures: fixtures/framing.json.

## Envelope

Requests: `{v:1,id:UUID,type:operation,hostEpoch:string,source:string,sourceEpoch:string,args:object}`. HELLO and AUTHORIZE bootstrap without a known host epoch. All other requests require current authorization and hostEpoch. Source-scoped operations require the selected package ID and current sourceEpoch. Session token changes generate a new sourceEpoch. Request IDs are 1–64 ASCII alphanumeric/hyphen characters. Request rate is bounded at 20 per second; outbound frames are bounded. Source metadata is bounded; oversized encoded replies return RESPONSE_TOO_LARGE rather than invalid JSON.

Native Mango selection verification changes also advance sourceEpoch when the effective native capability changes. Clients clear old library/queue/navigation state on that event; old revisions are rejected. Same-session parent proofs remain refreshable and selections still re-read current native metadata. Repeated confirmation of an unchanged capability does not advance the epoch. Media-session callback ownership is separate: late callbacks from a replaced or closed session cannot retire a newer session or publish its state.

Replies: `{v:1,id:requestID,hostEpoch:string,type:HELLO|ACCEPTED|RESULT|STATE_CHANGED|ERROR,data:object}`. STATE_CHANGED is unsolicited with an empty ID. ACCEPTED means validation/dispatch is pending, never audible success. A mutation RESULT with `outcome:dispatched` confirms only that the public transport API was invoked. Observed state follows separately. A 12-second controller timeout means unknown outcome; do not automatically retry mutations.

Duplicate IDs in the same authenticated controller/host epoch return cached results or pending, never execute again. Different content under the same ID yields REQUEST_ID_REUSED. The beta ledger holds 2,048 IDs per approved controller and does not evict: SESSION_LIMIT requires a companion restart and new host epoch. Exactly-once execution across crashes is not claimed. Reconnect clears unsent operations, performs HELLO/AUTHORIZE, then observes state; it must not change playback or volume.

## Implemented operations

| Operation | Arguments / reply |
|---|---|
| HELLO, AUTHORIZE | Bootstrap; local approval required for AUTHORIZE |
| LIST_SOURCES, GET_CAPABILITIES | sources array: IDs, versions, epochs, per-action capabilities and browse probe |
| GET_STATE, SUBSCRIBE_STATE | state object; observed metadata/playback/capabilities |
| SELECT_SOURCE | args.source changes target; does not launch/play automatically |
| SET_PLAYING | args.playing explicit boolean; advertised action or exact-version physically tested desired-state adapter |
| NEXT, PREVIOUS | Individually capability gated |
| SEEK | args.position milliseconds, bounded by source duration |
| BROWSE | args.parent nullable root, page >=0, revision; 25 items per page, max 50; source/revision-scoped IDs |
| PLAY_ITEM | args.item previously browsed ID and args.revision; requires canPlayItem |
| GET_QUEUE | optional args.offset (default0), args.revision required after offset0; up to50 source queue items with revision/offset/total/nextOffset |
| PLAY_QUEUE_ITEM | args.item must still exist in source queue and require canPlayQueueItem |
| GET_ARTWORK | args.key from current state; bounded public-metadata JPEG/base64 with SHA256 key |

Library nodes have id/title/subtitle/browsable/playable. Responses include parent, page, revision and nextPage (null at end). A legacy source returning an unpaged result is bounded to 10,000 items then paginated on DX340. No arbitrary remote file paths. Item knowledge is bounded at 10,000 IDs. Removable-storage and library-change revision detection still require physical implementation/verification; session epoch alone is not a complete library revision.

Queue pagination is additive within v1. An empty GET_QUEUE args object still returns the first window. New clients request the returned nextOffset with the current revision; nextOffset is null at the end. offset is an integer0–10000; source queues above10000 return QUEUE_LIMIT. Each window has at most50 complete entries and at most48KiB of encoded UTF-8 data, leaving room for the reply envelope. Oversized metadata reduces the entry count; nextOffset advances by the actual count. A single entry that cannot fit returns MESSAGE_TOO_LARGE, never partial JSON or a silently truncated title. A changed source epoch/order/ID/media identity/title changes the queue revision; any supplied old revision or positive offset without the current revision returns STALE_LIBRARY. Refresh with offset0 and no revision to recover. PLAY_QUEUE_ITEM still validates the complete current queue and revision again after source handoff. No paging read starts playback. Clients replace the visible window rather than combining different revisions and show its range/total, Next queue page and Start of queue. Older hosts return no continuation; newer clients do not invent one.

SEARCH, SET_SHUFFLE, SET_REPEAT and ADJUST_VOLUME return UNSUPPORTED_CAPABILITY in this beta. Public metadata artwork is scaled/compressed to at most44KiB, identified by SHA256, and cached with bounds on both clients. Bulk replies have lower priority than controls; actual BLE latency remains unmeasured. canAdjustVolume is false until DX340 output is verified. canResume and canColdLaunch are false. Complete source-switch/audio acceptance remains outstanding.

## Playback source handoff

SELECT_SOURCE changes the command target without starting playback. SET_PLAYING(true), PLAY_ITEM and PLAY_QUEUE_ITEM prepare playback by checking authorization, notification access, source identity/epoch and target validity before pausing a competing supported source. Preparation sends one pause and waits at most3.5s for the same session to report paused. Session replacement, lost authorization, changed target, unknown competing state or timeout prevents target start. Unsupported pause returns OTHER_SOURCE_PLAYING; failed observation returns SOURCE_HANDOFF_FAILED. The host holds mutation ownership throughout. Client request timeout still means unknown outcome; never automatically replay the command.

Item and queue identity/revision are checked again after handoff. Native item selection re-reads the parent and refuses to dispatch after its10s validation deadline. UI selection revalidates the visible downloaded row. A pause may already have succeeded when later validation fails: the previous source stays paused, and the user must issue a new deliberate command. No automatic rollback/resume or volume change occurs. A completed pause observation/dispatch does not establish acoustic silence, gapless switching or phone Bluetooth acceptance.

An already observed desired play/pause state is a no-op, including sources whose current action mask omits the redundant action. A real state-changing dispatch still requires the current capability. The host observes desired-state completion separately from library/queue item dispatch.

## Optional Apple Music UI-assisted browsing

The user accepted this mode's screen-on/unlocked requirement. It is opt-in and version-pinned to Apple Music6.5.3/code1599. Readiness is exposed through canBrowse/canPlayItem and browseMode=`ui-assisted`; ordinary session controls remain independent of UI readiness. Root BROWSE with null parent/revision navigates the public Library/Downloaded UI. Replies add uiAssisted=true, title, backId and a scroll-action parent token. Clients present this as a flat current screen: request backId with the current revision for Back, or parent/nextPage/current revision for forward scrolling. Root refresh recovers expired snapshots. Page sizes follow the visible source UI, bounded at50, and are not native25-item pages.

IDs bind service instance, active window, semantic snapshot and node path; revision must match the current snapshot before any navigation or play. Stale screens fail closed. Only rows bearing the source's Downloaded Song semantic badge are playable. No coordinate taps, gestures, download controls, sign-in forms or private app storage are used. Artist browsing is currently disabled pending model verification. A returned play dispatch is not proof of correct audible playback.

Mango's verified desired-state wrapper calls play only from observed paused state and pause only when required, with at most one callback and bounded state observation. Repeated desired state is a no-op. It avoids Mango's observed toggle-like raw repeated-play behavior. A timeout has unknown outcome and must not trigger automatic retry. This workaround is scoped to the tested app/firmware profile.

## Error codes

AUTHORIZATION_DENIED, LOCAL_APPROVAL_REQUIRED, CONTROLLER_BUSY, INCOMPATIBLE_PROTOCOL, STALE_HOST, STALE_SOURCE, WRONG_SOURCE, MISSING_SOURCE, SOURCE_NOT_READY, PERMISSION_REVOKED, UNSUPPORTED_CAPABILITY, BROWSE_UNAVAILABLE, BROWSE_DENIED, SOURCE_TIMEOUT, SOURCE_LIBRARY_LIMIT, STALE_LIBRARY, EXPIRED_ITEM, OTHER_SOURCE_PLAYING, SOURCE_HANDOFF_FAILED, SOURCE_BUSY, INVALID_ARGUMENT, MALFORMED_MESSAGE, REQUEST_ID_REUSED, RATE_LIMIT, SESSION_LIMIT, RESPONSE_TOO_LARGE. SD_CARD_REMOVED is reserved until an adapter can distinguish removal. Transport errors do not assert source state.

UI mode adds UI_MODE_DISABLED, UI_SCREEN_UNLOCK_REQUIRED, UI_VERSION_UNVERIFIED, UI_FOREGROUND_REQUIRED, UI_SETUP_REQUIRED, UI_UNEXPECTED_SCREEN, UI_DOWNLOADED_CONTEXT_REQUIRED, UI_TREE_LIMIT, UI_BUSY, UI_ACTION_FAILED and UI_ACTION_TIMEOUT. These require a visible setup/recovery explanation; clients must not substitute queue skipping for library browsing.

## Optional Mango local-folder mode

Mango native albums/artists/songs remain the default browse tree. When the user enables the separate Mango folder mode, the native root adds a `mango-ui:root` entry marked `uiAssisted:true`; it is browsable only while the assisted adapter is ready. State adds `canBrowseFolders`, `canPlayNativeItem` and `uiBrowseReason`; `browseMode` remains `direct` for the native tree. Clients decide assisted navigation from each response's `uiAssisted`, rather than assuming that every page for a source has the same mode. `canPlayItem` includes a ready assisted adapter; native item dispatch still requires its connected native browser and native capability/verification at initial validation, before handoff and at final dispatch. Native rows have `playable:false` until native selection is advertised or verified. The `nativeTestable` flag enables only the companion's explicit local diagnostic experiment, and must never enable phone playback.

Native item availability is not inferred from a MediaSession action bit when no native browse/revalidation path is connected. Clients must gate cached assisted actions separately from native capabilities: Apple uses its ui-assisted canBrowse readiness; hybrid Mango uses canBrowseFolders. On assisted readiness changes, discard assisted pages/pending browse context and require fresh browsing, preserving native pages/queues. Native capabilities must not reactivate a cached assisted file or optional folder entry whose service is absent.

Requesting `mango-ui:root` returns only Internal Storage and SD Card choices. Subsequent opaque IDs and revisions bind the service instance, window, semantic folder snapshot and exact row. Assisted responses use the same back/scroll contract as Apple mode. `mango-native:root` returns to native categories without reusing an assisted revision. No arbitrary path, Local Network or file editing action is accepted.

The adapter is opt-in and pinned to Mango4.1.37/code240. DX340 must remain awake/unlocked. It uses semantic Accessibility node actions, sibling drawing order to exclude overlapping cached ancestor pages, stable navigation snapshots and fresh exact labels before dispatch. Ambiguous pages fail with UI_ACTIVE_PAGE_UNVERIFIED; changing trees may return UI_TREE_CHANGED, and wrong storage returns UI_STORAGE_CONTEXT_REQUIRED. Only sampled FLAC/WAV file types are enabled in this experimental mode; actual format/audio acceptance remains separate. PLAY_ITEM uses the shared observed source handoff, with no automatic retry.

Both UI adapters check current source epoch and controller authorization throughout navigation and before replying/acting. Revocation, source changes, service interruption and unbinding cancel or reject pending work. An already completed navigation/pause cannot be undone; no music is automatically resumed. These guards and local adapter tests do not establish either phone's physical BLE or security acceptance.

## Elapsed-position estimates (optional v1 fields)

`state.position` is milliseconds at host snapshot creation, normalized from the source's public PlaybackState position, speed and monotonic last-update time. `-1` means unknown. `duration` remains source metadata in milliseconds. New hosts include optional `playbackSpeed`: positive finite rates up to16 only while the source reports playing, with a known position/duration and valid source timestamp; otherwise0. Projection clamps to duration. These fields describe source estimates, never audible-output observations. Android and phone monotonic timestamps must not be compared across devices.

Controllers may advance a labelled estimate locally between source events without requesting periodic state messages. Old hosts without speed remain fixed snapshots. Never infer a next track at duration, enable seeking from the estimate alone, or replay a command when the estimate becomes stale. Actual elapsed accuracy, latency and source/phone recovery still need physical tests.

## Native browse cancellation and session access

Native page requests bind authorization, source epoch and browser instance through completion. After timeout/cancellation, callbacks may not alter cached item/proof/revision data. Detected notification-access loss cancels pending native pages, invalidates source callbacks and item IDs, advances source epochs and disables advertised capabilities. Restoring access requires fresh discovery/observer registration and source state; old requests remain terminal. GET_STATE/SUBSCRIBE_STATE explicitly refreshes that discovery without playback or restarting the companion. These are implementation rules, with physical permission/BLE delivery still subject to acceptance tests.
