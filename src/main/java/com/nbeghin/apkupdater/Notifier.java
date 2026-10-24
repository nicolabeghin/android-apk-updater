package com.nbeghin.apkupdater;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;

/** Posts the "update downloaded, tap to install" notification. All text comes from overridable string resources. */
final class Notifier {

    static final String CHANNEL_ID = "apkupdater_updates";
    private static final int NOTIFICATION_ID = 0x4150; // "AP"

    private Notifier() {
    }

    static void cancel(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);
    }

    /** Returns false (and posts nothing) when the user has notifications switched off or never granted them. */
    static boolean showReady(Context context, File apk, int smallIcon) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, context.getString(R.string.apkupdater_channel_name), NotificationManager.IMPORTANCE_DEFAULT));
        }
        boolean canInstall = Installer.canInstall(context);
        PendingIntent tap = PendingIntent.getActivity(
                context, 0, Installer.intentFor(context, apk), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        manager.notify(NOTIFICATION_ID, new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(context.getString(canInstall
                        ? R.string.apkupdater_ready_title : R.string.apkupdater_permission_title))
                .setContentText(context.getString(canInstall
                        ? R.string.apkupdater_ready_text : R.string.apkupdater_permission_text))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build());
        return true;
    }
}
