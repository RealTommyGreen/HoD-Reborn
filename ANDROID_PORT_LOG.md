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

## 2026-05-25: Ingame-Menue-Toggle, Menue-Input und Restore-Fixes

### Menuebutton und Eingabelogik
- Touch-Menuebutton von normalem `ESCAPE` auf native Aktion `native_menu` umgestellt.
- `nativeToggleGameMenu()` als JNI-Bruecke ergaenzt.
- Alte Touch-Configs ohne `schema_version` werden jetzt trotzdem normalisiert:
  - `btn_menu` -> `native_menu`
  - Run -> `CTRL`
  - Jump -> `ENTER`
  - Shoot -> `SHIFT`
  - Shoot+Run -> `SPACE`
- `TOUCH_OVERLAY_CONFIG_VERSION` auf 11 angehoben.
- Im nativen Menue bestaetigt auf Android nur noch Jump/ENTER.
- Shoot/SHIFT funktioniert im Optionsmenue als Zurueck.
- Run/CTRL wird im Android-Menue nicht mehr als Menueaktion ausgewertet, damit D-Pad-Double-Tap-Run kein versehentliches Bestaetigen ausloest.

### Ingame-Menue ohne Level-Abbruch
- Menuebutton im Spiel oeffnet das native HoD-Menue nun innerhalb des laufenden `Game::levelMainLoop()`.
- Das Spiel setzt dabei nicht mehr `_endLevel` und springt nicht mehr in den aeusseren Android-Mainloop.
- Zweiter Druck auf den Menuebutton im Menue schliesst das Menue und kehrt in denselben Level-Kontext zurueck.
- Start-Hauptmenue ignoriert den Overlay-Menuebutton weiterhin, solange kein Spiel laeuft.

### Palette- und Sound-Restore nach Ingame-Menue
- Neue Android-Restore-Routine nach Ingame-Menue:
  - leert Input-Masks vor und nach dem Menue,
  - stoppt Menue-Soundobjekte via `resetSound()`,
  - leert damit Audiopuffer/Mixing-Queue,
  - stellt die aktuelle Spielpalette ueber `Video::updateGamePalette(_displayPaletteBuffer)` wieder her.
- Fix fuer verzerrte Farben nach Rueckkehr aus dem Menue.
- Fix fuer haengenden/repeatenden Menue-Sound nach Rueckkehr ins Spiel.

### Sound-Restore Nachfix
- Ursache: `Resource::loadDatMenuBuffers()` laedt fuer das native Hauptmenue eigene SSS-Daten aus der DAT und ersetzt damit die Level-SSS-Daten im gemeinsamen `Resource`-Objekt.
- Nach Rueckkehr aus dem Ingame-Menue werden jetzt unter Mixer-Lock:
  - aktive Menue-Soundobjekte und Audiopuffer geloescht,
  - die aktuelle Level-SSS-Datei erneut geladen (`_sssFile`, bzw. PSX `_lvlFile + _lvlSssOffset`),
  - Soundobjekte fuer die neu geladenen Level-SSS-Daten zurueckgesetzt,
  - aktuelle Screen-Hintergrundsounds via `setupBackgroundBitmap()` wieder initialisiert.
- Dadurch arbeitet der Spielsound nach dem Menue wieder mit Level-Sounddaten statt mit Menue-Samples.
- Geraetetest bestaetigt: Rueckkehr aus dem Ingame-Menue stellt Bild und Spielsound korrekt wieder her.

### Build- und Geraetepruefung
- `.\gradlew.bat assembleDebug` -> BUILD SUCCESSFUL.
- APK installiert mit `adb install -r android\app\build\outputs\apk\debug\app-debug.apk`.
- App gestartet; Logcat zeigt `Entering menu/level main loop`, kein Startcrash im geprueften Ausschnitt.
- Nach Sound-Restore-Nachfix erneut gebaut/installiert und Start-Logcat geprueft: kein Startcrash im geprueften Ausschnitt.
- Funktionaler Test auf dem Geraet erfolgreich: Menue oeffnen/schliessen im Spiel ohne Farbfehler und ohne haengenden Menue-Sound.

