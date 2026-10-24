package com.nbeghin.apkupdater;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Lowercase hex MD5, the identifier the update server keys builds on. */
final class Md5 {

    private Md5() {
    }

    static String hex(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return hex(in);
        }
    }

    static String hex(InputStream in) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is required on every Java platform", e);
        }
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder(32);
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
