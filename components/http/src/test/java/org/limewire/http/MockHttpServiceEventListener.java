/**
 * 
 */
package org.limewire.http;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpConnection;

public class MockHttpServiceEventListener implements HttpServiceEventListener {

    boolean requestReceived;

    boolean responseSent;

    boolean connectionClosed;

    boolean connectionOpened;

    Exception exception;

    HttpRequest request;

    HttpResponse response;

    public void requestReceived(NHttpConnection conn, HttpRequest request) {
        this.requestReceived = true;
        this.request = request;
    }

    public void responseSent(NHttpConnection conn, HttpResponse response) {
        this.responseSent = true;
        this.response = response;
    }

    public void connectionClosed(NHttpConnection conn) {
        this.connectionClosed = true;
    }

    public void connectionOpen(NHttpConnection conn) {
        this.connectionOpened = true;
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