package com.nbeghin.apkupdater;

import androidx.core.content.FileProvider;

/**
 * Exists only to give the library's provider a class name of its own: the manifest merger identifies providers by
 * {@code android:name}, so reusing {@link FileProvider} directly would clash with any FileProvider the host app declares.
 */
public final class ApkUpdaterFileProvider extends FileProvider {
}
