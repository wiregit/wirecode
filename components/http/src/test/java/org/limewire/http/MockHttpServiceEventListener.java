/**
 * 
 */
package org.limewire.http;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.nio.NHttpConnection;

public class MockHttpServiceEventListener implements HttpServiceEventListener {

    boolean requestReceived;

    boolean responseSent;

    boolean connectionClosed;

    boolean connectionOpened;

    Exception exception;

    public void requestReceived(NHttpConnection conn) {
        requestReceived = true;
    }

    public void responseSent(NHttpConnection conn) {
        responseSent = true;
    }

    public void connectionClosed(NHttpConnection conn) {
        connectionClosed = true;
    }

    public void connectionOpen(NHttpConnection conn) {
        connectionOpened = true;
    }

    public void connectionTimeout(NHttpConnection conn) {
        // should never happen
        throw new RuntimeException();
    }

    public void fatalIOException(IOException ex, NHttpConnection conn) {
        this.exception = ex;
    }

    public void fatalProtocolException(HttpException ex,
            NHttpConnection conn) {
        this.exception = ex;
    }

}