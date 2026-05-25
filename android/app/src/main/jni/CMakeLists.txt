cmake_minimum_required(VERSION 3.22.1)
project("hode" C CXX)

# Engine source root relative to this CMakeLists:
# jni -> src -> main -> app -> android -> repo root
set(ENGINE_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../..")
get_filename_component(ENGINE_ROOT "${ENGINE_ROOT}" ABSOLUTE)

# ── Include SDL2 sources ──────────────────────────────────────────
include(FetchContent)

FetchContent_Declare(
    SDL2
    URL https://github.com/libsdl-org/SDL/releases/download/release-2.30.11/SDL2-2.30.11.tar.gz
)
set(SDL_SHARED OFF CACHE BOOL "Build static SDL2" FORCE)
set(SDL_STATIC ON CACHE BOOL "Build static SDL2" FORCE)

FetchContent_MakeAvailable(SDL2)

# ── Engine sources ─────────────────────────────────────────────────
set(ENGINE_SOURCES
    "${ENGINE_ROOT}/andy.cpp"
    "${ENGINE_ROOT}/fileio.cpp"
    "${ENGINE_ROOT}/fs_android.cpp"
    "${ENGINE_ROOT}/game.cpp"
    "${ENGINE_ROOT}/level1_rock.cpp"
    "${ENGINE_ROOT}/level2_fort.cpp"
    "${ENGINE_ROOT}/level3_pwr1.cpp"
    "${ENGINE_ROOT}/level4_isld.cpp"
    "${ENGINE_ROOT}/level5_lava.cpp"
    "${ENGINE_ROOT}/level6_pwr2.cpp"
    "${ENGINE_ROOT}/level7_lar1.cpp"
    "${ENGINE_ROOT}/level8_lar2.cpp"
    "${ENGINE_ROOT}/level9_dark.cpp"
    "${ENGINE_ROOT}/lzw.cpp"
    "${ENGINE_ROOT}/main.cpp"
    "${ENGINE_ROOT}/mdec.cpp"
    "${ENGINE_ROOT}/menu.cpp"
    "${ENGINE_ROOT}/mixer.cpp"
    "${ENGINE_ROOT}/monsters.cpp"
    "${ENGINE_ROOT}/paf.cpp"
    "${ENGINE_ROOT}/random.cpp"
    "${ENGINE_ROOT}/resource.cpp"
    "${ENGINE_ROOT}/scaler_xbr.cpp"
    "${ENGINE_ROOT}/screenshot.cpp"
    "${ENGINE_ROOT}/sound.cpp"
    "${ENGINE_ROOT}/staticres.cpp"
    "${ENGINE_ROOT}/system_sdl2.cpp"
    "${ENGINE_ROOT}/util.cpp"
    "${ENGINE_ROOT}/video.cpp"
)

# Blacklisted (not built for Android):
#   benchmark.cpp, fs_posix.cpp, system_psp.cpp, system_wii.cpp

# Android-specific native entry point
set(ENGINE_SOURCES ${ENGINE_SOURCES}
    "${CMAKE_CURRENT_LIST_DIR}/src/android_main.cpp"
)

# ── Shared library ─────────────────────────────────────────────────
add_library(hode SHARED ${ENGINE_SOURCES})

target_include_directories(hode PRIVATE
    "${ENGINE_ROOT}"
    "${SDL2_SOURCE_DIR}/include"
)

target_include_directories(hode PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}"
)

target_compile_definitions(hode PRIVATE
    ANDROID_PACKAGE_NAME="com.heartofdarkness.reborn"
)

target_compile_options(hode PRIVATE
    -g -O2 -Wall
    -Wno-unused-parameter
    -Wno-sign-compare
)

target_link_libraries(hode
    SDL2-static
    log
    android
)

# NDK 27: libc++ is at a non-standard path, add directly
if(ANDROID)
    target_link_options(hode PRIVATE -lc++_shared)
endif()

# SDL_main entry point for Android
target_link_libraries(hode SDL2main)
