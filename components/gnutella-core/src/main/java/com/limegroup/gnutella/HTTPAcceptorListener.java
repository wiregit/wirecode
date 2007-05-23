package com.limegroup.gnutella;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpConnection;

/**
 * Defines the requirements for classes that listen to events sent by
 * {@link HTTPAcceptor}.
 */
public interface HTTPAcceptorListener {

    /**
     * Invoked when a new HTTP connection has been established before the first
     * request is received.
     */
    void connectionClosed(NHttpConnection conn);

    /**
     * Invoked when a HTTP connection has been closed.
     */
    void connectionOpen(NHttpConnection conn);

    /**
     * Invoked when a request has been received.
     */
    void requestReceived(NHttpConnection conn, HttpRequest request);

    /**
     * Invoked when a response has been sent.
     */
    void responseSent(NHttpConnection conn, HttpResponse response);
    
}
