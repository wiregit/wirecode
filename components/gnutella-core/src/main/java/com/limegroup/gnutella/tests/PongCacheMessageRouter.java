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
    protected void broadcastPingRequest(ManagedConnection receivingConnection)
    { 
        System.out.println("Broadcasting ping request to all outgoing");
        for(int i=0; i < outgoingConnections.size(); i++)
        {
            ManagedConnection conn = 
                (ManagedConnection)outgoingConnections.elementAt(i);
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

        for (int i=0; i<incomingConnections.size(); i++)
        {
            ManagedConnection c = 
                (ManagedConnection)incomingConnections.elementAt(i);

            if (c != connection)
            {
                //first make sure that the connection wants some pongs (i.e.,
                //sent at least one "real" ping request yet, not just a 
                //handshake ping.
                if (!c.receivedFirstPing())
                    continue;

                int[] neededPongs = c.getNeededPongsList();
                if (neededPongs[hops-1] > 0)
                {
                    byte[] guid = c.getLastPingGUID();
                    //send pong
                    PingReply pr = new PingReply(guid, ttl, hops, port, ip,
                        files, kbytes);
                    c.send(pr);
                    neededPongs[hops-1]--;
                }
            }
        }
    }

    /**
     * Override of super.sendMyOwnAddress for testing purposes.
     */
    public void sendMyAddress(PingRequest pingRequest, 
                              ManagedConnection receivingConnection)
    {
        respondToPingRequest(pingRequest, _acceptor, receivingConnection);
    }


    public void addIncomingConnection(ManagedConnection conn)
    {
        incomingConnections.add(conn);
    }

    public void addOutgoingConnection(ManagedConnection conn)
    {
        outgoingConnections.add(conn);
    }

    protected void handlePingRequest(PingRequest pingRequest,
                                     ManagedConnection receivingConnection)
    {
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







