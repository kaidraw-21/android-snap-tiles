# Release Notes

## v1.0.0 — 2026-03-17

### What's New
- Initial release
- Quick Settings tiles: USB Debugging, Developer Mode, Accessibility
- Floating button overlay
- Custom tile slots (up to 5) with Five Elements theme
- Configurable label and actions per slot
- Cache & restore for Accessibility services state
- Cache & restore for USB Debugging state when toggling Developer Mode

### Installation
1. Download `snap-tiles-v1.0.0.apk`
2. Install on device
3. Grant permission via ADB:
   ```
   adb shell pm grant com.snap.tiles android.permission.WRITE_SECURE_SETTINGS
   ```

### Requirements
- Android 8.0+ (API 26)
- ADB access for initial setup

### Known Issues
- None reported
