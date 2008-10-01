package com.limegroup.gnutella.downloader;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressConnector;
import org.limewire.net.address.AddressEvent;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.AbstractChannelInterestReader;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.util.Base32;
import org.limewire.util.BufferUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SocketProcessor;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PushRequestImpl;
import com.limegroup.gnutella.net.address.FirewalledAddress;
import com.limegroup.gnutella.util.MultiShutdownable;
import com.limegroup.gnutella.util.URLDecoder;

/**
 * Handles sending out pushes and awaiting incoming GIVs.
 */
@Singleton
public class PushDownloadManager implements ConnectionAcceptor, PushedSocketHandlerRegistry, AddressConnector, RegisteringEventListener<AddressEvent> {

    private static final Log LOG = LogFactory.getLog(PushDownloadManager.class);
    
    private static final int SPECIAL_INDEX = 0;
   
    /**
     * how long we think should take a host that receives an udp push
     * to connect back to us.
     */
    private static long UDP_PUSH_FAILTIME = 5000;
    
    /** Pool on which blocking HTTP PushProxy requests are handled */
    private final ExecutorService PUSH_THREAD_POOL =
        ExecutorsHelper.newFixedSizeThreadPool(10, "PushProxy Requests");
    
    /**
     * number of files that we have sent a udp push for and are waiting a connection.
     * LOCKING: obtain UDP_FAILOVER if manipulating the contained sets as well!
     */
    private final Map<byte[], AtomicInteger>  
        UDP_FAILOVER = new TreeMap<byte[], AtomicInteger>(new GUID.GUIDByteComparator());
    
    private final AtomicReference<Address> localAddress = new AtomicReference<Address>();
    

    /**
     * The processor to send brand-new incoming sockets to.
     * This is different than PushedSocketHandler in that the PushedSocketHandler
     * is used after we have read the GIV off the socket (and process the rest of
     * the request), whereas this is used to read and ensure it was a GIV.
     */
    private final Provider<SocketProcessor> socketProcessor;
    private final Provider<HttpExecutor> httpExecutor;
    private final Provider<HttpParams> defaultParams;
    private final ScheduledExecutorService backgroundExecutor;    
    private final NetworkManager networkManager;
    private final Provider<MessageRouter> messageRouter;    
    private final Provider<IPFilter> ipFilter;
    private final Provider<UDPService> udpService;
    private final CopyOnWriteArrayList<PushedSocketHandler> pushHandlers = new CopyOnWriteArrayList<PushedSocketHandler>();
    private final Provider<UDPSelectorProvider> udpSelectorProvider;
    
    private final Provider<PushEndpointCache> pushEndpointCache;

    private final RemoteFileDescFactory remoteFileDescFactory;
    
    @Inject
    public PushDownloadManager(
            Provider<MessageRouter> router,
            Provider<HttpExecutor> executor,
            @Named("defaults") Provider<HttpParams> defaultParams,
            @Named("backgroundExecutor") ScheduledExecutorService scheduler,
            Provider<SocketProcessor> processor,
    		NetworkManager networkManager,
    		Provider<IPFilter> ipFilter,
    		Provider<UDPService> udpService,
    		Provider<UDPSelectorProvider> udpSelectorProvider,
    		Provider<PushEndpointCache> pushEndpointCache,
    		RemoteFileDescFactory remoteFileDescFactory) {
    	this.messageRouter = router;
    	this.httpExecutor = executor;
        this.defaultParams = defaultParams;
        this.backgroundExecutor = scheduler;
    	this.socketProcessor = processor;
    	this.networkManager = networkManager;
        this.ipFilter = ipFilter;
        this.udpService = udpService;
        this.udpSelectorProvider = udpSelectorProvider;
        this.pushEndpointCache = pushEndpointCache;
        this.remoteFileDescFactory = remoteFileDescFactory;
    }

    public void register(PushedSocketHandler handler) {
        pushHandlers.add(handler);
    }
    
