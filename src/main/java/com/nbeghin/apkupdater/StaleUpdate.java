package com.nbeghin.apkupdater;

import android.content.Context;
import android.content.pm.PackageInfo;

import java.io.File;

/**
 * Housekeeping after an update: once the running app is at least as new as the APK we downloaded, that APK and the
 * "update downloaded" notification are obsolete. Without this the notification of an already-installed build stays
 * visible (and tappable) until the next successful check.
 */
final class StaleUpdate {

    private StaleUpdate() {
    }

    static void dropIfInstalled(Context context) {
        File dir = new File(context.getFilesDir(), UpdateChecker.DOWNLOAD_DIR);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        long installed;
        try {
            installed = InstalledApk.read(context).versionCode;
        } catch (java.io.IOException e) {
            return; // cannot tell; leave everything as it is
        }
        boolean removedAny = false;
        for (File f : files) {
            if (f.getName().endsWith(".part")) {
                continue; // a download in progress on another thread
            }
            PackageInfo downloaded = f.getName().endsWith(".apk")
                    ? context.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), 0) : null;
            // Unreadable or half-written files cannot be installed either way.
            if (downloaded == null || InstalledApk.versionOf(downloaded) <= installed) {
                removedAny |= f.delete();
            }
        }
        if (removedAny) {
            Notifier.cancel(context);
        }
    }
}
