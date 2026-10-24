package com.nbeghin.apkupdater;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Entry point. Point it at your android-apk-updater server, then either check on demand or schedule a periodic check:
 *
 * <pre>
 * ApkUpdater updater = new ApkUpdater.Builder(context)
 *         .baseUrl("https://updates.example.com")
 *         .build();
 * updater.schedule(12, TimeUnit.HOURS);   // background check, notifies when a build is ready
 * updater.checkNow(listener);             // or check right now and react in your own UI
 * </pre>
 */
public final class ApkUpdater {

    /** Callbacks arrive on the main thread. Override only what you need. */
    public interface Listener {
        default void onChecking() {
        }

        default void onNoUpdate() {
        }

        default void onDownloading() {
        }

        /** A verified APK is ready; {@code Installer.intentFor(context, apk)} gives the intent to install it. */
        default void onReady(@NonNull File apk) {
        }

        default void onError(@NonNull IOException error) {
        }
    }

    private static final String WORK_NAME = "apkupdater-periodic";
    // The server's own guidance for clients is "no more often than hourly".
    private static final long MIN_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);

    // The scheduled worker can start in a process where only Application.onCreate has run, and WorkManager input data
    // cannot carry a client, so the client given to the Builder is remembered here for the worker to use.
    private static volatile OkHttpClient sharedClient;

    private final Context context;
    private final String baseUrl;
    private final OkHttpClient client;
    private final boolean wifiOnly;
    private final int notificationIcon;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ApkUpdater(Builder b) {
        this.context = b.context.getApplicationContext();
        this.baseUrl = b.baseUrl;
        this.client = b.client != null ? b.client : defaultClient();
        if (b.client != null) {
            sharedClient = b.client;
        }
        this.wifiOnly = b.wifiOnly;
        this.notificationIcon = b.notificationIcon;
    }

    /** The client the host app configured, or the default one when it did not (or has not built an updater yet). */
    static OkHttpClient clientForWorker() {
        OkHttpClient shared = sharedClient;
        return shared != null ? shared : defaultClient();
    }

    static OkHttpClient defaultClient() {
        // The APK itself can be large, so the read timeout is generous; the check reply is a few bytes either way.
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /** Checks now, downloads and verifies a newer build if there is one, and reports through {@code listener}. */
    public void checkNow(@NonNull final Listener listener) {
        main.post(listener::onChecking);
        executor.execute(() -> {
            try {
                StaleUpdate.dropIfInstalled(context);
                UpdateChecker.Outcome outcome = new UpdateChecker(context, baseUrl, client)
                        .run(() -> main.post(listener::onDownloading));
                if (outcome.apk != null) {
                    main.post(() -> listener.onReady(outcome.apk));
                } else {
                    main.post(listener::onNoUpdate);
                }
            } catch (IOException e) {
                main.post(() -> listener.onError(e));
            }
        });
    }

    /**
     * Checks in the background every {@code interval} (at least one hour) and posts a notification when a build is
     * downloaded. Safe to call on every app start: it replaces the existing schedule instead of stacking.
     */
    public void schedule(long interval, @NonNull TimeUnit unit) {
        if (unit.toMillis(interval) < MIN_INTERVAL_MS) {
            throw new IllegalArgumentException("Interval must be at least 1 hour, got " + interval + " " + unit);
        }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(UpdateWorker.class, interval, unit)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(wifiOnly ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                        .build())
                .setInputData(new Data.Builder()
                        .putString(UpdateWorker.KEY_BASE_URL, baseUrl)
                        .putInt(UpdateWorker.KEY_ICON, notificationIcon)
                        .build())
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
        // Right after an update the "update downloaded" notification and APK are obsolete; do not wait for the next period.
        executor.execute(() -> StaleUpdate.dropIfInstalled(context));
    }

    public void cancelSchedule() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    public static final class Builder {
        private final Context context;
        private String baseUrl;
        private OkHttpClient client;
        private boolean wifiOnly = true;
        private int notificationIcon = android.R.drawable.stat_sys_download_done;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        /** Root of your android-apk-updater server, e.g. {@code https://updates.example.com} (no {@code /check}). */
        public Builder baseUrl(@NonNull String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** Reuse your own client (certificates, proxy, logging). The default has sensible timeouts. */
        public Builder okHttpClient(@NonNull OkHttpClient client) {
            this.client = client;
            return this;
        }

        /** Scheduled checks run only on unmetered networks (default true). On-demand checks always run. */
        public Builder wifiOnly(boolean wifiOnly) {
            this.wifiOnly = wifiOnly;
            return this;
        }

        /** Small notification icon; it should be a monochrome drawable. Defaults to a system download icon. */
        public Builder notificationIcon(int drawableRes) {
            this.notificationIcon = drawableRes;
            return this;
        }

        public ApkUpdater build() {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalStateException("baseUrl is required, e.g. https://updates.example.com");
            }
            return new ApkUpdater(this);
        }
    }
}
