# siv3d_and — Siv3D (SDL3 版) アプリの Android 外枠

[siv3d_app](https://github.com/wtnbgo/siv3d_app) 形式のアプリ (ソース・アセット・`siv3d-app.json`) を、
Android の APK / AAB にビルドするための Gradle プロジェクトです。エンジンは
[OpenSiv3D](https://github.com/wtnbgo/OpenSiv3D) の `develop_sdl3` を参照します。

アプリ側にはこのリポジトリをコピーしません。アプリのフォルダ (`PROJECT_DIR`) と OpenSiv3D (`SIV3D_BASE`) を
参照してビルドし、出力はアプリの `build/android/` に置きます。

```
work/
  OpenSiv3D/     ← SIV3D_BASE   (develop_sdl3)
  siv3d_and/     ← このリポジトリ (SIV3D_ANDROID)
  MyApp/         ← PROJECT_DIR  (siv3d_app のテンプレートから作ったアプリ)
```

## 必要なもの

- Android SDK (`ANDROID_HOME`)、NDK 27 (`siv3d-app.json` の `android.ndkVersion` で変更可)、SDK の CMake 4.1.2
- JDK 17 以上 (`JAVA_HOME`)
- vcpkg (`VCPKG_ROOT`)。Android 用の依存ライブラリ (`arm64-android` / `x64-android`) をビルドする

## ビルド

アプリのフォルダから、OpenSiv3D のドライバ経由で使うのが簡単です。

```bash
cd MyApp
python ../OpenSiv3D/tools/siv3d_app.py --target android all              # build + stage + package (APK)
python ../OpenSiv3D/tools/siv3d_app.py --target android --abis x86_64 all # エミュレータ用だけ
python ../OpenSiv3D/tools/siv3d_app.py --target android run              # adb install + 起動 (複数台つないでいるときは ANDROID_SERIAL=<serial>)
```

Gradle を直接使うこともできます。

```bash
cd siv3d_and
PROJECT_DIR=../MyApp SIV3D_BASE=../OpenSiv3D ./gradlew assembleDebug -Pabis=x86_64
```

`PROJECT_DIR` / `SIV3D_BASE` は環境変数・`-P` オプション・`local.properties` のどれでも指定でき、
省略すると隣の `../siv3d_app` / `../OpenSiv3D` を使います。

## siv3d-app.json の "android" 節

| キー | 既定 | 意味 |
|---|---|---|
| `applicationId` | `bundleId` | アプリの ID |
| `name` | `name` | ランチャーに出る名前 |
| `versionCode` | 1 | バージョンコード (versionName は `version`) |
| `abis` | `["arm64-v8a", "x86_64"]` | ビルドする ABI |
| `minSdk` / `targetSdk` | 28 / 36 | minSdk は vcpkg の opencv4 が 28 以上を要求する |
| `ndkVersion` | `27.3.13750724` | NDK |
| `cmake.options` | | CMake に渡すオプション (全機種共通の `cmake.options` に追加) |
| `package` | `["apk"]` | 配布物: `apk` / `aab` |

## しくみ

- ネイティブ部は `app/src/main/cpp/CMakeLists.txt`。OpenSiv3D の `Siv3DAppPrelude.cmake` / `Siv3DApp.cmake` を読み、
  アプリの `src/*.cpp` から `siv3d_add_app()` で `libmain.so` を作る。SDL3 も OpenSiv3D の CMake がソースから
  ビルドする (`libSDL3.so`)。ABI ごとに vcpkg の triplet (`arm64-android` / `x64-android`) を選ぶ
- Java 側は SDL 3.4.20 の `org.libsdl.app` (`android-project` から複製) と、それを継承した `org.siv3d.app.SivActivity`
- エンジンのリソース (`resources/engine`) とアプリのアセットは APK の `assets/siv3d/` に入れ、`SivActivity` が初回起動時
  (と更新後) に内部ストレージ (`files/siv3d/`) へ展開する。そのフォルダを起動引数で渡し、Siv3D はそこを
  カレントディレクトリと `resources/` の場所にする
- Siv3D は Linux 扱いでビルドされるため、`HOME` / `XDG_*` をアプリ専用の場所に設定している (キャッシュ・設定の保存先)
- ログは logcat に出る: `adb logcat -s Siv3D`

## 署名

`release` は現在 debug の鍵で署名します。配布用の鍵は `app/build.gradle` の `signingConfigs` に追加してください
(鍵とパスワードは `local.properties` や環境変数で渡し、リポジトリに入れないこと)。

## 既知の制限

- 確認済みは x86_64 エミュレータ (Android 16、Pixel 6) だけ。arm64-v8a の実機は未確認
- タッチ操作はマウスとして届くが、拡大表示 (Scene の拡大) のときにタッチ位置と Siv3D 側の座標がずれる
- 起動時に `OpenGL Error: 0x500` が 1 回ログに出る (描画には影響なし。macOS と同じ)
- `SIV3D_WITH_MOVIEPLAYER` は有効 (libvpx)。FFmpeg (`SIV3D_WITH_FFMPEG`) は Linux デスクトップ専用で Android では使わない
