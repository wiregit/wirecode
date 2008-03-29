package com.limegroup.gnutella;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpProtocolParams;
import org.limewire.http.httpclient.SocketWrappingHttpClient;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.rudp.RUDPUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.StringUtils;

import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
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

    static final int DIRECT_CONNECT_TIME = 10000; // 10 seconds.

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
    private final Provider<ReplyHandler> forMeReplyHandler;

    private final MessageFactory messageFactory;
    private final RemoteFileDescFactory remoteFileDescFactory;
    private final Provider<SocketWrappingHttpClient> clientProvider;
    private final NetworkInstanceUtils networkInstanceUtils;


    /**
     * @param guid The GUID you have associated on the front end with the
     *        results of this Browse Host request.
     * @param serventID May be null, non-null if I need to push
     * @param clientProvider used to make an HTTP client request over an *incoming* Socket
     */
    BrowseHostHandler(GUID guid, GUID serventID,
                      BrowseHostHandlerManager.BrowseHostCallback browseHostCallback,
                      ActivityCallback activityCallback, SocketsManager socketsManager,
                      Provider<PushDownloadManager> pushDownloadManager,
                      @Named("forMeReplyHandler")Provider<ReplyHandler> forMeReplyHandler,
                      MessageFactory messageFactory,
                      RemoteFileDescFactory remoteFileDescFactory,
                      Provider<SocketWrappingHttpClient> clientProvider, 
                      NetworkInstanceUtils networkInstanceUtils) {
        _guid = guid;
        _serventID = serventID;
        this.browseHostCallback = browseHostCallback;
        this.activityCallback = activityCallback;
        this.socketsManager = socketsManager;
        this.pushDownloadManager = pushDownloadManager;
        this.forMeReplyHandler = forMeReplyHandler;
        this.messageFactory = messageFactory;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.clientProvider = clientProvider;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /** 
     * Browses the files on the specified host and port.
     *
     * @param host The IP and port of the host you want to browse, can be null for firewalled endpoint
     * @param port The port of the host you want to browse.
     * @param proxies the <tt>Set</tt> of push proxies to try
     * @param canDoFWTransfer Whether or not this guy can do a firewall
     * transfer.
     */
    public void browseHost(Connectable host, Set<? extends IpPort> proxies,
                           boolean canDoFWTransfer) {
        
        if (host == null) {
            // if host is null we can't do fwts
            assert !canDoFWTransfer : "Can't do fwts without host";
            try {
                setState(STARTED);
                browseFirewalledHost(createInvalidHost(), proxies, canDoFWTransfer);
            } catch (UnknownHostException e) {
                failed();
                ErrorService.error(e, "Can't resolve host, should not happen");
            }
            return;
        }
        
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
                browseHost(socket);

                // browse was successful
                return;
            } catch (IOException e) {
                LOG.debug("Error during direct transfer", e);                
            } catch (HttpException e) {
                LOG.debug("Error during direct transfer", e);
            } catch (URISyntaxException e) {
                LOG.debug("Error during direct transfer", e);
            } catch (InterruptedException e) {
                LOG.debug("Error during direct transfer", e);
            }
        }
        
        // try pushing for fun.... (if we have the guid of the servent)
        // fall back on push if possible
        browseFirewalledHost(host, proxies, canDoFWTransfer);        
    }
    
    /**
     * Expects a non-null host, but host can be an invalid one.
     */
    private void browseFirewalledHost(Connectable host, Set<? extends IpPort> proxies,
                           boolean canDoFWTransfer) {
        
        LOG.debug("Attempting push connection");

        if ( _serventID == null ) {
        	LOG.debug("No serventID, failing");
        	failed();
        } else {
        	RemoteFileDesc fakeRFD = 
        		remoteFileDescFactory.createRemoteFileDesc(host.getAddress(), host.getPort(), SPECIAL_INDEX, "fake",
                    0, _serventID.bytes(), 0, false, 0, false, null, null, false, true, "", proxies,
                    -1, canDoFWTransfer ? RUDPUtils.VERSION : 0, host.isTLSCapable()); 
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
     * Creates an invalid host for pushes.
     */
    static Connectable createInvalidHost() throws UnknownHostException {
        return new ConnectableImpl("0.0.0.0", 1, false);
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

    void browseHost(Socket socket) throws IOException, URISyntaxException, HttpException, InterruptedException {
    	try {
            setState(EXCHANGING);
            HttpResponse response = makeHTTPRequest(socket);
            validateResponse(response);
            readQueryRepliesFromStream(response);
        } finally {
            IOUtils.close(socket);
    		setState(FINISHED);
    	}
    }

    private HttpResponse makeHTTPRequest(Socket socket) throws IOException, URISyntaxException, HttpException, InterruptedException {
        SocketWrappingHttpClient client = clientProvider.get();
        client.setSocket(socket);
        // TODO
        // hardcoding to "http" should work;
        // socket has already been established
        HttpGet get = new HttpGet("http://" + NetworkUtils.ip2string(socket.getInetAddress().getAddress()) + ":" + socket.getPort() + "/");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_1);
        
        get.addHeader("Host", NetworkUtils.ip2string(socket.getInetAddress().getAddress()) + ":" + socket.getPort());
        get.addHeader("User-Agent", LimeWireUtils.getVendor());
        get.addHeader("Accept", Constants.QUERYREPLY_MIME_TYPE);
        get.addHeader("Connection", "close");
        
        return client.execute(get);
    }

    private void validateResponse(HttpResponse response) throws IOException {
        if(response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
            throw new IOException("HTTP status code = " + response.getStatusLine().getStatusCode()); // TODO create Exception class containing http status code
        }
        Header contentType = response.getFirstHeader("Content-Type");
        if(contentType != null && StringUtils.indexOfIgnoreCase(contentType.getValue(), Constants.QUERYREPLY_MIME_TYPE, Locale.ENGLISH) < 0) { // TODO concat all values
            throw new IOException("Unsupported Content-Type: " + contentType.getValue());
        }
        Header contentEncoding = response.getFirstHeader("Content-Encoding");
        if(contentEncoding != null) { // TODO - define acceptable encoding?
            throw new IOException("Unsupported Content-Encoding: " + contentEncoding.getValue());
        }
        Header contentLength = response.getFirstHeader("Content-Length");
        if(contentLength != null) {
            try {
                _replyLength = Long.parseLong(contentLength.getValue());
            } catch (NumberFormatException nfe) {
            }
        }
    }

    private void readQueryRepliesFromStream(HttpResponse response) {
        if(response.getEntity() != null) {
            InputStream in;
            try {
                in = response.getEntity().getContent();
            } catch (IOException e) {
                LOG.debug("Unable to read a single message", e);
                return;
            }
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
                    LOG.debug("Unable to read create message");
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
    }


    /**
	 * Returns true, if browse should be attempted by push download, either
	 * because it is a private address or was unreachable in the past. Returns
	 * false, otherwise or if <tt>host</tt> is the local address. 
	 */
    private boolean canConnectDirectly(IpPort host) {
        return !ConnectionSettings.LOCAL_IS_PRIVATE.getValue() 
        		|| !networkInstanceUtils.isPrivateAddress(host.getAddress())
        		|| networkInstanceUtils.isMe(host.getAddress(), host.getPort());
    }

    /**
     * Returns true, if the user attempts to browse in the local network by
     * entering a host and port but not providing a <code>_serventID</code>.
     * This will make a push impossible so a direct connect is attempted
     * instead.
     */
    private boolean isLocalBrowse(IpPort host) {
        return _serventID == null && networkInstanceUtils.isPrivateAddress(host.getAddress());
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
