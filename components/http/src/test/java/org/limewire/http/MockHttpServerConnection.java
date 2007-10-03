/**
 * 
 */
package org.limewire.http;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.HttpParams;

public class MockHttpServerConnection extends DefaultNHttpServerConnection {

    private boolean closed;

    public MockHttpServerConnection(IOSession session,
            HttpRequestFactory requestFactory, HttpParams params) {
        super(session, requestFactory, new HeapByteBufferAllocator(), params);
    }

    public void setHttpRequest(HttpRequest request) {
        this.request = request;
    }

    public void setHttpResponse(HttpResponse response) {
        this.response = response;
    }

    public void setContentEncoder(ContentEncoder encoder) {
        this.contentEncoder = encoder;
    }

    @Override
    public void consumeInput(NHttpServiceHandler handler) {
        if (this.request != null) {
            handler.requestReceived(this);
        } else {
            super.consumeInput(handler);
        }
    }
 
    @Override
    public void close() throws IOException {
        this.closed = true;
    }
    
    public boolean isClosed() {
        return this.closed;
    }
    
    public void setHasBufferedInput(boolean hasBufferedInput) {
        this.hasBufferedInput = hasBufferedInput;
    }

    public void setHasBufferedOutput(boolean hasBufferedOutput) {
        this.hasBufferedOutput = hasBufferedOutput;
    }

    public void setContentDecoder(MockContentDecoder contentDecoder) {
        this.contentDecoder = contentDecoder;
    }

}