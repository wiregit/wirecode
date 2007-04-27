package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import junit.framework.Test;

import org.limewire.nio.ByteBufferCache;

import com.limegroup.gnutella.util.LimeTestCase;

public class FilePieceReaderTest extends LimeTestCase {

    private File file;

    private MyPieceListener listener;

    private byte[] data;

    public FilePieceReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FilePieceReaderTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        file = File.createTempFile("limewire", "");
        data = new byte[50000];
        Random r = new Random();
        r.nextBytes(data);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.write(data);
        raf.close();

        listener = new MyPieceListener();
    }

    public void testRead() throws Exception {
        FilePieceReader reader = new FilePieceReader(new ByteBufferCache(),
                file, 0, (int) file.length(), listener);
        reader.start();

        int read = 0;
        while (read < file.length()) {
            assertGreaterThan(0, listener.waitForNotification());
            Piece piece = reader.next();
            if (read == 0) {
                assertNotNull("Notification should not have been sent", piece);
            }
            while (piece != null) {
                assertEquals(piece.getBuffer().limit(), piece.getLength());
                assertEquals(piece.getBuffer().limit(), piece.getBuffer().remaining());
                assertEquals(read, piece.getOffset());
                assertEqualsToData(piece);
                read += piece.getBuffer().limit();
                reader.release(piece);
                piece = reader.next();
            }
        }
    }

    private void assertEqualsToData(Piece piece) {
        byte[] expectedContent = new byte[piece.getBuffer().remaining()];
        System.arraycopy(data, (int) piece.getOffset(), expectedContent, 0,
                expectedContent.length);
        byte[] readContent = new byte[piece.getBuffer().remaining()];
        piece.getBuffer().get(readContent);
        assertEquals("Unexpected data in piece at offset: " + piece.getOffset()
                + ", length: " + expectedContent.length, //
                expectedContent, readContent);
    }

    private class MyPieceListener implements PieceListener {

        // private List<Piece> read = new ArrayList<Piece>();
        IOException exception;

        int notificationCount;

        public synchronized void readSuccessful() {
            notificationCount++;
            this.notify();
        }

        public synchronized void readFailed(IOException e) {
            exception = e;
            this.notify();
        }

        public synchronized int waitForNotification()
                throws InterruptedException, IOException {
            while (notificationCount == 0 && exception == null) {
                this.wait();
            }
            if (exception != null) {
                throw exception;
            }
            return notificationCount--;
        }

    }

}
