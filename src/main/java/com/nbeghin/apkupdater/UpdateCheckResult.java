package com.nbeghin.apkupdater;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The server's answer to {@code POST /check}: {@code no update}, or {@code have update} followed by a
 * download URL on the next line. Any other first line is an error, not "no update", so a misconfigured
 * base URL (an HTML error page, say) is reported instead of silently looking up to date.
 */
public final class UpdateCheckResult {

    private static final Pattern MD5_FILE = Pattern.compile("([0-9a-fA-F]{32})\\.apk");

    private final String downloadUrl; // null when there is no update

    private UpdateCheckResult(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public static UpdateCheckResult parse(String body) throws UpdateException {
        if (body == null) {
            throw new UpdateException("Empty reply from update server");
        }
        String[] lines = body.trim().split("\\r?\\n");
        String status = lines[0].trim();
        if (status.equalsIgnoreCase("no update")) {
            return new UpdateCheckResult(null);
        }
        if (status.equalsIgnoreCase("have update")) {
            if (lines.length < 2 || lines[1].trim().isEmpty()) {
                throw new UpdateException("Server said \"have update\" but sent no download URL");
            }
            return new UpdateCheckResult(lines[1].trim());
        }
        String shown = status.length() > 60 ? status.substring(0, 60) + "..." : status;
        throw new UpdateException("Unexpected reply from update server (is the base URL right?): " + shown);
    }

    public boolean hasUpdate() {
        return downloadUrl != null;
    }

    /** Presigned download URL; it expires, so download straight away. Null when there is no update. */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * The server stores each build as {@code <package>/<md5>.apk}, and the presigned URL keeps that name, so the
     * expected MD5 can be read off the URL. Null when the URL does not end that way (nothing to verify against).
     */
    public String getExpectedMd5() {
        if (downloadUrl == null) {
            return null;
        }
        String path;
        try {
            path = new URI(downloadUrl).getPath();
        } catch (Exception e) {
            return null;
        }
        if (path == null) {
            return null;
        }
        String name = path.substring(path.lastIndexOf('/') + 1);
        Matcher m = MD5_FILE.matcher(name);
        return m.matches() ? m.group(1).toLowerCase() : null;
    }
}