    @Inject
    public void register(SocketsManager socketsManager) {
        socketsManager.registerConnector(this);
    }

    @Inject
    public void register(ListenerSupport<AddressEvent> addressEventListenerSupport) {
        addressEventListenerSupport.addListener(this);
    }

    public void handleEvent(AddressEvent event) {
        synchronized (localAddress) {
            localAddress.set(event.getSource());
            localAddress.notifyAll();
        }
    }
    
    private void waitForLocalAddress() {
        synchronized (localAddress) {
            while(localAddress.get() == null) {
                try {
                    localAddress.wait();
                } catch (InterruptedException e) {
                    LOG.error(e);
                }
            }
        }
    }

    public boolean isBlocking() {
        return true;
    }
    
    /**
     * Accepts the given socket for a push download to this host.
     * If the GIV is for a file that was never requested or
     * has already been downloaded, this will deal with it appropriately.
     * In any case this eventually closes the socket.
     * 
     * Non-blocking.
     * 
     * @modifies this
     * @requires "GIV " is already read from the socket
     */
    public void acceptConnection(String word, Socket socket) {
        ((NIOMultiplexor)socket).setReadObserver(new GivParser(socket));
    }
    
    @Override
    public void connect(Address addr, int timeout, ConnectObserver observer) {
        FirewalledAddress address = (FirewalledAddress)addr;
        Connectable publicAddress = address.getPublicAddress();
        RemoteFileDesc fakeRFD = 
            remoteFileDescFactory.createRemoteFileDesc(publicAddress.getAddress(), publicAddress.getPort(), SPECIAL_INDEX, "fake",
                0, address.getClientGuid().bytes(), 0, false, 0, false, null, null, false, true, "", address.getPushProxies(),
                -1, address.getFwtVersion(), publicAddress.isTLSCapable());
        PushedSocketHandlerAdapter handlerAdapter = new PushedSocketHandlerAdapter(address.getClientGuid(), observer);
        pushHandlers.add(handlerAdapter);
        sendPush(fakeRFD, true);
        scheduleExpirerFor(handlerAdapter, (int)(timeout * 1.5));
    }
    
