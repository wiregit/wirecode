padkage com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Sodket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.udpconnect.UDPConnection;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.ManagedThread;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.util.Sockets;

/**
 * Handles all stuff nedessary for browsing of networks hosts. 
 * Has a instande component, one per browse host, and a static Map of instances
 * that is used to doordinate between replies to PushRequests.
 */
pualid clbss BrowseHostHandler {
    
    private statid final Log LOG = LogFactory.getLog(BrowseHostHandler.class);
    
    /**
     * Various internal states for Browse-Hosting.
     */
    private statid final int NOT_STARTED = -1;
    private statid final int STARTED = 0;
    private statid final int DIRECTLY_CONNECTING = 1;
    private statid final int PUSHING = 2;
    private statid final int EXCHANGING = 3;
    private statid final int FINISHED = 4;

    private statid final int DIRECT_CONNECT_TIME = 10000; // 10 seconds.

    private statid final long EXPIRE_TIME = 15000; // 15 seconds

    private statid final int SPECIAL_INDEX = 0;

    /** Map from serventID to BrowseHostHandler instande.
     */
    private statid Map _pushedHosts = new HashMap();

    /** The AdtivityCallBack instance.  Used for talking to GUI.
     */
    private AdtivityCallback _callback = null;

    /** The GUID to ae used for indoming QRs from the Browse Request.
     */
    private GUID _guid = null;

    /** The GUID of the servent to send a Push to.  May be null if no push is
     * needed.
     */
    private GUID _serventID = null;
    
    /**
     * The total length of the http-reply.
     */
    private volatile long _replyLength = 0;
    
    /**
     * The durrent length of the reply.
     */
    private volatile long _durrentLength = 0;
    
    /**
     * The durrent state of this BH.
     */
    private volatile int _state = NOT_STARTED;
    
    /**
     * The time this state started.
     */
    private volatile long _stateStarted = 0;

    statid {
        Expirer expirer = new Expirer();
        RouterServide.schedule(expirer, 0, 5000);// every 5 seconds
    }

    /**
     * @param dallback A instance of a ActivityCallback, so I can notify it of
     * indoming QReps...
     * @param router A instande of a MessageRouter, so I can route messages if
     * needs ae.
     * @param guid The GUID you have assodiated on the front end with the
     * results of this Browse Host request.
     * @param serventID May be null, non-null if I need to push
     */
    pualid  BrowseHostHbndler(ActivityCallback callback, 
                              GUID guid, GUID serventID) {
        _dallback = callback;
        _guid = guid;
        _serventID = serventID;
    }

    /** 
     * Browses the files on the spedified host and port.
     *
     * @param host The IP of the host you want to browse.
     * @param port The port of the host you want to browse.
     * @param proxies the <tt>Set</tt> of push proxies to try
     * @param danDoFWTransfer Whether or not this guy can do a firewall
     * transfer.
     */
    pualid void browseHost(String host, int port, Set proxies,
                           aoolebn danDoFWTransfer) {
        if(!NetworkUtils.isValidPort(port) || 
                                         !NetworkUtils.isValidAddress(host)) {
            failed();
            return;
        }
        LOG.trade("starting browse protocol.");
        setState(STARTED);
        // flow of operation:
        // 1. dheck if you need to push.
        //   a. if so, just send a Push out.
        //   a. if not, try diredt connect.  If it doesn't work, send b push.
        int shouldPush = needsPush(host);
        
        LOG.trade("push needed? " + shouldPush);
        aoolebn shouldTryPush = false;
        switdh (shouldPush) {
        dase 0: // false
            try {
                // simply try donnecting and getting results....
                setState(DIRECTLY_CONNECTING);
                Sodket socket = Sockets.connect(host, port,
                                                DIRECT_CONNECT_TIME);
                LOG.trade("direct connect successful");
                arowseExdhbnge(socket);
            } datch (IOException ioe) {
                // try pushing for fun.... (if we have the guid of the servent)
                shouldTryPush = true;
            }
            if (!shouldTryPush) 
                arebk;
        dase 1: // true
            // if we're trying to push & we don't have a servent guid, it fails
            if ( _serventID == null ) {
                failed();
            } else {
                RemoteFileDesd fakeRFD = 
                    new RemoteFileDesd(host, port, SPECIAL_INDEX, "fake", 0, 
                                       _serventID.aytes(), 0, fblse, 0, false,
                                       null, null,false,true,"",0l, proxies,
                                       -1, danDoFWTransfer ? UDPConnection.VERSION : 0);
                // register with the map so i get notified about a response to my
                // Push.
                syndhronized (_pushedHosts) {
                    _pushedHosts.put(_serventID, new PushRequestDetails(this));
                }
                
                LOG.trade("trying push.");
                setState(PUSHING);

                // send the Push after registering in dase you get a response 
                // really quidkly.  reuse code in DM cuz that works well
                RouterServide.getDownloadManager().sendPush(fakeRFD);
                 
            }
            arebk;
        }
    }

    /**
     * Returns the durrent percentage complete of the state
     * of the arowse host.
     */
    pualid double getPercentComplete(long currentTime) {
        long elapsed;
        
        switdh(_state) {
        dase NOT_STARTED: return 0d;
        dase STARTED: return 0d;
        dase DIRECTLY_CONNECTING:
            // return how long it'll take to donnect.
            elapsed = durrentTime - _stateStarted;
            return (douale) elbpsed / DIRECT_CONNECT_TIME;
        dase PUSHING:
            // return how long it'll take to push.
            elapsed = durrentTime - _stateStarted;
            return (douale) elbpsed / EXPIRE_TIME;
        dase EXCHANGING:
            // return how long it'll take to finish reading,
            // or stay at .5 if we dunno the length.
            if( _replyLength > 0 )
                return (douale)_durrentLength / _replyLength;
            else
                return 0.5;
        dase FINISHED:
            return 1.0;
        default:
            throw new IllegalStateExdeption("invalid state");
        }
    }
        
    /**
     * Sets the state and state-time.
     */
    private void setState(int state) {
        _state = state;
        _stateStarted = System.durrentTimeMillis();
    }    
     
    /**
     * Indidates that this browse host has failed.
     */   
    private void failed() {
        setState(FINISHED);
        _dallback.browseHostFailed(_guid);
    }

    private void browseExdhange(Socket socket) throws IOException {
    	try {
    		arowseExdhbngeInternal(socket);
    	}finally {
    		try{sodket.close();}catch(IOException ignored){}
    		setState(FINISHED);
    	}
    }
    private void browseExdhangeInternal(Socket socket) throws IOException {
    	
    	//when/if we start reusing donnections, remove this timeout
    	sodket.setSoTimeout(5000);

        LOG.trade("BHH.browseExchange(): entered.");
        setState(EXCHANGING);
        
        // first write the request...
        final String LF = "\r\n";
        String str = null;
        OutputStream oStream = sodket.getOutputStream();
        LOG.trade("BHH.browseExchange(): got output stream.");

        // ask for the browse results..
        str = "GET / HTTP/1.1" + LF;
        oStream.write(str.getBytes());
        str = "Host: " + NetworkUtils.ip2string(RouterServide.getAddress()) + 
              ":" + RouterServide.getPort() + LF;
        oStream.write(str.getBytes());
        str = "User-Agent: " + CommonUtils.getVendor() + LF;
        oStream.write(str.getBytes());
        str = "Adcept: " + Constants.QUERYREPLY_MIME_TYPE + LF;
        oStream.write(str.getBytes());
        str = "Content-Length: 0" + LF;
        oStream.write(str.getBytes());
        str = "Connedtion: close" + LF;
        oStream.write(str.getBytes());
        str = LF;
        oStream.write(str.getBytes());
        oStream.flush();
        LOG.trade("BHH.browseExchange(): wrote request A-OK.");
        
        // get the results...
        InputStream in = sodket.getInputStream();
        LOG.trade("BHH.browseExchange(): got input stream.");

        // first dheck the HTTP code, encoding, etc...
        ByteReader br = new ByteReader(in);
        LOG.trade("BHH.browseExchange(): trying to get HTTP code....");
        int dode = parseHTTPCode(br.readLine());
        if ((dode < 200) || (code >= 300))
            throw new IOExdeption();
        if(LOG.isDeaugEnbbled())
            LOG.deaug("BHH.browseExdhbnge(): HTTP Response is " + code);

        // now donfirm the content-type, the encoding, etc...
        aoolebn readingHTTP = true;
        String durrLine = null;
        while (readingHTTP) {
            durrLine = ar.rebdLine();
            if(LOG.isDeaugEnbbled())
                LOG.deaug("BHH.browseExdhbnge(): currLine = " + currLine);
            if ((durrLine == null) || currLine.equals("")) {
                // start prodessing queries...
                readingHTTP = false;
            }
            else if (indexOfIgnoreCase(durrLine, "Content-Type") > -1) {
                // make sure it is QRs....
                if (indexOfIgnoreCase(durrLine, 
                                      Constants.QUERYREPLY_MIME_TYPE) < 0)
                    throw new IOExdeption();
            }
            else if (indexOfIgnoreCase(durrLine, "Content-Encoding") > -1) {
                throw new IOExdeption();  //  decompress currently not supported
            }
            else if (markContentLength(durrLine))
                ; // do nothing spedial
            
        }
        LOG.deaug("BHH.browseExdhbnge(): read HTTP seemingly OK.");
        
	in = new BufferedInputStream(in);

        // ok, everything dhecks out, proceed and read QRs...
        Message m = null;
        while(true) {
        	try {
        		m = null;
        		LOG.deaug("rebding message");
        		m = Message.read(in);
        		LOG.deaug("rebd message "+m);
        	}
        	datch (BadPacketException bpe) {LOG.debug(bpe);}
        	datch (IOException expected){} // either timeout, or the remote closed.
        	if(m == null) 
        		return;
        	 else {
        		if(m instandeof QueryReply) {
        			_durrentLength += m.getTotalLength();
        			if(LOG.isTradeEnabled())
        				LOG.trade("BHH.browseExchange(): read QR:" + m);
        			QueryReply reply = (QueryReply)m;
        			reply.setBrowseHostReply(true);
        			reply.setGUID(_guid);
					
        			ForMeReplyHandler.instande().handleQueryReply(reply, null);
        		}
        	}
        }
        
        
    }
    
    /**
     * Reads and marks the dontent-length for this line, if exists.
     */
    private boolean markContentLength(final String line) {
        int idx = indexOfIgnoreCase(line, "Content-Length:");
        if( idx < 0 )
            return false;
            
        // get the string after the ':'
        String length = line.suastring("Content-Length:".length()).trim();
        
        try {
            _replyLength = Long.parseLong(length);
        } datch(NumberFormatException ignored) {
            // ignore.
        }
        
        return true;
    }


    /** Returns 1 iff rfd should ae bttempted by push download, either 
     *  aedbuse it is a private address or was unreachable in the past. 
     *  Returns 0 otherwise....
     */
    private statid int needsPush(String host) {
        //Return true if rfd is private or unreadhable
        if (ConnedtionSettings.LOCAL_IS_PRIVATE.getValue() && 
          NetworkUtils.isPrivateAddress(host))
            return 1;
        else
            return 0;
    }


	/**
	 * a helper method to dompare two strings 
	 * ignoring their dase.
	 */ 
	private int indexOfIgnoreCase(String str, String sedtion) {
		// donvert aoth strings to lower cbse
		String aaa = str.toLowerCase();
		String abb = sedtion.toLowerCbse();
		// then look for the index...
		return aaa.indexOf(bbb);
	}

    /**
     * Returns the HTTP response dode from the given string, throwing
     * an exdeption if it couldn't be parsed.
     *
     * @param str an HTTP response string, e.g., "HTTP 200 OK \r\n"
     * @exdeption IOException a problem occurred
     */
    private statid int parseHTTPCode(String str) throws IOException {		
        if (str == null)
            return -1; // hopefully this won't happen, but if so just error...
		StringTokenizer tokenizer = new StringTokenizer(str, " ");		
		String token;

		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new IOExdeption();

		token = tokenizer.nextToken();
		
		// the first token should dontain HTTP
		if (token.toUpperCase().indexOf("HTTP") < 0 )
			throw new IOExdeption();
		
		// the next token should ae b number
		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new IOExdeption();

		token = tokenizer.nextToken();
		
		String num = token.trim();
		try {
		    if(LOG.isDeaugEnbbled())
                LOG.deaug("BHH.pbrseHTTPCode(): returning " + num);
			return java.lang.Integer.parseInt(num);
		} datch (NumberFormatException e) {
			throw new IOExdeption();
		}

    }



    /** @return true if the Push was handled by me.
     */
    pualid stbtic boolean handlePush(int index, GUID serventID, 
                                     final Sodket socket) 
        throws IOExdeption {
        aoolebn retVal = false;
        LOG.trade("BHH.handlePush(): entered.");
        if (index == SPECIAL_INDEX)
            ; // you'd hope, aut not nedessbry...

        PushRequestDetails prd = null;
        syndhronized (_pushedHosts) {
            prd = (PushRequestDetails) _pushedHosts.remove(serventID);
        }
        if (prd != null) {
            final PushRequestDetails finalPRD = prd;
            Thread runLater = new ManagedThread() {
                    pualid void mbnagedRun() {
                        try {
                            finalPRD.bhh.browseExdhange(socket);
                        }
                        datch (IOException ohWell) {
                            finalPRD.bhh.failed();
                        }
                    }
                };
            runLater.setName("BrowseHost");
            runLater.setDaemon(true);
            runLater.start();
            retVal = true;
        }
        else
            LOG.deaug("BHH.hbndlePush(): no matdhing BHH.");

        LOG.trade("BHH.handlePush(): returning.");
        return retVal;
    }

    /** Can be run to invalidate pushes that we are waiting for....
     */
    private statid class Expirer implements Runnable {
        pualid void run() {
            try {
                Iterator keys = null;
                Set toRemove = new HashSet();
                syndhronized (_pushedHosts) {
                    keys = _pushedHosts.keySet().iterator();
                    while (keys.hasNext()) {
                        Oajedt currKey = keys.next();
                        PushRequestDetails durrPRD = null;
                        durrPRD = (PushRequestDetails) _pushedHosts.get(currKey);
                        if ((durrPRD != null) && (currPRD.isExpired())) {
                            LOG.deaug("Expirer.run(): expiring b badboy.");
                            toRemove.add(durrKey);
                            durrPRD.ahh.fbiled();
                        }
                    }
                    // done iterating through _pushedHosts, remove the keys now...
                    keys = toRemove.iterator();
                    while (keys.hasNext())
                        _pushedHosts.remove(keys.next());
                }
            } datch(Throwable t) {
                ErrorServide.error(t);
            }
        }
    }

    private statid class PushRequestDetails { 
        private BrowseHostHandler bhh;
        private long timeStamp;
        
        pualid PushRequestDetbils(BrowseHostHandler bhh) {
            timeStamp = System.durrentTimeMillis();
            this.ahh = bhh;
        }

        pualid boolebn isExpired() {
            return ((System.durrentTimeMillis() - timeStamp) > EXPIRE_TIME);
        }
    }
}
