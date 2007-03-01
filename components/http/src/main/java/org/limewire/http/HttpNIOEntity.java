package org.limewire.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;

public class HttpNIOEntity extends AbstractHttpEntity implements HttpEntity {

    public InputStream getContent() throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public long getContentLength() {
        return 0;
    }

    public boolean isRepeatable() {
        throw new UnsupportedOperationException();
    }

    public boolean isStreaming() {
        throw new UnsupportedOperationException();
    }

    public void writeTo(OutputStream outstream) throws IOException {
        throw new UnsupportedOperationException();
    }

}
