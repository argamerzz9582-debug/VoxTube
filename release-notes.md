# VoxTube v1.0.0

**Release date:** 2026-11-26
**Status:** Stable

The first VoxTube release: a privacy-respecting YouTube and YouTube Music client with an on-device recommendation engine and a new look.

## Download

| File | What it is |
|---|---|
| `voxtube.apk` | Universal build. Updates itself from inside the app. **Pick this if unsure.** |
| `voxtube-arm64-v8a.apk` | Smaller build for most modern phones (64-bit ARM) |
| `voxtube-armeabi-v7a.apk` | Smaller build for older 32-bit phones |
| `voxtube-foss.apk` | FOSS build without the built-in updater and Discord integration (also as `voxtube-foss-arm64-v8a.apk` and `voxtube-foss-armeabi-v7a.apk`) |
| `checksums.txt` | SHA-256 of every APK |
| Source code (zip / tar.gz) | The full source of this release |

Android 8.0 or newer. VoxTube uses its own app ID (`com.voxtube.app`), so it installs next to other YouTube clients. Backups from Flow can be imported in Settings > Backup.

## Highlights

- New name, new logo and a new app ID
- Softer, rounder corners across the app as the first step of the Liquid Glass redesign
- Updates come straight from this repository's releases

## New

- Pinch the mini player to any size, drag a tucked mini player back out, and new TalkBack actions for it
- Fine scrubbing: pull up while dragging the seek bar to scroll through frames
- Remember video quality per network (#2178)
- Hashtags, music credits, verified badges and live viewer counts on the watch page
- Related videos keep loading, and now include playlists, Mixes and Shorts
- Autoplay on Home (off by default) and a top bar that slides away as you scroll
- Captions follow the audio track, and Shorts can show captions (#2377)
- Rich community posts: tappable links, playlist cards and image polls
- Liked music first in Your playlists on Music home (#2359)
- Spotify Canvas and a quality choice for animated album art (#2379)
- Works on school and work Wi-Fi that needs a certificate you installed (#2284)
- While Deep Focus is on, the keyboard does not learn what you type in search

## Improved and fixed

- Faster, more reliable downloads that survive network drops and expired links, with a free space check
- Smoother playback start, preloading and picture-in-picture, and lower memory use in Shorts
- Many fixes for live streams, the queue, music shuffle, HDR and Android TV
