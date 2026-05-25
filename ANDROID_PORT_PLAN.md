# HoD Android Port Plan

## Status: Phase 4 - Complete

## Phases

### Phase 1: Projektgeruest kopieren und umbenennen ✅
- [x] Gradle-Struktur aus `D:\Coding\BS Android\android` nach `D:\Coding\HoD Android\hode\android` kopieren
- [x] Rename auf `HeartOfDarknessAndroid`: rootProject.name, App-Label, Namespace/ApplicationId
- [x] Kotlin-Package von `com.bermuda.reborn` auf `com.heartofdarkness.reborn` migrieren
- [x] Activities benennen: `HodLauncherActivity`, `HodActivity`
- [x] Keine Assets/Spieldaten kopieren

**Ergebnisse:** 43 Dateien unter `android/` (Gradle Wrapper, Kotlin, Java, SDL, C++, Resources).
Keine BS-Assets/Keystore kopiert. SafImporter bereits mit HoD-Validierung (setup.dat, *_hod.*, PAF).

**Offene Risiken:**
- `launcher_background.png` noch von BS → Phase 3

### Phase 2: Native HoD-Build integrieren ✅
- [x] CMake shared library `hode` aus HoD-Quellen + SDL2 static via FetchContent
- [x] Native Sources: 29 .cpp-Dateien (alle Engine-Dateien ausser Blacklist)
- [x] Blacklist: fs_posix.cpp, system_psp.cpp, system_wii.cpp, benchmark.cpp
- [x] Compile definitions: `ANDROID_PACKAGE_NAME="com.heartofdarkness.reborn"`
- [x] Link Libraries: `SDL2-static`, `SDL2main`, `log`, `android` + `-lc++_shared` (NDK 27)
- [x] android_main.cpp: JNI-Funktionen auf `Java_com_heartofdarkness_reborn_HodActivity_*` umbenannt
- [x] android_main.cpp: Log-Tag von "BSNative" auf "HodNative" geaendert
- [x] android_main.cpp: BS-spezifische Features entfernt (musicPath, soundfontPath, screenMode, touchInventory, gameStatePtr)
- [x] android_main.cpp: Game-Instanz mit HoD-Konstruktor (dataPath, savePath, cheats)
- [x] android_main.cpp: Main-Loop auf HoD-Struktur (level-basiert) umgestellt
- [x] android_main.cpp: Includes um `util.h` und `video.h` ergaenzt
- [x] fs_android.cpp: `funopen`-Kompatibilitaet geprueft (BS-Port kompiliert mit NDK 27 → unveraendert)

**Ergebnisse:** CMakeLists.txt baut 29 HoD-Sourcen + android_main.cpp als Shared Library `hode`.
JNI-Bridge komplett auf HoD-Package umgestellt. BS-spezifische JNI-Funktionen (ScreenMode, TouchInventory, TouchInputContext) als Stubs erhalten fuer Kotlin-Kompatibilitaet (Phase 4 bereinigt).

**Offene Risiken:**
- `funopen` koennte auf manchen NDK-27-Konfigurationen fehlschlagen → dann `fopencookie`-Migration noetig
- `android_main.cpp` Audio-Callback-Lambda (`[](void *userdata, int16_t *buf, int len) {...}`) muss von NDK-27-Clang akzeptiert werden
- Keine APK gebaut (nur statische Pruefung) → Compile-Fehler erst bei Gradle-Sync sichtbar

### Phase 3: Launcher und Asset-Import auf HoD anpassen ✅
- [x] SafImporter validiert HoD-Dateien (setup.dat, *_hod.lvl, *_hod.sss, *_hod.mst, hod*.paf) ✅ (vorgezogen)
- [x] Import kopiert nach filesDir/imported_game/hode ✅ (vorgezogen)
- [x] Launcher-Text fuer HoD anpassen ✅ (vorgezogen)
- [x] Review: launcher_background ersetzen
- [x] Bei gueltigem Import → HodActivity starten ✅ (vorgezogen)

**Ergebnisse:** SafImporter, Import-Pfad, Launcher-Text und HodActivity-Startup waren bereits in Phase 1 korrekt vorgezogen.
`launcher_background.png` wurde durch ein HoD-eigenes 800x480 PNG ersetzt (dunkler Gradient mit goldenem "Heart of Darkness"-Schriftzug).
`ControllerDeviceDetector.kt` ist clean, kein BS-Branding.

**Offene Risiken:**
- `launcher_background.png` ist ein generisches Platzhalter-Background (kein offizielles Artwork) → kann spaeter durch echtes Artwork ersetzt werden

### Phase 4: Touch-Overlay, Controller und Cheats an HoD anpassen ✅
- [x] Overlay-Dateien aus BS uebernehmen, Branding auf HoD (13 Dateien im touch/ Package, alle HoD-Package)
- [x] Default Overlay: D-Pad links, Run/Jump/Shoot/Use rechts, Menu/Esc
- [x] Key-Mapping auf HoD-SDL-Mappings (SHIFT=Run, UP=Jump, SPACE=Shoot, ENTER=Use, ESCAPE=Menu)
- [x] Gamepad-Default: A=Jump, X=Run, B=Shoot, Y=Use, START=Menu (Inventory/Quick-Save/Load entfernt)
- [x] Cheat-System: 3 HoD-Cheats (SpectreFireballNoHit, OneHitPlasmaCannon, WalkOnLava), 6 im Native
- [x] BS-spezifische Features entfernt: touchInventory, screenMode, Widescreen-Toggle, contextualKeyCodes

**Ergebnisse:** Alle 13 touch/-Dateien sind auf HoD-Package. BS-spezifische Features (Inventory, Screen-Mode,
Widescreen, contextualKeyCodes) vollständig aus Kotlin entfernt. `nativeSetTouchInventoryEnabled` und
`nativeGetTouchInputContext` aus Kotlin entfernt (JNI-Stubs bleiben im Native für Binärkompatibilität).
`nativeSetScreenMode` aus Kotlin entfernt. `TouchOverlayConfig`-Schema auf v9 inkrementiert.

Default-Overlay auf 6 Buttons reduziert: Menu, D-Pad, Run, Jump, Shoot, Use (vorher 10 mit Inventory,
Status, Quick-Save/Load). Gamepad-Mapping auf 5 Aktionen reduziert.

**Offene Risiken:**
- JNI-Stubs (nativeSetScreenMode, nativeSetTouchInventoryEnabled, nativeGetTouchInputContext) sind
  noch in android_main.cpp — Phase 5 kann sie endgültig entfernen

### Phase 5: Android-Qualitaet und Buildbarkeit
- [ ] Manifest landscape/fullscreen, Touchscreen/Gamepad optional ✅ (bereits in Phase 1)
- [ ] minSdk=24, targetSdk=35, compileSdk=35, ndkVersion=27.2.12479018 ✅ (bereits in Phase 1)
- [ ] Release-Signing nicht von BS uebernehmen ✅ (kein Keystore kopiert)
- [ ] App darf ohne Import nicht crashen

## Key Constraints
- Nur in `D:\Coding\HoD Android\hode` schreiben
- `D:\Coding\BS Android` nur lesen
- Keine APK vor Codex-Endabnahme bauen
- Nach jeder Phase: Review, Log-Update, dann Stopp
- **Worktrees:** Falls ein `EnterWorktree` genutzt wird, muss der Worktree-Pfad (`.claude/worktrees/<name>`) und Branch in Log UND Plan dokumentiert werden. Nach Merge den Worktree mit `git worktree remove` aufraeumen und im Log vermerken.
