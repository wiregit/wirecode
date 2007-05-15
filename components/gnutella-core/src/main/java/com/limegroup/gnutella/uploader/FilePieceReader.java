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

    /**
     * The number of concurrent threads used to read pieces of the file.
     */
    private static final int THREAD_COUNT = 2;

    /**
     * The number of buffers used to cache pieces. 
     */
    private static final int MAX_BUFFERS = THREAD_COUNT * 2;

    /**
     * The size of a single piece.
     */
    private static int BUFFER_SIZE = 4096;

    /**
     * The list of buffers available for reading.
     * <p>
     * Note: Obtain <code>bufferPoolLock</code> when accessing.
     */
    private final LinkedList<ByteBuffer> bufferPool = new LinkedList<ByteBuffer>();

    /**
     * The list of cached pieces that have been read already sorted in ascending
     * order by file offset.
     * <p>
     * Note: Obtain <code>this</code> lock when accessing.
     */
    private final Queue<Piece> pieceQueue = new PriorityQueue<Piece>(
            MAX_BUFFERS);

//    private final ExecutorService QUEUE = ExecutorsHelper
//            .newProcessingQueue("DiskPieceReader");

    /**
     * Single queue that is used for all readers.
     */
    private final static ExecutorService QUEUE =
        ExecutorsHelper.newFixedSizeThreadPool(THREAD_COUNT, "DiskPieceReader");

    private final File file;

    private final PieceListener listener;

    private final FileChannel channel;

    private final RandomAccessFile raf;

    private final Object bufferPoolLock = new Object();

    private final ByteBufferCache bufferCache;

    /**
     * Number of buffers currently in use by jobs. 
     */
    private volatile int bufferCount;

    /**
     * The offset of the next piece that is going to be returned by
     * {@link #next()}. This field keeps track of the pieces read by the
     * consumer of this reader.
     */
    private volatile long readOffset;

    /**
     * The offset of the next piece that is going to be processed by a job. This
     * field keeps track of how far the file has been read from disk.
     */
    private long processingOffset;

    /**
     * The remaining number of bytes to read.
     */
    private long remaining;

    /**
     * If true, the reader has been shutdown.
     */
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

    /**
     * Invoked when data at <code>offset</code> has been read.
     */
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

    /**
     * Returns the file that is being read.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns true, if a {@link Piece} is available that can be retrieved
     * through {@link #next()}.
     */
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
        
        synchronized (bufferPoolLock) {
            bufferPool.add(buffer);
            bufferCount--;
        }
        spawnJobs();
    }

    public void release(Piece piece) {
        release(piece.getBuffer());
    }

    /**
     * Releases all resources and shuts down the processing queue that reads the
     * file.
     * <p>
     * Once the reader is shutdown it is not possible to restart it.
     */
    public void shutdown() {
        shutdown = true;
        
        synchronized (bufferPoolLock) {
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

    /**
     * Starts the processing queue that reads the file. To free resources
     * {@link #shutdown()} must be invoked when reading has been completed.
     */
    public void start() {
        assert !shutdown;
        
        spawnJobs();
    }

    /**
     * Spawns additional reader jobs if buffers are available for reading.
     */
    private void spawnJobs() {       
        synchronized (bufferPoolLock) {
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

    /**
     * A simple job that reads a chunk of data form a file channel.
     */
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
