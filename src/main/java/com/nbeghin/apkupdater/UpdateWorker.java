package com.nbeghin.apkupdater;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;

/** Periodic background check, scheduled by {@link ApkUpdater#schedule}. Posts a notification when a build is ready. */
public final class UpdateWorker extends Worker {

    static final String KEY_BASE_URL = "base_url";
    static final String KEY_ICON = "icon";
    private static final String TAG = "ApkUpdater";

    public UpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String baseUrl = getInputData().getString(KEY_BASE_URL);
        if (baseUrl == null) {
            Log.e(TAG, "UpdateWorker started without a base URL; schedule it through ApkUpdater.schedule()");
            return Result.failure();
        }
        try {
            UpdateChecker.Outcome outcome =
                    new UpdateChecker(getApplicationContext(), baseUrl, ApkUpdater.defaultClient()).run(null);
            if (outcome.apk != null) {
                int icon = getInputData().getInt(KEY_ICON, android.R.drawable.stat_sys_download_done);
                Notifier.showReady(getApplicationContext(), outcome.apk, icon);
            }
            return Result.success();
        } catch (UpdateException e) {
            // The server answered but we cannot use the answer; retrying will not help, the next period will try again.
            Log.e(TAG, "Update check failed: " + e.getMessage());
            return Result.failure();
        } catch (IOException e) {
            Log.w(TAG, "Update check could not reach the server, will retry: " + e.getMessage());
            return Result.retry();
        }
    }
}
