/*
 * Heart of Darkness Android entry point
 */
#include <SDL.h>
#include <android/log.h>
#include <jni.h>
#include <exception>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include "game.h"
#include "system.h"
#include "util.h"
#include "video.h"

#define HOD_LOGI(...) __android_log_print(ANDROID_LOG_INFO, "HodNative", __VA_ARGS__)
#define HOD_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "HodNative", __VA_ARGS__)

static Game *g_game = nullptr;
static uint32_t g_pendingCheatMask = 0;
static bool g_pendingControllerEnabled = false;
static char g_pendingControllerMapping[2048] = {0};
static bool g_pendingDpadDoubleTapRun = false;

extern "C" {

JNIEXPORT void JNICALL Java_com_heartofdarkness_reborn_HodActivity_nativeSetCheat(JNIEnv *env, jclass cls, jint cheatId, jboolean enabled) {
    // HoD cheat bits (game.h): kCheatSpectreFireballNoHit=1, kCheatOneHitPlasmaCannon=2,
    // kCheatOneHitSpecialPowers=4, kCheatWalkOnLava=8, kCheatGateNoCrush=16,
    // kCheatLavaNoHit=32, kCheatRockShadowNoHit=64
    uint32_t bit = 0;
    switch (cheatId) {
        case 0: bit = kCheatSpectreFireballNoHit; break;
        case 1: bit = kCheatOneHitPlasmaCannon; break;
        case 2: bit = kCheatWalkOnLava; break;
        case 3: bit = kCheatLavaNoHit; break;
        case 4: bit = kCheatGateNoCrush; break;
        case 5: bit = kCheatRockShadowNoHit; break;
    }
    if (!bit) return;
    if (enabled) {
        g_pendingCheatMask |= bit;
    } else {
        g_pendingCheatMask &= ~bit;
    }
    if (g_game) {
        g_game->_cheats = g_pendingCheatMask;
    }
    HOD_LOGI("nativeSetCheat cheatId=%d enabled=%d mask=0x%x", cheatId, enabled, g_pendingCheatMask);
}

JNIEXPORT void JNICALL Java_com_heartofdarkness_reborn_HodActivity_nativeSetScreenMode(JNIEnv *env, jclass cls, jint mode) {
    HOD_LOGI("nativeSetScreenMode mode=%d (stub, not applicable to HoD)", mode);
}

JNIEXPORT void JNICALL Java_com_heartofdarkness_reborn_HodActivity_nativeSetControllerConfig(JNIEnv *env, jclass cls, jboolean enabled, jstring mapping, jboolean dpadDoubleTapRunEnabled) {
    g_pendingControllerEnabled = (enabled == JNI_TRUE);
    g_pendingDpadDoubleTapRun = (dpadDoubleTapRunEnabled == JNI_TRUE);

    const char *mappingStr = nullptr;
    if (mapping) {
        mappingStr = env->GetStringUTFChars(mapping, nullptr);
    }
    if (mappingStr) {
        strncpy(g_pendingControllerMapping, mappingStr, sizeof(g_pendingControllerMapping) - 1);
        g_pendingControllerMapping[sizeof(g_pendingControllerMapping) - 1] = '\0';
        env->ReleaseStringUTFChars(mapping, mappingStr);
    } else {
        g_pendingControllerMapping[0] = '\0';
    }

    HOD_LOGI("nativeSetControllerConfig enabled=%d dpadDoubleTap=%d (stub)", g_pendingControllerEnabled, g_pendingDpadDoubleTapRun);
}

JNIEXPORT void JNICALL Java_com_heartofdarkness_reborn_HodActivity_nativeSetTouchInventoryEnabled(JNIEnv *env, jclass cls, jboolean enabled) {
    HOD_LOGI("nativeSetTouchInventoryEnabled enabled=%d (stub, not applicable to HoD)", enabled);
}

JNIEXPORT jint JNICALL Java_com_heartofdarkness_reborn_HodActivity_nativeGetTouchInputContext(JNIEnv *env, jclass cls) {
    return 0; // TOUCH_INPUT_CONTEXT_GAMEPLAY default
}

} // extern "C"

extern "C" int SDL_main(int argc, char *argv[]) {
    setvbuf(stdout, nullptr, _IONBF, 0);
    setvbuf(stderr, nullptr, _IONBF, 0);

    const char *dataPath = ".";
    const char *savePath = ".";
    int debugMask = 0;

    HOD_LOGI("SDL_main entered argc=%d", argc);
    for (int i = 0; i < argc; ++i) {
        HOD_LOGI("argv[%d]=%s", i, argv[i] ? argv[i] : "(null)");
    }

    for (int i = 1; i < argc; ++i) {
        if (strncmp(argv[i], "--datapath=", 11) == 0) {
            dataPath = argv[i] + 11;
        } else if (strncmp(argv[i], "--savepath=", 11) == 0) {
            savePath = argv[i] + 11;
        } else if (strncmp(argv[i], "--debug=", 8) == 0) {
            debugMask = atoi(argv[i] + 8);
        }
    }

    HOD_LOGI("Parsed args datapath=%s savepath=%s debug=%d",
            dataPath, savePath, debugMask);

    try {
        g_debugMask = debugMask;
        HOD_LOGI("Creating HoD Game instance");
        g_game = new Game(dataPath, savePath, g_pendingCheatMask);
        HOD_LOGI("Game created, initializing display");
        g_game->_res->loadSetupDat();
        const bool isPsx = g_game->_res->_isPsx;
        g_system->init("Heart of Darkness", Video::W, Video::H, true, false, isPsx);

        // Setup audio
        {
            AudioCallback cb;
            cb.proc = [](void *userdata, int16_t *buf, int len) {
                ((Game *)userdata)->mixAudio(buf, len);
            };
            cb.userdata = g_game;
            g_system->startAudio(cb);
        }

        if (isPsx) {
            g_game->_video->initPsx();
        }
        g_game->displayLoadingScreen();

        HOD_LOGI("Entering level main loop");
        bool resume = true;
        int level = 0;
        int checkpoint = 0;
        do {
            g_game->loadSetupCfg(resume);
            bool levelChanged = false;
            while (!g_system->inp.quit && level < kLvl_test) {
                g_game->displayLoadingScreen();
                g_game->mainLoop(level, checkpoint, levelChanged);
                if (resume) {
                    g_game->saveSetupCfg();
                }
                if (g_game->_res->_isDemo) {
                    break;
                }
                level = g_game->_currentLevel + 1;
                checkpoint = 0;
                levelChanged = true;
            }
        } while (!g_system->inp.quit && resume && !isPsx);

        HOD_LOGI("Main loop finished; cleaning up");
        g_system->stopAudio();
        g_system->destroy();
        delete g_game;
        g_game = nullptr;
        HOD_LOGI("SDL_main returning normally");
        return 0;
    } catch (const std::exception &e) {
        HOD_LOGE("Unhandled C++ exception in SDL_main: %s", e.what());
    } catch (...) {
        HOD_LOGE("Unhandled non-standard exception in SDL_main");
    }

    if (g_game) {
        delete g_game;
        g_game = nullptr;
    }
    return -1;
}
