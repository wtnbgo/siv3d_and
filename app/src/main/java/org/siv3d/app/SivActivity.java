package org.siv3d.app;

import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Siv3D (SDL3 版) アプリのアクティビティ。
 *
 * - APK の assets/siv3d/ (エンジンのリソースとアプリのアセット) を、初回起動時 (と更新後) に
 *   内部ストレージ (filesDir/siv3d/) へ展開する。Siv3D はファイルを通常のファイルとして読むため
 * - そのフォルダを起動引数で渡す。Siv3D は最初の引数をアプリディレクトリとして、
 *   カレントディレクトリと resources/ の場所に使う (Siv3DMain.cpp)
 */
public class SivActivity extends SDLActivity {
    private static final String TAG = "Siv3D";
    private static final String ASSET_ROOT = "siv3d";

    private File appDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appDir = new File(getFilesDir(), ASSET_ROOT);
        setupEnvironment();
        try {
            extractAssetsIfNeeded();
        } catch (IOException e) {
            Log.e(TAG, "failed to extract assets", e);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        final boolean finishing = isFinishing();
        super.onDestroy();
        // Siv3D のエンジン (ネイティブの静的な状態) は同じプロセスで作り直せない。アプリの終了
        // (Main からの return・戻るボタン) でアクティビティが終わったらプロセスも終了し、次の起動を
        // 新しいプロセスで始める (残すと SDL が二重起動とみなして、次の起動がすぐ終わってしまう)
        if (finishing) {
            Log.v(TAG, "activity finished; exiting the process");
            System.exit(0);
        }
    }

    @Override
    protected String[] getLibraries() {
        return new String[] { "SDL3", "main" };
    }

    @Override
    protected String[] getArguments() {
        return new String[] { appDir.getAbsolutePath() };
    }

    // Siv3D (Linux 扱い) は HOME / XDG_* からキャッシュや設定の場所を決めるので、アプリ専用の場所を指す
    // (Android のプロセスには HOME が無く、既定の /var/cache などには書き込めない)
    private void setupEnvironment() {
        try {
            Os.setenv("HOME", getFilesDir().getAbsolutePath(), true);
            Os.setenv("XDG_DATA_HOME", new File(getFilesDir(), "data").getAbsolutePath(), true);
            Os.setenv("XDG_CONFIG_HOME", new File(getFilesDir(), "config").getAbsolutePath(), true);
            Os.setenv("XDG_CACHE_HOME", getCacheDir().getAbsolutePath(), true);
            Os.setenv("TMPDIR", getCacheDir().getAbsolutePath(), true);
        } catch (ErrnoException e) {
            Log.e(TAG, "failed to set environment variables", e);
        }
    }

    // APK の更新時刻が変わったときだけ展開し直す
    private void extractAssetsIfNeeded() throws IOException {
        String stamp;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            stamp = info.lastUpdateTime + "/" + info.getLongVersionCode();
        } catch (Exception e) {
            stamp = "unknown";
        }

        File stampFile = new File(appDir, ".siv3d_assets");
        if (stampFile.exists() && stamp.equals(readText(stampFile))) {
            return;
        }

        Log.i(TAG, "extracting assets to " + appDir);
        deleteRecursive(appDir);
        copyAssetDir(getAssets(), ASSET_ROOT, appDir);
        writeText(stampFile, stamp);
    }

    private static void copyAssetDir(AssetManager am, String path, File dest) throws IOException {
        String[] children = am.list(path);
        if (children == null || children.length == 0) {
            // ファイル
            dest.getParentFile().mkdirs();
            try (InputStream in = am.open(path); OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
            return;
        }
        dest.mkdirs();
        for (String child : children) {
            copyAssetDir(am, path + "/" + child, new File(dest, child));
        }
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursive(c);
                }
            }
        }
        f.delete();
    }

    private static String readText(File f) {
        try (InputStream in = new java.io.FileInputStream(f)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static void writeText(File f, String s) throws IOException {
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }
}
