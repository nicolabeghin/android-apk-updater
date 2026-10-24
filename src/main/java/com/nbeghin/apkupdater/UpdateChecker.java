package com.nbeghin.apkupdater;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Blocking work: ask the server, and if there is a newer build download and verify it. Call off the main thread
 * ({@link ApkUpdater} and {@link UpdateWorker} both do).
 */
final class UpdateChecker {

    /** What a run found. {@link #apk} is set only when a verified download is waiting to be installed. */
    static final class Outcome {
        final boolean updateAvailable;
        final File apk;

        Outcome(boolean updateAvailable, File apk) {
            this.updateAvailable = updateAvailable;
            this.apk = apk;
        }
    }

    interface Progress {
        void onDownloading();
    }

    private final Context context;
    private final String baseUrl;
    private final OkHttpClient client;

    UpdateChecker(Context context, String baseUrl, OkHttpClient client) {
        this.context = context.getApplicationContext();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.client = client;
    }

    Outcome run(Progress progress) throws IOException {
        InstalledApk self = InstalledApk.read(context);
        UpdateCheckResult result = check(self);
        if (!result.hasUpdate()) {
            return new Outcome(false, null);
        }
        if (progress != null) {
            progress.onDownloading();
        }
        return new Outcome(true, download(result));
    }

    private UpdateCheckResult check(InstalledApk self) throws IOException {
        // The server matches `version > client AND md5 != client`, so all three fields are required.
        Request request = new Request.Builder()
                .url(baseUrl + "/check")
                .post(new FormBody.Builder()
                        .add("pkgname", self.packageName)
                        .add("version", Long.toString(self.versionCode))
                        .add("md5", self.md5)
                        .build())
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new UpdateException("Update check failed: HTTP " + response.code() + " from " + request.url());
            }
            return UpdateCheckResult.parse(body.string());
        }
    }

    private File download(UpdateCheckResult result) throws IOException {
        File dir = new File(context.getFilesDir(), "apkupdater");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create " + dir);
        }
        String expected = result.getExpectedMd5();
        File target = new File(dir, (expected != null ? expected : "update") + ".apk");

        // A previous run may already have fetched exactly this build.
        if (expected != null && target.isFile() && expected.equals(Md5.hex(target))) {
            deleteOthers(dir, target);
            return target;
        }

        File part = new File(dir, target.getName() + ".part");
        Request request = new Request.Builder().url(result.getDownloadUrl()).build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new UpdateException("APK download failed: HTTP " + response.code()
                        + " (download links expire after 20 minutes)");
            }
            try (InputStream in = body.byteStream(); OutputStream out = new FileOutputStream(part)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
        } catch (IOException e) {
            part.delete();
            throw e;
        }

        if (expected != null) {
            String actual = Md5.hex(part);
            if (!expected.equals(actual)) {
                part.delete();
                throw new UpdateException("Downloaded APK is corrupt: MD5 " + actual + ", expected " + expected);
            }
        }
        if (target.exists() && !target.delete()) {
            throw new IOException("Cannot replace " + target);
        }
        if (!part.renameTo(target)) {
            throw new IOException("Cannot move " + part + " to " + target);
        }
        deleteOthers(dir, target);
        return target;
    }

    /** Keeps the directory to a single APK so old downloads do not pile up. */
    private static void deleteOthers(File dir, File keep) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (!f.equals(keep)) {
                f.delete();
            }
        }
    }
}
