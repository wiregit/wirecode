package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.limewire.http.AbstractHttpNIOEntity;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.Shutdownable;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RouterService;

/**
 * An event based {@link HttpEntity} that uploads a {@link File}. A
 * corresponding {@link HTTPUploader} is updated with progress.
 */
public class FileResponseEntity extends AbstractHttpNIOEntity implements Shutdownable {

    private static final Log LOG = LogFactory.getLog(FileResponseEntity.class);
    
    private final HTTPUploader uploader;

    private final File file;

    /** Buffer that is currently transferred. */
    private ByteBuffer buffer;

    /** Total number of bytes to transfer. */
    private long length;

    /** Offset of the first byte. */
    private long begin;

    /** Number of bytes remaining to be read from disk. */
    private long remaining;

    private FilePieceReader reader;

    /** Piece that is currently transferred. */
    private Piece piece;

    /** Cancels the transfer if inactivity for too long. */  
    private StalledUploadWatchdog watchdog;
    
    public FileResponseEntity(HTTPUploader uploader, File file) {
        this.uploader = uploader;
        this.file = file;

        setContentType(Constants.FILE_MIME_TYPE);

        begin = uploader.getUploadBegin();
        long end = uploader.getUploadEnd();
        length = end - begin;
        remaining = length;
    }
    
    @Override
    public long getContentLength() {
        return length;
    }

    @Override
    public void initialize() throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Initializing upload of " + file.getName() + " [begin=" + begin + ",length=" + length + "]");

        watchdog = new StalledUploadWatchdog();
        
        uploader.getSession().getIOSession().setThrottle(RouterService
                .getBandwidthManager().getWriteThrottle(uploader.getSession().getIOSession().getSocket()));

        reader = new FilePieceReader(NIODispatcher.instance().getBufferCache(), file, begin, length, new PieceHandler());
        reader.start();
    }
    
    @Override
    public void finished() {
        if (watchdog != null) {
            watchdog.deactivate();
        }
        if (reader != null) {
            reader.shutdown();
        }
    }
    
    @Override
    public boolean handleWrite() throws IOException {
        // flush current buffer
        if (buffer != null && buffer.hasRemaining()) {
            int written = write(buffer);
            uploader.addAmountUploaded(written);
            if (buffer.hasRemaining()) {
                watchdog.activate(this);
                return true;
            } else if (remaining == 0) {
                reader.release(piece);
                return false;
            }
        }

        int written;
        do {
            if (buffer == null || !buffer.hasRemaining()) {
                if (piece != null) {
                    reader.release(piece);
                }

                // get next piece from file
                synchronized (this) {
                    piece = reader.next();
                    if (piece == null) {
                        // need to wait for the disk, PieceHandler will turn
                        // interest back on when the next piece is available
                        buffer = null;
                        interestWrite(false);
                        watchdog.activate(this);
                        return true;
                    }
                    buffer = piece.getBuffer();
                    remaining -= buffer.remaining();
                }
            }
            
            if (LOG.isTraceEnabled())
                LOG.debug("Uploading " + file.getName() + " [read=" + buffer.remaining() + ",remaining=" + remaining + "]");

            written = write(buffer);
            uploader.addAmountUploaded(written);
        } while (written > 0 && remaining > 0);

        watchdog.activate(this);
        return remaining > 0 || buffer.hasRemaining();
    }

    private synchronized void interestWrite(boolean status) {
        interestWrite(this, status);
    }

    @Override
    public void shutdown() {
        if (LOG.isWarnEnabled())
            LOG.warn("File transfer timed out: " + uploader);
        uploader.stop();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [file=" + file.getName() + "]"; 
    }
    
    private class PieceHandler implements PieceListener {

        public void readFailed(IOException e) {
            if (LOG.isWarnEnabled())
                LOG.warn("Error reading file from disk: " + uploader, e);
            uploader.stop();
        }

        public void readSuccessful() {
            FileResponseEntity.this.interestWrite(true);
        }

    }
    
}