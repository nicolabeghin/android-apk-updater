package com.nbeghin.apkupdater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/** Every translation must define exactly the keys of the default strings, or a locale silently falls back. */
public class StringsTest {

    private static final Pattern NAME = Pattern.compile("<string name=\"(\\w+)\">([^<]+)</string>");

    private static Set<String> keys(File file) throws IOException {
        String xml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        Set<String> names = new TreeSet<>();
        Matcher m = NAME.matcher(xml);
        while (m.find()) {
            assertTrue(file + ": empty value for " + m.group(1), !m.group(2).trim().isEmpty());
            names.add(m.group(1));
        }
        return names;
    }

    @Test
    public void everyLocaleHasTheSameKeysAsDefault() throws IOException {
        File res = new File("src/main/res");
        Set<String> expected = keys(new File(res, "values/strings.xml"));
        File[] dirs = res.listFiles((d, n) -> n.startsWith("values-"));
        assertTrue("no translations found", dirs != null && dirs.length >= 6);
        for (File dir : dirs) {
            assertEquals(dir.getName(), expected, keys(new File(dir, "strings.xml")));
        }
    }
}