---

## 2026-05-25: Asset-Startfix, Videofilter-Menue und Sensor-Deaktivierung

### Asset-Startfix auf Geraet
- Import-/Validierungslogik fuer `storage/emulated/games/heart of darkness/` robuster gemacht:
  - Asset-Pruefung rekursiv und case-insensitive statt nur auf Root-Ebene.
  - `hod_oem.paf` als gueltige PAF-Variante beruecksichtigt.
  - PAF bei der Launcher-Validierung optional behandelt, damit gueltige Datensaetze ohne harte Fehlmeldung starten.
- Android-Dateisystemzugriff korrigiert:
  - Datapath-Lookups rekursiv und case-insensitive.
  - Globalen Datapath-State repariert, damit Native-Code wirklich den importierten Asset-Pfad nutzt.
- Launcher-Hinweise an die erwartete HoD-Asset-Struktur angepasst.

### Videofilter im Touch-Overlay-Settings-Menue
- Touch-Overlay-Konfiguration auf Schema-Version 10 angehoben.
- Neues persistiertes Feld `video_filter` mit Default `nearest`.
- Zahnrad-Settings enthalten jetzt eine Video-Sektion:
  - `Original Pixels` = originaler Look / nearest, Default.
  - `Soft Linear` = linearer Filter.
  - `xBR Smooth` = xBR-Scaler.
- Native JNI-Bruecke `nativeSetVideoFilter()` hinzugefuegt.
- SDL2-Renderer kann den Scaler zur Laufzeit wechseln und Texturen/Logical Size neu anlegen.
- App setzt den gespeicherten Filter beim Start und bei jeder Settings-Aenderung live.

### Gyroskop-/Sensorsteuerung deaktiviert
- Accelerometer-Registrierung in SDL-Resume/Startpfad abgeschaltet.
- `SDLSurface.onSensorChanged()` liefert keine Sensorwerte mehr an `onNativeAccel()`.
- Sensorbasierte Landscape-Ausrichtung durch feste Landscape-Ausrichtung ersetzt.

### Build- und Geraetepruefung
- `.\gradlew.bat assembleDebug` -> BUILD SUCCESSFUL.
- APK installiert mit `adb install -r android\app\build\outputs\apk\debug\app-debug.apk`.
- App auf dem Geraet gestartet.
- Logcat-Pruefung:
  - `nativeSetVideoFilter filter=nearest scaler=nearest multiplier=1`
  - `Entering level main loop`
  - Kein Startcrash im geprueften Log-Ausschnitt.

### Geaenderte Hauptdateien
- `SafImporter.kt`
- `HodLauncherActivity.kt`
- `fs_android.cpp`
- `system_sdl2.cpp`
- `android_main.cpp`
- `HodActivity.kt`
- `TouchButtonModels.kt`
- `TouchOverlayController.kt`
- `TouchOverlaySettingsDialog.kt`
- `SDLSurface.java`
- `SDLActivity.java`

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

---

## 2026-05-25: Phase 5 Complete

### Phase 5: Android-Qualitaet und Buildbarkeit

**Aktionen:**

1. **JNI-Stubs aus android_main.cpp entfernt:**
   - `nativeSetScreenMode` entfernt (Zeilen 52-54)
   - `nativeSetTouchInventoryEnabled` entfernt (Zeilen 75-77)
   - `nativeGetTouchInputContext` entfernt (Zeilen 79-81)
   - Verbleibende JNI-Funktionen: nur `nativeSetCheat` und `nativeSetControllerConfig`
   - Datei von 175 auf 164 Zeilen reduziert

2. **TouchButtonPresets.kt gefixt:**
   - `"Run/Holster"` → `"Run"` korrigiert (HoD hat kein Holster)

3. **VIBRATE-Permission geprueft:**
   - Wird von SDL2's `SDLControllerManager.java` fuer Gamepad-Haptik genutzt
   - Permission bleibt im Manifest (korrekt/notwendig)

4. **Finale grep-Pruefung:**
   - `bermuda|Bermuda|BermudaActivity|BermudaLauncher|BSNative|bsnative` in `*.{kt,java,xml,kts,cpp,h,properties}`:
     **0 Treffer** ✅
   - Alle Dateien sind BS-Referenz-frei

