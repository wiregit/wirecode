package com.limegroup.gnutella;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.SecureMessage;
import com.limegroup.gnutella.messages.SecureMessageCallback;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.FixedsizeForgetfulHashMap;
import com.limegroup.gnutella.util.IntWrapper;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * This is the class that goes in the route table when a request is
 * sent whose reply is for me.
 */
public final class ForMeReplyHandler implements ReplyHandler, SecureMessageCallback {
    
    private static final Log LOG = LogFactory.getLog(ForMeReplyHandler.class);
    
    /**
     * Keeps track of what hosts have sent us PushRequests lately.
     */
    private final Map /* String -> IntWrapper */ PUSH_REQUESTS = 
        Collections.synchronizedMap(new FixedsizeForgetfulHashMap(200));

    private final Map /* GUID -> GUID */ GUID_REQUESTS = 
        Collections.synchronizedMap(new FixedsizeForgetfulHashMap(200));

	/**
	 * Instance following singleton.
	 */
	private static final ReplyHandler INSTANCE =
		new ForMeReplyHandler();

	/**
	 * Singleton accessor.
	 *
	 * @return the <tt>ReplyHandler</tt> instance for this node
	 */
	public static ReplyHandler instance() {
		return INSTANCE;
	}
	   
	/**
	 * Private constructor to ensure that only this class can construct
	 * itself.
	 */
	private ForMeReplyHandler() {
	    //Clear push requests every 30 seconds.
	    RouterService.schedule(new Runnable() {
	        public void run() {
	            PUSH_REQUESTS.clear();
	        }
	    }, 30 * 1000, 30 * 1000);
    }

	public void handlePingReply(PingReply pingReply, ReplyHandler handler) {
        //Kill incoming connections that don't share.  Note that we randomly
        //allow some freeloaders.  (Hopefully they'll get some stuff and then
        //share!)  Note that we only consider killing them on the first ping.
        //(Message 1 is their ping, message 2 is their reply to our ping.)
        if ((pingReply.getHops() <= 1)
			&& (handler.getNumMessagesReceived() <= 2)
			&& (!handler.isOutgoing())
			&& (handler.isKillable())
			&& (pingReply.getFiles() < SharingSettings.FREELOADER_FILES.getValue())
			&& ((int)(Math.random()*100.f) >
				SharingSettings.FREELOADER_ALLOWED.getValue())
			&& (handler instanceof ManagedConnection)
            && (handler.isStable())) {
			ConnectionManager cm = RouterService.getConnectionManager();
            cm.remove((ManagedConnection)handler);
        }
	}
	
	public void handleQueryReply(QueryReply reply, ReplyHandler handler) {
		if(handler != null && handler.isPersonalSpam(reply)) return;
		
        // do not allow a faked multicast reply.
        if(reply.isFakeMulticast())
            return;
        
		// Drop if it's a reply to mcast and conditions aren't met ...
        if( reply.isReplyToMulticastQuery() ) {
            if( reply.isTCP() )
                return; // shouldn't be on TCP.
            if( reply.getHops() != 1 || reply.getTTL() != 0 )
                return; // should only have hopped once.
        }
        
        if (reply.isUDP()) {
        	Assert.that(handler instanceof UDPReplyHandler);
        	UDPReplyHandler udpHandler = (UDPReplyHandler)handler;
        	reply.setOOBAddress(udpHandler.getInetAddress(),udpHandler.getPort());
        }
        
        // XML must be added to the response first, so that
        // whomever calls toRemoteFileDesc on the response
        // will create the cachedRFD with the correct XML.
        boolean validResponses = addXMLToResponses(reply);
        // responses invalid?  exit.
        if(!validResponses)
            return;
        
        if(reply.hasSecureData() && ApplicationSettings.USE_SECURE_RESULTS.getValue()) {
            RouterService.getSecureMessageVerifier().verify(reply, this);
        } else {
            routeQueryReplyInternal(reply);
        }
    }
    
