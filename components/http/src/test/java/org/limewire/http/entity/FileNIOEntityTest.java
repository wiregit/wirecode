package org.limewire.http.entity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.limewire.http.HttpTestUtils;

public class FileNIOEntityTest extends TestCase {

    private static final String ALPHABET = "abcdefghijklmonpqrstuvwxyz";

    public void testGetContentLength() throws IOException {
        File file = File.createTempFile("lime", null);
        FileNIOEntity entity = new FileNIOEntity(file, "content-type", new FileTransferMonitorAdapter());
        assertEquals(0, entity.getContentLength());
        assertEquals(file, entity.getFile());
        
        HttpTestUtils.writeData(file, ALPHABET);
        entity = new FileNIOEntity(file, "content-type", new FileTransferMonitorAdapter());
        assertEquals(26, entity.getContentLength());
        assertEquals(file, entity.getFile());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        assertEquals(ALPHABET, new String(out.toByteArray()));
    }

    public void testGetFile() throws Exception {
        File file = File.createTempFile("lime", null);
        HttpTestUtils.writeData(file, ALPHABET);
        FileNIOEntity entity = new FileNIOEntity(file, "content-type", new FileTransferMonitorAdapter(), 2, 3);
        assertEquals(3, entity.getContentLength());
        assertEquals(file, entity.getFile());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        assertEquals("cde", new String(out.toByteArray()));
    }

}
