/**
 * 
 */
package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.http.AbstractHttpNIOEntity;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.Shutdownable;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RouterService;

public class FileResponseEntity extends AbstractHttpNIOEntity implements Shutdownable {

    private static final Log LOG = LogFactory.getLog(FileResponseEntity.class);
    
    private HTTPUploader uploader;

    private ByteBuffer buffer;

    private long length;

    private long begin;

    private File file;

    private long remaining;

    private FilePieceReader reader;

    private Piece piece;
    
    private StalledUploadWatchdog watchdog;
    
    private FileResponseEntity(HTTPUploader uploader) {
        this.uploader = uploader;
        
        setContentType(Constants.FILE_MIME_TYPE);

        begin = uploader.getUploadBegin();
        long end = uploader.getUploadEnd();
        length = end - begin;
        remaining = length;
    }
    
    public FileResponseEntity(HTTPUploader uploader, File file) {
        this(uploader);
        
        this.file = file;
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
        if (reader == null) {
            return false;
        }
           
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
            if (piece != null) {
                reader.release(piece);
            }

            synchronized (this) {
                piece = reader.next();
                if (piece == null) {
                    buffer = null;
                    interest(false);  
                    return true;
                }
            }

            buffer = piece.getBuffer();
            remaining -= buffer.remaining();

            if (LOG.isTraceEnabled())
                LOG.debug("Uploading " + file.getName() + " [read=" + buffer.remaining() + ",remaining=" + remaining + "]");

            written = write(buffer);
            uploader.addAmountUploaded(written);
        } while (written > 0 && remaining > 0 && !buffer.hasRemaining());

        watchdog.activate(this);
        return remaining > 0 || buffer.hasRemaining();
    }

    public synchronized void interest(boolean status) {
        interest(this, status);
    }

    @Override
    public void shutdown() {
        if (LOG.isWarnEnabled())
            LOG.warn("File transfer timed out: " + uploader);
        uploader.stop();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [file=" + file.getName() + ",read=" + buffer.remaining() + ",remaining=" + remaining + "]"; 
    }
    
    private class PieceHandler implements PieceListener {

        public void readFailed(IOException e) {
            if (LOG.isWarnEnabled())
                LOG.warn("Error reading file from disk: " + uploader, e);
            uploader.stop();
        }

        public void readSuccessful() {
            FileResponseEntity.this.interest(true);
        }

    }
    
}