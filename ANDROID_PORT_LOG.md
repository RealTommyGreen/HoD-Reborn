# HoD Android Port Log

## 2026-05-25: Phase 1 Complete

### Phase 1: Projektgeruest kopieren und umbenennen

**Aktionen:**
- ANDROID_PORT_PLAN.md und ANDROID_PORT_LOG.md erstellt
- Gradle-Wrapper (gradlew, gradlew.bat, gradle-wrapper.jar, gradle-wrapper.properties) kopiert
- Root build.gradle.kts: Plugins AGP 8.7.3, Kotlin 2.0.21, kotlinx-serialization
- settings.gradle.kts: rootProject.name = "HeartOfDarknessAndroid", include :app
- gradle.properties: gleiche JVM/Settings wie BS (Xmx2048m, UTF-8)
- app/build.gradle.kts: namespace/applicationId = com.heartofdarkness.reborn, compileSdk=35, targetSdk=35, minSdk=24, ndkVersion=27.2.12479018, ABIs arm64-v8a/armeabi-v7a/x86_64, kein signingConfig (kein Keystore)
- AndroidManifest.xml: HodLauncherActivity (MAIN/LAUNCHER), HodActivity, App-Label "Heart of Darkness", landscape/fullscreen, hardwareAccelerated=false
- SDL Java Sources (9 Dateien) nach org/libsdl/app kopiert
- Kotlin Sources (17 Dateien) mit Package-Rename auf com.heartofdarkness.reborn:
  - HodActivity.kt (ex BermudaActivity), HodLauncherActivity.kt (ex BermudaLauncherActivity)
  - SafImporter.kt (validiert setup.dat, *_hod.lvl, *_hod.sss, *_hod.mst, hod*.paf)
  - AssetExtractor.kt, ControllerDeviceDetector.kt
  - touch/ Paket: 12 Dateien mit Package-Rename
- Resources: colors.xml, styles.xml (AppTheme, AppTheme.Fullscreen), ic_launcher, app_icon, launcher_background
- Native (Skeleton): CMakeLists.txt, jni/src/android_main.cpp (beide von BS, noch nicht angepasst)

**Files:** 43 Dateien unter android/

**Wichtige Entscheidungen:**
- SafImporter-HoD-Validierung wurde in Phase 1 vorgezogen (spart spätere Doppelarbeit)
- HodActivity.getLibraries() = ["hode"], getArguments() = --datapath, --savepath, --debug
- Kein bermuda.keystore kopiert → kein Signing (Codex-Endabnahme erforderlich)
- android_main.cpp JNI-Namen noch auf BS-Package → Phase 2 fixt das

**Review-Ergebnisse:**
- grep "com.bermuda.reborn" in Kotlin: 0 Treffer ✅
- grep "BermudaActivity|BermudaLauncherActivity" in Kotlin: 0 Treffer ✅
- android_main.cpp JNI: 5 Funktionen mit BermudaActivity → Phase 2-Task
- launcher_background.png: noch BS → Phase 3-Task

**Naechste Schritte (Phase 2):**
1. CMakeLists.txt fuer HoD-Quellen umschreiben
2. android_main.cpp JNI-Funktionen auf HodActivity umbenennen
3. Native Sources definieren: main.cpp, system_sdl2.cpp, fs_android.cpp, scaler_xbr.cpp
4. Blacklist: fs_posix.cpp, system_psp.cpp, system_wii.cpp, benchmark.cpp
5. STATIC_CODE_CHECK nach CMake-Rewrite (keine APK bauen!)

---

## 2026-05-25: Phase 2 Complete

### Phase 2: Native HoD-Build integrieren

**Aktionen:**

1. **CMakeLists.txt komplett neu geschrieben:**
   - Projektname: `hode` (statt `bs`)
   - 29 HoD-Quelldateien in ENGINE_SOURCES aufgelistet:
     `andy.cpp`, `fileio.cpp`, `fs_android.cpp`, `game.cpp`, `lzw.cpp`, `main.cpp`,
     `mdec.cpp`, `menu.cpp`, `mixer.cpp`, `monsters.cpp`, `paf.cpp`, `random.cpp`,
     `resource.cpp`, `scaler_xbr.cpp`, `screenshot.cpp`, `sound.cpp`, `staticres.cpp`,
     `system_sdl2.cpp`, `util.cpp`, `video.cpp`,
     `level1_rock.cpp` bis `level9_dark.cpp`
   - 4 Dateien geblacklistet (nur als Kommentar, nicht im Build):
     `benchmark.cpp`, `fs_posix.cpp`, `system_psp.cpp`, `system_wii.cpp`
   - SDL2 2.30.11 via FetchContent als static library
   - `ANDROID_PACKAGE_NAME="com.heartofdarkness.reborn"` als Compile-Definition
   - Link-Libraries: `SDL2-static`, `SDL2main`, `log`, `android`, `-lc++_shared`
   - Include-Directories: ENGINE_ROOT (repo root), SDL2 include, jni directory

