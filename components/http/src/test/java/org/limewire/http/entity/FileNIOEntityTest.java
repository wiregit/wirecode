package org.limewire.http.entity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import junit.framework.TestCase;

import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.nio.codecs.IdentityEncoder;
import org.apache.http.impl.nio.reactor.SessionOutputBuffer;
import org.limewire.http.HttpTestUtils;
import org.limewire.http.MockIOControl;

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
        FileNIOEntity entity = new FileNIOEntity(file, "content-type", new FileTransferMonitorAdapter());
        assertEquals(26, entity.getContentLength());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        assertEquals(ALPHABET, new String(out.toByteArray()));
    }

    public void testGetFileNIO() throws Exception {
        File file = File.createTempFile("lime", null);
        HttpTestUtils.writeData(file, ALPHABET);
        FileNIOEntity entity = new FileNIOEntity(file, "content-type", new FileTransferMonitorAdapter());
        assertEquals(26, entity.getContentLength());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel(out);
        IdentityEncoder encoder = new IdentityEncoder(channel, new SessionOutputBuffer(0, 0), new HttpTransportMetricsImpl());
        readAllNIO(entity, encoder);
        assertTrue(encoder.isCompleted());
        assertEquals(ALPHABET, new String(out.toByteArray()));
    }

    public void testGetFileRange() throws Exception {
        File file = File.createTempFile("lime", null);
        HttpTestUtils.writeData(file, ALPHABET);
        FileNIOEntity entity = new FileNIOEntity(file, "content-type", new FileTransferMonitorAdapter(), 2, 3);
        assertEquals(3, entity.getContentLength());
        assertEquals(file, entity.getFile());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        assertEquals("cde", new String(out.toByteArray()));
    }

    public void testGetFileRangeNIO() throws Exception {
        File file = File.createTempFile("lime", null);
        HttpTestUtils.writeData(file, ALPHABET);
        FileNIOEntity entity = new FileNIOEntity(file, "content-type", new FileTransferMonitorAdapter(), 2, 3);
        assertEquals(3, entity.getContentLength());
        assertEquals(file, entity.getFile());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel(out);
        IdentityEncoder encoder = new IdentityEncoder(channel, new SessionOutputBuffer(0, 0), new HttpTransportMetricsImpl());
        readAllNIO(entity, encoder);
        assertTrue(encoder.isCompleted());
        assertEquals("cde", new String(out.toByteArray()));
    }

    private void readAllNIO(FileNIOEntity entity, IdentityEncoder encoder)
            throws IOException, InterruptedException {
        MockIOControl control = new MockIOControl();
        long startTime = System.currentTimeMillis();
        while (!encoder.isCompleted() && startTime + 1000 > System.currentTimeMillis()) {
            entity.produceContent(encoder, control);
            if (!control.outputRequested) {
                Thread.sleep(50);
            }
        }
        entity.finished();
    }

    public void testGetEmptyFile() throws Exception {
        File file = File.createTempFile("lime", null);
        FileNIOEntity entity = new FileNIOEntity(file, "content-type", new FileTransferMonitorAdapter());
        assertEquals(0, entity.getContentLength());
        assertEquals(file, entity.getFile());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        assertEquals("", new String(out.toByteArray()));
    }

    public void testGetEmptyFileNIO() throws Exception {
        File file = File.createTempFile("lime", null);
        FileNIOEntity entity = new FileNIOEntity(file, "content-type", new FileTransferMonitorAdapter());
        assertEquals(0, entity.getContentLength());
        assertEquals(file, entity.getFile());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel(out);
        IdentityEncoder encoder = new IdentityEncoder(channel, new SessionOutputBuffer(0, 0), new HttpTransportMetricsImpl());
        entity.produceContent(encoder, new MockIOControl());
        assertTrue(encoder.isCompleted());
        assertEquals("", new String(out.toByteArray()));
        entity.finished();
        assertEquals("", new String(out.toByteArray()));
    }

}
