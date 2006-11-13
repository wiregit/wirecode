package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.ConnectionAcceptor;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.http.HTTPClientListener;
import com.limegroup.gnutella.io.AbstractChannelInterestRead;
import com.limegroup.gnutella.io.BufferUtils;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.NIOMultiplexor;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.DownloadStat;
import com.limegroup.gnutella.statistics.HTTPStat;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.DefaultThreadPool;
import com.limegroup.gnutella.util.IntWrapper;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.ThreadPool;
import com.limegroup.gnutella.util.URLDecoder;

public class PushDownloadManager implements ConnectionAcceptor {

	 private static final Log LOG = LogFactory.getLog(PushDownloadManager.class);
	 
    /**
     * Threadpool on which to execute push requests
     */
    private static final ThreadPool PUSH_THREAD_POOL = 
    	new DefaultThreadPool("push request pool",10);
    
    /**
     * number of files that we have sent a udp push for and are waiting a connection.
     * LOCKING: obtain UDP_FAILOVER if manipulating the contained sets as well!
     */
    private final Map<byte[], IntWrapper>  
        UDP_FAILOVER = new TreeMap<byte[], IntWrapper>(new GUID.GUIDByteComparator());
    
    /**
     * how long we think should take a host that receives an udp push
     * to connect back to us.
     */
    private static long UDP_PUSH_FAILTIME=5000;

    private MessageRouter router;
    
