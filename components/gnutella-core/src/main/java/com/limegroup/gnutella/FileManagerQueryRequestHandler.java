package com.limegroup.gnutella;

import java.io.IOException;

/**
 * An implementation of QueryRequestHandler that broadcasts the QueryRequest
 * and composes a response for this host using information from the singleton
 * FileManager.
 *
 * @author Ron Vogl
 */
public class FileManagerQueryRequestHandler
    implements QueryRequestHandler
{
    private static FileManagerQueryRequestHandler _instance;

    private FileManagerQueryRequestHandler() {}

    public static FileManagerQueryRequestHandler instance()
    {
        if(_instance == null)
            _instance = new FileManagerQueryRequestHandler();
        return _instance;
    }

    public void handleQueryRequest(QueryRequest queryRequest,
                                   ManagedConnection receivingConnection,
                                   MessageRouter router,
                                   ActivityCallback callback,
                                   Acceptor acceptor,
                                   ConnectionManager connectionManager)
    {
        // Reduce TTL and increment hops, then broadcast the ping
        // If the old value of TTL was 0 or 1, don't broadcast the message
        if(queryRequest.hop() > 1)
            router.broadcastQueryRequest(queryRequest, receivingConnection);

        // Apply the personal filter to decide whether the callback
        // should be informed of the query
        if (!receivingConnection.isPersonalSpam(queryRequest)) {
            callback.handleQueryString(queryRequest.getQuery());
        }

        // Run the local query
        Response[] responses = FileManager.getFileManager().query(queryRequest);

        // If we have responses, send back a QueryReply
        if (responses.length > 0)
        {
            byte[] guid = queryRequest.getGUID();
            byte ttl = (byte)(queryRequest.getHops() + 1);
            int port = acceptor.getPort();
            byte[] ip = acceptor.getAddress();
            long speed = SettingsManager.instance().getConnectionSpeed();
            byte[] clientGUID = router.getClientGUID();

            // Modified by Sumeet Thadani
            // If the number of responses is more 255, we
            // are going to drop the responses after index
            // 255. This can be corrected post beta, so
            // that the extra responses can be sent along as
            // another query reply.
            if (responses.length > 255)
            {
                Response[] res = new Response[255];
                //copy first 255 elements of old array
                for(int i=0; i<255;i++)
                    res[i] = responses[i];
                responses = res;
            }
            QueryReply queryReply = new QueryReply(guid, ttl, port, ip, speed,
                                                   responses, clientGUID);
            try
            {
                router.sendQueryReply(queryReply, receivingConnection);
            }
            catch(IOException e) {}
        }
    }
}
