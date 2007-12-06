package com.limegroup.gnutella;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.ByteReader;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.rudp.UDPConnection;

import com.google.inject.Provider;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.LimeWireUtils;

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

    /** The GUID to be used for incoming QRs from the Browse Request. */
    private GUID _guid = null;
    /** The GUID of the servent to send a Push to.  May be null if no push is needed. */
    private GUID _serventID = null;
    
    /** The total length of the http-reply. */
    private volatile long _replyLength = 0;    
    /** The current length of the reply. */
    private volatile long _currentLength = 0;    
    /** The current state of this BH. */
    private volatile int _state = NOT_STARTED;    
    /** The time this state started. */
    private volatile long _stateStarted = 0;
    
    private final BrowseHostHandlerManager.BrowseHostCallback browseHostCallback;
    private final ActivityCallback activityCallback;
    private final SocketsManager socketsManager;
    private final Provider<PushDownloadManager> pushDownloadManager;
    private final Provider<ForMeReplyHandler> forMeReplyHandler;

    private final MessageFactory messageFactory;

    /**
     * @param callback A instance of a ActivityCallback, so I can notify it of
     *        incoming QReps...
     * @param router A instance of a MessageRouter, so I can route messages if
     *        needs be.
     * @param guid The GUID you have associated on the front end with the
     *        results of this Browse Host request.
     * @param serventID May be null, non-null if I need to push
     */
    BrowseHostHandler(GUID guid, GUID serventID,
            BrowseHostHandlerManager.BrowseHostCallback browseHostCallback,
            ActivityCallback activityCallback, SocketsManager socketsManager,
            Provider<PushDownloadManager> pushDownloadManager,
            Provider<ForMeReplyHandler> forMeReplyHandler,
            MessageFactory messageFactory) {
        _guid = guid;
        _serventID = serventID;
        this.browseHostCallback = browseHostCallback;
        this.activityCallback = activityCallback;
        this.socketsManager = socketsManager;
        this.pushDownloadManager = pushDownloadManager;
        this.forMeReplyHandler = forMeReplyHandler;
        this.messageFactory = messageFactory;
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
    public void browseHost(Connectable host, Set<? extends IpPort> proxies,
                           boolean canDoFWTransfer) {
        
        // If this wasn't initially resolved, resolve it now...
        if(host.getInetSocketAddress().isUnresolved()) {
            try {
                host = new ConnectableImpl(host.getAddress(), host.getPort(), host.isTLSCapable());
            } catch(UnknownHostException uhe) {
                failed();
                return;
            }
        }
        
        if(!NetworkUtils.isValidIpPort(host)) {
            failed();
            return;
        }
        
        LOG.trace("Starting browse protocol");
        setState(STARTED);
        
        // flow of operation:
        // 1. check if you need to push.
        //   a. if so, just send a Push out.
        //   b. if not, try direct connect.  If it doesn't work, send a push.
        
        if (canConnectDirectly(host) || isLocalBrowse(host)) {
            try {
                // simply try connecting and getting results....
                setState(DIRECTLY_CONNECTING);
                ConnectType type = host.isTLSCapable() ? ConnectType.TLS : ConnectType.PLAIN;
                if(LOG.isDebugEnabled())
                    LOG.debug("Attempting direct connection with type: " + type);
                Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                DIRECT_CONNECT_TIME, type);
                LOG.trace("Direct connect successful");
                browseExchange(socket);
                
                // browse was successful
                return;
            } catch (IOException ioe) {
                LOG.debug("Error during direct transfer", ioe);
                // try pushing for fun.... (if we have the guid of the servent)
            }
        }
        
        LOG.debug("Attempting push connection");

        if ( _serventID == null ) {
        	LOG.debug("No serventID, failing");
        	failed();
        } else {
        	RemoteFileDesc fakeRFD = 
        		new RemoteFileDesc(host.getAddress(), host.getPort(), SPECIAL_INDEX, "fake", 0, 
        				_serventID.bytes(), 0, false, 0, false,
        				null, null,false,true,"", proxies,
        				-1, canDoFWTransfer ? UDPConnection.VERSION : 0,
        				host.isTLSCapable()); 
        	// register with the map so i get notified about a response to my
        	// Push.
            browseHostCallback.putInfo(_serventID, new PushRequestDetails(this));

        	LOG.trace("Sending push request");
        	setState(PUSHING);

        	// send the Push after registering in case you get a response 
        	// really quickly. 
        	pushDownloadManager.get().sendPush(fakeRFD);
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
    void failed() {
        setState(FINISHED);
        activityCallback.browseHostFailed(_guid);
    }

    void browseExchange(Socket socket) throws IOException {
    	try {
    		browseExchangeInternal(socket);
    	}finally {
            IOUtils.close(socket);
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
        str = "Host: " + NetworkUtils.ip2string(socket.getInetAddress().getAddress()) + ":" + socket.getPort() + LF;
        oStream.write(str.getBytes());
        str = "User-Agent: " + LimeWireUtils.getVendor() + LF;
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
        LOG.trace("BHH.browseExchange(): got input stream");

        // first check the HTTP code, encoding, etc...
        ByteReader br = new ByteReader(in);
        LOG.trace("BHH.browseExchange(): trying to get HTTP code....");
        int code = parseHTTPCode(br.readLine());
        if ((code < 200) || (code >= 300)) {
            if(LOG.isDebugEnabled())
                LOG.debug("Bad code: " + code);
            throw new IOException();
        }
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
        		m = messageFactory.read(in, Network.TCP);
        	} catch (BadPacketException bpe) {
                LOG.debug("BPE while reading", bpe);
        	} catch (IOException expected){
        	    LOG.debug("IOE while reading", expected);
            } // either timeout, or the remote closed.
            
        	if(m == null)  {
                LOG.debug("Unable to read a message");
        		return;
            } else {
        		if(m instanceof QueryReply) {
        			_currentLength += m.getTotalLength();
        			if(LOG.isTraceEnabled())
        				LOG.trace("BHH.browseExchange(): read QR:" + m);
        			QueryReply reply = (QueryReply)m;
        			reply.setGUID(_guid);
        			reply.setBrowseHostReply(true);
					
        			forMeReplyHandler.get().handleQueryReply(reply, null);
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


    /**
	 * Returns true, if browse should be attempted by push download, either
	 * because it is a private address or was unreachable in the past. Returns
	 * false, otherwise or if <tt>host</tt> is the local address. 
	 */
    private boolean canConnectDirectly(IpPort host) {
        return !ConnectionSettings.LOCAL_IS_PRIVATE.getValue() 
        		|| !NetworkUtils.isPrivateAddress(host.getAddress())
        		|| NetworkUtils.isMe(host.getAddress(), host.getPort());
    }

    /**
     * Returns true, if the user attempts to browse in the local network by
     * entering a host and port but not providing a <code>_serventID</code>.
     * This will make a push impossible so a direct connect is attempted
     * instead.
     */
    private boolean isLocalBrowse(IpPort host) {
        return _serventID == null && NetworkUtils.isPrivateAddress(host.getAddress());
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
            throw new IOException("couldn't read anything");
		StringTokenizer tokenizer = new StringTokenizer(str, " ");		
		String token;

		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new IOException("no tokens to read: " + str);

		token = tokenizer.nextToken();
		
		// the first token should contain HTTP
		if (token.toUpperCase().indexOf("HTTP") < 0 )
			throw new IOException("didn't contain HTTP, had: " + token);
		
		// the next token should be a number
		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new IOException("no number token: " + str);

		token = tokenizer.nextToken();
		
		String num = token.trim();
		try {
		    if(LOG.isDebugEnabled())
                LOG.debug("BHH.parseHTTPCode(): returning " + num);
			return java.lang.Integer.parseInt(num);
		} catch (NumberFormatException e) {
			throw new IOException("not a number: " + num);
		}

    }

    public static class PushRequestDetails { 
        private BrowseHostHandler bhh;
        private long timeStamp;
        
        public PushRequestDetails(BrowseHostHandler bhh) {
            timeStamp = System.currentTimeMillis();
            this.bhh = bhh;
        }

        public boolean isExpired() {
            return ((System.currentTimeMillis() - timeStamp) > EXPIRE_TIME);
        }
        
        public BrowseHostHandler getBrowseHostHandler() {
            return bhh;
        }
    }
}
