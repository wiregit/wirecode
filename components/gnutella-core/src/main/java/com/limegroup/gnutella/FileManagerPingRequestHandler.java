package com.limegroup.gnutella;

import java.io.IOException;

/**
 * An implementation of PingRequestHandler that checks TTL, broadcasts the
 * PingRequest, and composes a response for this host using information from the
 * singleton FileManager.
 *
 * @author Ron Vogl
 */
public class FileManagerPingRequestHandler
    implements PingRequestHandler
{
    private static FileManagerPingRequestHandler _instance;

    private FileManagerPingRequestHandler() {}

    public static FileManagerPingRequestHandler instance()
    {
        if(_instance == null)
            _instance = new FileManagerPingRequestHandler();
        return _instance;
    }

    public void handlePingRequest(PingRequest pingRequest,
                                  ManagedConnection receivingConnection,
                                  MessageRouter router,
                                  ActivityCallback callback,
                                  Acceptor acceptor,
                                  ConnectionManager connectionManager)
    {
        // Reduce TTL and increment hops, then broadcast the ping
        // If the old value of TTL was 0 or 1, don't broadcast the message
        if(pingRequest.hop() > 1)
            router.broadcastPingRequest(pingRequest, receivingConnection);

        // Now, send our own response
        FileManager fm = FileManager.getFileManager();
        int kilobytes = fm.getSize()/1024;
        int num_files = fm.getNumFiles();

        Message pingReply =
            new PingReply(pingRequest.getGUID(),
                          (byte)(pingRequest.getHops()+1),
                          acceptor.getPort(),
                          acceptor.getAddress(),
                          num_files,
                          kilobytes);

        receivingConnection.send(pingReply);
    }
}
