# Snap Tiles

Quick Settings tiles to toggle system settings on Android with a single tap — no need to open the Settings app.

## Features

- **Fixed Tiles** — USB Debugging, Developer Mode, Accessibility (always enabled)
- **Custom Tiles** — Up to 5 configurable slots with multiple actions per tile
- **Smart Caching** — Remembers Accessibility services and USB state, restores on re-enable
- **System Controls** — Stay Awake, Running Services, Force RTL Layout
- **Advanced Debugging** — Profile GPU Rendering, Demo Mode, Animator Duration Scale

## Installation

### Download APK

Grab the latest release from the [Releases page](https://github.com/nickolasddiaz/tile-debug-app/releases).

### Build from source

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## ADB Setup

Grant the required permission once via ADB:

```bash
adb shell pm grant com.snap.tiles android.permission.WRITE_SECURE_SETTINGS
```

Then pull down Quick Settings → Edit (pencil icon) → drag the tiles you want into your panel.

### Notes
- Permission `WRITE_SECURE_SETTINGS` is required once via ADB. No ADB needed after that.
- No root required.
- Fixed tiles are always enabled — no on/off switch.
- Accessibility tile caches active services and restores them on re-enable.

## Demo

![Demo](media/howtouse.gif)

## Changelog

See [RELEASES.md](RELEASES.md) for full release notes.