5. **No-Crash-Garantie bei fehlendem Import (Code-Review):**
   - `HodLauncherActivity.onCreate()` → `isImportValid()` → `false` → `createImportUI()`
   - `SafImporter.validateSource()` → validiert vor Import, zeigt Fehler bei ungueltigem Ordner
   - `SafImporter.import()` → faengt Exceptions, loescht Partial-Imports
   - Kein Crash-Pfad identifizierbar ✅

**Files geaendert (4):**
- `android/app/src/main/jni/src/android_main.cpp` — 3 JNI-Stubs entfernt
- `touch/TouchButtonPresets.kt` — "Run/Holster"→"Run"
- `ANDROID_PORT_PLAN.md` — Phase 5 Status
- `ANDROID_PORT_LOG.md` — Phase 5 Eintrag

**Review-Ergebnisse:**
- grep "bermuda\|Bermuda\|BSNative" in android/: 0 Treffer ✅
- grep "nativeSetScreenMode\|nativeSetTouchInventory\|nativeGetTouchInput" in android/: 0 Treffer ✅
- android_main.cpp: 2 JNI-Funktionen (nativeSetCheat, nativeSetControllerConfig) + SDL_main ✅
- Gesamtanzahl Dateien unter android/: 46 (unveraendert: SDL Java, Resources, Gradle)
- Keine BS-Assets/Keystore/Spieldaten im Projekt ✅

**Alle 5 Phasen abgeschlossen. Projekt bereit fuer Codex-Endabnahme.**

---

## 2026-05-25: Codex-Endbefund 1 — Alle 6 Blocker behoben

### Codex-Review ergab 6 Blocker, alle gefixt:

**1. Doppeltes SDL_main (BUILD BLOCKER)** ✅
- Ursache: CMakeLists.txt baute sowohl `main.cpp` (desktop `main()`) als auch `android_main.cpp` (`SDL_main()`); SDL mappt beides auf `SDL_main`
- Fix: `main.cpp` aus `ENGINE_SOURCES` in CMakeLists.txt entfernt
- android_main.cpp enthaelt eigene SDL_main mit voller HoD-Initialisierung

**2. Klammerfehler in TouchInputDispatcher.kt (BUILD BLOCKER)** ✅
- Ursache: Extra `}` nach `performButtonAction()` schloss die Klasse vorzeitig
- Fix: Klammerstruktur repariert; Klasse korrekt am Dateiende geschlossen
- Alle Methoden (releaseAll, performDpadDirection, companion) jetzt innerhalb der Klasse

**3. Touch-Overlay Keymapping falsch** ✅
- Vorher: Run=SHIFT, Jump=UP, Shoot=SPACE (falsch), kein Shoot+Run
- Nach system_sdl2.cpp:778-802: Run=CTRL, Jump=ENTER, Shoot=SHIFT, Shoot+Run=SPACE
- `btn_use` → `btn_shoot` (SHIFT), `btn_weapon` → `btn_shoot_run` (SPACE) hinzugefuegt

**4. Controller-Mapping-UI wirkungslos** ✅
- nativeSetControllerConfig() ist Stub; system_sdl2.cpp hat hart codierte Joystick-Mappings
- Controller-Mapping-Button aus Settings-Dialog entfernt; stattdessen Info-Text
- Gamepad-Defaults an natives Mapping angepasst: A=Run, B=Jump, X=Shoot, Y=Shoot+Run

**5. isImportValid() prueft zu wenig** ✅
- Vorher: nur Manifest + setup.dat
- Jetzt: zusaetzlich *_hod.lvl, *_hod.sss, *_hod.mst (je mind. 1) + PAF (mind. 1), case-insensitive

**6. SDK-Pfad fehlt** ✅
- `android/local.properties` erstellt mit `sdk.dir=C\:\\Users\\Tommy Green\\AppData\\Local\\Android\\Sdk`