    public void initialize() {
        RouterService.getConnectionDispatcher().
        addConnectionAcceptor(this,
        		new String[]{"GIV"},
        		false,
        		true);
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
            byte[] addr = RouterService.getNonForcedAddress();
            int port = RouterService.getNonForcedPort();
            if( NetworkUtils.isValidAddress(addr) &&
                NetworkUtils.isValidPort(port) ) {
                PushRequest pr = new PushRequest(guid,
                                         (byte)1, //ttl
                                         file.getClientGUID(),
                                         file.getIndex(),
                                         addr,
                                         port,
                                         Message.N_MULTICAST);
                router.sendMulticastPushRequest(pr);
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
                new PushRequest(guid,
                                (byte)2,
                                file.getClientGUID(),
                                file.getIndex(),
                                RouterService.getAddress(),
                                RouterService.getPort(),
                                Message.N_UDP);
        if (LOG.isInfoEnabled())
                LOG.info("Sending push request through udp " + pr);
                    
        UDPService udpService = UDPService.instance();
        //and send the push to the node 
        try {
            InetAddress address = InetAddress.getByName(file.getHost());
            
            //don't bother sending direct push if the node reported invalid
            //address and port.
            if (NetworkUtils.isValidAddress(address) &&
                    NetworkUtils.isValidPort(file.getPort())) {
                udpService.send(pr, address, file.getPort());
            }
        } catch(UnknownHostException notCritical) {}
    
        IPFilter filter = RouterService.getIpFilter();
        //make sure we send it to the proxies, if any
        Set proxies = file.getPushProxies();
        for (Iterator iter = proxies.iterator();iter.hasNext();) {
            IpPort ppi = (IpPort)iter.next();
            if (filter.allow(ppi.getAddress())) {
                udpService.send(pr,ppi.getInetAddress(),ppi.getPort());
            }
        }
        
        return true;
    }
    
    /**
     * Sends a push through TCP.
     *
     * Returns true if we have a valid push route, or if a push proxy
     * gave us a succesful sending notice.
     */
    private void sendPushTCP(final RemoteFileDesc file, final byte[] guid, PushConnector observer) {
        // if this is a FW to FW transfer, we must consider special stuff
        final boolean shouldDoFWTransfer = file.supportsFWTransfer() &&
                         UDPService.instance().canDoFWT() &&
                        !RouterService.acceptedIncomingConnection();


    	PushMessageSender sender = new PushMessageSender(observer,file,guid, shouldDoFWTransfer);

    	// if there are no proxies, send through the network
        Set proxies = file.getPushProxies();
        if(proxies.isEmpty()) {
            sender.sendPushMessage();
            return;
        }
            
        byte[] externalAddr = RouterService.getExternalAddress();
        // if a fw transfer is necessary, but our external address is invalid,
        // then exit immediately 'cause nothing will work.
        if (shouldDoFWTransfer && !NetworkUtils.isValidAddress(externalAddr)) {
        	observer.shutdown();
            return;
        }

        byte[] addr = RouterService.getAddress();
        int port = RouterService.getPort();

        //TODO: investigate not sending a HTTP request to a proxy
        //you are directly connected to.  How much of a problem is this?
        //Probably not much of one at all.  Classic example of code
        //complexity versus efficiency.  It may be hard to actually
        //distinguish a PushProxy from one of your UP connections if the
        //connection was incoming since the port on the socket is ephemeral 
        //and not necessarily the proxies listening port
        // we have proxy info - give them a try

        // set up the request string --
        // if a fw-fw transfer is required, add the extra "file" parameter.
        final String request = "/gnutella/push-proxy?ServerID=" + 
                               Base32.encode(file.getClientGUID()) +
          (shouldDoFWTransfer ? ("&file=" + PushRequest.FW_TRANS_INDEX) : "");
            
        final String nodeString = "X-Node";
        final String nodeValue =
            NetworkUtils.ip2string(shouldDoFWTransfer ? externalAddr : addr) +
            ":" + port;

        // the methods to execute
        List<HeadMethod> methods = new ArrayList<HeadMethod>();
        IPFilter filter = RouterService.getIpFilter();
        // try to contact each proxy
        for(Iterator iter = proxies.iterator(); iter.hasNext(); ) {
            IpPort ppi = (IpPort)iter.next();
            if (!filter.allow(ppi.getAddress()))
                continue;
            final String ppIp = ppi.getAddress();
            final int ppPort = ppi.getPort();
            String connectTo =  "http://" + ppIp + ":" + ppPort + request;
            HeadMethod head = new HeadMethod(connectTo);
            head.addRequestHeader(nodeString, nodeValue);
            head.addRequestHeader("Cache-Control", "no-cache");
            methods.add(head);
        }
        final List<HeadMethod> methodsCopy = new ArrayList<HeadMethod>(methods);
        
        
        HTTPClientListener l = new PushHttpClientListener(methodsCopy, shouldDoFWTransfer, file, sender);
        
        Shutdownable s = RouterService.getHttpExecutor().executeAny(
        		l,5000, PUSH_THREAD_POOL, methods);
        observer.updateCancellable(s);
    }
    
    private class PushHttpClientListener implements HTTPClientListener {
    	private final Collection<? extends HttpMethod> methods;
    	private final boolean shouldDoFWTransfer;
    	private final RemoteFileDesc file;
    	private final PushMessageSender sender;
    	PushHttpClientListener(Collection <? extends HttpMethod> methods,
    			boolean shouldDoFWTransfer,
    			RemoteFileDesc file,
    			PushMessageSender sender) {
    		this.methods = methods;
    		this.shouldDoFWTransfer = shouldDoFWTransfer;
    		this.file = file;
    		this.sender = sender;
    	}
    	
    	public void requestFailed(HttpMethod method, IOException exc) {
    		LOG.warn("PushProxy request exception", exc);
    		RouterService.getHttpExecutor().releaseResources(method);
    		methods.remove(method);
    		if (methods.isEmpty()) // all failed 
    			sender.sendPushMessage();
    	}
    	
    	public void requestComplete(HttpMethod method) {
    		methods.remove(method);
    		int statusCode = method.getStatusCode();
    		RouterService.getHttpExecutor().releaseResources(method);
    		if (statusCode == 202) {
    			
    			if(LOG.isInfoEnabled())
    				LOG.info("Succesful push proxy: " + method);
    			
    			if (shouldDoFWTransfer) {
    				UDPConnection socket = new UDPConnection();
    				socket.connect(file.getSocketAddress(), 20000, new FWTConnectObserver());
    				
    				// update the PushConnector cancellable delegate with the socket
    				Shutdownable otherMethods = sender.observer.updateCancellable(socket);
    				if (otherMethods != null)
    					otherMethods.shutdown(); // shutdown any remaining methods
    			}
    		} else {
    			if(LOG.isWarnEnabled())
    				LOG.warn("Invalid push proxy: " + method +
    						", response: " + method.getStatusCode());
    			if (methods.isEmpty()) // all failed 
    				sender.sendPushMessage();
    		}
    	}
    }
    
    private class PushMessageSender {
    	private final PushConnector observer;
    	private final RemoteFileDesc file;
    	private final byte [] guid;
    	private final boolean shouldDoFWTransfer;
    	PushMessageSender(PushConnector observer,
    			RemoteFileDesc file,
    			byte [] guid,
    			boolean shouldDoFWTransfer) {
    		this.observer = observer;
    		this.file = file;
    		this.guid = guid;
    		this.shouldDoFWTransfer = shouldDoFWTransfer;
    	}
    	
    	void sendPushMessage() {
    		// at this stage, there is no additional shutdownable to notify.
    		observer.updateCancellable(null);
    		
    		// if push proxies failed, but we need a fw-fw transfer, give up.
            if(shouldDoFWTransfer && !RouterService.acceptedIncomingConnection())
                observer.shutdown();
                
            byte[] addr = RouterService.getAddress();
            int port = RouterService.getPort();
            if(!NetworkUtils.isValidAddressAndPort(addr, port))
                observer.shutdown();

            PushRequest pr = 
                new PushRequest(guid,
                                ConnectionSettings.TTL.getValue(),
                                file.getClientGUID(),
                                file.getIndex(),
                                addr, port);
            if(LOG.isInfoEnabled())
                LOG.info("Sending push request through Gnutella: " + pr);
            try {
                router.sendPushRequest(pr);
            } catch (IOException e) {
                // this will happen if we have no push route.
                observer.shutdown();
            }
    	}
    }
    
    void pushThroughProxiesFailed(final RemoteFileDesc file, final byte[] guid, ConnectObserver observer) {
    	
    }
    
    /**
     * Accepts the given socket for a push download to this host.
     * If the GIV is for a file that was never requested or has already
     * been downloaded, this will deal with it appropriately.  In any case
     * this eventually closes the socket.  Non-blocking.
     *     @modifies this
     *     @requires "GIV " was just read from s
     */
    public void acceptDownload(Socket socket) {
        ((NIOMultiplexor)socket).setReadObserver(new GivParser(socket));
    }
    
    public void acceptConnection(String word, Socket sock) {
    	HTTPStat.GIV_REQUESTS.incrementStat();
    	acceptDownload(sock);
    }
    
    private void handleGIV(Socket socket, GIVLine line) {
        String file = line.file;
        int index = 0;
        byte[] clientGUID = line.clientGUID;
        
        
        // if the push was sent through udp, make sure we cancel the failover push.
        cancelUDPFailover(clientGUID);
        
        RouterService.getDownloadManager().acceptDownload(file, index, clientGUID, socket);

    }

    
    /**
     * Sends a push for the given file.
     */
    public void sendPush(RemoteFileDesc file) {
        sendPush(file, new NullPushConnector());
    }

    /**
     * Sends a push request for the given file.
     *
     * @param file the <tt>RemoteFileDesc</tt> constructed from the query 
     *  hit, containing data about the host we're pushing to
     * @param observer The ConnectObserver to notify of success or failure
     * @return <tt>true</tt> if the push was successfully sent, otherwise
     *  <tt>false</tt>
     */
    public void sendPush(final RemoteFileDesc file, final PushConnector observer) {
        //Make sure we know our correct address/port.
        // If we don't, we can't send pushes yet.
        byte[] addr = RouterService.getAddress();
        int port = RouterService.getPort();
        if(!NetworkUtils.isValidAddress(addr) || !NetworkUtils.isValidPort(port)) {
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
        if(!RouterService.acceptedIncomingConnection()) {
            // if we can do FWT, offload a TCP pusher.
        	if(UDPService.instance().canDoFWT())  
        		sendPushTCP(file, guid, observer);
        	else if(observer != null)
        			observer.shutdown();
        	
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
            RouterService.schedule(new PushFailoverRequestor(file, guid, observer),
            		UDP_PUSH_FAILTIME, 0);
        }

        sendPushUDP(file,guid);
    }
    
    /**
     * Adds the necessary data into UDP_FAILOVER so that a PushFailoverRequestor
     * knows if it should send a request.
     * @param file
     */
    private void addUDPFailover(RemoteFileDesc file) {
        synchronized (UDP_FAILOVER) {
            byte[] key = file.getClientGUID();
            IntWrapper requests = UDP_FAILOVER.get(key);
            if (requests == null) {
                requests = new IntWrapper(0);
                UDP_FAILOVER.put(key, requests);
            }
            requests.addInt(1);
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
            IntWrapper requests = UDP_FAILOVER.get(key);
            if (requests != null) {
            	requests.addInt(-1);
                if (requests.getInt() <= 0)
                    UDP_FAILOVER.remove(key);
            }
        }
    }
    
    /**
     * sends a tcp push if the udp push has failed.
     */
    private class PushFailoverRequestor implements Runnable {

    	final RemoteFileDesc _file;
    	final byte [] _guid;
    	final PushConnector connector;

    	public PushFailoverRequestor(RemoteFileDesc file,
    			byte[] guid,
    			PushConnector connector) {
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
    			IntWrapper requests = UDP_FAILOVER.get(key);
    			if (requests!=null && requests.getInt() > 0) {
    				requests.addInt(-1);
    				if (requests.getInt() == 0)
    					UDP_FAILOVER.remove(key);
    				return true;
    			}
    		}

    		return false;
    	}
   }
    
    private class GivParser extends AbstractChannelInterestRead {
        private final Socket socket;
        private final StringBuilder givSB   = new StringBuilder();
        private final StringBuilder blankSB = new StringBuilder();
        private boolean readBlank;
        private GIVLine giv;
        
        GivParser(Socket socket) {
            this.socket = socket;
        }

        protected int getBufferSize() {
            return 1024;
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
    
    // ///////////////// Internal Method to Parse GIV String ///////////////////

    private static final class GIVLine {
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

        public void handleIOException(IOException iox) {}

        public void handleConnect(Socket socket) throws IOException {
            DownloadStat.FW_FW_SUCCESS.incrementStat();
            RouterService.getAcceptor().accept(socket, "GIV");
        }

        public void shutdown() {
            DownloadStat.FW_FW_FAILURE.incrementStat();
        }
    }
    
    private static class NullPushConnector extends PushConnector {
    	NullPushConnector() {
    		super(null,false,false);
    	}

		@Override
		public void handleConnect(Socket socket) {}

		@Override
		void setPushDetails(PushDetails details) {}

		@Override
		public void shutdown() {}

		@Override
		public Shutdownable updateCancellable(Shutdownable newCancel) {
			return this;
		}
    }
}