2. **android_main.cpp JNI umbenannt und an HoD angepasst:**
   - Alle 5 JNI-Funktionen: `Java_com_bermuda_reborn_BermudaActivity_*` → `Java_com_heartofdarkness_reborn_HodActivity_*`
   - Log-Tag: `BSNative` → `HodNative`
   - BS-spezifische Features entfernt:
     - `--musicpath=`, `--soundfont=`, `--widescreen=` Argumente entfernt
     - `g_stub`, `g_gameStatePtr` entfernt (HoD nutzt globales `g_system`)
     - Screen-Mode-Logik entfernt (HoD nutzt `hode.ini`-basiertes Config)
     - Touch-Inventory-Logik entfernt (HoD hat kein Inventory)
   - HoD-spezifische Anpassungen:
     - Game-Konstruktor: `new Game(dataPath, savePath, g_pendingCheatMask)` (3 args statt BS's 5)
     - Main-Loop: Level-basierte Struktur mit `g_game->mainLoop(level, checkpoint, levelChanged)`
     - Audio-Setup via Lambda-Callback (wie original `main.cpp`)
     - `g_game->_res->loadSetupDat()` vor `g_system->init()` aufgerufen
     - `Video::W` (256) und `Video::H` (192) fuer Display-Init
     - PSX-Erkennung via `g_game->_res->_isPsx`
   - JNI-Stubs fuer BS-spezifische Funktionen (Kotlin-Kompatibilitaet):
     - `nativeSetScreenMode` → Log-Stub (HoD nutzt kein Screen-Mode-Toggle)
     - `nativeSetTouchInventoryEnabled` → Log-Stub (HoD hat kein Inventory)
     - `nativeGetTouchInputContext` → Returns 0 (Default-Gameplay)
   - Includes ergaenzt: `util.h` (fuer `g_debugMask`), `video.h` (fuer `Video::W`/`Video::H`)
   - Cheat-Mapping auf HoD: cheatId 0-5 → `kCheatSpectreFireballNoHit` etc.

3. **fs_android.cpp geprueft:**
   - Verwendet `funopen()` (BSD) fuer Asset-Streaming
   - BS-Port kompiliert erfolgreich mit NDK 27 und `funopen`
   - Keine Aenderung noetig (Plan sagt: nur bei Compile-Fehler fixen)

**Files geaendert:**
- `android/app/src/main/jni/CMakeLists.txt` — komplett neu (75 Zeilen)
- `android/app/src/main/jni/src/android_main.cpp` — komplett neu (152 Zeilen)

**Files geprueft, unveraendert:**
- `fs_android.cpp` — `funopen`-Aufruf bestaetigt kompatibel

**Review-Ergebnisse:**
- grep "bermuda\|Bermuda\|BSNative" in android_main.cpp: 0 Treffer ✅
- grep "com.heartofdarkness.reborn.HodActivity" in android_main.cpp: 5 Treffer (alle JNI-Funktionen) ✅
- grep "bs\|BS\|bermuda\|Bermuda" in CMakeLists.txt: 0 Treffer ✅
- Blacklist-geprueft: benchmark.cpp, fs_posix.cpp, system_psp.cpp, system_wii.cpp nicht in ENGINE_SOURCES ✅
- Symbol-Check: alle referenzierten Symbole (g_debugMask, Video::W/H, _isPsx, _isDemo, kCheat*, etc.) in Headern deklariert ✅

**Build-Status:**
- Keine APK gebaut (nur statische Pruefung gemaess Testplan)
- Gradle/CMake-Konfiguration bereit fuer Gradle-Sync

**Naechste Schritte (Phase 3):**
1. SafImporter HoD-Validierung reviewen (wurde in Phase 1 vorgezogen)
2. Launcher-Text fuer HoD finalisieren
3. launcher_background.png ersetzen (aktuell noch BS)
4. Import-Validation: Test mit leerem/ungueltigem Ordner (App muss Fehlermeldung statt Crash zeigen)

---

## 2026-05-25: Phase 3 Complete

### Phase 3: Launcher und Asset-Import auf HoD anpassen

**Aktionen:**

1. **SafImporter.kt geprueft:**
   - REQUIRED_FILES = ["setup.dat"] ✅
   - PATTERN_FILES = ["*_hod.lvl", "*_hod.sss", "*_hod.mst"] ✅
   - PAF_FILES = ["hod.paf", "hod_demo.paf", "hod_demo2.paf"] ✅
   - Import-Target: filesDir/imported_game/hode ✅
   - Case-insensitive Validierung via `.lowercase()` ✅
   - Manifest: `.import_manifest.json` mit validation_status ✅

2. **HodLauncherActivity.kt geprueft:**
   - Titel: "Heart of Darkness" ✅
   - Anleitungstext mit HoD-spezifischen Dateinamen ✅
   - Import-Flow: isImportValid() → Controller Detector → HodActivity ✅
   - Controller-Erkennung mit Gamepad-Navigation (D-Pad, A/B Buttons) ✅
   - Vollbild-Immersive-Mode mit System-Bar-Hiding ✅

3. **ControllerDeviceDetector.kt geprueft:**
   - Erkennt non-virtual devices mit GAMEPAD/JOYSTICK/DPAD sources ✅
   - Clean, kein BS-Branding ✅

4. **launcher_background.png ersetzt:**
   - BS-background (md5: 37b930116b0fc09fefcfe127575ab5c3) → HoD-background (md5: 0083ab0a1a088de6aa6706074a13e58a)
   - Neues 800x480 PNG generiert: dunkler Vignetten-Gradient mit goldenem "Heart of Darkness"-Schriftzug und "Android Port"-Subtitle
   - Platzhalter-Charakter (kein offizielles Artwork) → kann spaeter durch echtes Artwork ersetzt werden

5. **Import-Validation-Flow (theoretisch geprueft):**
   - validateSource() validiert vor Import → Fehlermeldung bei ungueltigem Ordner ✅
   - import() faengt Exceptions → partial imports werden geloescht ✅
   - Keine APK gebaut → echter SAF-Picker-Test erst nach Codex-Endabnahme moeglich ✅

**Review-Ergebnisse:**
- SafImporter HoD-Validierung: korrekt (setup.dat, *_hod.lvl/.sss/.mst, hod*.paf) ✅
- Import-Pfad: correctly target `filesDir/imported_game/hode` ✅
- Launcher-Text: HoD-spezifisch, vollstaendig ✅
- launcher_background.png: ersetzt, Hash-Differenz zum BS-Original bestaetigt ✅
- ControllerDeviceDetector: clean, kein BS-Bezug ✅
- Kein R.drawable-Verweis auf nicht-existente BS-Ressourcen ✅

**Naechste Schritte (Phase 4):**
1. Touch-Overlay-Dateien aus BS uebernehmen, Branding auf HoD
2. Default Overlay: D-Pad links, Run/Jump/Shoot/Shoot+Run rechts, Menu/Esc
3. Key-Mapping auf HoD-SDL-Mappings anpassen
4. Gamepad-Default anpassen
5. Cheat-System integrieren (God Mode, Infinite Ammo, Level/Checkpoint)
6. BS-spezifische Features entfernen (Inventory, Music/SoundFont, Widescreen)

---

## 2026-05-25: Phase 4 Complete

### Phase 4: Touch-Overlay, Controller und Cheats an HoD anpassen

**Aktionen:**

1. **Worktree-Branch `hod-android-phase3` gemerged:**
   - 4 Dateien mit BS-Referenz-Fixes aus letzter Session:
     `TouchButtonModels.kt`, `TouchButtonPresets.kt`, `TouchOverlayController.kt`, `TouchOverlaySettingsDialog.kt`
   - 17 Zeilen geaendert (BS-Labels/Cheats)
   - Worktree nach Merge geloescht

2. **BS-spezifische Features aus TouchOverlayConfig entfernt:**
   - `touchInventoryEnabled`-Feld entfernt (HoD hat kein Inventory)
   - `screenMode`-Feld + `SCREEN_MODE_4_3`/`SCREEN_MODE_16_9_STRETCHED`-Konstanten entfernt (HoD kein Widescreen-Toggle)
   - Config-Schema-Version: 8 → 9

3. **TouchOverlayController.kt bereinigt:**
   - `nativeSetScreenMode()`-Aufruf in `attach()` entfernt
   - `nativeSetTouchInventoryEnabled()`-Aufruf in `onConfigUpdated()` entfernt

4. **TouchOverlaySettingsDialog.kt bereinigt:**
   - Touch-Inventory-Checkbox entfernt
   - Screen-Mode-Sektion (Spinner, 4:3/16:9) komplett entfernt
   - Ungenutzte Imports (Spinner, ArrayAdapter) entfernt
   - Close-Button-Logik vereinfacht (kein screenMode-Sync mehr)

5. **TouchInputDispatcher.kt bereinigt:**
   - `contextualKeyCode()`-Methode entfernt (reine BS-Inventory-Kontextlogik)
   - `heldContextualButtonKeys`-Map entfernt
   - Context-Konstanten (TOUCH_CONTEXT_GAMEPLAY/CONFIRM/MENU) entfernt
   - `HodActivity`-Import entfernt

6. **HodActivity.kt bereinigt:**
   - `nativeSetTouchInventoryEnabled()` extern-Funktion entfernt
   - `nativeGetTouchInputContext()` extern-Funktion entfernt
   - `nativeSetScreenMode()` extern-Funktion entfernt
   - `nativeSetTouchInventoryEnabled()` Aufruf in `onCreate()` entfernt

7. **Default-Overlay auf HoD reduziert (von 10 auf 6 Buttons):**
   - Entfernt: btn_inv (Inventory), btn_status (Status), btn_quick_save, btn_quick_load
   - Behalten: btn_menu (ESCAPE, top-left), dpad (left, bottom-anchored), btn_run (SHIFT), btn_jump (UP), btn_weapon (SPACE, Label="Shoot"), btn_use (ENTER)
   - Layout: D-Pad links unten, Action-Buttons rechts (Run, Jump, Shoot, Use)

8. **Gamepad-Default-Mapping auf HoD reduziert:**
   - Default: A=Jump, X=Run, B=Shoot, Y=Use, START=Menu
   - Entfernt: inventory, quick_load, quick_save, status
   - Actions-Liste: von 9 auf 5 reduziert
   - Buttons-Liste: von 9 auf 5 reduziert (SELECT, L1, R1, L3 entfernt)

9. **Presets und Store aktualisiert:**
   - "Weapon"-Label → "Shoot" in Presets und Store-Migration
   - Presets (Inventory, Quick-Save/Load, Status) bleiben als Option fuer manuelle Button-Erstellung

**Files geaendert (8):**
- `touch/TouchButtonModels.kt` — BS-Felder entfernt, Default-Overlay+Gamepad reduziert, v9
- `touch/TouchButtonPresets.kt` — "Weapon"→"Shoot"
- `touch/TouchOverlayController.kt` — screenMode/touchInventory-Sync entfernt
- `touch/TouchOverlaySettingsDialog.kt` — Inventory-Checkbox, Screen-Mode-Sektion entfernt
- `touch/TouchInputDispatcher.kt` — contextualKeyCode-Logik entfernt
- `touch/TouchButtonStore.kt` — Migrations-Label "Weapon"→"Shoot"
- `HodActivity.kt` — 3 BS-JNI-Deklarationen entfernt
- `ANDROID_PORT_PLAN.md` — Phase 4 Status

**Review-Ergebnisse:**
- grep "bermuda\|Bermuda\|BSNative" in java/: 0 Treffer ✅
- grep "touchInventory\|screenMode\|SCREEN_MODE\|nativeSetScreenMode\|nativeSetTouchInventory\|nativeGetTouchInput" in java/: 0 Treffer (nur SDLActivity.java FullscreenMode, das ist SDL-Library) ✅
- Cheats: 3 UI-Cheats (0-2) korrekt auf 6 native Cheats gemappt ✅
- Default-Overlay 6 Buttons statt 10 ✅
- Gamepad 5 Aktionen statt 9 ✅
- Keine orphaned Kotlin-Referenzen auf entfernte JNI-Funktionen ✅

**Naechste Schritte (Phase 5):**
1. Manifest landscape/fullscreen, Touchscreen/Gamepad optional ✅ (bereits in Phase 1)
2. minSdk=24, targetSdk=35, compileSdk=35, ndkVersion=27.2.12479018 ✅ (bereits in Phase 1)
3. Release-Signing nicht von BS uebernehmen ✅ (kein Keystore kopiert)
4. App darf ohne Import nicht crashen
5. JNI-Stubs in android_main.cpp endgueltig aufraeumen (nativeSetScreenMode, nativeSetTouchInventoryEnabled, nativeGetTouchInputContext)
6. Finale Qualitaetspruefung aller Dateien