    /**
     * Creates an invalid host for pushes.
     */
    static Connectable createInvalidHost() {
        try {
            return new ConnectableImpl("0.0.0.0", 1, false);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private void scheduleExpirerFor(final PushedSocketHandlerAdapter handlerAdapter, int timeout) {
        backgroundExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                pushHandlers.remove(handlerAdapter);
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }
    
    public void sendPush(RemoteFileDesc file) {
        sendPush(file, false);
    }

    /**
     * Sends a push for the given file.
     */
    private void sendPush(RemoteFileDesc file, boolean waitForLocalAddress) {
        sendPush(file, new NullMultiShutdownable(), waitForLocalAddress);
    }
    
    public void sendPush(RemoteFileDesc file, MultiShutdownable observer) {
        sendPush(file, observer, false);
    }

    /**
     * Sends a push request for the given file.
     *
     * @param file the <tt>RemoteFileDesc</tt> constructed from the query 
     *  hit, containing data about the host we're pushing to
     * @param observer The ConnectObserver to notify of success or failure
     * @param waitForLocalAddress whether or not to wait for the local address
     * to be determined
     * @return <tt>true</tt> if the push was successfully sent, otherwise
     *  <tt>false</tt>
     */
    private void sendPush(RemoteFileDesc file, MultiShutdownable observer, boolean waitForLocalAddress) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending push: " + file);
        }
        
        if(waitForLocalAddress) {
            waitForLocalAddress();
        }
        //Make sure we know our correct address/port.
        // If we don't, we can't send pushes yet.
        byte[] addr = networkManager.getAddress();
        // TODO check stable udp port if we're interested in fwts?
        int port = networkManager.getPort();
        if(!NetworkUtils.isValidAddress(addr) || !NetworkUtils.isValidPort(port)) {
            LOG.debug("no valid address or port yet");
            if(observer != null)
                observer.shutdown();
            return;
        }
        
        final byte[] guid = GUID.makeGuid();
        
        // If multicast worked, try nothing else.
        if (sendPushMulticast(file,guid))
            return;
        
        // if we can't accept incoming connections, we can only try
        // using the TCP push proxy, which will do fw-fw transfers.
        if (!networkManager.acceptedIncomingConnection()) {
            // if we can do FWT, offload a TCP pusher.
            if (networkManager.canDoFWT())
                sendPushTCP(file, guid, observer);
            else if (observer != null) {
                LOG.debug("Firewalled and can't do FWT yet");
                observer.shutdown();
            }

            return;
        }
        
        // remember that we are waiting a push from this host 
        // for the specific file.
        // do not send tcp pushes to results from alternate locations.
        if (!file.isFromAlternateLocation()) {
            addUDPFailover(file);
            
            // schedule the failover tcp pusher, which will run
            // if we don't get a response from the UDP push
            // within the UDP_PUSH_FAILTIME timeframe
            backgroundExecutor.schedule(
                new PushFailoverRequestor(file, guid, observer), UDP_PUSH_FAILTIME, TimeUnit.MILLISECONDS);
        }

        sendPushUDP(file,guid);
    }
    
    
    /**
     * Sends a push through multicast.
     *
     * Returns true only if the RemoteFileDesc was a reply to a multicast query
     * and we wanted to send through multicast.  Otherwise, returns false,
     * as we shouldn't reply on the multicast network.
     */
    private boolean sendPushMulticast(RemoteFileDesc file, byte []guid) {
        // Send as multicast if it's multicast.
        if( file.isReplyToMulticast() ) {
            byte[] addr = networkManager.getNonForcedAddress();
            int port = networkManager.getNonForcedPort();
            if( NetworkUtils.isValidAddress(addr) &&
                NetworkUtils.isValidPort(port) ) {
                PushRequest pr = new PushRequestImpl(guid,
                                         (byte)1, //ttl
                                         file.getClientGUID(),
                                         file.getIndex(),
                                         addr,
                                         port,
                                         Network.MULTICAST,
                                         networkManager.isIncomingTLSEnabled());
                messageRouter.get().sendMulticastPushRequest(pr);
                if (LOG.isInfoEnabled())
                    LOG.info("Sending push request through multicast " + pr);
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a push through UDP.
     *
     * This always returns true, because a UDP push is always sent.
     */    
    private boolean sendPushUDP(RemoteFileDesc file, byte[] guid) {
        PushRequest pr = 
                new PushRequestImpl(guid,
                                (byte)2,
                                file.getClientGUID(),
                                file.getIndex(),
                                networkManager.getAddress(),
                                networkManager.getPort(),
                                Network.UDP,
                                networkManager.isIncomingTLSEnabled());
        if (LOG.isInfoEnabled())
                LOG.info("Sending push request through udp " + pr);
                    
        UDPService udpService = this.udpService.get();
        //and send the push to the node 
        try {
            InetAddress address = InetAddress.getByName(file.getAddress());
            
            //don't bother sending direct push if the node reported invalid
            //address and port.
            if (NetworkUtils.isValidAddress(address) &&
                    NetworkUtils.isValidPort(file.getPort())) {
                udpService.send(pr, address, file.getPort());
            }
        } catch(UnknownHostException notCritical) {}
    
        //make sure we send it to the proxies, if any
        for(IpPort ppi : file.getPushProxies()) {
            if (ipFilter.get().allow(ppi.getAddress())) {
                udpService.send(pr, ppi.getInetSocketAddress());
            } else {
                removePushProxy(file.getClientGUID(), ppi);
            }
        }
        
        return true;
    }
    
    /**
     * Sends a push through TCP.
     *
     * This method will always return immediately,
     * and the PushConnector will be notified or success or failure.
     */
    private void sendPushTCP(RemoteFileDesc file, final byte[] guid, MultiShutdownable observer) {
        // if this is a FW to FW transfer, we must consider special stuff
        final boolean shouldDoFWTransfer = file.supportsFWTransfer() &&
                         networkManager.canDoFWT() &&
                        !networkManager.acceptedIncomingConnection();

    	PushData data = new PushData(observer, file, guid, shouldDoFWTransfer);

    	// if there are no proxies, send through the network
        Set<? extends IpPort> proxies = file.getPushProxies();
        if(proxies.isEmpty()) {
            sendPushThroughNetwork(data);
            return;
        }
        
        // Try and send the push through proxies -- if none of the proxies work,
        // the PushMessageSender will send it through the network.
        sendPushThroughProxies(data, proxies);
    }
    
    /**
     * Sends a push through the network.  The observer is notified upon failure.
     * 
     * @param data
     */
    private void sendPushThroughNetwork(PushData data) {
        LOG.debug("sending through network");
        // at this stage, there is no additional shutdownable to notify.
        data.getMultiShutdownable().addShutdownable(null);

        // if push proxies failed, but we need a fw-fw transfer, give up.
        if (data.isFWTransfer() && !networkManager.acceptedIncomingConnection()) {
            LOG.debug("through network, FWT and not accepted incoming connection");
            data.getMultiShutdownable().shutdown();
            return;
        }

        byte[] addr = networkManager.getAddress();
        int port = networkManager.getPort();
        if (!NetworkUtils.isValidAddressAndPort(addr, port)) {
            LOG.debug("through network, no valid address or port");
            data.getMultiShutdownable().shutdown();
            return;
        }

        PushRequest pr = new PushRequestImpl(data.getGuid(), 
                                         ConnectionSettings.TTL.getValue(),
                                         data.getFile().getClientGUID(),
                                         data.getFile().getIndex(),
                                         addr,
                                         port,
                                         Network.TCP,
                                         networkManager.isIncomingTLSEnabled());
        
        if (LOG.isInfoEnabled())
            LOG.info("Sending push request through Gnutella: " + pr);
        
        try {
            messageRouter.get().sendPushRequest(pr);
        } catch (IOException e) {
            LOG.debug("no route sending through network");
            // this will happen if we have no push route.
            data.getMultiShutdownable().shutdown();
        }
    }
    
    /**
     * Attempts to send a push through the given proxies.  If any succeed,
     * the observer will be notified immediately.  If all fail, the PushMessageSender
     * is told to send the push through the network.
     */
    private void sendPushThroughProxies(PushData data, Set<? extends IpPort> proxies) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("sending through proxies: " + proxies);
        }
        byte[] externalAddr = networkManager.getExternalAddress();
        // if a fw transfer is necessary, but our external address is invalid,
        // then exit immediately 'cause nothing will work.
        if (data.isFWTransfer() && !NetworkUtils.isValidAddress(externalAddr)) {
            LOG.debug("FWT, but no valid external address yet");
        	data.getMultiShutdownable().shutdown();
            return;
        }

        //TODO: send push msg directly to a proxy if you're connected to it.

        // set up the request string --
        // if a fw-fw transfer is required, add the extra "file" parameter.
        final String request = "/gnutella/push-proxy?ServerID=" + 
                               Base32.encode(data.getFile().getClientGUID()) +
          (data.isFWTransfer() ? ("&file=" + PushRequest.FW_TRANS_INDEX) : "") +
          (networkManager.isIncomingTLSEnabled() ? "&tls=true" : "");
            
        final String nodeString = HTTPHeaderName.NODE.httpStringValue();
        final String nodeValue = data.isFWTransfer() ?
                NetworkUtils.ip2string(externalAddr) + ":" + networkManager.getStableUDPPort() : 
                    NetworkUtils.ip2string(networkManager.getAddress()) + ":" + networkManager.getPort();

        // the methods to execute
        final List<HttpHead> methods = new ArrayList<HttpHead>();

        // try to contact each proxy
        for(IpPort ppi : proxies) {
            if (!ipFilter.get().allow(ppi.getAddress())) {
                removePushProxy(data.file.getClientGUID(), ppi);
                continue;
            }
            String protocol = "http://";
            if(ppi instanceof Connectable) {
                if(((Connectable)ppi).isTLSCapable())
                    protocol = "tls://";
            }
            String connectTo =  protocol + ppi.getAddress() + ":" + ppi.getPort() + request;
            HttpHead head = null;
            head = new HttpHead(connectTo);
            head.addHeader(nodeString, nodeValue);
            head.addHeader("Cache-Control", "no-cache");
            methods.add(head);
        }        
        
        if(!methods.isEmpty()) {
            HttpClientListener l = new PushHttpClientListener(methods, data);
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 5000);
            HttpConnectionParams.setSoTimeout(params, 5000);
            params = new DefaultedHttpParams(params, defaultParams.get());
            Shutdownable s = httpExecutor.get().executeAny(l, PUSH_THREAD_POOL, methods, params, data.getMultiShutdownable());
            data.getMultiShutdownable().addShutdownable(s);
        } else {
            sendPushThroughNetwork(data);    
        }
    }
    
