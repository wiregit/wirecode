package org.limewire.core.api.friend.client;

import org.limewire.net.ConnectBackRequest;

/**
 * Requirements for a class that non-blockingly sends connect back requests over the
 * wire to a user specified in <code>userId</code>.
 * 
 * The information is loosely typed, so that it can be implemented in a protocol
 * agnostic way.
 */
public interface ConnectBackRequestSender {

    /**
     * Asynchronously sends a connect request to <code>userId</code>
     * @param userId the peer that should be notified of the connect request
     * @return false if it could be determined immediately that the request will
     * not be sent successfully
     */
    public boolean send(String userId, ConnectBackRequest connectBackRequest);
    
}
