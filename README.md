# HoD Reborn

Android port of the classic 1998 cinematic platformer **Heart of Darkness** by Amazing Studio.

Built on [hode](https://github.com/cyxx/hode) 0.2.9f — the incredible reverse-engineered engine by **Gregory Montoir** that made this possible.

**Version:** 1.0.0 | **Min SDK:** 24 (Android 7.0) | **Target SDK:** 35

---

## Features

- Full 9-level campaign with checkpoint support
- PSX and PC data file compatibility (see [RELEASES.yaml](RELEASES.yaml))
- Touch overlay with 5 customizable buttons (D-Pad, Run, Jump, Shoot, Use)
- SVG icon system with dynamic menu-aware icons
- Hardware gamepad support (auto-detected)
- Video scalers: nearest-neighbor (original pixel look), linear, and xBR
- Ingame menu toggle without level restart — full palette and sound state restored
- Cheats: God Mode, One-Hit Kill, Walk on Lava
- SAF-based game file import (no root required)
- Signed release APK

## Download

**Latest APK:** [HoD-Reborn-v1.0.0.apk](https://drive.google.com/file/d/1i1nOgttOCstF4ob5fTDkemYxnE8QMKmv/view?usp=drivesdk) (12.6 MB, arm64-v8a + armeabi-v7a + x86_64)

See [CHANGELOG.md](CHANGELOG.md) for version history.

> [!IMPORTANT]
> This APK does **not** include any game data files. You must provide your own licensed copy of Heart of Darkness.

## Game Data Setup

1. Copy the following files from your Heart of Darkness installation to a folder on your device (e.g. `storage/emulated/0/games/heart of darkness/`):
   - `setup.dat`
   - `*_hod.lvl`, `*_hod.sss`, `*_hod.mst` (one set per level)
   - `hod.paf` (or `hod_demo.paf` / `hod_oem.paf`)

2. Launch the app — the launcher will guide you through selecting and importing your game folder.

The import is case-insensitive and validates all required files before proceeding.

## Building from Source

### Requirements

- Android Studio (Hedgehog or later recommended)
- NDK 27.2.12479018
- AGP 8.7.3
- Kotlin 2.0.21

### Build

```bash
cd android
./gradlew assembleRelease
```

The signed APK is at `android/app/build/outputs/apk/release/app-release.apk`.

To build for debug (unsigned, faster):

```bash
./gradlew assembleDebug
```

### Signing

Release builds require a keystore at `android/app/hod-release.jks` and two environment variables:

- `HOD_KEYSTORE_PASSWORD`
- `HOD_KEY_PASSWORD`

## Architecture

| Layer | Technology |
|-------|-----------|
| Engine | C++17, CMake, NDK 27 |
| Platform | SDL2 2.30.11 (static, FetchContent) |
| UI | Kotlin, Android Views |
| SVG Icons | AndroidSVG 1.4 |
| Serialization | kotlinx-serialization-json |
| Build | Gradle 8.9, AGP 8.7.3 |

Native sources are compiled as a single shared library (`libhode.so`) across 3 ABIs.

## Credits

### Original Game

**Heart of Darkness** (1998) by Amazing Studio & Infogrames. One of the finest cinematic platformers ever made.

### Engine

**[hode](https://github.com/cyxx/hode)** by **Gregory Montoir** — A meticulous reverse-engineering of the Heart of Darkness engine. This Android port would not exist without his work.

### Android Port

Touch overlay, SVG icon system, gamepad support, SAF import, and all Android-specific integration.

### Third-Party

- [SDL2](https://github.com/libsdl-org/SDL) — zlib license
- [AndroidSVG](https://github.com/BigBadaboom/androidsvg) — Apache 2.0

## License

This project is published for educational and preservation purposes. The Heart of Darkness game data files are copyrighted by Amazing Studio / Infogrames and are **not** included.

See the upstream [hode](https://github.com/cyxx/hode) repository for engine licensing.

---

*Dedicated to everyone at Amazing Studio who created this masterpiece, and to Gregory Montoir for bringing it back to life.*
