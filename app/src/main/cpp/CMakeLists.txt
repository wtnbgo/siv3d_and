cmake_minimum_required(VERSION 3.21)

# -----------------------------------------------------------------------------
# Siv3D (SDL3 版) アプリの Android ネイティブ部 (libmain.so)
#   Gradle (app/build.gradle) から SIV3D_BASE (OpenSiv3D) と SIV3D_APP_DIR (アプリのフォルダ) が渡される。
#   アプリのソースは SIV3D_APP_DIR/src 以下の .cpp (siv3d-app.json の android.sources で変更可)
# -----------------------------------------------------------------------------

# ABI → vcpkg の triplet / NDK の toolchain (project() より前に決める)
if(ANDROID_ABI STREQUAL "arm64-v8a")
  set(VCPKG_TARGET_TRIPLET "arm64-android" CACHE STRING "")
elseif(ANDROID_ABI STREQUAL "x86_64")
  set(VCPKG_TARGET_TRIPLET "x64-android" CACHE STRING "")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
  set(VCPKG_TARGET_TRIPLET "arm-neon-android" CACHE STRING "")
else()
  message(FATAL_ERROR "unsupported ANDROID_ABI: ${ANDROID_ABI}")
endif()
if(NOT DEFINED VCPKG_CHAINLOAD_TOOLCHAIN_FILE)
  set(VCPKG_CHAINLOAD_TOOLCHAIN_FILE "${ANDROID_NDK}/build/cmake/android.toolchain.cmake" CACHE FILEPATH "")
endif()
# vcpkg の Android triplet は環境変数 ANDROID_NDK_HOME で NDK を探す
set(ENV{ANDROID_NDK_HOME} "${ANDROID_NDK}")

include("${SIV3D_BASE}/cmake/Siv3DAppPrelude.cmake")

project(Siv3DAndroidApp LANGUAGES C CXX)

include("${SIV3D_BASE}/cmake/Siv3DApp.cmake")

file(GLOB_RECURSE SIV3D_APP_SOURCES CONFIGURE_DEPENDS "${SIV3D_APP_DIR}/src/*.cpp")

siv3d_add_app(Siv3DAndroidApp
  SOURCES ${SIV3D_APP_SOURCES}
  CONFIG "${SIV3D_APP_DIR}/siv3d-app.json"
)
