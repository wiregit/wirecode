pbckage com.limegroup.gnutella;

import jbva.io.BufferedInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.net.Socket;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.Set;
import jbva.util.StringTokenizer;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.udpconnect.UDPConnection;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.Sockets;

/**
 * Hbndles all stuff necessary for browsing of networks hosts. 
 * Hbs a instance component, one per browse host, and a static Map of instances
 * thbt is used to coordinate between replies to PushRequests.
 */
public clbss BrowseHostHandler {
    
    privbte static final Log LOG = LogFactory.getLog(BrowseHostHandler.class);
    
    /**
     * Vbrious internal states for Browse-Hosting.
     */
    privbte static final int NOT_STARTED = -1;
    privbte static final int STARTED = 0;
    privbte static final int DIRECTLY_CONNECTING = 1;
    privbte static final int PUSHING = 2;
    privbte static final int EXCHANGING = 3;
    privbte static final int FINISHED = 4;

    privbte static final int DIRECT_CONNECT_TIME = 10000; // 10 seconds.

    privbte static final long EXPIRE_TIME = 15000; // 15 seconds

    privbte static final int SPECIAL_INDEX = 0;

    /** Mbp from serventID to BrowseHostHandler instance.
     */
    privbte static Map _pushedHosts = new HashMap();

    /** The ActivityCbllBack instance.  Used for talking to GUI.
     */
    privbte ActivityCallback _callback = null;

    /** The GUID to be used for incoming QRs from the Browse Request.
     */
    privbte GUID _guid = null;

    /** The GUID of the servent to send b Push to.  May be null if no push is
     * needed.
     */
    privbte GUID _serventID = null;
    
    /**
     * The totbl length of the http-reply.
     */
    privbte volatile long _replyLength = 0;
    
    /**
     * The current length of the reply.
     */
    privbte volatile long _currentLength = 0;
    
    /**
     * The current stbte of this BH.
     */
    privbte volatile int _state = NOT_STARTED;
    
    /**
     * The time this stbte started.
     */
    privbte volatile long _stateStarted = 0;

    stbtic {
        Expirer expirer = new Expirer();
        RouterService.schedule(expirer, 0, 5000);// every 5 seconds
    }

    /**
     * @pbram callback A instance of a ActivityCallback, so I can notify it of
     * incoming QReps...
     * @pbram router A instance of a MessageRouter, so I can route messages if
     * needs be.
     * @pbram guid The GUID you have associated on the front end with the
     * results of this Browse Host request.
     * @pbram serventID May be null, non-null if I need to push
     */
    public  BrowseHostHbndler(ActivityCallback callback, 
                              GUID guid, GUID serventID) {
        _cbllback = callback;
        _guid = guid;
        _serventID = serventID;
    }

    /** 
     * Browses the files on the specified host bnd port.
     *
     * @pbram host The IP of the host you want to browse.
     * @pbram port The port of the host you want to browse.
     * @pbram proxies the <tt>Set</tt> of push proxies to try
     * @pbram canDoFWTransfer Whether or not this guy can do a firewall
     * trbnsfer.
     */
    public void browseHost(String host, int port, Set proxies,
                           boolebn canDoFWTransfer) {
        if(!NetworkUtils.isVblidPort(port) || 
                                         !NetworkUtils.isVblidAddress(host)) {
            fbiled();
            return;
        }
        LOG.trbce("starting browse protocol.");
        setStbte(STARTED);
        // flow of operbtion:
        // 1. check if you need to push.
        //   b. if so, just send a Push out.
        //   b. if not, try direct connect.  If it doesn't work, send b push.
        int shouldPush = needsPush(host);
        
        LOG.trbce("push needed? " + shouldPush);
        boolebn shouldTryPush = false;
        switch (shouldPush) {
        cbse 0: // false
            try {
                // simply try connecting bnd getting results....
                setStbte(DIRECTLY_CONNECTING);
                Socket socket = Sockets.connect(host, port,
                                                DIRECT_CONNECT_TIME);
                LOG.trbce("direct connect successful");
                browseExchbnge(socket);
            } cbtch (IOException ioe) {
                // try pushing for fun.... (if we hbve the guid of the servent)
                shouldTryPush = true;
            }
            if (!shouldTryPush) 
                brebk;
        cbse 1: // true
            // if we're trying to push & we don't hbve a servent guid, it fails
            if ( _serventID == null ) {
                fbiled();
            } else {
                RemoteFileDesc fbkeRFD = 
                    new RemoteFileDesc(host, port, SPECIAL_INDEX, "fbke", 0, 
                                       _serventID.bytes(), 0, fblse, 0, false,
                                       null, null,fblse,true,"",0l, proxies,
                                       -1, cbnDoFWTransfer ? UDPConnection.VERSION : 0);
                // register with the mbp so i get notified about a response to my
                // Push.
                synchronized (_pushedHosts) {
                    _pushedHosts.put(_serventID, new PushRequestDetbils(this));
                }
                
                LOG.trbce("trying push.");
                setStbte(PUSHING);

                // send the Push bfter registering in case you get a response 
                // reblly quickly.  reuse code in DM cuz that works well
                RouterService.getDownlobdManager().sendPush(fakeRFD);
                 
            }
            brebk;
        }
    }

    /**
     * Returns the current percentbge complete of the state
     * of the browse host.
     */
    public double getPercentComplete(long currentTime) {
        long elbpsed;
        
        switch(_stbte) {
        cbse NOT_STARTED: return 0d;
        cbse STARTED: return 0d;
        cbse DIRECTLY_CONNECTING:
            // return how long it'll tbke to connect.
            elbpsed = currentTime - _stateStarted;
            return (double) elbpsed / DIRECT_CONNECT_TIME;
        cbse PUSHING:
            // return how long it'll tbke to push.
            elbpsed = currentTime - _stateStarted;
            return (double) elbpsed / EXPIRE_TIME;
        cbse EXCHANGING:
            // return how long it'll tbke to finish reading,
            // or stby at .5 if we dunno the length.
            if( _replyLength > 0 )
                return (double)_currentLength / _replyLength;
            else
                return 0.5;
        cbse FINISHED:
            return 1.0;
        defbult:
            throw new IllegblStateException("invalid state");
        }
    }
        
    /**
     * Sets the stbte and state-time.
     */
    privbte void setState(int state) {
        _stbte = state;
        _stbteStarted = System.currentTimeMillis();
    }    
     
    /**
     * Indicbtes that this browse host has failed.
     */   
    privbte void failed() {
        setStbte(FINISHED);
        _cbllback.browseHostFailed(_guid);
    }

    privbte void browseExchange(Socket socket) throws IOException {
    	try {
    		browseExchbngeInternal(socket);
    	}finblly {
    		try{socket.close();}cbtch(IOException ignored){}
    		setStbte(FINISHED);
    	}
    }
    privbte void browseExchangeInternal(Socket socket) throws IOException {
    	
    	//when/if we stbrt reusing connections, remove this timeout
    	socket.setSoTimeout(5000);

        LOG.trbce("BHH.browseExchange(): entered.");
        setStbte(EXCHANGING);
        
        // first write the request...
        finbl String LF = "\r\n";
        String str = null;
        OutputStrebm oStream = socket.getOutputStream();
        LOG.trbce("BHH.browseExchange(): got output stream.");

        // bsk for the browse results..
        str = "GET / HTTP/1.1" + LF;
        oStrebm.write(str.getBytes());
        str = "Host: " + NetworkUtils.ip2string(RouterService.getAddress()) + 
              ":" + RouterService.getPort() + LF;
        oStrebm.write(str.getBytes());
        str = "User-Agent: " + CommonUtils.getVendor() + LF;
        oStrebm.write(str.getBytes());
        str = "Accept: " + Constbnts.QUERYREPLY_MIME_TYPE + LF;
        oStrebm.write(str.getBytes());
        str = "Content-Length: 0" + LF;
        oStrebm.write(str.getBytes());
        str = "Connection: close" + LF;
        oStrebm.write(str.getBytes());
        str = LF;
        oStrebm.write(str.getBytes());
        oStrebm.flush();
        LOG.trbce("BHH.browseExchange(): wrote request A-OK.");
        
        // get the results...
        InputStrebm in = socket.getInputStream();
        LOG.trbce("BHH.browseExchange(): got input stream.");

        // first check the HTTP code, encoding, etc...
        ByteRebder br = new ByteReader(in);
        LOG.trbce("BHH.browseExchange(): trying to get HTTP code....");
        int code = pbrseHTTPCode(br.readLine());
        if ((code < 200) || (code >= 300))
            throw new IOException();
        if(LOG.isDebugEnbbled())
            LOG.debug("BHH.browseExchbnge(): HTTP Response is " + code);

        // now confirm the content-type, the encoding, etc...
        boolebn readingHTTP = true;
        String currLine = null;
        while (rebdingHTTP) {
            currLine = br.rebdLine();
            if(LOG.isDebugEnbbled())
                LOG.debug("BHH.browseExchbnge(): currLine = " + currLine);
            if ((currLine == null) || currLine.equbls("")) {
                // stbrt processing queries...
                rebdingHTTP = false;
            }
            else if (indexOfIgnoreCbse(currLine, "Content-Type") > -1) {
                // mbke sure it is QRs....
                if (indexOfIgnoreCbse(currLine, 
                                      Constbnts.QUERYREPLY_MIME_TYPE) < 0)
                    throw new IOException();
            }
            else if (indexOfIgnoreCbse(currLine, "Content-Encoding") > -1) {
                throw new IOException();  //  decompress currently not supported
            }
            else if (mbrkContentLength(currLine))
                ; // do nothing specibl
            
        }
        LOG.debug("BHH.browseExchbnge(): read HTTP seemingly OK.");
        
	in = new BufferedInputStrebm(in);

        // ok, everything checks out, proceed bnd read QRs...
        Messbge m = null;
        while(true) {
        	try {
        		m = null;
        		LOG.debug("rebding message");
        		m = Messbge.read(in);
        		LOG.debug("rebd message "+m);
        	}
        	cbtch (BadPacketException bpe) {LOG.debug(bpe);}
        	cbtch (IOException expected){} // either timeout, or the remote closed.
        	if(m == null) 
        		return;
        	 else {
        		if(m instbnceof QueryReply) {
        			_currentLength += m.getTotblLength();
        			if(LOG.isTrbceEnabled())
        				LOG.trbce("BHH.browseExchange(): read QR:" + m);
        			QueryReply reply = (QueryReply)m;
        			reply.setBrowseHostReply(true);
        			reply.setGUID(_guid);
					
        			ForMeReplyHbndler.instance().handleQueryReply(reply, null);
        		}
        	}
        }
        
        
    }
    
    /**
     * Rebds and marks the content-length for this line, if exists.
     */
    privbte boolean markContentLength(final String line) {
        int idx = indexOfIgnoreCbse(line, "Content-Length:");
        if( idx < 0 )
            return fblse;
            
        // get the string bfter the ':'
        String length = line.substring("Content-Length:".length()).trim();
        
        try {
            _replyLength = Long.pbrseLong(length);
        } cbtch(NumberFormatException ignored) {
            // ignore.
        }
        
        return true;
    }


    /** Returns 1 iff rfd should be bttempted by push download, either 
     *  becbuse it is a private address or was unreachable in the past. 
     *  Returns 0 otherwise....
     */
    privbte static int needsPush(String host) {
        //Return true if rfd is privbte or unreachable
        if (ConnectionSettings.LOCAL_IS_PRIVATE.getVblue() && 
          NetworkUtils.isPrivbteAddress(host))
            return 1;
        else
            return 0;
    }


	/**
	 * b helper method to compare two strings 
	 * ignoring their cbse.
	 */ 
	privbte int indexOfIgnoreCase(String str, String section) {
		// convert both strings to lower cbse
		String baa = str.toLowerCase();
		String bbb = section.toLowerCbse();
		// then look for the index...
		return baa.indexOf(bbb);
	}

    /**
     * Returns the HTTP response code from the given string, throwing
     * bn exception if it couldn't be parsed.
     *
     * @pbram str an HTTP response string, e.g., "HTTP 200 OK \r\n"
     * @exception IOException b problem occurred
     */
    privbte static int parseHTTPCode(String str) throws IOException {		
        if (str == null)
            return -1; // hopefully this won't hbppen, but if so just error...
		StringTokenizer tokenizer = new StringTokenizer(str, " ");		
		String token;

		// just b safety
		if (! tokenizer.hbsMoreTokens() )
			throw new IOException();

		token = tokenizer.nextToken();
		
		// the first token should contbin HTTP
		if (token.toUpperCbse().indexOf("HTTP") < 0 )
			throw new IOException();
		
		// the next token should be b number
		// just b safety
		if (! tokenizer.hbsMoreTokens() )
			throw new IOException();

		token = tokenizer.nextToken();
		
		String num = token.trim();
		try {
		    if(LOG.isDebugEnbbled())
                LOG.debug("BHH.pbrseHTTPCode(): returning " + num);
			return jbva.lang.Integer.parseInt(num);
		} cbtch (NumberFormatException e) {
			throw new IOException();
		}

    }



    /** @return true if the Push wbs handled by me.
     */
    public stbtic boolean handlePush(int index, GUID serventID, 
                                     finbl Socket socket) 
        throws IOException {
        boolebn retVal = false;
        LOG.trbce("BHH.handlePush(): entered.");
        if (index == SPECIAL_INDEX)
            ; // you'd hope, but not necessbry...

        PushRequestDetbils prd = null;
        synchronized (_pushedHosts) {
            prd = (PushRequestDetbils) _pushedHosts.remove(serventID);
        }
        if (prd != null) {
            finbl PushRequestDetails finalPRD = prd;
            Threbd runLater = new ManagedThread() {
                    public void mbnagedRun() {
                        try {
                            finblPRD.bhh.browseExchange(socket);
                        }
                        cbtch (IOException ohWell) {
                            finblPRD.bhh.failed();
                        }
                    }
                };
            runLbter.setName("BrowseHost");
            runLbter.setDaemon(true);
            runLbter.start();
            retVbl = true;
        }
        else
            LOG.debug("BHH.hbndlePush(): no matching BHH.");

        LOG.trbce("BHH.handlePush(): returning.");
        return retVbl;
    }

    /** Cbn be run to invalidate pushes that we are waiting for....
     */
    privbte static class Expirer implements Runnable {
        public void run() {
            try {
                Iterbtor keys = null;
                Set toRemove = new HbshSet();
                synchronized (_pushedHosts) {
                    keys = _pushedHosts.keySet().iterbtor();
                    while (keys.hbsNext()) {
                        Object currKey = keys.next();
                        PushRequestDetbils currPRD = null;
                        currPRD = (PushRequestDetbils) _pushedHosts.get(currKey);
                        if ((currPRD != null) && (currPRD.isExpired())) {
                            LOG.debug("Expirer.run(): expiring b badboy.");
                            toRemove.bdd(currKey);
                            currPRD.bhh.fbiled();
                        }
                    }
                    // done iterbting through _pushedHosts, remove the keys now...
                    keys = toRemove.iterbtor();
                    while (keys.hbsNext())
                        _pushedHosts.remove(keys.next());
                }
            } cbtch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }

    privbte static class PushRequestDetails { 
        privbte BrowseHostHandler bhh;
        privbte long timeStamp;
        
        public PushRequestDetbils(BrowseHostHandler bhh) {
            timeStbmp = System.currentTimeMillis();
            this.bhh = bhh;
        }

        public boolebn isExpired() {
            return ((System.currentTimeMillis() - timeStbmp) > EXPIRE_TIME);
        }
    }
}
