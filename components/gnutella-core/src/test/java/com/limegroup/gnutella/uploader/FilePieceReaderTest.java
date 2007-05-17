package com.limegroup.gnutella.uploader;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import org.limewire.nio.ByteBufferCache;

import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public class FilePieceReaderTest extends LimeTestCase {

    private File file;

    private MyPieceListener listener;

    private byte[] data;

    private int read;

    private FilePieceReader reader;

    public FilePieceReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FilePieceReaderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        listener = new MyPieceListener();
        read = 0;
    }

    @Override
    protected void tearDown() throws Exception {
        if (reader != null) {
            reader.shutdown();
            reader = null;
        }
    }

    public void testRead() throws Exception {
        createFile(50000);

        reader = new FilePieceReader(new ByteBufferCache(), file, 0, (int) file
                .length(), listener);
        reader.start();

        while (read < file.length()) {
            assertGreaterThan(0, listener.waitForNotification());
            Piece piece = reader.next();
            if (read == 0) {
                assertNotNull("Notification should not have been sent", piece);
            }
            while (piece != null) {
                assertEquals(piece.getBuffer().limit(), piece.getLength());
                assertEquals(piece.getBuffer().limit(), piece.getBuffer()
                        .remaining());
                assertEquals(read, piece.getOffset());
                assertEqualsToData(piece);
                read += piece.getBuffer().limit();
                reader.release(piece);
                if (read == file.length()) {
                    break;
                }
                piece = reader.next();
            }
        }
    }

    public void testRelease() throws Exception {
        int filesize = FilePieceReader.BUFFER_SIZE * 3;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        assertEquals(0, cache.getHeapCacheSize());

        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        assertEquals(0, cache.buffers);

        reader.start();

        Piece piece1 = getNext();
        assertGreaterThanOrEquals(1, cache.buffers);
        assertGreaterThanOrEquals(piece1.getLength(), cache.bytes);
        Piece piece2 = getNext();
        Piece piece3 = getNext();
        assertEquals(3, cache.buffers);
        assertEquals(filesize, cache.bytes);
        reader.release(piece1);
        assertEquals(3, cache.buffers);
        reader.release(piece3);
        assertEquals(3, cache.buffers);
        reader.shutdown();
        assertEquals(1, cache.buffers);
        assertEquals(FilePieceReader.BUFFER_SIZE, cache.bytes);
        reader.release(piece2);
        assertEquals(0, cache.buffers);
        assertEquals(0, cache.bytes);
    }

    public void testReleaseVerySmall() throws Exception {
        int filesize = FilePieceReader.BUFFER_SIZE - 1;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        reader.start();
        assertEquals(1, cache.buffers);
    }

    public void testReleaseSmall() throws Exception {
        int filesize = FilePieceReader.BUFFER_SIZE + 1;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        reader.start();
        assertEquals(2, cache.buffers);
    }

    public void testReadTooMuch() throws Exception {
        int filesize = FilePieceReader.BUFFER_SIZE;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        reader.start();
        getNext();
        try {
            Piece piece = getNext();
            fail("Expected EOFException, got: " + piece);
        } catch (EOFException e) {
        }
    }
    
    public void testReadException() throws Exception {
        int filesize = 20000;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        reader.start();
        Piece piece1 = getNext();
        reader.failed(new IOException());
        reader.waitForShutdown(1000);
        assertEquals(1, cache.buffers);
        try {
            Piece piece2 = getNext();
            fail("Expected EOFException, got: " + piece2);
        } catch (EOFException e) {
        }
        assertEquals(1, cache.buffers);
        reader.release(piece1);
        assertEquals(0, cache.buffers);
    }
    
    
    private Piece getNext() throws Exception {
        while (read < file.length()) {
            Piece piece = reader.next();
            if (piece != null) {
                read += piece.getBuffer().limit();
                return piece;
            }
        }
        throw new EOFException();
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

    private void createFile(int size) throws IOException {
        file = File.createTempFile("limewire", "");
        file.deleteOnExit();
        data = LimeTestUtils.writeRandomData(file, size);
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
