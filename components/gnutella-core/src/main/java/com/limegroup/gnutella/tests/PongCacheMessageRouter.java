package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.util.*;

/**
 * Message Router that is used for testing the pong caching.  "No-ops" any
 * methods that handle queries or pushes, only deals with PingRequests and
 * PingReplies (i.e., Pongs).
 */
public class PongCacheMessageRouter extends MessageRouter
{
    //since we are not using a connection manager, we need to somehow have
    //a list of connections (test connections, really)
    private Vector outgoingConnections; 
    private Vector incomingConnections; //pong cache test connection
    private byte[] ip; //ip address to return in direct pongs.
    private int port; //port to return in direct pongs.

    /**
     * @param myIP - IP Address that process is running on, used in responding
     *               to ping requests.
     * @param port - port that process is listening on, used in responding to
     *               ping requests.
     */
    public PongCacheMessageRouter(byte[] ip, int port)
    {
        this.ip = ip;
        this.port = port;
        outgoingConnections = new Vector();
        incomingConnections = new Vector();
    }

    /**
     * For testing the pong cache, we dont' use an acceptor and we don't check
     * if the user is firewalled (via Acceptor), so we need to override.  In
     * this case, we will always return our own address. 
     */
    protected void respondToPingRequest(PingRequest pingRequest,
                                        Acceptor acceptor,
                                        ManagedConnection connection)
    {
        PingReply pingReply = new PingReply(pingRequest.getGUID(),
                                            (byte)(pingRequest.getHops()+1),
                                            port, ip, 0, 0);
        connection.send(pingReply);
    }

    /**
     * Same as super.broadcastPingRequest, but we don't use a connection manager,
     * so, we have to use our own list of outgoing connections.  
     */
    protected void broadcastPingRequest()
    { 
        System.out.println("Broadcasting ping request to all outgoing");
        for(int i=0; i < outgoingConnections.size(); i++)
        {
            ManagedConnection conn = 
                (ManagedConnection)outgoingConnections.elementAt(i);
            if (!conn.isOldClient())
                conn.send(new PingRequest((byte)this.MAX_TTL_FOR_CACHE_REFRESH));
        }
    }
   
    /**
     * Same basic functionality as super.sendPongToOtherConnections, but we have 
     * to override, because we are not using a Connection Manager, so we have to
     * use our own list of incoming connections (which are the Pong Cache Test
     * connections).
     */
    protected void sendPongToOtherConnections(PingReply pingReply,
                                              ManagedConnection connection)
    {
        //necessary fields for forwarding the PingReply
        byte hops = pingReply.getHops();
        byte[] ip = pingReply.getIPBytes();
        int port = pingReply.getPort();
        long files = pingReply.getFiles();
        long kbytes = pingReply.getKbytes();
        byte ttl = pingReply.getTTL();
        byte[] guid = pingReply.getGUID();

        for (int i=0; i<incomingConnections.size(); i++)
        {
            ManagedConnection c = 
                (ManagedConnection)incomingConnections.elementAt(i);
            if (c != connection)
            {
                //send pong
                PingReply pr = new PingReply(guid, ttl, hops, port, ip,
                    files, kbytes);
                c.send(pr);
            }
        }
    }

    public void addIncomingConnection(ManagedConnection conn)
    {
        incomingConnections.add(conn);
    }

    public void addOutgoingConnection(ManagedConnection conn)
    {
        outgoingConnections.add(conn);
    }

    public void handlePingRequest(PingRequest pingRequest,
                                  ManagedConnection receivingConnection)
    {
        System.out.println("Received ping request");
        super.handlePingRequest(pingRequest, receivingConnection);
    }

    /**
     * Override to do nothing.
     */
    public void handleQueryRequest(QueryRequest queryRequest,
                                   ManagedConnection receivingConnection)
    {
    }

    /**
     * Override to do nothing.
     */
    public void handlePushRequest(PushRequest pushRequest,
                                  ManagedConnection receivingConnection)
    {
    }

    /**
     * Implement to do nothing
     */
    protected void handlePingReplyForMe(PingReply pingReply,
                                        ManagedConnection receivingConnection)
    {
    }

    /**
     * Implement to do nothing
     */
    protected void respondToQueryRequest(QueryRequest queryRequest,
                                         Acceptor acceptor,
                                         byte[] clientGUID)
    {
    }

    /**
     * Implement to do nothing
     */
    protected void handleQueryReplyForMe(QueryReply queryReply,
                                         ManagedConnection receivingConnection)
    {
    }

    /**
     * Implement to do nothing
     */
    protected void handlePushRequestForMe(PushRequest pushRequest,
                                          ManagedConnection receivingConnection)
    {
    }

}







