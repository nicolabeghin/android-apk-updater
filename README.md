# android-apk-updater

Android client library for the [android-apk-updater](https://android-apk-updater.com) server: keep an app that
lives outside the Play Store up to date.

It is a **fork and replacement of [Auto-Update-APK](https://github.com/lazydroid/auto-update-apk-client)**, whose
client has been unmaintained since 2017 and whose site last shows a 2019 copyright. The update-check protocol is unchanged, so migrating means
swapping one class for this library and pointing it at the new server. The original sources are kept in the git
history; see [NOTICE](NOTICE).

## What it does

1. Sends the app's package name, `versionCode` and the MD5 of its installed APK to `POST <baseUrl>/check`.
2. If the server answers `have update`, downloads the build from the (20-minute, presigned) URL it returned.
3. Verifies the download against the MD5 in that URL, so a truncated or corrupt file is never offered for install.
4. Notifies the user, or hands your app the file, and opens the system installer.

Compared with the original client: OkHttp instead of `HttpURLConnection` + `AsyncTask`, WorkManager instead of a
connectivity receiver, a verified download, a clear error when the server's reply isn't what's expected (instead of
silently looking "up to date"), and the Android 8+ "Install unknown apps" permission handled.

## Install

The library is published through [JitPack](https://jitpack.io). Add the repository:

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

and the dependency (use the latest tag from the [releases page](https://github.com/nicolabeghin/android-apk-updater/releases)):

```groovy
// app/build.gradle
dependencies {
    implementation 'com.github.nicolabeghin:android-apk-updater:<tag>'
}
```

Requires `minSdk 21` and Java 8 language level. OkHttp 4 and WorkManager come in transitively.

## Use

```java
ApkUpdater updater = new ApkUpdater.Builder(context)
        .baseUrl("https://www.android-apk-updater.com")   // or your own host; no trailing /check
        .notificationIcon(R.drawable.ic_stat_update)       // monochrome; optional
        .build();

// Background: check every 12 hours (minimum 1 hour), notify when a build is downloaded.
// Safe to call on every start; it replaces the existing schedule.
updater.schedule(12, TimeUnit.HOURS);

// Or check right now and drive your own UI. Callbacks arrive on the main thread.
updater.checkNow(new ApkUpdater.Listener() {
    @Override public void onReady(File apk) {
        startActivity(Installer.intentFor(context, apk));
    }
    @Override public void onError(IOException e) { Log.e("update", e.getMessage()); }
});
```

Upload your **universal APK** to the server for each release, and bump `versionCode` every time: the server only
offers a build whose `versionCode` is higher than the one installed.

### Notifications and permissions

- On Android 13+ your app must request `POST_NOTIFICATIONS` itself; without it the scheduled check still downloads
  but cannot notify. `checkNow` does not need it.
- On Android 8+ the user must allow *Install unknown apps* for your app. When it isn't allowed, the notification
  opens that Settings page instead of the installer (`Installer.canInstall(context)` tells you which case applies).
- Notification text comes from string resources (`apkupdater_*`); override them in your app to translate.
- The library declares its own `FileProvider` (`<applicationId>.apkupdater.fileprovider`), so it doesn't clash with one
  you already have.

## Migrating from Auto-Update-APK

| Before | After |
| --- | --- |
| `aua = new AutoUpdateApk(getApplicationContext());` | `new ApkUpdater.Builder(ctx).baseUrl("https://…").build().schedule(12, TimeUnit.HOURS);` |
| `aua.setUpdateInterval(...)`, `addSchedule(...)` | the `schedule(interval, unit)` argument; WorkManager decides the exact moment |
| `aua.enableMobileUpdates()` | `.wifiOnly(false)` on the builder (Wi-Fi only by default) |
| `aua.checkUpdatesManually()` | `updater.checkNow(listener)` |
| `Observable` status strings | `ApkUpdater.Listener` callbacks |

Remove the old `AutoUpdateApk.java` and its FileProvider entry. Apps already in the field keep calling the old host
until you ship one build with the new URL; after that, updates come from here.

## Known limitations

- The notification's tap target is fixed when it is posted, so if it was posted before the user allowed *Install
  unknown apps*, tapping it still opens Settings until the next check re-posts it.
- Installation uses the system installer through an intent. `PackageInstaller` sessions (and unattended updates for
  device-owner apps) are not implemented.
- App-bundle installs have no single APK to hash, so the reported MD5 never matches an upload; the server then
  decides on `versionCode` alone, which is correct, but it means MD5-keyed distribution policies don't apply.
- The pure logic (reply parsing, MD5) has unit tests. The Android parts have not yet been run on a device.

## Build

```bash
./gradlew assembleRelease
./gradlew test
```

## Licence

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
