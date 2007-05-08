package org.limewire.http;

import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.protocol.HttpService;

/**
 * Defines the requirements for classes that listen to events from
 * {@link HttpService}. In addition to the methods in {@link EventListener}
 * this interface provides notification for request and repsonse handling.
 */
public interface HttpServiceEventListener extends EventListener {

    /**
     * Invoked when a request has been received.
     * 
     * @param conn the underlying connection
     */
    void requestReceived(NHttpConnection conn);
    
    /**
     * Invoked after a response has been sent.
     * 
     * @param conn the underlying connection
     */
    void responseSent(NHttpConnection conn);

}
