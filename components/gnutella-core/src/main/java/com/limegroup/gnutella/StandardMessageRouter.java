package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.sun.java.util.collections.*;

/**
 * This class is the message routing implementation for TCP messages.
 */
public class StandardMessageRouter extends MessageRouter {

    private ActivityCallback _callback;
    private FileManager _fileManager;

    /**
     * Creates a new <tt>StandardMessageRouter</tt> with the specified
     * <tt>ActivityCallback</tt> and <tt>FileManager</tt>.
     *
     * @param callback the <tt>ActivityCallback</tt> instance to use
     * @param fm the <tt>FileManager</tt> for querying the set of 
     *  shared files
     */
    public StandardMessageRouter(ActivityCallback callback, FileManager fm) {
        _callback = callback;
        _fileManager = fm;
    }


    /**
     * Responds to the PingRequest by getting information from the FileManager
     * and the Acceptor.  However, it only sends a Ping Reply back if we
     * can currently accept incoming connections or the hops + ttl <= 2 (to allow
     * for crawler pings).
     */
    protected void respondToPingRequest(PingRequest pingRequest) {
        //If this wasn't a handshake or crawler ping, check if we can accept
        //incoming connection for old-style unrouted connections, ultrapeers, or
        //leaves.  TODO: does this mean leaves always respond to pings?
        int hops = (int)pingRequest.getHops();
        int ttl = (int)pingRequest.getTTL();
        if (   (hops+ttl > 2) 
            && !_manager.allowAnyConnection())
            return;

        //SPECIAL CASE: for crawler ping
        // TODO:: this means that we can never send TTL=2 pings without
        // them being interpreted as from the crawler!!
        if(hops ==1 && ttl==1) {
            handleCrawlerPing(pingRequest);
            //Note that the while handling crawler ping, we dont send our own
            //pong, as that is unnecessary, since crawler already has our
            //address. We though return it below for compatibility with old
            //ConnectionWatchdogPing which had TTL=2 (instead of 1).
        }
        
        //send its own ping in all the cases
        int newTTL = hops+1;
        if ( (hops+ttl) <=2)
            newTTL = 1;        

        PingReply pr = 
            PingReply.create(pingRequest.getGUID(), (byte)newTTL);

        try {
            sendPingReply(pr);
        }
        catch(IOException e) {}
    }

	/**
	 * Responds to a ping request received over a UDP port.  This is
	 * handled differently from all other ping requests.  Instead of
	 * responding with a pong from this node, we respond with a pong
	 * from other UltraPeers supporting UDP from our cache.
	 *
	 * @param request the <tt>PingRequest</tt> to service
     * @param datagram the <tt>DatagramPacket</tt> containing the IP
     *  and port of the client node
	 */
	protected void respondToUDPPingRequest(PingRequest request, 
										   DatagramPacket datagram) {
		List unicastEndpoints = UNICASTER.getUnicastEndpoints();
		Iterator iter = unicastEndpoints.iterator();
		if(iter.hasNext()) {
			while(iter.hasNext()) {
				GUESSEndpoint host = (GUESSEndpoint)iter.next();				
                PingReply pr = 
                    PingReply.createExternal(request.getGUID(), (byte)1,
                                             host.getPort(),
                                             host.getAddress().getAddress(),
                                             true);
				try {
					sendPingReply(pr);
				} catch(IOException e) {					
					// we can't do anything other than try to send it
					continue;
				}
			}
		} else {
			// always respond with something
			sendAcknowledgement(datagram, request.getGUID());
		}
	}

    /**
     * Handles the crawler ping of Hops=0 & TTL=2, by sending pongs 
     * corresponding to all its leaves
     * @param m The ping request received
     * @exception In case any I/O error occurs while writing Pongs over the
     * connection
     */
    private void handleCrawlerPing(PingRequest m) {
        //TODO: why is this any different than the standard pong?  In other
        //words, why no ultrapong marking, proper address calculation, etc?
        
        //send the pongs for leaves
        List /*<ManagedConnection>*/ leafConnections 
            = _manager.getInitializedClientConnections2();
        
        for(Iterator iterator = leafConnections.iterator(); 
            iterator.hasNext();) {
            //get the next connection
            ManagedConnection connection = (ManagedConnection)iterator.next();
            //create the pong for this connection

            PingReply pr = 
                PingReply.createExternal(m.getGUID(), (byte)2, 
                                         connection.getOrigPort(),
                                         connection.getInetAddress().getAddress(),
                                         false);
                                                    
            
            //hop the message, as it is ideally coming from the connected host
            pr.hop();
            
            try {
                sendPingReply(pr);
            }
            catch(IOException e) {}
        }
        
        //pongs for the neighbors will be sent by neighbors themselves
        //as ping will be broadcasted to them (since TTL=2)        
    }
    
    protected void handlePingReply(PingReply pingReply,
								   ReplyHandler receivingConnection) {
        //We override the super's method so the receiving connection's
        //statistics are updated whether or not this is for me.
		if(receivingConnection instanceof ManagedConnection) {
			ManagedConnection mc = (ManagedConnection)receivingConnection;
			mc.updateHorizonStats(pingReply);
		}
        super.handlePingReply(pingReply, receivingConnection);
    }


    protected void respondToQueryRequest(QueryRequest queryRequest,
                                         byte[] clientGUID) {
        // Run the local query
        Response[] responses = _fileManager.query(queryRequest);

        sendResponses(queryRequest, responses, queryRequest, clientGUID);
        
    }

    public void sendResponses(QueryRequest query, 
                              Response[] responses, 
							  QueryRequest queryRequest,
							  byte[] clientGUID) {
        // if either there are no responses or, the
        // response array came back null for some reason,
        // exit this method
        if ( (responses == null) || ((responses.length < 1)) )
            return;

        // get the appropriate queryReply information

        //Return measured speed if possible, or user's speed otherwise.
        long speed = 
		    RouterService.getUploadManager().measuredUploadSpeed();
        boolean measuredSpeed=true;
        if (speed==-1) {
            speed=SettingsManager.instance().getConnectionSpeed();
            measuredSpeed=false;
        }

        //convert responses to QueryReplies
        Iterator /*<QueryReply>*/iterator= responsesToQueryReplies(responses,
                                                                   queryRequest);
        //send the query replies
        try {
            while(iterator.hasNext()) {
                QueryReply queryReply = (QueryReply)iterator.next();
                sendQueryReply(query, queryReply);
            }
        } 
        catch (IOException e) {
            // if there is an error, do nothing..
        }
    }
}
