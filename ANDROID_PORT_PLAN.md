# HoD Android Port Plan

## Status: Phase 2 - Complete

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

### Phase 3: Launcher und Asset-Import auf HoD anpassen
- [ ] SafImporter validiert HoD-Dateien (setup.dat, *_hod.lvl, *_hod.sss, *_hod.mst, hod*.paf) ✅ (vorgezogen)
- [ ] Import kopiert nach filesDir/imported_game/hode ✅ (vorgezogen)
- [ ] Launcher-Text fuer HoD anpassen ✅ (vorgezogen)
- [ ] Review: launcher_background ersetzen
- [ ] Bei gueltigem Import → HodActivity starten ✅ (vorgezogen)

### Phase 4: Touch-Overlay, Controller und Cheats an HoD anpassen
- [ ] Overlay-Dateien aus BS uebernehmen, Branding auf HoD
- [ ] Default Overlay: D-Pad links, Run/Jump/Shoot/Shoot+Run rechts, Menu/Esc
- [ ] Key-Mapping auf HoD-SDL-Mappings
- [ ] Gamepad-Default anpassen
- [ ] Cheat-System integrieren (God Mode, Infinite Ammo, Level/Checkpoint)
- [ ] BS-spezifische Features entfernen (Inventory, Music/SoundFont, Widescreen)

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
