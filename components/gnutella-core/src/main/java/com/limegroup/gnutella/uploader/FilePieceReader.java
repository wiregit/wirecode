package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.nio.ByteBufferCache;

/**
 * Reads chunks from a file into ByteBuffers.  
 */
public class FilePieceReader implements PieceReader {

    private static final Log LOG = LogFactory.getLog(FilePieceReader.class);

    private static final int THREAD_COUNT = 2;

    private static final int MAX_BUFFERS = THREAD_COUNT * 2;

    private static int BUFFER_SIZE = 4096;

    private final LinkedList<ByteBuffer> bufferPool = new LinkedList<ByteBuffer>();

    private final Queue<Piece> pieceQueue = new PriorityQueue<Piece>(
            MAX_BUFFERS);

//    private final ExecutorService QUEUE = ExecutorsHelper
//            .newProcessingQueue("DiskPieceReader");

    private final static ExecutorService QUEUE =
        ExecutorsHelper.newFixedSizeThreadPool(THREAD_COUNT, "DiskPieceReader");

    private final File file;

    private final PieceListener listener;

    private final FileChannel channel;

    private final RandomAccessFile raf;

    private final Object bufferLock = new Object();

    private final ByteBufferCache bufferCache;

    private volatile int bufferCount;

    private volatile long readOffset;

    private long processingOffset;

    private long remaining;

    private boolean shutdown;

    public FilePieceReader(ByteBufferCache bufferCache, File file, long offset,
            long length, PieceListener listener) throws IOException {
        if (bufferCache == null || file == null || listener == null) {
            throw new IllegalArgumentException();
        }

        this.bufferCache = bufferCache;
        this.file = file;
        this.readOffset = offset;
        this.processingOffset = offset;
        this.remaining = length;
        this.listener = listener;

        for (int i = 0; i < MAX_BUFFERS; i++) {
            bufferPool.add(bufferCache.getHeap(BUFFER_SIZE));
        }

        raf = new RandomAccessFile(file, "r");
        channel = raf.getChannel();
    }

    private void add(long offset, ByteBuffer buffer) {
        if (shutdown) {
            release(buffer);
            return;
        }
        
        assert offset >= readOffset;

        Piece piece = new Piece(offset, buffer);
        synchronized (this) {
            pieceQueue.add(piece);
        }

        if (offset == readOffset) {
            listener.readSuccessful();
        }
    }

    private void failed(IOException exception) {
        shutdown();
        listener.readFailed(exception);
    }

    public File getFile() {
        return file;
    }

    public synchronized boolean hasNext() {
        assert !shutdown;
        
        if (pieceQueue.isEmpty()) {
            return false;
        }
        return pieceQueue.peek().getOffset() == readOffset;
    }

    public synchronized Piece next() {
        assert !shutdown;
        
        Piece piece = pieceQueue.peek();
        if (piece != null && piece.getOffset() == readOffset) {
            pieceQueue.remove();
            readOffset += piece.getBuffer().remaining();
            return piece;
        }
        return null;
    }

    private void release(ByteBuffer buffer) {
        if (shutdown) {
            bufferCache.release(buffer);
            return;
        }
        
        synchronized (bufferLock) {
            bufferPool.add(buffer);
            bufferCount--;
        }
        spawnJobs();
    }

    public void release(Piece piece) {
        release(piece.getBuffer());
    }

    public void shutdown() {
        shutdown = true;
        
        synchronized (bufferLock) {
            for (ByteBuffer buffer : bufferPool) {
                release(buffer);
            }
        }
        
        try {
            channel.close();
        } catch (IOException e) {
            LOG.warn("Error closing channel for file: " + file, e);
        }
        try {
            raf.close();
        } catch (IOException e) {
            LOG.warn("Error closing file: " + file, e);
        }
    }

    public void start() {
        assert !shutdown;
        
        spawnJobs();
    }

    private void spawnJobs() {       
        synchronized (bufferLock) {
            while (!shutdown && remaining > 0 && bufferCount < MAX_BUFFERS) {
                int length = (int) Math.min(BUFFER_SIZE, remaining);
                ByteBuffer buffer = bufferPool.remove();
                bufferCount++;
                Runnable job = new PieceReaderJob2(buffer, processingOffset,
                        length);
                processingOffset += length;
                remaining -= length;
                QUEUE.submit(job);
            }
        }
    }

//    private class PieceReaderJob implements Runnable {
//
//        private ByteBuffer buffer;
//
//        private long offset;
//
//        private int length;
//
//        public PieceReaderJob(ByteBuffer buffer, long offset, int length) {
//            this.buffer = buffer;
//            this.offset = offset;
//            this.length = length;
//        }
//
//        public void run() {
//            buffer.clear();
//            buffer.limit(length);
//
//            IOException exception = null;
//            RandomAccessFile raf = null;
//            try {
//                raf = new RandomAccessFile(FilePieceReader.this.getFile(), "r");
//                raf.seek(offset);
//                raf.readFully(buffer.array(), 0, length);
//            } catch (IOException e) {
//                exception = e;
//            } finally {
//                if (raf != null) {
//                    try {
//                        raf.close();
//                    } catch (IOException e) {
//                        LOG.error("Could not close file: ", e);
//                    }
//                }
//            }
//
//            if (exception != null) {
//                FilePieceReader.this.failed(exception);
//            } else {
//                FilePieceReader.this.add(offset, buffer);
//            }
//        }
//
//    }

    private class PieceReaderJob2 implements Runnable {

        private ByteBuffer buffer;

        private long offset;

        private int length;

        public PieceReaderJob2(ByteBuffer buffer, long offset, int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }

        public void run() {
            buffer.clear();
            buffer.limit(length);

            IOException exception = null;
            try {
                while (buffer.hasRemaining()) {
                    channel.read(buffer, offset + buffer.position());
                }
            } catch (IOException e) {
                exception = e;
            }

            if (exception != null) {
                FilePieceReader.this.failed(exception);
            } else {
                buffer.flip();
                assert buffer.remaining() == length;

                FilePieceReader.this.add(offset, buffer);
            }
        }

    }

}
