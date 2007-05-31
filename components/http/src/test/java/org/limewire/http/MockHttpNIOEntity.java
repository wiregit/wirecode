/**
 * 
 */
package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MockHttpNIOEntity extends AbstractHttpNIOEntity {

    IOException exception;
    
    boolean finished;
    
    long contentLength;
    
    int transferred;

    String data;

    boolean initialized;
    
    public MockHttpNIOEntity(String data) {
        this.data = data;
        this.contentLength = data.length();
    }
    
    @Override
    public void finished() {
        this.finished = true;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public boolean handleWrite() throws IOException {
        if (exception != null) {
            throw new IOException();
        }
        write(ByteBuffer.wrap(data.getBytes(), transferred, 1));
        transferred++;
        return transferred < contentLength;
    }

    @Override
    public void initialize() throws IOException {
        initialized = true;
    }
    
}