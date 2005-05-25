package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Sockets;

/**
 * Handles all stuff necessary for browsing of networks hosts. 
 * Has a instance component, one per browse host, and a static Map of instances
 * that is used to coordinate between replies to PushRequests.
 */
public class BrowseHostHandler {
    
    private static final Log LOG = LogFactory.getLog(BrowseHostHandler.class);
    
    /**
     * Various internal states for Browse-Hosting.
     */
    private static final int NOT_STARTED = -1;
    private static final int STARTED = 0;
    private static final int DIRECTLY_CONNECTING = 1;
    private static final int PUSHING = 2;
    private static final int EXCHANGING = 3;
    private static final int FINISHED = 4;

    private static final int DIRECT_CONNECT_TIME = 10000; // 10 seconds.

    private static final long EXPIRE_TIME = 15000; // 15 seconds

    private static final int SPECIAL_INDEX = 0;

    /** Map from serventID to BrowseHostHandler instance.
     */
    private static Map _pushedHosts = new HashMap();

    /** The ActivityCallBack instance.  Used for talking to GUI.
     */
    private ActivityCallback _callback = null;

    /** The GUID to be used for incoming QRs from the Browse Request.
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
     * The current length of the reply.
     */
    private volatile long _currentLength = 0;
    
    /**
     * The current state of this BH.
     */
    private volatile int _state = NOT_STARTED;
    
    /**
     * The time this state started.
     */
    private volatile long _stateStarted = 0;

    static {
        Expirer expirer = new Expirer();
        RouterService.schedule(expirer, 0, 5000);// every 5 seconds
    }

    /**
     * @param callback A instance of a ActivityCallback, so I can notify it of
     * incoming QReps...
     * @param router A instance of a MessageRouter, so I can route messages if
     * needs be.
     * @param guid The GUID you have associated on the front end with the
     * results of this Browse Host request.
     * @param serventID May be null, non-null if I need to push
     */
    public  BrowseHostHandler(ActivityCallback callback, 
                              GUID guid, GUID serventID) {
        _callback = callback;
        _guid = guid;
        _serventID = serventID;
    }

    /** 
     * Browses the files on the specified host and port.
     *
     * @param host The IP of the host you want to browse.
     * @param port The port of the host you want to browse.
     * @param proxies the <tt>Set</tt> of push proxies to try
     * @param canDoFWTransfer Whether or not this guy can do a firewall
     * transfer.
     */
    public void browseHost(String host, int port, Set proxies,
                           boolean canDoFWTransfer) {
        if(!NetworkUtils.isValidPort(port) || 
                                         !NetworkUtils.isValidAddress(host)) {
            failed();
            return;
        }
        LOG.trace("starting browse protocol.");
        setState(STARTED);
        // flow of operation:
        // 1. check if you need to push.
        //   a. if so, just send a Push out.
        //   b. if not, try direct connect.  If it doesn't work, send a push.
        int shouldPush = needsPush(host);
        
        LOG.trace("push needed? " + shouldPush);
        boolean shouldTryPush = false;
        switch (shouldPush) {
        case 0: // false
            try {
                // simply try connecting and getting results....
                setState(DIRECTLY_CONNECTING);
                Socket socket = Sockets.connect(host, port,
                                                DIRECT_CONNECT_TIME);
                LOG.trace("direct connect successful");
                browseExchange(socket);
            } catch (IOException ioe) {
                // try pushing for fun.... (if we have the guid of the servent)
                shouldTryPush = true;
            }
            if (!shouldTryPush) 
                break;
        case 1: // true
            // if we're trying to push & we don't have a servent guid, it fails
            if ( _serventID == null ) {
                failed();
            } else {
                RemoteFileDesc fakeRFD = 
                    new RemoteFileDesc(host, port, SPECIAL_INDEX, "fake", 0, 
                                       _serventID.bytes(), 0, false, 0, false,
                                       null, null,false,true,"",0l, proxies,
                                       -1, canDoFWTransfer ? UDPConnection.VERSION : 0);
                // register with the map so i get notified about a response to my
                // Push.
                synchronized (_pushedHosts) {
                    _pushedHosts.put(_serventID, new PushRequestDetails(this));
                }
                
                LOG.trace("trying push.");
                setState(PUSHING);

                // send the Push after registering in case you get a response 
                // really quickly.  reuse code in DM cuz that works well
                RouterService.getDownloadManager().sendPush(fakeRFD);
                 
            }
            break;
        }
    }

