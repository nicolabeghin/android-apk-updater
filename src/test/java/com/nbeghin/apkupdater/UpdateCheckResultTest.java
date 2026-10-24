package com.nbeghin.apkupdater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class UpdateCheckResultTest {

    private static final String URL =
            "https://bucket.s3.eu-west-1.amazonaws.com/com.acme.app/9f2c1d4e5a6b7c8d9e0f1a2b3c4d5e6f.apk?X-Amz-Expires=1200&X-Amz-Signature=abc";

    @Test
    public void noUpdate() throws Exception {
        UpdateCheckResult r = UpdateCheckResult.parse("no update");
        assertFalse(r.hasUpdate());
        assertNull(r.getDownloadUrl());
        assertNull(r.getExpectedMd5());
    }

    @Test
    public void haveUpdateReadsUrlAndMd5() throws Exception {
        UpdateCheckResult r = UpdateCheckResult.parse("have update\n" + URL + "\n");
        assertTrue(r.hasUpdate());
        assertEquals(URL, r.getDownloadUrl());
        assertEquals("9f2c1d4e5a6b7c8d9e0f1a2b3c4d5e6f", r.getExpectedMd5());
    }

    @Test
    public void toleratesCrlfAndCase() throws Exception {
        UpdateCheckResult r = UpdateCheckResult.parse("Have Update\r\n" + URL);
        assertTrue(r.hasUpdate());
    }

    @Test
    public void md5IsNullWhenUrlHasNoMd5Name() throws Exception {
        UpdateCheckResult r = UpdateCheckResult.parse("have update\nhttps://example.com/files/app-release.apk");
        assertTrue(r.hasUpdate());
        assertNull(r.getExpectedMd5());
    }

    @Test
    public void haveUpdateWithoutUrlIsAnError() {
        try {
            UpdateCheckResult.parse("have update");
            fail("expected UpdateException");
        } catch (UpdateException expected) {
            assertTrue(expected.getMessage().contains("no download URL"));
        }
    }

    @Test
    public void unknownReplyIsAnErrorNotNoUpdate() {
        try {
            UpdateCheckResult.parse("<html><body>404 Not Found</body></html>");
            fail("expected UpdateException");
        } catch (UpdateException expected) {
            assertTrue(expected.getMessage().contains("base URL"));
        }
    }

    @Test
    public void emptyReplyIsAnError() {
        try {
            UpdateCheckResult.parse("");
            fail("expected UpdateException");
        } catch (UpdateException expected) {
            // "" splits to one empty line, which is not a known status
        }
    }
}