### Build-Verifikation
- `.\gradlew.bat :app:compileDebugKotlin -x externalNativeBuildDebug` → **BUILD SUCCESSFUL** (9s)
- `.\gradlew.bat :app:compileDebugKotlin :app:externalNativeBuildDebug` → **BUILD SUCCESSFUL** (2m 40s, 3 ABIs)

### Files geaendert (8):
- `CMakeLists.txt` — main.cpp entfernt
- `TouchInputDispatcher.kt` — Klammerstruktur repariert
- `TouchButtonModels.kt` — Default-Overlay + Gamepad auf HoD-Mappings korrigiert
- `TouchOverlaySettingsDialog.kt` — Controller-Mapping-UI entfernt, TEXT_MUTED hinzugefuegt
- `SafImporter.kt` — isImportValid() erweitert
- `local.properties` — SDK-Pfad (neu erstellt)
- `ANDROID_PORT_PLAN.md` — Codex-Befund dokumentiert
- `ANDROID_PORT_LOG.md` — Dieser Eintrag

### FreeClaude Eigenpruefung
- grep "bermuda\|Bermuda\|BSNative" in android/: 0 Treffer ✅
- Kotlin compile: SUCCESSFUL (nur API-Deprecation-Warnings) ✅
- Native compile: SUCCESSFUL (arm64-v8a, armeabi-v7a, x86_64) ✅
- Keine APK gebaut (Codex-Endabnahme steht noch aus)

**Projekt erneut bereit fuer Codex-Endabnahme. Keine APK gebaut.**

---

## 2026-05-25: Codex-Endbefund 2 — APK-Freigabe erteilt

### Codex-Abnahme
- `compileDebugKotlin -x externalNativeBuildDebug` → BUILD SUCCESSFUL ✅
- `compileDebugKotlin :externalNativeBuildDebug` (3 ABIs) → BUILD SUCCESSFUL ✅
- `rg bermuda|Bermuda|BSNative` → 0 Treffer ✅
- Keine APK/AAB im Source-Tree ✅

### APK-Build
- **Befehl:** `.\gradlew.bat :app:assembleDebug`
- **Ergebnis:** BUILD SUCCESSFUL in 10s
- **APK-Pfad:** `D:\Coding\HoD Android\hode\android\app\build\outputs\apk\debug\app-debug.apk`
- **Dateigroesse:** 14 MB
- **ABIs:** arm64-v8a, armeabi-v7a, x86_64
- **Version:** 0.1.0 (versionCode 1)
- **Signing:** Android Debug Keystore (default)
- **Warnungen:** Nur API-Deprecation-Hinweise (FLAG_FULLSCREEN, systemUiVisibility) — keine Fehler
- **GDrive-Upload:** Erfolgreich durchgefuehrt

### Projektstatus
- Alle 5 Phasen + beide Codex-Endbefunde abgeschlossen
- Debug-APK liegt lokal und auf Google Drive
- Keine Release-APK, kein Signing — nur Debug-Build gemaess Freigabe

---

## 2026-06-04: SVG Phase 1 Complete — Build-Config + Assets + JNI

### Aktionen

**1a. AndroidSVG Dependency:**
- `android/app/build.gradle.kts`: `implementation("com.caverock:androidsvg-aar:1.4")` nach kotlinx-serialization-json hinzugefuegt (Zeile 68)

