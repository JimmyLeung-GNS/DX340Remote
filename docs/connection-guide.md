# Connecting your phone to the player

Music plays through the player's existing music app and headphone output. The phone sends controls over Bluetooth; everyday control does not need Wi-Fi, a hotspot or internet.

## First setup

1. On DX340, open **Companion**. If needed, choose **Review permissions and access** and complete the Bluetooth and playback-access setup shown there.
2. Choose **Set up first phone**. If another phone is already approved, choose **Enable phone control** if shown, then **Set up a new phone**.
3. On the phone, open Remote → **Setup → Connect**. Allow Bluetooth/Nearby Devices access when requested. If a list appears, choose the player you recognize.
4. If the operating systems show matching pairing codes, confirm those codes on both devices before the prompts expire. Android may put the request in the notification shade.
5. In Companion's **Controller access** section, approve the identified phone using **Approve [phone name]**. Remote checks for this approval automatically during the live setup attempt. Wait for **Connected to DX340**, then choose **Open Player**.

If the setup attempt expires, start **Connect** again. An old closed prompt is not a live approval request. Bluetooth pairing and Companion approval are separate steps; completing one does not complete the other.

## Everyday connection

Keep Bluetooth on and Companion's phone control enabled. In Remote → Setup, tap **Connect to DX340**. The approved player is remembered; you do not need another new-phone setup window or another approval for each connection.

Use Player, Library and Queue for the capabilities available from the chosen source. Optional Apple Music downloads and Mango folders/playlists browsing need the DX340 awake and unlocked. These browsing options can stay enabled; ordinary supported playback controls and optional screen-assisted browsing have different requirements.

If Queue says **Read-only queue**, choose tracks from **Library**. Some sources expose a queue for display without supporting selection from it; those entries stay disabled.

If browsing asks you to close a dialog, check the music app on the player, handle its prompt locally, then browse again from the phone. The remote leaves source-app prompts for you to decide.

Mango can keep an old SD-folder listing. A fresh SD-root browse may briefly switch the player through Internal Storage and back to reload it. If an old folder was removed, browse again after the error; the remote does not retry that folder action.

If **SD Card** is unavailable, check the card on your player and browse again after it is mounted. Internal Storage stays separate. A cached SD screen can still require local player handling; the current test report records that limitation.

**Disconnect** ends phone control and leaves music playing on the player. It keeps the saved approval for next time. **Stop Bluetooth remote** in Companion also stops phone control without pausing the source music app.

## If connection needs attention

| What Remote shows | What to do |
| --- | --- |
| Turn on Bluetooth | Open Bluetooth settings, enable Bluetooth, return to Remote and tap Connect. |
| Allow Bluetooth access | Allow Nearby Devices/Bluetooth access. If Android no longer asks, use Open app settings → Permissions → Nearby devices → Allow, then return and Connect. |
| Approve this phone on your player | Open Companion's new-phone setup and approve the identified requesting phone while the attempt is live. |
| Another controller is connected | Leave it in control, or deliberately choose Take control on this phone if offered. This disconnects the other approved controller while music stays on the player. |
| Connection needs another try / timed out | Make sure Companion has phone control enabled and the player is nearby, then Connect again. For an already approved phone, another pairing window is normally unnecessary. |
| Your saved player is unavailable | Use Connection help → Choose another player only when you intend to change players. Remote does not silently substitute another player. |
| A library page is stale | Browse again or refresh Queue before selecting. Repeatedly tapping an old song does not recover a stale page. |

On the tested Companion644/Pixel4318 build, an enabled Companion also recovered automatically after its process was terminated in the background, without reopening it or pairing again. Choosing Advanced → Stop Bluetooth remote keeps phone control off; enable it locally when you want to use it again. Force-stop, reboot and other players still require their own checks. [Test evidence](test-results/2026-10-31-host-automatic-recovery.md). If the retry window finishes before the player becomes available, tap Connect deliberately after restoring Companion. Pending playback commands are discarded during recovery; check the current track before issuing another command.

This guide describes the current shared interface. Recorded physical checks cover the DX340 and Pixel9a at the package versions in [status](status.md); iPhone device signing/Bluetooth and wider player compatibility remain unverified. Native reference apps and older APKs may have different labels. Keep existing approved controllers and system bonds when troubleshooting; do not remove them as a routine reconnect step.
