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

    public StandardMessageRouter(ActivityCallback callback, FileManager fm) {
        _callback = callback;
        _fileManager = fm;
    }

    /**
     * Override of handleQueryRequest to send query strings to the callback.
     */
    protected void handleQueryRequest(QueryRequest queryRequest,
                                      ReplyHandler receivingConnection) {
        // Apply the personal filter to decide whether the callback
        // should be informed of the query
        if (!receivingConnection.isPersonalSpam(queryRequest)) {
            _callback.handleQueryString(queryRequest.getQuery());
        }

        super.handleQueryRequest(queryRequest, receivingConnection);
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

        //SPECIAL CASE: for Crawle ping
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
        
        int num_files = RouterService.getNumSharedFiles();
        int kilobytes = RouterService.getSharedFileSize()/1024;

        //We mark our ping replies if currently in the supernode state.
        boolean markPong=RouterService.isSupernode();
        //Daily average uptime.  If too slow, use FRACTIONAL_UPTIME property.
        //This results in a GGEP extension, which will be stripped before
        //sending it to older clients.
        int dailyUptime=Statistics.instance().calculateDailyUptime();
        PingReply pingReply = new PingReply(pingRequest.getGUID(),
                                            (byte)newTTL,
                                            RouterService.getPort(),
                                            RouterService.getAddress(),
                                            num_files,
                                            kilobytes,
                                            markPong,
                                            dailyUptime);

        try {
            sendPingReply(pingReply);
        }
        catch(IOException e) {}
    }

	/**
	 * Responds to a ping request received over a UDP port.  This is
	 * handled differently from all other ping requests.  Instead of
	 * responding with a pong from this node, we respond with a pong
	 * from other UltraPeers supporting UDP from our cache.  This method
	 * should only be called if this host is an UltraPeer, as only UltaPeers
	 * should accept messages over UDP.
	 *
	 * @param request the <tt>PingRequest</tt> to service
	 */
	protected void respondToUDPPingRequest(PingRequest request, 
										   DatagramPacket datagram) {
		List unicastEndpoints = UNICASTER.getUnicastEndpoints();
		Iterator iter = unicastEndpoints.iterator();
		if(iter.hasNext()) {
			while(iter.hasNext()) {
				GUESSEndpoint host = (GUESSEndpoint)iter.next();				
				PingReply reply = 
					new PingReply(request.getGUID(), (byte)1, 
								  host.getPort(),
								  host.getAddress().getAddress(), 
								  (long)0, (long)0, 
								  true);
				try {
					sendPingReply(reply);
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
            PingReply pr = new PingReply(m.getGUID(), (byte)2,
                connection.getOrigPort(),
                connection.getInetAddress().getAddress(), 0, 0); 
            
            //hop the message, as it is ideally coming from the connected host
            pr.hop();
            
            try
            {
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

        sendResponses(responses, queryRequest, clientGUID);
        
    }

    public void sendResponses(Response[] responses, 
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
                sendQueryReply(queryReply);
            }
        } 
        catch (IOException e) {
            // if there is an error, do nothing..
        }
    }

    /** @see MessageRouter.addQueryRoutingEntries */
    protected void addQueryRoutingEntries(QueryRouteTable qrt) {
        Iterator words = _fileManager.getKeyWords().iterator();
        while(words.hasNext())
            qrt.add((String)words.next());
        // get 'indivisible' words and handle appropriately - you don't want the
        // qrt to divide these guys up....
        Iterator indivisibleWords = _fileManager.getIndivisibleKeyWords().iterator();
        while (indivisibleWords.hasNext()) 
            qrt.addIndivisible((String) indivisibleWords.next());
        /*
          File[] files = _fileManager.getSharedFiles(null);
          for (int i=0; i<files.length; i++)
            qrt.add(files[i].getAbsolutePath());
        */
    }
}
