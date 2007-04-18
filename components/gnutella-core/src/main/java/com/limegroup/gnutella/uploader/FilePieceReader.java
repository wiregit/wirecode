package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.nio.ByteBufferCache;

public class FilePieceReader implements PieceReader {

    private static final Log LOG = LogFactory.getLog(FilePieceReader.class);

    private static final int MAX_BUFFERS = 3;

    //private static final int JOB_THRESHOLD = 3;
    
    private static int BUFFER_SIZE = 8192;

    private Queue<Piece> pieceQueue = new PriorityQueue<Piece>(MAX_BUFFERS); 
    
    private final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("DiskPieceReader");

    private File file;

    private long readOffset;

    private long processingOffset;

    private long remaining;

    private PieceListener listener;

    private boolean running;
    
    private volatile int bufferCount;

    private ByteBufferCache bufferCache;
    
    public FilePieceReader(ByteBufferCache bufferCache, File file, long offset, long length, PieceListener listener) {
        this.bufferCache = bufferCache;
        this.file = file;
        this.readOffset = offset;
        this.processingOffset = offset;
        this.remaining = length;
        this.listener = listener;
    }
    
    private synchronized void add(long offset, ByteBuffer buffer) {
        assert offset >= readOffset;
        
        Piece piece = new Piece(offset, buffer);       
        pieceQueue.add(piece);
        if (offset == readOffset) {
            listener.readSuccessful();
        }
        spawnJobs();
    }

    public void failed(IOException exception) {
        shutdown();
        listener.readFailed(exception);
    }

    public File getFile() {
        return file;
    }

    public synchronized boolean hasNext() {
        if (pieceQueue.isEmpty()) {
            return false;
        }
        return pieceQueue.peek().getOffset() == readOffset;
    }
    
    public synchronized Piece next() {
        if (hasNext()) {
            Piece piece = pieceQueue.remove();
            assert piece.getOffset() == readOffset;
            readOffset += piece.getBuffer().remaining();
            return piece;
        }
        return null;
    }

    public void release(Piece piece) {
        bufferCache.release(piece.getBuffer());
        bufferCount--;
        spawnJobs();
    }

    public void resume() {
        this.running = true;
    }

    public void shutdown() {
        QUEUE.shutdown();
    }

    public void suspend() {
        this.running = false;
    }

    public void start() {
        spawnJobs();
    }
    
    private synchronized void spawnJobs() {
        while (remaining > 0 && bufferCount < MAX_BUFFERS) {
            int length = (int) Math.min(BUFFER_SIZE, remaining);
            ByteBuffer buffer = ByteBuffer.allocate(length);
            bufferCount++;
            PieceReaderJob job = new PieceReaderJob(buffer, processingOffset, length);
            processingOffset += length;
            remaining -= length;
            QUEUE.submit(job);
        }
    }
    
    private class PieceReaderJob implements Runnable {

        private ByteBuffer buffer;
        private long offset;
        private int length;

        public PieceReaderJob(ByteBuffer buffer, long offset, int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }
        
        public void run() {
            buffer.clear();
            buffer.limit(length);
            
            IOException exception = null;
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(FilePieceReader.this.getFile(), "r");
                raf.seek(offset);
                raf.readFully(buffer.array(), 0, length);
            } catch (IOException e) {
                exception = e;
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        LOG.error("Could not close file: ", e);
                    }   
                }
            }
            
            if (exception != null) {
                FilePieceReader.this.failed(exception);
            } else {
                FilePieceReader.this.add(offset, buffer);
            }
        }
        
    }
    
}
