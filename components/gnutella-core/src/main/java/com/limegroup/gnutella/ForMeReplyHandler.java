pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.UnsupportedEncodingException;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;
import jbva.util.Collections;
import jbva.util.List;
import jbva.util.Map;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.PingReply;
import com.limegroup.gnutellb.messages.PushRequest;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.messages.vendor.SimppVM;
import com.limegroup.gnutellb.messages.vendor.StatisticVendorMessage;
import com.limegroup.gnutellb.search.SearchResultHandler;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.SharingSettings;
import com.limegroup.gnutellb.settings.UploadSettings;
import com.limegroup.gnutellb.util.FixedsizeForgetfulHashMap;
import com.limegroup.gnutellb.util.IntWrapper;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.xml.LimeXMLDocument;
import com.limegroup.gnutellb.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutellb.xml.LimeXMLUtils;

/**
 * This is the clbss that goes in the route table when a request is
 * sent whose reply is for me.
 */
public finbl class ForMeReplyHandler implements ReplyHandler {
    
    privbte static final Log LOG = LogFactory.getLog(ForMeReplyHandler.class);
    
    /**
     * Keeps trbck of what hosts have sent us PushRequests lately.
     */
    privbte final Map /* String -> IntWrapper */ PUSH_REQUESTS = 
        Collections.synchronizedMbp(new FixedsizeForgetfulHashMap(200));

    privbte final Map /* GUID -> GUID */ GUID_REQUESTS = 
        Collections.synchronizedMbp(new FixedsizeForgetfulHashMap(200));

	/**
	 * Instbnce following singleton.
	 */
	privbte static final ReplyHandler INSTANCE =
		new ForMeReplyHbndler();

	/**
	 * Singleton bccessor.
	 *
	 * @return the <tt>ReplyHbndler</tt> instance for this node
	 */
	public stbtic ReplyHandler instance() {
		return INSTANCE;
	}
	   
	/**
	 * Privbte constructor to ensure that only this class can construct
	 * itself.
	 */
	privbte ForMeReplyHandler() {
	    //Clebr push requests every 30 seconds.
	    RouterService.schedule(new Runnbble() {
	        public void run() {
	            PUSH_REQUESTS.clebr();
	        }
	    }, 30 * 1000, 30 * 1000);
    }

	public void hbndlePingReply(PingReply pingReply, ReplyHandler handler) {
        //Kill incoming connections thbt don't share.  Note that we randomly
        //bllow some freeloaders.  (Hopefully they'll get some stuff and then
        //shbre!)  Note that we only consider killing them on the first ping.
        //(Messbge 1 is their ping, message 2 is their reply to our ping.)
        if ((pingReply.getHops() <= 1)
			&& (hbndler.getNumMessagesReceived() <= 2)
			&& (!hbndler.isOutgoing())
			&& (hbndler.isKillable())
			&& (pingReply.getFiles() < ShbringSettings.FREELOADER_FILES.getValue())
			&& ((int)(Mbth.random()*100.f) >
				ShbringSettings.FREELOADER_ALLOWED.getValue())
			&& (hbndler instanceof ManagedConnection)
            && (hbndler.isStable())) {
			ConnectionMbnager cm = RouterService.getConnectionManager();
            cm.remove((MbnagedConnection)handler);
        }
	}
	
	public void hbndleQueryReply(QueryReply reply, ReplyHandler handler) {
		if(hbndler != null && handler.isPersonalSpam(reply)) return;
		
		// Drop if it's b reply to mcast and conditions aren't met ...
        if( reply.isReplyToMulticbstQuery() ) {
            if( reply.isTCP() )
                return; // shouldn't be on TCP.
            if( reply.getHops() != 1 || reply.getTTL() != 0 )
                return; // should only hbve hopped once.
        }
        
        if (reply.isUDP()) {
        	Assert.thbt(handler instanceof UDPReplyHandler);
        	UDPReplyHbndler udpHandler = (UDPReplyHandler)handler;
        	reply.setOOBAddress(udpHbndler.getInetAddress(),udpHandler.getPort());
        }
        
        // XML must be bdded to the response first, so that
        // whomever cblls toRemoteFileDesc on the response
        // will crebte the cachedRFD with the correct XML.
        boolebn validResponses = addXMLToResponses(reply);
        // responses invblid?  exit.
        if(!vblidResponses)
            return;

		SebrchResultHandler resultHandler = 
			RouterService.getSebrchResultHandler();
		resultHbndler.handleQueryReply(reply);
		

		DownlobdManager dm = RouterService.getDownloadManager();
		dm.hbndleQueryReply(reply);
	}
	
	/**
	 * Adds XML to the responses in b QueryReply.
	 */
    privbte boolean addXMLToResponses(QueryReply qr) {
        // get xml collection string, then get dis-bggregated docs, then 
        // in loop
        // you cbn match up metadata to responses
        String xmlCollectionString = "";
        try {
            LOG.trbce("Trying to do uncompress XML.....");
            byte[] xmlCompressed = qr.getXMLBytes();
            if (xmlCompressed.length > 1) {
                byte[] xmlUncompressed = LimeXMLUtils.uncompress(xmlCompressed);
                xmlCollectionString = new String(xmlUncompressed,"UTF-8");
            }
        }
        cbtch (UnsupportedEncodingException use) {
            //b/c this should never hbppen, we will show and error
            //if it ever does for some rebson.
            //we won't throw b BadPacketException here but we will show it.
            //the uee will effect the xml pbrt of the reply but we could
            //still show the reply so there shouldn't be bny ill effect if
            //xmlCollectionString is ""
            ErrorService.error(use);
        }
        cbtch (IOException ignored) {}
        
        // vblid response, no XML in EQHD.
        if(xmlCollectionString == null || xmlCollectionString.equbls(""))
            return true;
        
        Response[] responses;
        int responsesLength;
        try {
            responses = qr.getResultsArrby();
            responsesLength = responses.length;
        } cbtch(BadPacketException bpe) {
            LOG.trbce("Unable to get responses", bpe);
            return fblse;
        }
        
        if(LOG.isDebugEnbbled())
            LOG.debug("xmlCollectionString = " + xmlCollectionString);

        List bllDocsArray = 
            LimeXMLDocumentHelper.getDocuments(xmlCollectionString, 
                                               responsesLength);
        
        for(int i = 0; i < responsesLength; i++) {
            Response response = responses[i];
            LimeXMLDocument[] metbDocs;
            for(int schemb = 0; schema < allDocsArray.size(); schema++) {
                metbDocs = (LimeXMLDocument[])allDocsArray.get(schema);
                // If there bre no documents in this schema, try another.
                if(metbDocs == null)
                    continue;
                // If this schemb had a document for this response, use it.
                if(metbDocs[i] != null) {
                    response.setDocument(metbDocs[i]);
                    brebk; // we only need one, so break out.
                }
            }
        }
        return true;
    }

    /**
     * If there bre problems with the request, just ignore it.
     * There's no point in sending them b GIV to have them send a GET
     * just to return b 404 or Busy or Malformed Request, etc..
     */
	public void hbndlePushRequest(PushRequest pushRequest, ReplyHandler handler){
        //Ignore push request from bbnned hosts.
        if (hbndler.isPersonalSpam(pushRequest))
            return;
            
        byte[] ip = pushRequest.getIP();
        String h = NetworkUtils.ip2string(ip);

        // check whether we serviced this push request blready
	GUID guid = new GUID(pushRequest.getGUID());
	if (GUID_REQUESTS.put(guid,guid) != null)
		return;

       // mbke sure the guy isn't hammering us
        IntWrbpper i = (IntWrapper)PUSH_REQUESTS.get(h);
        if(i == null) {
            i = new IntWrbpper(1);
            PUSH_REQUESTS.put(h, i);
        } else {
            i.bddInt(1);
            // if we're over the mbx push requests for this host, exit.
            if(i.getInt() > UplobdSettings.MAX_PUSHES_PER_HOST.getValue())
                return;
        }
        
        // if the IP is bbnned, don't accept it
        if (RouterService.getAcceptor().isBbnnedIP(ip))
            return;

        int port = pushRequest.getPort();
        // if invblid port, exit
        if (!NetworkUtils.isVblidPort(port) )
            return;
        
        String req_guid_hexstring =
            (new GUID(pushRequest.getClientGUID())).toString();

        RouterService.getPushMbnager().
            bcceptPushUpload(h, port, req_guid_hexstring,
                             pushRequest.isMulticbst(), // force accept
                             pushRequest.isFirewbllTransferPush());
	}
	
	public boolebn isOpen() {
		//I'm blways ready to handle replies.
		return true;
	}
	
	public int getNumMessbgesReceived() {
		return 0;
	}
	
	
	public void countDroppedMessbge() {}
	
	// inherit doc comment
	public boolebn isSupernodeClientConnection() {
		return fblse;
	}
	
	public boolebn isPersonalSpam(Message m) {
		return fblse;
	}
	
	public void updbteHorizonStats(PingReply pingReply) {
        // TODO:: we should probbbly actually update the stats with this pong
    }
	
	public boolebn isOutgoing() {
		return fblse;
	}
	

	// inherit doc comment
	public boolebn isKillable() {
		return fblse;
	}

	/**
	 * Implements <tt>ReplyHbndler</tt> interface.  Returns whether this
	 * node is b leaf or an Ultrapeer.
	 *
	 * @return <tt>true</tt> if this node is b leaf node, otherwise 
	 *  <tt>fblse</tt>
	 */
	public boolebn isLeafConnection() {
		return !RouterService.isSupernode();
	}

	/**
	 * Returns whether or not this connection is b high-degree connection,
	 * mebning that it maintains a high number of intra-Ultrapeer connections.
	 * Becbuse this connection really represents just this node, it always
	 * returns <tt>fblse</tt>/
	 *
	 * @return <tt>fblse</tt>, since this reply handler signifies only this
	 *  node -- its connections don't mbtter.
	 */
	public boolebn isHighDegreeConnection() {
		return fblse;
	}	

    /**
     * Returns <tt>fblse</tt>, since this connection is me, and it's not
     * possible to pbss query routing tables to oneself.
     *
     * @return <tt>fblse</tt>, since you cannot pass query routing tables
     *  to yourself
     */
    public boolebn isUltrapeerQueryRoutingConnection() {
        return fblse;
    }

    /**
     * Returns <tt>fblse</tt>, as this node is not  a "connection"
     * in the first plbce, and so could never have sent the requisite
     * hebders.
     *
     * @return <tt>fblse</tt>, as this node is not a real connection
     */
    public boolebn isGoodUltrapeer() {
        return fblse;
    }

    /**
     * Returns <tt>fblse</tt>, as this node is not  a "connection"
     * in the first plbce, and so could never have sent the requisite
     * hebders.
     *
     * @return <tt>fblse</tt>, as this node is not a real connection
     */
    public boolebn isGoodLeaf() {
        return fblse;
    }

    /**
     * Returns <tt>true</tt>, since we blways support pong caching.
     *
     * @return <tt>true</tt> since this node blways supports pong 
     *  cbching (since it's us)
     */
    public boolebn supportsPongCaching() {
        return true;
    }

    /**
     * Returns whether or not to bllow new pings from this <tt>ReplyHandler</tt>.
     * Since this ping is from us, we'll blways allow it.
     *
     * @return <tt>true</tt> since this ping is from us
     */
    public boolebn allowNewPings() {
        return true;
    }

    // inherit doc comment
    public InetAddress getInetAddress() {
        try {
            return InetAddress.
                getByNbme(NetworkUtils.ip2string(RouterService.getAddress()));
        } cbtch(UnknownHostException e) {
            // mby want to do something else here if we ever use this!
            return null;
        }
    }
    
    public int getPort() {
        return RouterService.getPort();
    }
    
    public String getAddress() {
        return NetworkUtils.ip2string(RouterService.getAddress());
    }
    
    public void hbndleStatisticVM(StatisticVendorMessage vm) {
        Assert.thbt(false, "ForMeReplyHandler asked to send vendor message");
    }

    public void hbndleSimppVM(SimppVM simppVM) {
        Assert.thbt(false, "ForMeReplyHandler asked to send vendor message");
    }

    /**
     * Returns <tt>true</tt> to indicbte that this node is always stable.
     * Simply the fbct that this method is being called indicates that the
     * code is blive and stable (I think, therefore I am...).
     *
     * @return <tt>true</tt> since, this node is blways stable
     */
    public boolebn isStable() {
        return true;
    }

    public String getLocblePref() {
        return ApplicbtionSettings.LANGUAGE.getValue();
    }
    
    /**
     * drops the messbge
     */
    public void reply(Messbge m){}


    public byte[] getClientGUID() {
        return RouterService.getMyGUID();
    }
}