**1b. res/raw/ Assets:**
- `res/raw/` Verzeichnis neu angelegt
- 5 SVGs aus `D:\Coding\HoD_Icons\` kopiert, lowercase umbenannt:
  - `hod_run.svg`, `hod_jump.svg`, `hod_shoot.svg`, `hod_cancel.svg`, `hod_check.svg`
- `res/raw/iconset.json`: Original-iconset.json mit lowercase SVG-Referenzen (`HoD_Run.svg` → `hod_run.svg`, etc.)
- `res/raw/iconmappings.json`: 1:1 Mapping `{"HoD_Run": "hod_run", "HoD_Jump": "hod_jump", "HoD_Shoot": "hod_shoot", "HoD_Cancel": "hod_cancel", "HoD_Check": "hod_check"}`

**1c. JNI-Bruecke nativeIsMenuOpen():**
- `android_main.cpp`: Forward-Declaration `extern "C" bool Android_isMenuOpenedFromGame()` vor JNI-Block (Zeile 31), neue JNI-Funktion `Java_com_hod_reborn_HodActivity_nativeIsMenuOpen()` (Zeile 105), returned `Android_isMenuOpenedFromGame() ? JNI_TRUE : JNI_FALSE`
- `HodActivity.kt`: Neue `external fun nativeIsMenuOpen(): Boolean` in companion object (Zeile 34)

### Review-Ergebnisse
- grep `nativeIsMenuOpen`: 2 Treffer (cpp JNI-Implementierung + kt Deklaration) ✅
- grep `androidsvg`: 1 Treffer in build.gradle.kts ✅
- res/raw/ enthaelt 5 SVGs + iconset.json + iconmappings.json ✅
- `compileDebugKotlin`: BUILD SUCCESSFUL ✅
- `externalNativeBuildDebug` (3 ABIs): BUILD SUCCESSFUL ✅

### Files geaendert/erstellt (9)
- `android/app/build.gradle.kts` — Dependency
- `android/app/src/main/res/raw/` (NEU) — 7 Dateien (5 SVGs, iconset.json, iconmappings.json)
- `android/app/src/main/jni/src/android_main.cpp` — Forward-Declaration + JNI
- `android/app/src/main/java/com/hod/reborn/HodActivity.kt` — external fun

### Naechste Schritte (Phase 2)
- `touch/SvgIconManager.kt` — Port von JA2 Reborn (1:1 mit Package-Rename und R.raw-Anpassung)
- `HodActivity.onCreate()` ruft `SvgIconManager.init(this)` auf

---

## 2026-06-04: SVG Phase 2 Complete — SvgIconManager Port

### Aktionen

**SvgIconManager.kt (NEU):**
- 1:1 Port von JA2 Reborn `SvgIconManager.kt` (181 Zeilen)
- Package: `com.ja2.reborn.touch` → `com.hod.reborn.touch`
- R.raw Referenzen: `R.raw.iconset` und `R.raw.iconmappings` (in Phase 1 angelegt)
- Enthaelt: `init()`, `hasIcon()`, `getIconFill()`, `renderIcon()`, `loadSvgBitmap()`, `targetRect()`
- 512px Bitmap-Cache mit `ICON_PADDING_FRACTION = 0.08f`
- `IconSetEntry` data class mit iconFill, iconOffsetX/Y, iconScaleX/Y

**HodActivity.kt:**
- Import `com.hod.reborn.touch.SvgIconManager` hinzugefuegt
- `SvgIconManager.init(this)` in `onCreate()` vor Controller-Setup aufgerufen

### Review-Ergebnisse
- grep `SvgIconManager`: 4 Treffer (HodActivity Import + Aufruf, SvgIconManager.kt Deklaration + TAG) ✅
- grep `com.ja2` in SvgIconManager.kt: 0 Treffer ✅
- `compileDebugKotlin`: BUILD SUCCESSFUL ✅

### Files geaendert/erstellt (2)
- `touch/SvgIconManager.kt` (NEU) — 1:1 JA2-Port
- `HodActivity.kt` — Import + `SvgIconManager.init(this)`

### Naechste Schritte (Phase 3)
- Button-Geometrie-Fix + SVG-Rendering in TouchOverlayButtonView.kt
- iconFill-Feld zu TouchButtonConfig
- btn_shoot_run entfernen, Icon-Namen aktualisieren, Button-Groessen skalieren
- Schema-Version 11→12, Config-Migration

---

## 2026-06-04: SVG Phase 3 Complete — Button-Geometrie + SVG-Rendering + Migration

### Aktionen

**3a-b. TouchOverlayButtonView.kt — Geometrie-Fix + SVG-Rendering:**
- `computeOuterShapeBounds()`: `buttonHeight = minOf(h, w / 1.8f)`, shape-spezifische Bounds-Berechnung
- `computeIconShapeBounds()`: Circle 0.85× Faktor, sonst Outer-Bounds
- `drawShape()`: Zeichnet Shape-Hintergrund/Rahmen in Outer-Bounds
- `iconClipPath()`: Clip-Pfad basierend auf Shape-Typ
- `onDraw()`: DPAD-Check (`icon == "dpad_map"`) → kein Shape-Hintergrund, nutzt `drawShape()` + `drawIcon()`
- `drawIcon()`: SVG-first via `SvgIconManager.renderIcon()` mit Clip-Pfad, Fallback auf programmatische Icons mit `shapeDim`-basiertem Scale
- `drawCenteredText()`: Nutzt `computeOuterShapeBounds()` als Referenz (wie JA2)

**3c. `iconFill`-Feld:**
- `TouchButtonConfig`: Neues Feld `@SerialName("icon_fill") val iconFill: Float = -1f`
- `-1f` Sentinel = Default aus iconset.json verwenden

**3d-e-f. Default-Overlay aktualisiert:**
- btn_shoot_run ENTFERNT (redundant)
- Icon-Namen: `run` → `HoD_Run`, `jump` → `HoD_Jump`, `weapon` → `HoD_Shoot`
- Button-Groessen: Run/Jump/Shoot `0.115` → `0.180` (~1.56×)
- Menu-Button und DPAD unveraendert

**3g-h. Schema-Version + Migration:**
- `TOUCH_OVERLAY_CONFIG_VERSION`: 11 → **12**
- `migrateConfig()`: `btn_shoot_run` wird aus alten Config-Lists gefiltert (`.filter { it.id != "btn_shoot_run" }`)
- Migration mappt alte Icons auf HoD-SVG-Namen

**TouchButtonPresets.kt:**
- Preset-Icons aktualisiert: `jump` → `HoD_Jump`, `weapon` → `HoD_Shoot`, `shoot_run` → `HoD_Shoot`, `run` → `HoD_Run`

### Review-Ergebnisse
- grep `btn_shoot_run`: Nur 1 Treffer (filter in Migration) ✅
- grep `HoD_Run|HoD_Jump|HoD_Shoot`: In defaultButtons() ✅
- grep `iconFill`: Feld in TouchButtonConfig ✅
- grep `computeOuterShapeBounds|computeIconShapeBounds|drawShape|SvgIconManager`: Alle in TouchOverlayButtonView ✅
- `compileDebugKotlin`: BUILD SUCCESSFUL ✅

### Files geaendert (4)
- `touch/TouchOverlayButtonView.kt` — Geometrie-Fix + SVG-Rendering
- `touch/TouchButtonModels.kt` — iconFill, Defaults, Schema 12
- `touch/TouchButtonPresets.kt` — Icon-Namen
- `touch/TouchButtonStore.kt` — Migration btn_shoot_run-Filter

### Naechste Schritte (Phase 4)
- `TouchOverlayController.kt` — Menue-Polling + dynamische Icons
- `applyMenuState()`: btn_jump (HoD_Jump↔HoD_Check), btn_shoot (HoD_Shoot↔HoD_Cancel), btn_run visibility

---

## 2026-06-04: SVG Phase 4 Complete — Dynamische Menue-Icons

### Aktionen

**TouchOverlayController.kt — Menue-Polling + dynamische Icons:**

- `lastMenuOpenState: Boolean?` Feld hinzugefuegt (initial null)
- `menuPollRunnable`: 100ms Polling-Loop via `root.postDelayed()`, ruft `HodActivity.nativeIsMenuOpen()` auf
- `applyMenuState(isMenuOpen: Boolean)`:
  - `btn_jump`: Icon wechselt `HoD_Jump` ↔ `HoD_Check`
  - `btn_shoot`: Icon wechselt `HoD_Shoot` ↔ `HoD_Cancel`
  - `btn_run`: `visibility = GONE` wenn Menue offen, `VISIBLE` sonst
  - btn_menu + dpad: unveraendert
- Polling startet in `attach()` via `root.postDelayed(menuPollRunnable, 100L)`
- Polling stoppt in `detach()` via `root.removeCallbacks(menuPollRunnable)`

### Review-Ergebnisse
- grep `menuPollRunnable|applyMenuState|lastMenuOpenState|nativeIsMenuOpen`: 6 Treffer ✅
- `compileDebugKotlin`: BUILD SUCCESSFUL ✅

### Files geaendert (1)
- `touch/TouchOverlayController.kt` — Menue-Polling + `applyMenuState()`

### Naechste Schritte (Phase 5)
- Voll-Build `gradlew assembleDebug`
- Logcat pruefen (SvgIconManager laedt 5 Iconsets + 5 Mappings)
- Visuelle Pruefung auf Geraet: SVG-Renderings, 5-Button-Default, Menue-Icon-Wechsel, DPAD kein Shape
- Alte Config-Migration verifizieren

---

## 2026-06-04: SVG Phase 5 Complete — Build + Verifikation

### Build
- `gradlew assembleDebug` → BUILD SUCCESSFUL in 11s
- APK: `app-debug.apk` (16 MB), 3 ABIs (arm64-v8a, armeabi-v7a, x86_64)

### Geraetetest (initial)
- App startet ohne Crash ✅
- `SvgIconManager`: 5 Iconsets geladen, 5 Mappings geladen ✅
- `TouchOverlayController`: 5 Button Views erstellt (btn_shoot_run ist raus) ✅
- **Problem 1:** SVGs nicht gerendert — Button-Texte zeigen `HoD_Check`/`HoD_Cancel` statt Icons
- **Problem 2:** DPAD winzig nach Geometrie-Fix
- **Problem 3:** Menue-Icons nur nach Menue-Wechsel korrekt, nicht bei App-Start

### Hotfixes (3 Bugs)

**Fix 1: SVG-Rendering (iconset.json name-mismatch)**
- Ursache: `iconset.json` `name`-Felder waren `"HoD_Run"` etc., aber `iconmappings.json`-Values sind `"hod_run"` etc. Die `SvgIconManager`-Pre-load-Schleife sucht `entries[svgName]` wo `svgName` aus den Mappings kommt → `entries["hod_run"]` fand nichts weil Entry unter `"HoD_Run"` gespeichert war
- Fix: `iconset.json` `name`-Felder auf lowercase-Namen geaendert (`"HoD_Run"` → `"hod_run"`), passend zu Mapping-Values
- Pre-load-Log jetzt: `Pre-load SVG hod_run.svg: OK` etc. (5/5 OK)

**Fix 2: DPAD-Skalierung (SQUARE-Geometrie)**
- Ursache: `computeOuterShapeBounds()` fuer SQUARE nutzte `buttonHeight = minOf(h, w / 1.8f)`, was bei quadratischem View `h / 1.8` ergibt — viel kleiner als die View
- Fix: SQUARE-Buttons nutzen jetzt `side = minOf(w, h)` (volle View-Dimension) statt `buttonHeight`

**Fix 3: Menue-Icons bei App-Start**
- Ursache: `g_menuOpenedFromGame` war nur im Level-Loop gesetzt, nie im initialen Hauptmenue. Polling sah `false` und zeigte In-Game-Icons
- Fix: `g_menuOpenedFromGame = true` vor dem `do`-Block in `SDL_main` gesetzt, damit das Hauptmenue sofort als Menue-Zustand erkannt wird

### Geraetetest (nach Hotfixes)
- SVGs werden korrekt gerendert (HoD_Run, HoD_Jump, HoD_Shoot) ✅
- Im Hauptmenue: Check-Icon (Jump), Cancel-Icon (Shoot), Run ausgeblendet ✅ (Fix 3)
- DPAD wieder normal gross ✅ (Fix 2)
- Menue-Wechsel funktioniert ✅
- 5-Button-Default (kein btn_shoot_run) ✅
- Visuell top ✅

### Overlay Icon Manual aktualisiert
- Bug #14: SVG-Name-Mismatch iconset.json vs iconmappings.json
- Bug #15: Menue-Icons nicht bei App-Start (native Flag)
- Bug #16: DPAD zu klein nach Geometrie-Fix (SQUARE-Button-Geometrie)

### Files geaendert (3)
- `res/raw/iconset.json` — name-Felder lowercase
- `touch/TouchOverlayButtonView.kt` — SQUARE-Geometrie-Fix
- `android_main.cpp` — `g_menuOpenedFromGame = true` vor Hauptmenue

### Alle 5 SVG-Phasen abgeschlossen. Projekt bereit fuer naechste Aufgaben.

---

## 2026-06-04: Button-Groessen-Anpassung + Cancel-Button im Hauptmenue

### Default-Button-Groessen
- Menu: 0.103 → **0.18**
- Run/Jump/Shoot: 0.180 → **0.26**
- DPAD: 0.430 → **0.500**
- Size-SeekBar Max: 0.450 → **0.600**

### Cancel-Button im Hauptmenue
- Problem: Im Hauptmenue fuehrte Cancel (SHIFT) nicht zurueck ins Spiel, nur der Menue-Button oder "Play" funktionierten
- Fix: In `menu.cpp handleTitleScreen()`: Wenn `Android_isMenuOpenedFromGame()` und `SYS_INP_SHOOT` released → zurueck ins Spiel (wie Menue-Button)
- In den Optionen bleibt SHIFT weiterhin der Zurueck-Befehl innerhalb des Optionsmenues
- Nur aktiv wenn aus laufendem Spiel heraus ins Hauptmenue gewechselt wurde (nicht beim initialen App-Start)

### Files geaendert (4)
- `touch/TouchButtonModels.kt` — Default-Groessen
- `touch/TouchOverlayEditDialog.kt` — Size-SeekBar bis 0.600
- `menu.cpp` — SHIFT=Zurueck im Hauptmenue

### SVG-Phasen alle abgeschlossen. Herz der Finsternis Android Port v0.1.0 ist fertig.

---

## 2026-06-14: Package-Rename, Branch-Cleanup, Signing, Release-APK

### Package-Rename: com.heartofdarkness.reborn → com.hod.reborn
- Package-Pfad vereinfacht: `android/app/src/main/java/com/heartofdarkness/reborn/` → `com/hod/reborn/`
- Alle 18 Kotlin-Dateien verschoben (Git erkannte Renames korrekt)
- `HodActivity.kt`, `HodLauncherActivity.kt`, `SafImporter.kt`, `AssetExtractor.kt`, `ControllerDeviceDetector.kt`
- `touch/` Paket: 13 Dateien (inkl. `SvgIconManager.kt`)
- `HodLauncherActivity.kt` interner Package-Import auf `com.hod.reborn.R` aktualisiert

### Branch-Struktur bereinigt
- `main`-Branch war mit Android-Port vermischt → keine Trennung zur Original-Engine
- Neuer `main`: zurueckgesetzt auf `abaa416` (letzter Pre-Android-Commit, Original-Engine)
- Alter `main` umbenannt in `reborn` (b405cdb, enthaelt alle Android-Arbeiten Phasen 1-5 + SVG 1-5)
- `.gitignore` auf `main` (nur `android/` ignoriert) und auf `reborn` (Build-Artefakte + Keystore ignoriert)
- Keine Worktrees — beide Branches teilen Working Directory, sauberes `git checkout`

### Release-Signing
- Keystore `android/app/hod-release.jks` (vorhanden seit 2026-05-26)
- `android/app/build.gradle.kts`: signingConfigs mit Keystore-Pfad, Alias `hod`, Passwoerter via Umgebungsvariablen
- Signing nur im Release-Build aktiv (Debug nutzt weiterhin Android Debug Keystore)

### Release-APK Build
- `gradlew assembleRelease` → BUILD SUCCESSFUL in 4m 39s
- APK: `app-release.apk` (13 MB), 3 ABIs (arm64-v8a, armeabi-v7a, x86_64)
- GDrive-Upload: `HoD-Reborn-2026-06-14.apk`
- Nur API-Deprecation-Warnings (FLAG_FULLSCREEN, systemUiVisibility) + C++ Unused-Variable-Warnings — keine Fehler

### Files geaendert (ca. 20)
- 18 Kotlin-Dateien: Package-Rename (verschoben)
- `.gitignore` — Build-Artefakte + Keystore
- `ANDROID_PORT_LOG.md` — Dieser Eintrag

---
