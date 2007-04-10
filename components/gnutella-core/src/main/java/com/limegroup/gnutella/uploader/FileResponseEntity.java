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

import org.apache.http.protocol.HttpContext;
import org.limewire.http.AbstractHttpNIOEntity;

import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.http.HttpContextParams;

public class FileResponseEntity extends AbstractHttpNIOEntity {

    private HTTPUploader uploader;

    private FileDesc fd;

    private ByteBuffer buffer;

    private long length;

    private long begin;

    private InputStream in;

    private File file;

    private long remaining;

    private HttpContext context;

    private static int BUFFER_SIZE = 512;

    private FileResponseEntity(HttpContext context, HTTPUploader uploader) {
        this.context = context;
        this.uploader = uploader;
        
        setContentType(Constants.FILE_MIME_TYPE);

        begin = uploader.getUploadBegin();
        long end = uploader.getUploadEnd();
        length = end - begin;
        remaining = length;
    }
    
    public FileResponseEntity(HttpContext context, HTTPUploader uploader, FileDesc fd) {
        this(context, uploader);
        
        this.fd = fd;
    }

    public FileResponseEntity(HTTPUploader uploader, File file) {
        this(null, uploader);
        
        this.file = file;
    }
    
    @Override
    public long getContentLength() {
        return length;
    }

    @Override
    public void initialize() throws IOException {
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

        HttpContextParams.getIOSession(context).setThrottle(true);
        
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.flip(); 
    }
    
    @Override
    public void finished() throws IOException {
        in.close();
    }
    
    @Override
    public boolean handleWrite() throws IOException {
        System.out.println("FileRepsonseEntity: write");
        if (buffer == null) {
            return false;
        }
        
        if (buffer.hasRemaining()) {
             int written = write(buffer);
             uploader.addAmountUploaded(written);
             if (buffer.hasRemaining()) {
                 return true;
             }
        }
 
        buffer.clear();
        int read = in.read(buffer.array(), 0, (int) Math.min(buffer.remaining(), remaining));
        remaining -= read;
        
        buffer.flip();
        buffer.limit(read);
        int written = write(buffer);
        uploader.addAmountUploaded(written);
        return remaining > 0;
    }

}