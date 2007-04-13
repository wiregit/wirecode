/**
 * 
 */
package com.limegroup.gnutella.uploader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.http.AbstractHttpNIOEntity;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;

public class FileResponseEntity extends AbstractHttpNIOEntity {

    private static final Log LOG = LogFactory.getLog(FileResponseEntity.class);
    
    private HTTPUploader uploader;

    private FileDesc fd;

    private ByteBuffer buffer;

    private long length;

    private long begin;

    private InputStream in;

    private File file;

    private long remaining;

    private static int BUFFER_SIZE = 2048;

    private FileResponseEntity(HTTPUploader uploader) {
        this.uploader = uploader;
        
        setContentType(Constants.FILE_MIME_TYPE);

        begin = uploader.getUploadBegin();
        long end = uploader.getUploadEnd();
        length = end - begin;
        remaining = length;
    }
    
    public FileResponseEntity(HTTPUploader uploader, FileDesc fd) {
        this(uploader);
        
        this.fd = fd;
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
            LOG.debug("Initializing upload of " + fd.getFileName() + " [begin=" + begin + ",length=" + length + "]");

        if (fd != null) {
            in = fd.createInputStream();
        } else {
            in = new BufferedInputStream(new FileInputStream(file));
        }
        
        if (begin > 0) {
            long skipped = in.skip(begin);
            if (skipped != begin) {
                throw new IOException("Could not skip to begin offset: " + skipped + " < " + begin);
            }
        }

        uploader.getSession().getIOSession().setThrottle(RouterService
                .getBandwidthManager().getWriteThrottle());
        
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
        // don't write on the first call to handleWrite
        buffer.limit(0); 
    }
    
    @Override
    public void finished() throws IOException {
        in.close();
        
        uploader.getSession().getIOSession().setThrottle(null);
    }
    
    @Override
    public boolean handleWrite() throws IOException {
        if (buffer == null) {
            return false;
        }
        
        if (buffer.hasRemaining()) {
             int written = write(buffer);
             uploader.addAmountUploaded(written);
             if (buffer.hasRemaining()) {
                 return true;
             } else if (remaining == 0) {
                 return false;
             }
        }
 
        buffer.clear();
        int read = in.read(buffer.array(), 0, (int) Math.min(buffer.remaining(), remaining));
        remaining -= read;
        
        if (LOG.isTraceEnabled())
            LOG.debug("Uploading " + fd.getFileName() + " [read=" + read + ",remaining=" + remaining + "]");
        
        buffer.limit(read);
        int written = write(buffer);
        uploader.addAmountUploaded(written);
        return remaining > 0 || buffer.hasRemaining();
    }

}