package com.nbeghin.apkupdater;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Builds the intent that installs a downloaded APK. On Android 8+ the system silently drops the install intent
 * unless the user has allowed "Install unknown apps" for this app, so in that case the intent opens that
 * Settings page instead.
 */
public final class Installer {

    private static final String APK_MIME = "application/vnd.android.package-archive";

    private Installer() {
    }

    /** True when the system will accept an install intent from this app. */
    public static boolean canInstall(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || context.getPackageManager().canRequestPackageInstalls();
    }

    /** Intent that opens the "Install unknown apps" page for this app. */
    public static Intent permissionIntent(Context context) {
        return new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.getPackageName()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** Intent that opens the system installer for {@code apk}. Check {@link #canInstall} first. */
    public static Intent installIntent(Context context, File apk) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".apkupdater.fileprovider", apk);
        return new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** The right intent for the current permission state. */
    public static Intent intentFor(Context context, File apk) {
        return canInstall(context) ? installIntent(context, apk) : permissionIntent(context);
    }
}