    /**
     * Returns the current percentage complete of the state
     * of the browse host.
     */
    public double getPercentComplete(long currentTime) {
        long elapsed;
        
        switch(_state) {
        case NOT_STARTED: return 0d;
        case STARTED: return 0d;
        case DIRECTLY_CONNECTING:
            // return how long it'll take to connect.
            elapsed = currentTime - _stateStarted;
            return (double) elapsed / DIRECT_CONNECT_TIME;
        case PUSHING:
            // return how long it'll take to push.
            elapsed = currentTime - _stateStarted;
            return (double) elapsed / EXPIRE_TIME;
        case EXCHANGING:
            // return how long it'll take to finish reading,
            // or stay at .5 if we dunno the length.
            if( _replyLength > 0 )
                return (double)_currentLength / _replyLength;
            else
                return 0.5;
        case FINISHED:
            return 1.0;
        default:
            throw new IllegalStateException("invalid state");
        }
    }
        
    /**
     * Sets the state and state-time.
     */
    private void setState(int state) {
        _state = state;
        _stateStarted = System.currentTimeMillis();
    }    
     
    /**
     * Indicates that this browse host has failed.
     */   
    private void failed() {
        setState(FINISHED);
        _callback.browseHostFailed(_guid);
    }

    private void browseExchange(Socket socket) throws IOException {
    	try {
    		browseExchangeInternal(socket);
    	}finally {
    		try{socket.close();}catch(IOException ignored){}
    		setState(FINISHED);
    	}
    }
    private void browseExchangeInternal(Socket socket) throws IOException {
    	
    	//when/if we start reusing connections, remove this timeout
    	socket.setSoTimeout(5000);

        LOG.trace("BHH.browseExchange(): entered.");
        setState(EXCHANGING);
        
        // first write the request...
        final String LF = "\r\n";
        String str = null;
        OutputStream oStream = socket.getOutputStream();
        LOG.trace("BHH.browseExchange(): got output stream.");

        // ask for the browse results..
        str = "GET / HTTP/1.1" + LF;
        oStream.write(str.getBytes());
        str = "Host: " + NetworkUtils.ip2string(RouterService.getAddress()) + 
              ":" + RouterService.getPort() + LF;
        oStream.write(str.getBytes());
        str = "User-Agent: " + CommonUtils.getVendor() + LF;
        oStream.write(str.getBytes());
        str = "Accept: " + Constants.QUERYREPLY_MIME_TYPE + LF;
        oStream.write(str.getBytes());
        str = "Content-Length: 0" + LF;
        oStream.write(str.getBytes());
        str = "Connection: close" + LF;
        oStream.write(str.getBytes());
        str = LF;
        oStream.write(str.getBytes());
        oStream.flush();
        LOG.trace("BHH.browseExchange(): wrote request A-OK.");
        
        // get the results...
        InputStream in = socket.getInputStream();
        LOG.trace("BHH.browseExchange(): got input stream.");

        // first check the HTTP code, encoding, etc...
        ByteReader br = new ByteReader(in);
        LOG.trace("BHH.browseExchange(): trying to get HTTP code....");
        int code = parseHTTPCode(br.readLine());
        if ((code < 200) || (code >= 300))
            throw new IOException();
        if(LOG.isDebugEnabled())
            LOG.debug("BHH.browseExchange(): HTTP Response is " + code);

        // now confirm the content-type, the encoding, etc...
        boolean readingHTTP = true;
        String currLine = null;
        while (readingHTTP) {
            currLine = br.readLine();
            if(LOG.isDebugEnabled())
                LOG.debug("BHH.browseExchange(): currLine = " + currLine);
            if ((currLine == null) || currLine.equals("")) {
                // start processing queries...
                readingHTTP = false;
            }
            else if (indexOfIgnoreCase(currLine, "Content-Type") > -1) {
                // make sure it is QRs....
                if (indexOfIgnoreCase(currLine, 
                                      Constants.QUERYREPLY_MIME_TYPE) < 0)
                    throw new IOException();
            }
            else if (indexOfIgnoreCase(currLine, "Content-Encoding") > -1) {
                throw new IOException();  //  decompress currently not supported
            }
            else if (markContentLength(currLine))
                ; // do nothing special
            
        }
        LOG.debug("BHH.browseExchange(): read HTTP seemingly OK.");
        
	in = new BufferedInputStream(in);

        // ok, everything checks out, proceed and read QRs...
        Message m = null;
        while(true) {
        	try {
        		m = null;
        		LOG.debug("reading message");
        		m = Message.read(in);
        		LOG.debug("read message "+m);
        	}
        	catch (BadPacketException bpe) {LOG.debug(bpe);}
        	catch (IOException expected){} // either timeout, or the remote closed.
        	if(m == null) 
        		return;
        	 else {
        		if(m instanceof QueryReply) {
        			_currentLength += m.getTotalLength();
        			if(LOG.isTraceEnabled())
        				LOG.trace("BHH.browseExchange(): read QR:" + m);
        			QueryReply reply = (QueryReply)m;
        			reply.setBrowseHostReply(true);
        			reply.setGUID(_guid);
					
        			ForMeReplyHandler.instance().handleQueryReply(reply, null);
        		}
        	}
        }
        
        
    }
    
