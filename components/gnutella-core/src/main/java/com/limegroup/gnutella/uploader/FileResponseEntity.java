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

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RouterService;

public class FileResponseEntity extends AbstractHttpNIOEntity {

    private static final Log LOG = LogFactory.getLog(FileResponseEntity.class);
    
    private HTTPUploader uploader;

    private ByteBuffer buffer;

    private long length;

    private long begin;

    private File file;

    private long remaining;

    private FilePieceReader reader;

    private Piece piece;
    
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

//        if (fd != null) {
//            in = fd.createInputStream();
//        } else {
//            in = new BufferedInputStream(new FileInputStream(file));
//        }
//        
//        if (begin > 0) {
//            long skipped = in.skip(begin);
//            if (skipped != begin) {
//                throw new IOException("Could not skip to begin offset: " + skipped + " < " + begin);
//            }
//        }

        uploader.getSession().getIOSession().setThrottle(RouterService
                .getBandwidthManager().getWriteThrottle());

        reader = new FilePieceReader(NIODispatcher.instance().getBufferCache(), file, begin, length, new PieceHandler());
        reader.start();
        
//        buffer = ByteBuffer.allocate(BUFFER_SIZE);
//        // don't write on the first call to handleWrite
//        buffer.limit(0); 
    }
    
    @Override
    public void finished() throws IOException {
//        in.close();
        reader.shutdown();
        
        uploader.getSession().getIOSession().setThrottle(null);
    }
    
    @Override
    public boolean handleWrite() throws IOException {
//        if (buffer == null) {
//            return false;
//        }
//        
//        if (buffer.hasRemaining()) {
//             int written = write(buffer);
//             uploader.addAmountUploaded(written);
//             if (buffer.hasRemaining()) {
//                 return true;
//             } else if (remaining == 0) {
//                 return false;
//             }
//        }
// 
//        buffer.clear();
//        int read = in.read(buffer.array(), 0, (int) Math.min(buffer.remaining(), remaining));
//        if (read == -1) {
//            throw new EOFException("Unexpected end of input stream");
//        }
//        
//        remaining -= read;
//        
//        if (LOG.isTraceEnabled())
//            LOG.debug("Uploading " + fd.getFileName() + " [read=" + read + ",remaining=" + remaining + "]");
//        
//        buffer.limit(read);
//        int written = write(buffer);
//        uploader.addAmountUploaded(written);
//        return remaining > 0 || buffer.hasRemaining();
        
        if (reader == null) {
            return false;
        }
           
        if (buffer != null && buffer.hasRemaining()) {
            int written = write(buffer);
            uploader.addAmountUploaded(written);
            if (buffer.hasRemaining()) {
                return true;
            } else if (remaining == 0) {
                reader.release(piece);
                return false;
            }
        }

        do {
            if (piece != null) {
                reader.release(piece);
            }

            piece = reader.next();
            if (piece == null) {
                buffer = null;
                interest(this, false);
                return true;
            }

            buffer = piece.getBuffer();
            remaining -= buffer.remaining();

            if (LOG.isTraceEnabled())
                LOG.debug("Uploading " + file.getName() + " [read=" + buffer.remaining() + ",remaining=" + remaining + "]");

            int written = write(buffer);
            uploader.addAmountUploaded(written);
        } while (remaining > 0 && !buffer.hasRemaining());
            
        return remaining > 0 || buffer.hasRemaining();
    }
    
    private class PieceHandler implements PieceListener {

        public void readFailed(IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        public void readSuccessful() {
            FileResponseEntity.this.interest(FileResponseEntity.this, true);
        }
        
    }

}