    private void removePushProxy(byte[] guid, URI uri) {
        try {
            IpPort pushProxy = new IpPortImpl(uri.getHost(), uri.getPort());
            removePushProxy(guid, pushProxy);
        } catch (UnknownHostException uhe) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("exception on host that should have worked", uhe);
            }
        }
    }
    
    private void removePushProxy(byte[] guid, IpPort pushProxy) {
        pushEndpointCache.get().removePushProxy(guid, pushProxy);
    }
    
    /**
     * Listener for callbacks from http requests succeeding or failing.
     * This will ensure that only enough proxies are contacted as necessary,
     * and send through the network if necessary.
     */
    private class PushHttpClientListener implements HttpClientListener {
        /** The HttpMethods that are being executed. */
    	private final Collection<HttpUriRequest> methods;
        /** Information about the push. */
        private final PushData data;
        
    	PushHttpClientListener(Collection <? extends HttpUriRequest> methods, PushData data) {
    		this.methods = new LinkedList<HttpUriRequest>(methods);
    		this.data = data;
    	}
    	
    	public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
    	    if (LOG.isWarnEnabled()) {
    	        LOG.warn("PushProxy request exception: " + request.getURI(), exc);
    	    }
    		httpExecutor.get().releaseResources(response);
    		methods.remove(request);

    		URI uri = request.getURI();
    		if (LOG.isDebugEnabled()) {
                LOG.debug("Removing push proxy: " + uri + " for " + new GUID(data.file.getClientGUID()));
            }
    		removePushProxy(data.file.getClientGUID(), uri);
    		
    		if (methods.isEmpty()) // all failed
                sendPushThroughNetwork(data);
            return true;
    	}
    	
    	public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
    		methods.remove(request);
    		int statusCode = response.getStatusLine().getStatusCode();
    		httpExecutor.get().releaseResources(response);
    		if (statusCode == 202) {
    			if(LOG.isDebugEnabled())
    				LOG.debug("Successful push proxy: " + request.getURI());
    			
    			if (data.isFWTransfer()) {
    			    LOG.debug("Starting fwt communication");
    			    AbstractNBSocket socket = udpSelectorProvider.get().openSocketChannel().socket();
                    data.getMultiShutdownable().addShutdownable(socket);
    				socket.connect(data.getFile().getInetSocketAddress(), 20000, new FWTConnectObserver(socketProcessor.get()));
                }
                
                return false; // don't need to process any more methods.
    		} 
            
    		if(LOG.isWarnEnabled())
    		    LOG.warn("Invalid push proxy: " + request.getURI() + ", response: " + response.getStatusLine().getStatusCode() + ", reason: " + response.getStatusLine().getReasonPhrase());
    		
    		removePushProxy(data.file.getClientGUID(), request.getURI());

    		if (methods.isEmpty()) // all failed 
    		    sendPushThroughNetwork(data);
            
            return true; // try more.
    	}

        @Override
        public boolean allowRequest(HttpUriRequest request) {
            return true;
        }
    }
     
    /** Accepts a socket that has had a GIV read off it already. */
    void handleGIV(Socket socket, GIVLine line) {
        String file = line.file;
        int index = 0;
        byte[] clientGUID = line.clientGUID;
        
        // if the push was sent through udp, make sure we cancel the failover push.
        cancelUDPFailover(clientGUID);

        boolean accepted = false;
        for(PushedSocketHandler handler : pushHandlers) {
            if(handler.acceptPushedSocket(file, index, clientGUID, socket)) {
                accepted = true;
                break;
            }
        }
        if(!accepted) {
            IOUtils.close(socket);
        }
    }
    
    /**
     * Adds the necessary data into UDP_FAILOVER so that a PushFailoverRequestor
     * knows if it should send a request.
     * @param file
     */
    private void addUDPFailover(RemoteFileDesc file) {
        synchronized (UDP_FAILOVER) {
            byte[] key = file.getClientGUID();
            AtomicInteger requests = UDP_FAILOVER.get(key);
            if (requests == null) {
                requests = new AtomicInteger(0);
                UDP_FAILOVER.put(key, requests);
            }
            requests.addAndGet(1);
        }
    }
    
    /**
     * Removes data from UDP_FAILOVER, indicating a push has used it.
     * 
     * @param guid
     */
    private void cancelUDPFailover(byte[] clientGUID) {
        synchronized (UDP_FAILOVER) {
            byte[] key = clientGUID;
            AtomicInteger requests = UDP_FAILOVER.get(key);
            if (requests != null) {
                if (requests.decrementAndGet() <= 0)
                    UDP_FAILOVER.remove(key);
            }
        }
    }
    
    /** A struct-like container storing push information. */
    private class PushData {
        private final MultiShutdownable observer;
        private final RemoteFileDesc file;
        private final byte [] guid;
        private final boolean shouldDoFWTransfer;
        
        PushData(MultiShutdownable observer,
                 RemoteFileDesc file,
                 byte [] guid,
                 boolean shouldDoFWTransfer) {
            this.observer = observer;
            this.file = file;
            this.guid = guid;
            this.shouldDoFWTransfer = shouldDoFWTransfer;
        }

        public RemoteFileDesc getFile() {
            return file;
        }

        public byte[] getGuid() {
            return guid;
        }

        public MultiShutdownable getMultiShutdownable() {
            return observer;
        }

        public boolean isFWTransfer() {
            return shouldDoFWTransfer;
        }
        
    }
    
    /**
     * sends a tcp push if the udp push has failed.
     */
    private class PushFailoverRequestor implements Runnable {

    	final RemoteFileDesc _file;
    	final byte [] _guid;
    	final MultiShutdownable connector;

    	public PushFailoverRequestor(RemoteFileDesc file,
    			byte[] guid,
                MultiShutdownable connector) {
    		_file = file;
    		_guid = guid;
    		this.connector = connector;
    	}

    	public void run() {
    		if (shouldProceed()) 
    			sendPushTCP(_file, _guid, connector);
    	}

    	protected boolean shouldProceed() {
    		byte[] key =_file.getClientGUID();

    		synchronized(UDP_FAILOVER) {
    			AtomicInteger requests = UDP_FAILOVER.get(key);
    			if (requests!=null && requests.get() > 0) {
    			    if (requests.decrementAndGet() == 0) {
    			        UDP_FAILOVER.remove(key);
    			    }
                    return true;
    			}
    		}
    		return false;
    	}
   }
    
    /**
     * Non-blocking read-channel to parse the rest of a GIV request
     * and hand it off to handleGIV.
     */
    private class GivParser extends AbstractChannelInterestReader {
        private final Socket socket;
        private final StringBuilder givSB   = new StringBuilder();
        private final StringBuilder blankSB = new StringBuilder();
        private boolean readBlank;
        private GIVLine giv;
        
        GivParser(Socket socket) {
            super(1024);

            this.socket = socket;
        }

        public void handleRead() throws IOException {
            // Fill up our buffer as much we can.
            while(true) {
                int read = 0;
                while(buffer.hasRemaining() && (read = source.read(buffer)) > 0);
                if(buffer.position() == 0) {
                    if(read == -1)
                        close();
                    break;
                }
                
                buffer.flip();
                if(giv == null) {
                    if(BufferUtils.readLine(buffer, givSB))
                        giv = parseLine(givSB.toString());
                }
                
                if(giv != null && !readBlank) {
                    readBlank = BufferUtils.readLine(buffer, blankSB);
                    if(blankSB.length() > 0)
                        throw new IOException("didn't read blank line");
                }
                
                buffer.compact();
                if(readBlank) {
                    handleGIV(socket, giv);
                    break;
                }
            }
        }
        
        private GIVLine parseLine(String command) throws IOException{
            //2. Parse and return the fields.
            try {
                //a) Extract file index.  IndexOutOfBoundsException
                //   or NumberFormatExceptions will be thrown here if there's
                //   a problem.  They're caught below.
                int i=command.indexOf(":");
                int index=Integer.parseInt(command.substring(0,i));
                //b) Extract clientID.  This can throw
                //   IndexOutOfBoundsException or
                //   IllegalArgumentException, which is caught below.
                int j=command.indexOf("/", i);
                byte[] guid=GUID.fromHexString(command.substring(i+1,j));
                //c). Extract file name.
                String filename=URLDecoder.decode(command.substring(j+1));
    
                return new GIVLine(filename, index, guid);
            } catch (IndexOutOfBoundsException e) {
                throw new IOException();
            } catch (NumberFormatException e) {
                throw new IOException();
            } catch (IllegalArgumentException e) {
                throw new IOException();
            }          
        }
    }
    
    static final class GIVLine {
        final String file;
        final int index;
        final byte[] clientGUID;
        GIVLine(String file, int index, byte[] clientGUID) {
            this.file=file;
            this.index=index;
            this.clientGUID=clientGUID;
        }
    }
    

    /** Simple ConnectObserver for FWT connections. */
    private static class FWTConnectObserver implements ConnectObserver {

    	private final SocketProcessor processor;
    	
    	FWTConnectObserver(SocketProcessor processor) {
    		this.processor = processor;
    	}
    	
        public void handleIOException(IOException iox) {}

        public void handleConnect(Socket socket) throws IOException {
            processor.processSocket(socket, "GIV");
        }

        public void shutdown() {
        }
    }
    
    /** Shutdownable that does nothing, because it's not possible for it to be shutdown. */
    private static class NullMultiShutdownable implements MultiShutdownable {
		public void shutdown() {}
        
		public void addShutdownable(Shutdownable newCancel) {}
        
        /** Returns true iff cancelled. */
        public boolean isCancelled() {
            return false;
        }
    }

    @Override
    public boolean canConnect(Address address) {
        return address instanceof FirewalledAddress;
    }

    /**
     * Adapts {@link PushedSocketHandler} to {@link ConnectObserver}.
     */
    private class PushedSocketHandlerAdapter implements PushedSocketHandler {

        private final GUID clientGuid;
        private final ConnectObserver observer;

        public PushedSocketHandlerAdapter(GUID clientGuid, ConnectObserver observer) {
            this.clientGuid = clientGuid;
            this.observer = observer;
        }
        
        @Override
        public boolean acceptPushedSocket(String file, int index, byte[] clientGUID, Socket socket) {
            if (Arrays.equals(clientGuid.bytes(), clientGUID)) {
                pushHandlers.remove(this);
                try {
                    observer.handleConnect(socket);
                } catch (IOException e) {
                    IOUtils.close(socket);
                }
                return true;
            }
            return false;
        }
        
    }

}