    /**
     * Reads and marks the content-length for this line, if exists.
     */
    private boolean markContentLength(final String line) {
        int idx = indexOfIgnoreCase(line, "Content-Length:");
        if( idx < 0 )
            return false;
            
        // get the string after the ':'
        String length = line.substring("Content-Length:".length()).trim();
        
        try {
            _replyLength = Long.parseLong(length);
        } catch(NumberFormatException ignored) {
            // ignore.
        }
        
        return true;
    }


    /** Returns 1 iff rfd should be attempted by push download, either 
     *  because it is a private address or was unreachable in the past. 
     *  Returns 0 otherwise....
     */
    private static int needsPush(String host) {
        //Return true if rfd is private or unreachable
        if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() && 
          NetworkUtils.isPrivateAddress(host))
            return 1;
        else
            return 0;
    }


	/**
	 * a helper method to compare two strings 
	 * ignoring their case.
	 */ 
	private int indexOfIgnoreCase(String str, String section) {
		// convert both strings to lower case
		String aaa = str.toLowerCase();
		String bbb = section.toLowerCase();
		// then look for the index...
		return aaa.indexOf(bbb);
	}

    /**
     * Returns the HTTP response code from the given string, throwing
     * an exception if it couldn't be parsed.
     *
     * @param str an HTTP response string, e.g., "HTTP 200 OK \r\n"
     * @exception IOException a problem occurred
     */
    private static int parseHTTPCode(String str) throws IOException {		
        if (str == null)
            return -1; // hopefully this won't happen, but if so just error...
		StringTokenizer tokenizer = new StringTokenizer(str, " ");		
		String token;

		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new IOException();

		token = tokenizer.nextToken();
		
		// the first token should contain HTTP
		if (token.toUpperCase().indexOf("HTTP") < 0 )
			throw new IOException();
		
		// the next token should be a number
		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new IOException();

		token = tokenizer.nextToken();
		
		String num = token.trim();
		try {
		    if(LOG.isDebugEnabled())
                LOG.debug("BHH.parseHTTPCode(): returning " + num);
			return java.lang.Integer.parseInt(num);
		} catch (NumberFormatException e) {
			throw new IOException();
		}

    }



    /** @return true if the Push was handled by me.
     */
    public static boolean handlePush(int index, GUID serventID, 
                                     final Socket socket) 
        throws IOException {
        boolean retVal = false;
        LOG.trace("BHH.handlePush(): entered.");
        if (index == SPECIAL_INDEX)
            ; // you'd hope, but not necessary...

        PushRequestDetails prd = null;
        synchronized (_pushedHosts) {
            prd = (PushRequestDetails) _pushedHosts.remove(serventID);
        }
        if (prd != null) {
            final PushRequestDetails finalPRD = prd;
            Thread runLater = new ManagedThread() {
                    public void managedRun() {
                        try {
                            finalPRD.bhh.browseExchange(socket);
                        }
                        catch (IOException ohWell) {
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
            LOG.debug("BHH.handlePush(): no matching BHH.");

        LOG.trace("BHH.handlePush(): returning.");
        return retVal;
    }

    /** Can be run to invalidate pushes that we are waiting for....
     */
    private static class Expirer implements Runnable {
        public void run() {
            try {
                Iterator keys = null;
                Set toRemove = new HashSet();
                synchronized (_pushedHosts) {
                    keys = _pushedHosts.keySet().iterator();
                    while (keys.hasNext()) {
                        Object currKey = keys.next();
                        PushRequestDetails currPRD = null;
                        currPRD = (PushRequestDetails) _pushedHosts.get(currKey);
                        if ((currPRD != null) && (currPRD.isExpired())) {
                            LOG.debug("Expirer.run(): expiring a badboy.");
                            toRemove.add(currKey);
                            currPRD.bhh.failed();
                        }
                    }
                    // done iterating through _pushedHosts, remove the keys now...
                    keys = toRemove.iterator();
                    while (keys.hasNext())
                        _pushedHosts.remove(keys.next());
                }
            } catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }

    private static class PushRequestDetails { 
        private BrowseHostHandler bhh;
        private long timeStamp;
        
        public PushRequestDetails(BrowseHostHandler bhh) {
            timeStamp = System.currentTimeMillis();
            this.bhh = bhh;
        }

        public boolean isExpired() {
            return ((System.currentTimeMillis() - timeStamp) > EXPIRE_TIME);
        }
    }
}