    /** Notification that a message is secure.  Currently only possible for a QueryReply. */
    public void handleSecureMessage(SecureMessage sm, boolean passed) {
        if (passed)
            routeQueryReplyInternal((QueryReply) sm);
    }
    
    /** Passes the QueryReply off to where it should go. */
    private void routeQueryReplyInternal(QueryReply reply) {
        SearchResultHandler resultHandler = RouterService.getSearchResultHandler();
        resultHandler.handleQueryReply(reply);

        DownloadManager dm = RouterService.getDownloadManager();
        dm.handleQueryReply(reply);
    }
	
	/**
	 * Adds XML to the responses in a QueryReply.
	 */
    private boolean addXMLToResponses(QueryReply qr) {
        // get xml collection string, then get dis-aggregated docs, then 
        // in loop
        // you can match up metadata to responses
        String xmlCollectionString = "";
        try {
            LOG.trace("Trying to do uncompress XML.....");
            byte[] xmlCompressed = qr.getXMLBytes();
            if (xmlCompressed.length > 1) {
                byte[] xmlUncompressed = LimeXMLUtils.uncompress(xmlCompressed);
                xmlCollectionString = new String(xmlUncompressed,"UTF-8");
            }
        }
        catch (UnsupportedEncodingException use) {
            //b/c this should never happen, we will show and error
            //if it ever does for some reason.
            //we won't throw a BadPacketException here but we will show it.
            //the uee will effect the xml part of the reply but we could
            //still show the reply so there shouldn't be any ill effect if
            //xmlCollectionString is ""
            ErrorService.error(use);
        }
        catch (IOException ignored) {}
        
        // valid response, no XML in EQHD.
        if(xmlCollectionString == null || xmlCollectionString.equals(""))
            return true;
        
        Response[] responses;
        int responsesLength;
        try {
            responses = qr.getResultsArray();
            responsesLength = responses.length;
        } catch(BadPacketException bpe) {
            LOG.trace("Unable to get responses", bpe);
            return false;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("xmlCollectionString = " + xmlCollectionString);

        List allDocsArray = 
            LimeXMLDocumentHelper.getDocuments(xmlCollectionString, 
                                               responsesLength);
        
        for(int i = 0; i < responsesLength; i++) {
            Response response = responses[i];
            LimeXMLDocument[] metaDocs;
            for(int schema = 0; schema < allDocsArray.size(); schema++) {
                metaDocs = (LimeXMLDocument[])allDocsArray.get(schema);
                // If there are no documents in this schema, try another.
                if(metaDocs == null)
                    continue;
                // If this schema had a document for this response, use it.
                if(metaDocs[i] != null) {
                    response.setDocument(metaDocs[i]);
                    break; // we only need one, so break out.
                }
            }
        }
        return true;
    }

    /**
     * If there are problems with the request, just ignore it.
     * There's no point in sending them a GIV to have them send a GET
     * just to return a 404 or Busy or Malformed Request, etc..
     */
	public void handlePushRequest(PushRequest pushRequest, ReplyHandler handler){
        //Ignore push request from banned hosts.
        if (handler.isPersonalSpam(pushRequest))
            return;
            
        byte[] ip = pushRequest.getIP();
        String h = NetworkUtils.ip2string(ip);

        // check whether we serviced this push request already
	GUID guid = new GUID(pushRequest.getGUID());
	if (GUID_REQUESTS.put(guid,guid) != null)
		return;

       // make sure the guy isn't hammering us
        IntWrapper i = (IntWrapper)PUSH_REQUESTS.get(h);
        if(i == null) {
            i = new IntWrapper(1);
            PUSH_REQUESTS.put(h, i);
        } else {
            i.addInt(1);
            // if we're over the max push requests for this host, exit.
            if(i.getInt() > UploadSettings.MAX_PUSHES_PER_HOST.getValue())
                return;
        }
        
        // if the IP is banned, don't accept it
        if (RouterService.getAcceptor().isBannedIP(ip))
            return;

        int port = pushRequest.getPort();
        // if invalid port, exit
        if (!NetworkUtils.isValidPort(port) )
            return;
        
        String req_guid_hexstring =
            (new GUID(pushRequest.getClientGUID())).toString();

        RouterService.getPushManager().
            acceptPushUpload(h, port, req_guid_hexstring,
                             pushRequest.isMulticast(), // force accept
                             pushRequest.isFirewallTransferPush());
	}
	
	public boolean isOpen() {
		//I'm always ready to handle replies.
		return true;
	}
	
	public int getNumMessagesReceived() {
		return 0;
	}
	
	
	public void countDroppedMessage() {}
	
	// inherit doc comment
	public boolean isSupernodeClientConnection() {
		return false;
	}
	
	public boolean isPersonalSpam(Message m) {
		return false;
	}
	
	public void updateHorizonStats(PingReply pingReply) {
        // TODO:: we should probably actually update the stats with this pong
    }
	
	public boolean isOutgoing() {
		return false;
	}
	

	// inherit doc comment
	public boolean isKillable() {
		return false;
	}

	/**
	 * Implements <tt>ReplyHandler</tt> interface.  Returns whether this
	 * node is a leaf or an Ultrapeer.
	 *
	 * @return <tt>true</tt> if this node is a leaf node, otherwise 
	 *  <tt>false</tt>
	 */
	public boolean isLeafConnection() {
		return !RouterService.isSupernode();
	}

	/**
	 * Returns whether or not this connection is a high-degree connection,
	 * meaning that it maintains a high number of intra-Ultrapeer connections.
	 * Because this connection really represents just this node, it always
	 * returns <tt>false</tt>/
	 *
	 * @return <tt>false</tt>, since this reply handler signifies only this
	 *  node -- its connections don't matter.
	 */
	public boolean isHighDegreeConnection() {
		return false;
	}	

    /**
     * Returns <tt>false</tt>, since this connection is me, and it's not
     * possible to pass query routing tables to oneself.
     *
     * @return <tt>false</tt>, since you cannot pass query routing tables
     *  to yourself
     */
    public boolean isUltrapeerQueryRoutingConnection() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, as this node is not  a "connection"
     * in the first place, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real connection
     */
    public boolean isGoodUltrapeer() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, as this node is not  a "connection"
     * in the first place, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real connection
     */
    public boolean isGoodLeaf() {
        return false;
    }

    /**
     * Returns <tt>true</tt>, since we always support pong caching.
     *
     * @return <tt>true</tt> since this node always supports pong 
     *  caching (since it's us)
     */
    public boolean supportsPongCaching() {
        return true;
    }

    /**
     * Returns whether or not to allow new pings from this <tt>ReplyHandler</tt>.
     * Since this ping is from us, we'll always allow it.
     *
     * @return <tt>true</tt> since this ping is from us
     */
    public boolean allowNewPings() {
        return true;
    }

    // inherit doc comment
    public InetAddress getInetAddress() {
        try {
            return InetAddress.
                getByName(NetworkUtils.ip2string(RouterService.getAddress()));
        } catch(UnknownHostException e) {
            // may want to do something else here if we ever use this!
            return null;
        }
    }
    
    public int getPort() {
        return RouterService.getPort();
    }
    
    public String getAddress() {
        return NetworkUtils.ip2string(RouterService.getAddress());
    }
    
    public void handleSimppVM(SimppVM simppVM) {
        Assert.that(false, "ForMeReplyHandler asked to send vendor message");
    }

    /**
     * Returns <tt>true</tt> to indicate that this node is always stable.
     * Simply the fact that this method is being called indicates that the
     * code is alive and stable (I think, therefore I am...).
     *
     * @return <tt>true</tt> since, this node is always stable
     */
    public boolean isStable() {
        return true;
    }

    public String getLocalePref() {
        return ApplicationSettings.LANGUAGE.getValue();
    }
    
    /**
     * drops the message
     */
    public void reply(Message m){}


    public byte[] getClientGUID() {
        return RouterService.getMyGUID();
    }
}



