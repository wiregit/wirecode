package com.limegroup.gnutella;

import java.io.IOException;

/**
 * This interface by singleton classes for implementing different strategies for
 * handling a PingRequest.
 * PingRequestHandlers are not responsible for handling routing of the request;
 * they should examine the contents of the PingRequest as necessary and handle
 * the message accordingly
 *
 * @author Ron Vogl
 */
public interface PingRequestHandler
{
    /**
     * Handle the PingRequest, failing silently
     */
    void handlePingRequest(PingRequest pingRequest,
                           ManagedConnection receivingConnection,
                           MessageRouter router,
                           ActivityCallback callback,
                           Acceptor acceptor);
}
