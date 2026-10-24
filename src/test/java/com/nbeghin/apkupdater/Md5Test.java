package com.nbeghin.apkupdater;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class Md5Test {

    @Test
    public void knownVectors() throws Exception {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Md5.hex(new ByteArrayInputStream(new byte[0])));
        assertEquals("900150983cd24fb0d6963f7d28e17f72",
                Md5.hex(new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII))));
    }

    @Test
    public void largerThanTheBuffer() throws Exception {
        byte[] data = new byte[20000]; // crosses the 8192-byte read boundary twice
        assertEquals("b2fe457348de243a579a1591d3815170", Md5.hex(new ByteArrayInputStream(data))); // md5sum of 20000 zero bytes
    }
}
