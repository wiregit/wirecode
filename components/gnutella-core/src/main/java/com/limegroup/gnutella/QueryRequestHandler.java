package com.limegroup.gnutella;

import java.io.IOException;

/**
 * This interface by singleton classes for implementing different strategies for
 * handling a QueryRequest.
 * QueryRequestHandlers are not responsible for handling routing of the request;
 * they should examine the contents of the QueryRequest as necessary and handle
 * the message accordingly
 *
 * @author Ron Vogl
 */
public interface QueryRequestHandler
{
    /**
     * Handle the QueryRequest, failing silently
     */
    void handleQueryRequest(QueryRequest queryRequest,
                            ManagedConnection receivingConnection,
                            MessageRouter router,
                            ActivityCallback callback,
                            Acceptor acceptor,
                            ConnectionManager connectionManager);
}
