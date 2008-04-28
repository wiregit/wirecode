package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.NetworkUtils;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.NBSocket;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.rudp.UDPSelectorProvider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.http.HTTPConnectionData;
import com.limegroup.gnutella.settings.SSLSettings;

/**
 * Manages state for push upload requests.
 */
@Singleton
public final class PushManager {
    
    private static final Log LOG = LogFactory.getLog(PushManager.class);

    /**
     * The timeout for the connect time while establishing the socket. Set to
     * the same value as NORMAL_CONNECT_TIME is ManagedDownloader.
     */
    private static final int CONNECT_TIMEOUT = 10000;//10 secs
    
    private final Provider<FileManager> fileManager;
    private final Provider<SocketsManager> socketsManager;
    private final Provider<HTTPAcceptor> httpAcceptor;
    private final Provider<UDPSelectorProvider> udpSelectorProvider;

    /**
     * @param fileManager
     * @param socketsManager
     * @param httpAcceptor
     */
    @Inject
    public PushManager(Provider<FileManager> fileManager,
            Provider<SocketsManager> socketsManager,
            Provider<HTTPAcceptor> httpAcceptor,
            Provider<UDPSelectorProvider> udpSelectorProvider) {
        this.fileManager = fileManager;
        this.socketsManager = socketsManager;
        this.httpAcceptor = httpAcceptor;
        this.udpSelectorProvider = udpSelectorProvider;
    }    

	/**
	 * Accepts a new push upload.
     * NON-BLOCKING: creates a new thread to transfer the file.
	 * <p>
     * The thread connects to the other side, waits for a GET/HEAD,
     * and delegates to the UploaderManager.acceptUpload method with the
     * socket it created.
     * Essentially, this is a reverse-Acceptor.
     * <p>
     * No file and index are needed since the GET/HEAD will include that
     * information.  Just put in our first file and filename to create a
     * well-formed.
	 * @param host the ip address of the host to upload to
	 * @param port the port over which the transfer will occur
	 * @param guid the unique identifying client guid of the uploading client
     * @param lan whether or not this is a request over a local network (
     * (force the UploadManager to accept this request when it comes back)
     * @param isFWTransfer whether or not to use a UDP pipe to service this
     * upload.
	 */
	public void acceptPushUpload(final String host, 
                                 final int port, 
                                 final String guid,
                                 final boolean lan,
                                 final boolean isFWTransfer,
                                 final boolean tlsCapable) {
        if (LOG.isDebugEnabled())
            LOG.debug("Accepting Push Upload from ip:" + host + " port:" + port + " FW:" + isFWTransfer);
                                    
        if( host == null )
            throw new NullPointerException("null host");
        if( !NetworkUtils.isValidPort(port) )
            throw new IllegalArgumentException("invalid port: " + port);
        if( guid == null )
            throw new NullPointerException("null guid");
        
        // TODO: why is this check here?  it's a tiny optimization,
        // but could potentially kill any sharing of files that aren't
        // counted in the library.
        if (fileManager.get().getNumFiles() < 1 && fileManager.get().getNumIncompleteFiles() < 1)
            return;

        // We used to have code here that tested if the guy we are pushing to is
        // 1) hammering us, or 2) is actually firewalled.  1) is done above us
        // now, and 2) isn't as much an issue with the advent of connectback
        
        PushData data = new PushData(host, port, guid, lan);
        
        // If the transfer is to be done using FW-FW, then immediately start a new thread
        // which will connect using FWT.  Otherwise, do a non-blocking connect and have
        // the observer spawn the thread only if it succesfully connected.
        if(isFWTransfer) {
            if(LOG.isDebugEnabled())
                LOG.debug("Adding push observer FW-FW to host: " + host + ":" + port);
            // TODO: should FW-FW connections also use TLS?
            NBSocket socket = udpSelectorProvider.get().openSocketChannel().socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT*2, new PushObserver(data, true, httpAcceptor.get()));
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Adding push observer to host: " + host + ":" + port);
            try {
                ConnectType type = tlsCapable && SSLSettings.isOutgoingTLSEnabled() ? ConnectType.TLS : ConnectType.PLAIN;
                socketsManager.get().connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT, new PushObserver(data, false, httpAcceptor.get()), type);
            } catch(IOException iox) {
            }
        }
    }
    
    /** A simple collection of Push information */
    private static class PushData {
        private final String host;
        private final int port;
        private final String guid;
        private final boolean lan;
        
        PushData(String host, int port, String guid, boolean lan) {
            this.host = host;
            this.port = port;
            this.guid = guid;
            this.lan = lan;
        }
        
        public boolean isLan() {
            return lan;
        }
        public String getGuid() {
            return guid;
        }
        public String getHost() {
            return host;
        }
        public int getPort() {
            return port;
        }
    }
    
    /** Non-blocking observer for connect events related to pushing. */
    private static class PushObserver implements ConnectObserver {
        private final PushData data;
        private final boolean fwt;
        private final HTTPAcceptor httpAcceptor;
        
        PushObserver(PushData data, boolean fwt, HTTPAcceptor httpAcceptor) {
            this.data = data;
            this.fwt = fwt;
            this.httpAcceptor = httpAcceptor;
        }        
        
        public void handleIOException(IOException iox) {}

        /** Increments the PUSH_FAILED stat and does nothing else. */
        public void shutdown() {
            if(LOG.isDebugEnabled())
                LOG.debug("Push (fwt: " + fwt + ") connect to: " + data.getHost() + ":" + data.getPort() + " failed");
        }

        /** Starts a new thread that'll do the pushing. */
        public void handleConnect(Socket socket) throws IOException {
            if(LOG.isDebugEnabled())
                LOG.debug("Push (fwt: " + fwt + ") connect to: " + data.getHost() + ":" + data.getPort() + " succeeded");
            ((NIOMultiplexor) socket).setWriteObserver(new PushConnector(socket, data, fwt, httpAcceptor));
        }
    }    

    /** Non-blocking observer for connect events related to pushing. */
    private static class PushConnector implements ChannelWriter {
        
        private InterestWritableByteChannel channel;
        private final ByteBuffer buffer;
        private final Socket socket;
        private HTTPConnectionData data;
        private HTTPAcceptor httpAcceptor;

        public PushConnector(Socket socket, PushData data, boolean fwTransfer,
                HTTPAcceptor httpAcceptor) throws IOException {
            this.socket = socket;
            this.data = new HTTPConnectionData();
            this.data.setPush(true);
            this.data.setLocal(data.isLan());
            this.data.setFirewalled(fwTransfer);
            this.httpAcceptor = httpAcceptor;
            
            socket.setSoTimeout(30 * 1000);
            
            String giv = "GIV 0:" + data.getGuid() + "/file\n\n";
            this.buffer = ByteBuffer.wrap(giv.getBytes());
        }

        public boolean handleWrite() throws IOException {
            if (!buffer.hasRemaining()) {
                return false;
            }

            while (buffer.hasRemaining()) {
                int written = channel.write(buffer);
                if (written == 0) {
                    return true;
                }
            }

            httpAcceptor.acceptConnection(socket, data);
            return false;
        }

        public void handleIOException(IOException iox) {
            throw new RuntimeException();
        }

        public void shutdown() {
            // ignore
    }

        public InterestWritableByteChannel getWriteChannel() {
            return channel;
        }

        public void setWriteChannel(InterestWritableByteChannel newChannel) {
            this.channel = newChannel;

            if (newChannel != null) {
                newChannel.interestWrite(this, true);
        }
    }
    }


}
