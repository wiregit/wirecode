package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import org.limewire.nio.ByteBufferCache;

import com.limegroup.gnutella.util.LimeTestCase;

public class FilePieceReaderTest extends LimeTestCase {

    private File file;

    private MyPieceListener listener;

    private byte[] data;

    public FilePieceReaderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        file = File.createTempFile("limewire", "");
        data = new byte[10000];
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
                assertNotNull(piece);
            }
            while (piece != null) { 
                assertEquals(read, piece.getOffset());
                assertEqualsToData(piece);
                read += piece.getBuffer().remaining();
                System.out.println(read);
                reader.release(piece);
                piece = reader.next();
            }
        }
    }

    private void assertEqualsToData(Piece piece) {
        byte[] content = new byte[piece.getBuffer().remaining()];
        System.arraycopy(data, (int) piece.getOffset(), content, 0, content.length);
        assertEquals(content, piece.getBuffer().array());
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
        
        public synchronized int waitForNotification() throws InterruptedException, IOException {
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
