package org.limewire.http.entity;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.limewire.http.HttpNIOEntity;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.Shutdownable;

/**
 * An event based {@link HttpEntity} that uploads a {@link File}. A
 * corresponding {@link FileTransfer} is updated with progress.
 */
public class FileNIOEntity extends FileEntity implements HttpNIOEntity, Shutdownable {

    private static final Log LOG = LogFactory.getLog(FileNIOEntity.class);
    
    private final FileTransfer transfer;

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
//    private StalledUploadWatchdog watchdog;

    private IOControl ioctrl;
    
    public FileNIOEntity(File file, String contentType, FileTransfer transfer, long beginIndex, long length) {
        super(file, contentType);

        this.transfer = transfer;
        this.file = file;
        this.begin = beginIndex;
        this.length = length;
        this.remaining = length;
    }
    
    public FileNIOEntity(File file, String contentType, FileTransfer transfer) {
        this(file, contentType, transfer, 0, file.length());
    }
    
    @Override
    public long getContentLength() {
        return length;
    }

    public void initializeReader() throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Initializing upload of " + file.getName() + " [begin=" + begin + ",length=" + length + "]");

//        watchdog = new StalledUploadWatchdog();

        transfer.start();

        reader = new FilePieceReader(NIODispatcher.instance().getBufferCache(), file, begin, length, new PieceHandler());
        reader.start();
    }

    public void initializeWriter() throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Initializing download of " + file.getName() + " [begin=" + begin + ",length=" + length + "]");

//        watchdog = new StalledUploadWatchdog();

        transfer.start();

        reader = new FilePieceReader(NIODispatcher.instance().getBufferCache(), file, begin, length, new PieceHandler());
        reader.start();
    }

    public void finished() {
//        if (watchdog != null) {
//            watchdog.deactivate();
//            watchdog = null;
//        }
        if (reader != null) {
            reader.shutdown();
            reader = null;
        }
        
        ioctrl = null;
    }
    
    public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
        if (this.ioctrl == null) {
            this.ioctrl = ioctrl;

            initializeReader();
        }

        // flush current buffer
        if (buffer != null && buffer.hasRemaining()) {
            int written = encoder.write(buffer);
            transfer.addAmountUploaded(written);
            if (buffer.hasRemaining()) {
//                watchdog.activate(this);
                return;
            } else if (remaining == 0) {
                reader.release(piece);
                encoder.complete();
                return;
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
                        ioctrl.suspendInput();
//                        watchdog.activate(this);
                        return;
                    }
                    buffer = piece.getBuffer();
                    remaining -= buffer.remaining();
                }
            }
            
            if (LOG.isTraceEnabled())
                LOG.trace("Uploading " + file.getName() + " [read=" + buffer.remaining() + ",remaining=" + remaining + "]");

            written = encoder.write(buffer);
            transfer.addAmountUploaded(written);
        } while (written > 0 && remaining > 0);

        if (remaining == 0 && !buffer.hasRemaining()) {
            encoder.complete();
        } else {
//            watchdog.activate(this);
        }
    }

    public int consumeContent(ContentDecoder decoder, IOControl ioctrl)
        throws IOException {
        //      TODO Auto-generated method stub
        return 0;
    }
    
    public void shutdown() {
        if (LOG.isWarnEnabled())
            LOG.warn("File transfer timed out: " + transfer);
        transfer.stop();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [file=" + file.getName() + "]"; 
    }
    
    private class PieceHandler implements PieceListener {

        public void readFailed(IOException e) {
            if (LOG.isWarnEnabled())
                LOG.warn("Error reading file from disk: " + transfer, e);
            transfer.stop();
        }

        public void readSuccessful() {
            ioctrl.requestOutput();
        }

    }

}
