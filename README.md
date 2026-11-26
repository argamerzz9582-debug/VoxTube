<div align="center">
  <img alt="VoxTube" src="Assets/logo.png" width="96">

  <h1>VoxTube</h1>

  <h3>A privacy-respecting YouTube and YouTube Music client for Android — with a Liquid Glass UI and a recommendation engine that stays on your phone.</h3>

  <p>
    <a href="https://github.com/argamerzz9582-debug/VoxTube/releases"><b>Download APK</b></a> ·
    <a href="https://github.com/argamerzz9582-debug/VoxTube/issues"><b>Report a bug</b></a> ·
    <a href="#license--copyright"><b>License</b></a>
  </p>
</div>

---

## Why VoxTube?

Most open-source YouTube clients give you playback but no way to discover new content. You either use the official app and get tracked, or you use an alternative and lose recommendations entirely.

VoxTube gives you both. The recommendation engine learns what you like by analyzing your watch behavior locally. It never leaves your device. You can inspect everything it knows about you, adjust it, or wipe it at any time.

VoxTube is a rebranded and redesigned fork of [Flow](https://github.com/Flow-Tube/Flow). It keeps Flow's engine and features and gives them a new Liquid Glass look.

---

## Features

### Video
- High-quality playback via ExoPlayer (Media3) with resolution switching (1080p, 720p, 480p, 360p)
- SponsorBlock — automatically skips sponsors, intros, outros, and filler
- DeArrow — replaces clickbait thumbnails and titles with community-sourced alternatives
- Return Youtube Dislike (RYD)
- Background playback — listen to audio with the screen off
- Picture-in-Picture — keep watching while using other apps
- Casting to smart TVs and streaming devices
- Playback speed control (0.25x to 2x)
- Video chapters with seek jumping
- Gesture controls for brightness, volume, and seeking
- Subtitles with customizable font size, color, and background
- Downloads with VP9, AV1, and standard format support
- Resume playback from where you left off

### Music
- Dedicated music player with album art and audio visualizations
- Queue management with add, remove, and reorder
- Shuffle and repeat (single/all)
- Persistent mini player across the app
- Synchronized lyrics display
- Fetches tracks from YouTube Music
- Playback speed control for music playback

### Recommendations (on-device Neuro engine)
- Runs 100% on-device — no server, no telemetry, no account needed
- Learns from what you watch, skip, like, dislike, search for, and how long you watch
- Distinguishes weekday and weekend patterns, morning and night preferences
- Detects when you're getting bored of a topic and mixes in new content
- Prevents your feed from collapsing into the same 2-3 topics
- Surfaces related videos from your recent watches to create natural topic transitions
- Uses engagement signals (like-to-view ratios) to filter out low-quality content
- Full transparency dashboard — see what the algorithm knows and why it recommended something
- Export/import your entire recommendation profile as a file

### Library
- Local watch history
- Favorites and custom playlists
- Shorts feed with bookmarking
- Continue watching shelf
- Subscription management with cached feeds

### Privacy
- No Google account required
- No ads, analytics, or tracking
- All data stored locally on your device
- Import subscriptions and history from NewPipe
- Export or delete everything at any time

### Appearance
- **Liquid Glass UI** — translucent, refractive tab bar, toolbars and sheets that bend the content behind them (full effect on Android 13+, frosted on Android 12, solid fallback on older devices)
- Softer, larger-radius shape system across the app
- 25+ colour palettes, Material You and custom themes — the glass picks up whichever palette you choose
- Built entirely with Jetpack Compose


---

## Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Home Feed</b><br><img src="Assets/Home.jpeg" width="240"></td>
      <td align="center"><b>Video Player</b><br><img src="Assets/VideoPlayer.jpeg" width="240"></td>
      <td align="center"><b>Shorts</b><br><img src="Assets/Shorts.jpeg" width="240"></td>
    </tr>
  </table>
  <details>
    <summary><b>More screenshots</b></summary>
    <table>
      <tr>
        <td align="center"><b>Subscriptions</b><br><img src="Assets/Subscriptions.jpeg" width="240"></td>
        <td align="center"><b>Channel View</b><br><img src="Assets/Channel.jpeg" width="240"></td>
        <td align="center"><b>Playlist</b><br><img src="Assets/Playlist.jpeg" width="240"></td>
      </tr>
      <tr>
        <td align="center"><b>Music Hub</b><br><img src="Assets/Music.jpeg" width="240"></td>
        <td align="center"><b>Music Player</b><br><img src="Assets/MusicPlayer.jpeg" width="240"></td>
        <td align="center"><b>Lyrics</b><br><img src="Assets/Lyrics.jpeg" width="240"></td>
      </tr>
      <tr>
        <td align="center"><b>Artist Page</b><br><img src="Assets/Artist.jpeg" width="240"></td>
        <td align="center"><b>Album</b><br><img src="Assets/Album.jpeg" width="240"></td>
        <td align="center"><b>Your Library</b><br><img src="Assets/Library.jpeg" width="240"></td>
      </tr>
      <tr>
        <td align="center"><b>Your Taste</b><br><img src="Assets/YourTaste.jpeg" width="240"></td>
        <td align="center"></td>
        <td align="center"></td>
      </tr>
    </table>
  </details>
</div>

> Screenshots show the pre-redesign UI and will be refreshed as the Liquid Glass redesign lands.

---

## Download

Get the latest APK from the [Releases page](https://github.com/argamerzz9582-debug/VoxTube/releases).

- **GitHub build:** updates itself from inside the app and supports Discord Rich Presence.
- **FOSS build:** the same app without the built-in updater and the Discord integration.

VoxTube uses its own application ID (`com.voxtube.app`), so it installs next to Flow and keeps its own data. To move over, export a backup in Flow and import it in VoxTube.

**Minimum requirement:** Android 8.0+

---

## Building

```bash
./gradlew assembleGithubDebug   # or assembleFossDebug
```

Always use a flavor-prefixed task. See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md) for the code and design guidelines, including the Liquid Glass rules.

---

## 🙏 Acknowledgments

VoxTube stands on the shoulders of giants. Special thanks to:

*   **[Flow](https://github.com/Flow-Tube/Flow) by A-EDev:** VoxTube is a fork of Flow. The player, extraction, sync and recommendation engine are Flow's work.
*   **[Kyant Backdrop / AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass):** The Liquid Glass effects behind VoxTube's redesign.

*   **[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor):** Where Flow's data extraction began, and the source of the relative-date word lists still shipped.
*   **[NewPipe](https://github.com/TeamNewPipe/NewPipe):** For inspiration from their solid foundation for YouTube data handling.
*   **[PipePipe](https://codeberg.org/NullPointerException/PipePipe):** For their SABR and InnerTube playback implementation, which guided the YouTube streaming pipeline.
*   **[PipePipe Developer Docs](https://priveetee.github.io/Docs-PipePipe/):** For their reference documentation on SABR, BotGuard/PoToken attestation, and InnerTube extraction internals.
*   **[MetroList](https://github.com/MetrolistGroup/Metrolist):** Inspiration for the Hybrid Music fetching approach, Lyrics handling and some icons design references.
*   **[LibreTube](https://github.com/LibreTube/LibreTube):** Inspiration for SponsorBlock and DeArrow handling and some icons design references.
*   **[ExoPlayer](https://github.com/google/ExoPlayer):** The gold standard for Android media playback.
*   **[Jetpack Compose](https://developer.android.com/jetpack/compose):** For enabling the beautiful, modern UI.
*   **[Material Design 3](https://m3.material.io/):** For the design system and guidelines.

---

<div align="center">

---

<a id="license--copyright"></a>
## 📄 License & Copyright

**VoxTube** is Free Software: you can use, study, share, and improve it at your will.
It is distributed under the **GNU General Public License v3 (GPLv3)**, the same license as Flow.

- VoxTube modifications © 2026 VoxTube contributors
- Original Flow code **Copyright © 2025-2026 A-EDev** — see [Flow](https://github.com/Flow-Tube/Flow)

> 🚨 **For Developers:**
> The GPLv3 requires that any project using this source code (including the on-device recommendation engine) must also be **Open Source** under the GPLv3. You may not use this code in a proprietary or closed-source application. Keep the upstream license headers and credits intact.
