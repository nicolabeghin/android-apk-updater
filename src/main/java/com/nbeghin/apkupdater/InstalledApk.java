package com.nbeghin.apkupdater;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.io.IOException;

/**
 * What the update server needs to know about the running app: package name, versionCode and the MD5 of the
 * installed APK. The MD5 is cached against the APK's size and mtime, so it is only recomputed after an update.
 *
 * With split APKs (an app bundle install) {@code sourceDir} is only the base split, so this MD5 will not match
 * any uploaded build. That is harmless: the server still answers on versionCode. Upload universal APKs.
 */
final class InstalledApk {

    private static final String PREFS = "apkupdater";

    final String packageName;
    final long versionCode;
    final String md5;

    private InstalledApk(String packageName, long versionCode, String md5) {
        this.packageName = packageName;
        this.versionCode = versionCode;
        this.md5 = md5;
    }

    static InstalledApk read(Context context) throws IOException {
        String pkg = context.getPackageName();
        PackageInfo info;
        try {
            info = context.getPackageManager().getPackageInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IOException("Cannot read own package info for " + pkg, e);
        }
        long version = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;

        File apk = new File(context.getApplicationInfo().sourceDir);
        String stamp = apk.length() + ":" + apk.lastModified();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String md5;
        if (stamp.equals(prefs.getString("md5_stamp", null)) && prefs.getString("md5", null) != null) {
            md5 = prefs.getString("md5", null);
        } else {
            md5 = Md5.hex(apk);
            prefs.edit().putString("md5", md5).putString("md5_stamp", stamp).apply();
        }
        return new InstalledApk(pkg, version, md5);
    }
}
