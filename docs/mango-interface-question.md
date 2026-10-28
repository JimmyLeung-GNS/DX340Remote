# Prepared Mango interface question

Prepared for the user to send if desired. No vendor contact has occurred. This is an unresolved integration question, not a requirement to replace Mango.

We are developing a stock-Android13 Bluetooth control companion for iBasso DX340 and want to keep Mango Android4.1.37(code240) as the audio engine. Our own ordinary MediaBrowser client can connect to com.ibasso.music/.services.MusicService and browse Now Playing, All Music, Album, Artist and Genre. Playing returned media IDs works in a user-confirmed sample.

We need a supported way to browse local folders and existing playlists, identify a file stably across rescans/SD changes, and select it through Mango. Three sampled All Music descriptions returned positional-looking IDs, display titles with numeric suffixes, no media URI and no extra keys. The installed package advertises no ACTION_VIEW filter for our tested FLAC/WAV file/content intents.

Does Mango provide a documented public library/file/playlist contract or SDK that an ordinary third-party app may use without root, firmware changes or identity spoofing? In particular:

- Are returned MediaBrowser IDs stable across rescans and removable-storage changes, including duplicate titles/artists?
- Can a client obtain a persistent file URI/ID and format, or enumerate folder/playlist nodes through an officially supported root/connection option?
- Is there a supported public play-file or stable play-by-ID route that preserves Mango decoding/output behavior?
- What version/firmware compatibility and permission requirements apply?

We do not need access to another app’s protected downloads or an undocumented internal contract. A negative answer is useful so that the user can make an informed decision about optional UI-assisted folder browsing while retaining Mango playback.
