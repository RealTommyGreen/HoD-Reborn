# Heart of Darkness Reborn — Android Port Changelog

Based on [hode](https://github.com/cyxx/hode) 0.2.9f by Gregory Montoir.

---

## v1.0.0 (2026-06-14)

First stable release.

### Project Structure
- Package renamed from `com.heartofdarkness.reborn` to `com.hod.reborn`
- Repository branches cleaned up: `main` tracks original hode engine, `reborn` contains all Android work
- `.gitignore` added for build artifacts and keystore

### Release Build
- Release signing configured via `hod-release.jks`
- Signed release APK (13 MB, 3 ABIs: arm64-v8a, armeabi-v7a, x86_64)

---

## v0.2.0 (2026-06-04)

### SVG Icon System
- 5 custom SVG icons for touch overlay: Run, Jump, Shoot, Cancel, Check
- `SvgIconManager` for SVG rendering (AndroidSVG-based, 512px bitmap cache)
- Dynamic menu icons: Jump/Shoot swap to Check/Cancel when menu is open
- Run button auto-hides in menu
- Menu state polling via `nativeIsMenuOpen()` JNI bridge

### Touch Overlay Improvements
- `btn_shoot_run` removed (redundant); default layout simplified to 5 buttons
- Button geometry fix: proper outer/icon shape bounds
- `iconFill` field added to button config with iconset.json defaults
- Config migration: old layouts with `btn_shoot_run` automatically cleaned up (schema v12)

### Menu Behavior
- Cancel button (SHIFT) now returns to game from main menu when accessed in-game

### Visual
- Default button sizes increased (Menu 0.18, Actions 0.26, DPAD 0.50)
- Size slider range extended to 0.60

### Bug Fixes
- SVG name mismatch between iconset.json and iconmappings.json fixed
- DPAD scaling corrected (SQUARE shape now uses full view dimensions)
- Menu icons now display correctly on initial app start

---

## v0.1.0 (2026-05-25)

Initial Android port of the Heart of Darkness engine.

### Core Engine
- Native C++ engine built as shared library via CMake + NDK 27
- 29 HoD source files compiled across 3 ABIs (arm64-v8a, armeabi-v7a, x86_64)
- SDL2 2.30.11 integrated via FetchContent
- Level-based main loop with checkpoint support
- PSX and PC data file detection

### Android Integration
- Android App Bundle structure with Kotlin + Gradle (AGP 8.7.3)
- Target SDK 35, minimum SDK 24
- SAF-based asset importer with case-insensitive HoD file validation (`setup.dat`, `*_hod.lvl`, `*_hod.sss`, `*_hod.mst`, PAF variants)
- Imported assets stored in app-local storage (`filesDir/imported_game/hode`)
- Launcher with gamepad navigation, import status display, and controller detection
- Graceful handling of missing imports (no crash)

### Touch Overlay
- 6-button default layout: Menu, D-Pad, Run, Jump, Shoot, Use
- Key mappings: Run=CTRL, Jump=ENTER, Shoot=SHIFT, Shoot+Run=SPACE, Menu=ESCAPE
- Configurable via settings dialog (button size, position, shape)
- Persistent per-button configuration (schema v11)
- Video filter selector: Nearest (original), Linear, xBR Smooth

### Gamepad Support
- Hardware controller detection via Android InputDevice API
- Default mapping: A=Run, B=Jump, X=Shoot, Y=Shoot+Run, START=Menu
- Controller mapping info displayed in settings

### Cheats
- God Mode (no damage from spectre fireballs)
- One-Hit Kill (plasma cannon)
- Walk on Lava

### Ingame Menu
- Menu button opens native HoD menu without aborting the level
- Second press closes menu and returns to gameplay
- Full palette and sound state restored on return
- Level SSS data correctly reloaded after menu close

### Graphics
- SDL2 renderer with runtime-scaler switching (nearest/linear/xBR)
- 256x192 internal resolution, aspect-ratio-correct scaling
- Hardware acceleration disabled for pixel-accurate rendering

### Audio
- SDL2 audio callback with HoD mixer integration
- 16-channel software mixer with original sound limiter

### Build
- Debug APK: 14 MB, 3 ABIs
- No game data files included (user must provide licensed assets)

---

The upstream engine changelog is preserved in [CHANGES.txt](CHANGES.txt).
Game version compatibility is documented in [RELEASES.yaml](RELEASES.yaml).
