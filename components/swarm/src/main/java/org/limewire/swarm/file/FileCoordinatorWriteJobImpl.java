package org.limewire.swarm.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.FileContentDecoder;
import org.apache.http.nio.IOControl;
import org.limewire.collection.Range;
import org.limewire.nio.ByteBufferCache;
import org.limewire.util.BufferUtils;

class FileCoordinatorWriteJobImpl implements WriteJob {
    
    private static final Log LOG = LogFactory.getLog(FileCoordinatorWriteJobImpl.class);
    
    private static final int BUFFER_SIZE = 8192;
    
    private final IOControl ioctrl;
    private final ExecutorService jobScheduler;
    private final FileCoordinator fileCoordinator;
    private final ByteBufferCache byteBufferCache;
    private final SwarmFile fileWriter;
    private final Object scheduleLock = new Object();
    
    private long startPosition;
    private ByteBuffer buffer;    
    private Future<Void> scheduledJob;
    
    public FileCoordinatorWriteJobImpl(long position, IOControl ioctrl,
            ExecutorService jobScheduler, FileCoordinator fileCoordinator,
            ByteBufferCache byteBufferCache, SwarmFile fileWriter) {
        this.startPosition = position;
        this.ioctrl = ioctrl;
        this.byteBufferCache = byteBufferCache;
        this.jobScheduler = jobScheduler;
        this.fileCoordinator = fileCoordinator;
        this.fileWriter = fileWriter;
    }
    
    public void cancel() {
        synchronized(scheduleLock) {
            LOG.debug("Cancelling Write Job");
            if(scheduledJob != null) {
                scheduledJob.cancel(false);
                scheduledJob = null;
            }
            
            if(hasPendingData()) {
                fileCoordinator.unpending(Range.createRange(startPosition, startPosition + buffer.remaining() - 1));
                byteBufferCache.release(buffer);
                buffer = null;
                startPosition = -1;
            }
        }
    }
    
    public long consumeContent(ContentDecoder decoder) throws IOException {
        synchronized(scheduleLock) {
            if(buffer == null) {
                buffer = byteBufferCache.get(BUFFER_SIZE);
                assert buffer.position() == 0;
            }
            
            long priorLength = buffer.position();            
            long read = decoder.read(buffer);
            if(LOG.isTraceEnabled())
                LOG.trace("Read: " + read + " from decoder");
            
            if(read == -1)
                throw new IOException("Read EOF");
            
            if(read > 0) {
                assert buffer.position() > 0;
                long low = startPosition + priorLength;
                if(LOG.isTraceEnabled())
                    LOG.trace("Marking: [" + low + ", " + (low+read-1) + "] as pending");
                fileCoordinator.pending(Range.createRange(low, low + read - 1));
            }
            
            if(buffer.remaining() == 0) {
                LOG.trace("No space remaining in buffer, suspending I/O.");
                // Suspend both input & output, so that we don't send another
                // request before we can read its response.
                ioctrl.suspendInput();
                ioctrl.suspendOutput();
            } 
            
            if(buffer.position() > 0 && (scheduledJob == null || scheduledJob.isDone())) {
                LOG.trace("Scheduling new job...");
                scheduledJob = jobScheduler.submit(new Callable<Void>() {
                    public Void call() throws Exception {
                        writeData();
                        return null;
                    }
                });
            }
            
            return read;
        }
    }
    
    private boolean hasPendingData() {
        return startPosition != -1 && buffer != null && buffer.position() > 0;
    }
    
    /**
     * Writes data from the buffer into a temporary buffer,
     * and then to disk.  This roundabout way of writing
     * is required to ensure that no shared lock is used
     * both during disk & network I/O.  (Disk I/O is inherently
     * blocking, and can stall network I/O otherwise.)
     */
    private void writeData() throws IOException {
        long position;
        ByteBuffer tempBuffer;
        
        for(;;) {
            synchronized(scheduleLock) {
                if(!hasPendingData()) {
                    scheduledJob = null;
                    return;
                }

                tempBuffer = byteBufferCache.get(BUFFER_SIZE);
                assert tempBuffer.position() == 0;
                
                buffer.flip();
                tempBuffer.put(buffer);
                assert !buffer.hasRemaining();
                byteBufferCache.release(buffer);
                buffer = null;
                position = startPosition;
                startPosition += tempBuffer.position();
                
                // Request I/O again ASAP.
                LOG.trace("Re-requesting I/O");
                ioctrl.requestInput();
                ioctrl.requestOutput();
            }
             
            tempBuffer.flip();
            try {
                writeImpl(tempBuffer, position);
                assert !tempBuffer.hasRemaining();
            } finally {
                byteBufferCache.release(tempBuffer);
            }
        }
    }
    
    private long writeImpl(ByteBuffer dataBuffer, long position) throws IOException {
        long totalWrote = 0;
        while(dataBuffer.hasRemaining()) {
            try {
                if(LOG.isTraceEnabled())
                    LOG.trace("Writing from: " + dataBuffer + ", starting at: " + position);
                long wrote = fileWriter.transferFrom(new BufferDecoder(dataBuffer), position);
                fileCoordinator.wrote(Range.createRange(position, position + wrote -1 ));
                position += wrote;
                totalWrote += wrote;
            } catch(IOException iox) {
                LOG.debug("Exception writing", iox);
                fileCoordinator.unpending(Range.createRange(position, dataBuffer.limit() + position - 1));
                ioctrl.shutdown();
                throw iox;
            } catch(RuntimeException re) {
                LOG.debug("Unhandled error writing", re);
                fileCoordinator.unpending(Range.createRange(position, dataBuffer.limit() + position - 1));
                ioctrl.shutdown();
                throw re;
            }
        }
        return totalWrote;
    }
    
    private static class BufferDecoder implements FileContentDecoder {
        private final ByteBuffer buffer;
        
        public BufferDecoder(ByteBuffer buffer) {
            this.buffer = buffer;
        }
        
        public boolean isCompleted() {
            throw new UnsupportedOperationException();
        }
        
        public int read(ByteBuffer dst) throws IOException {
            return BufferUtils.transfer(buffer, dst, false);
        }
        
        public long transfer(FileChannel dst, long position, long count) throws IOException {
            int oldLimit = buffer.limit();
            int newCount = (int)Math.min(Integer.MAX_VALUE, count);
            if(newCount < buffer.remaining()) {
                // If we want to transfer less than is available,
                // change the buffer limit to be only what we want.
                buffer.limit(buffer.position() + newCount);
            }
            long wrote = dst.write(buffer, position);
            buffer.limit(oldLimit);
            return wrote;
        }
    }
}