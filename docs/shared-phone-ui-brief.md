# Shared phone presentation

Scope: Flutter phone migration; Operate mode. User confirmed native system appearance, then explicitly requested shared Android/iPhone interface/UI and stable use/connection. Preserve the four existing destinations and actual native BLE/source mechanisms. This is an extension of the selected system appearance, not a replacement visual identity. No approved comp or concept tournament; existing native captures are references for content and states.

## Direction contract

**THESIS:** One maintained phone interface lets the owner find DX340, approve control, play music and browse available libraries while actual playback stays on DX340. Connection state and source capabilities decide the available actions.

**OWN-WORLD:** System light/dark appearance, system typography, Material Android and Cupertino iPhone controls. Stock components and clear text labels carry the interface. Preserve native disabled, selected and focus states; keep setup instructions within scrolling content.

**STORY:** Setup presents one Connect action that requests access, discovers or reconnects the remembered approved player, and observes local approval without a manual retry step. Multiple/other players and controller handoff remain deliberate choices. Each connection phase shows its next action or Cancel; detailed help is secondary. Player exposes separate Play/Pause and source selection; Library handles native pages or an explicit assisted screen; Queue uses source-provided IDs. A disconnected list gives a working route back to Setup. The Companion guides enabling phone control and setting up a new phone separately from normal saved-grant reconnect.

**FIRST VIEWPORT:** Navigation title above a scrolling status/content area, four stable bottom destinations. Player begins with available track metadata and large labeled controls; disconnected state provides Open Setup. No fixed connection banner covers content at large text. Cupertino fixed-height tab labels follow native scaling while reading content scales and scrolls.

**FORM:** Inherited native phone structure, adapted to one set of Dart screens. No seed key applies to this specified migration of an established appearance. Real connected-library data is not simulated in product code; synthetic tests are labeled in their evidence.

**FINISH:** unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Quality bar

Clear current/disabled/recovery states; readable normal/dark/large-text screens; at least48 logical-pixel action targets; bottom destinations remain usable; long titles wrap; stale cache never enables playback. Review scope must identify screenshots versus tap tests versus synthetic tests. Simulator/launch/UI review cannot establish pairing, BLE, source audio, screen-reader or walking/battery acceptance. iOS template launcher artwork remains provisional and separately documented in phone/ASSET_ORIGINS.md.
