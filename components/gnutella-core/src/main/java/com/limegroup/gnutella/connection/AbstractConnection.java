package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.setting.StringSetting;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.Handshaker;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;

public abstract class AbstractConnection implements Connection {

    /** Lock for maintaining accurate data for when to allow ping forwarding. */
    private final Object pingLock = new Object();

    /** Lock for maintaining accurate data for when to allow pong forwarding. */
    private final Object pongLock = new Object();

    /**
     * The underlying socket, its address, and input and output streams. sock,
     * in, and out are null iff this is in the unconnected state. For thread
     * synchronization reasons, it is important that this only be modified by
     * the send(m) and receive() methods.
     */
    private final ConnectType connectType;

    /** The address of the remote host. */
    private final String host;

    /** The port the remote host is listening on. */
    private volatile int port;

    /** The socket connecting us to the remote host. */
    protected volatile Socket socket;

    /** True if this was an outgoing connection. */
    private final boolean outgoing;
    
    private volatile byte softMax;

    /**
     * Trigger an opening connection to close after it opens. This flag is set
     * in shutdown() and then checked in initialize() to insure the
     * _socket.close() happens if shutdown is called asynchronously before
     * initialize() completes. Note that the connection may have been remotely
     * closed even if _closed==true. This also protects us from calling methods
     * on the Inflater/Deflater objects after end() has been called on them.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Variable for the next time to allow a ping. Volatile to avoid multiple
     * threads caching old data for the ping time.
     */
    private volatile long nextPingTime = Long.MIN_VALUE;

    /**
     * Variable for the next time to allow a pong. Volatile to avoid multiple
     * threads caching old data for the pong time.
     */
    private volatile long nextPongTime = Long.MIN_VALUE;

    /** Factory for constructing new CapabilitiesVMs. */
    private final CapabilitiesVMFactory capabilitiesVMFactory;

    /** The sole MessagesSupportedVM this sends. */
    private final MessagesSupportedVendorMessage supportedVendorMessage;

    private final ConnectionCapabilities connectionCapabilities;

    private final ConnectionBandwidthStatistics connectionBandwidthStatistics;

    private volatile long connectionTime;

    private final NetworkManager networkManager;

    private final Acceptor acceptor;
    
    private final SimpleProtocolBandwidthTracker simpleProtocolBandwidthTracker;

    /**
     * Cache the 'connection closed' exception, so we have to allocate one for
     * every closed connection.
     */
    protected static final IOException CONNECTION_CLOSED = new IOException("connection closed");

    /**
     * Creates an uninitialized outgoing Gnutella connection.
     * 
     * @param host the name of the host to connect to
     * @param port the port of the remote host
     * @param connectType the type of connection that should be made
     */
    AbstractConnection(String host, int port, ConnectType connectType,
            CapabilitiesVMFactory capabilitiesVMFactory,
            MessagesSupportedVendorMessage supportedVendorMessage,
            NetworkManager networkManager, Acceptor acceptor) {
        this(host, port, connectType, null, capabilitiesVMFactory, supportedVendorMessage, networkManager, acceptor);
    }

    /**
     * Creates an uninitialized incoming Gnutella connection.
     * 
     * @param socket the socket accepted by a ServerSocket. The word "GNUTELLA "
     *        and nothing else must have been read from the socket.
     */
    AbstractConnection(Socket socket, CapabilitiesVMFactory capabilitiesVMFactory,
            MessagesSupportedVendorMessage supportedVendorMessage,
            NetworkManager networkManager, Acceptor acceptor) {
        this(socket.getInetAddress().getHostAddress(), socket.getPort(), SSLUtils
                .isTLSEnabled(socket) ? ConnectType.TLS : ConnectType.PLAIN, socket,
                capabilitiesVMFactory, supportedVendorMessage, networkManager, acceptor);
    }

    private AbstractConnection(String host, int port, ConnectType connectType, 
            Socket socket, CapabilitiesVMFactory capabilitiesVMFactory,
            MessagesSupportedVendorMessage supportedVendorMessage,
            NetworkManager networkManager, Acceptor acceptor) {
        if (host == null)
            throw new NullPointerException("null host");
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("illegal port: " + port);

        this.host = host;
        this.port = port;
        this.outgoing = socket == null;
        this.connectType = connectType;
        this.socket = socket;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.supportedVendorMessage = supportedVendorMessage;
        this.connectionCapabilities = new ConnectionCapabilitiesImpl();
        this.connectionBandwidthStatistics = new ConnectionBandwidthStatisticsImpl();
        this.networkManager = networkManager;
        this.acceptor = acceptor;
        this.simpleProtocolBandwidthTracker = new SimpleProtocolBandwidthTracker();        

        if(!outgoing) {
            connectionBandwidthStatistics.setTlsOption(
                    SSLUtils.isTLSEnabled(socket),
                    SSLUtils.getSSLBandwidthTracker(socket));
        }
        
        connectionBandwidthStatistics.setRawBandwidthTracker(simpleProtocolBandwidthTracker);
    }

    /**
     * Call this method when the Connection has been initialized and accepted as
     * 'long-lived'.
     */
    public void sendPostInitializeMessages() {
        try {
            if (getConnectionCapabilities().getHeadersRead().supportsVendorMessages() > 0) {
                send(supportedVendorMessage);
                send(capabilitiesVMFactory.getCapabilitiesVM());
            }
        } catch (IOException ioe) {
        }
    }

    /**
     * Call this method if you want to send your neighbours a message with your
     * updated capabilities.
     */
    public void sendUpdatedCapabilities() {
        try {
            if (getConnectionCapabilities().getHeadersRead().supportsVendorMessages() > 0)
                send(capabilitiesVMFactory.getCapabilitiesVM());
        } catch (IOException iox) {
        }
    }

    /**
     * Call this method when you want to handle us to handle a VM. We may....
     */
    public void handleVendorMessage(VendorMessage vm) {
        if (vm instanceof MessagesSupportedVendorMessage) {
            getConnectionCapabilities().setMessagesSupportedVendorMessage(
                    (MessagesSupportedVendorMessage) vm);
        } else if (vm instanceof CapabilitiesVM) {
            getConnectionCapabilities().setCapabilitiesVendorMessage((CapabilitiesVM) vm);
        } else if (vm instanceof HeaderUpdateVendorMessage) {
            HeaderUpdateVendorMessage huvm = (HeaderUpdateVendorMessage) vm;
            Properties props = getConnectionCapabilities().getHeadersRead().props();
            props.putAll(huvm.getProperties());
            setHeaders(HandshakeResponse.createResponse(props), null);
        }
    }

    /** Sets the headers read & written. null headers are ignored. */
    protected void setHeaders(HandshakeResponse headersRead, HandshakeResponse headersWritten) {
        if (headersRead != null) {
            getConnectionCapabilities().setHeadersRead(headersRead);
        }

        if (headersWritten != null) {
            getConnectionCapabilities().setHeadersWritten(headersWritten);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#isOutgoing()
     */
    public boolean isOutgoing() {
        return outgoing;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#getAddress()
     */
    public String getAddress() {
        return host;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#getPort()
     */
    public int getPort() {
        return port;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#getListeningPort()
     */
    public int getListeningPort() {
        if (isOutgoing()) {
            if (socket == null) {
                return -1;
            } else {
                return socket.getPort();
            }
        } else {
            return getConnectionCapabilities().getHeadersRead().getListeningPort();
        }
    }

    /**
     * Sets the port where the conected node listens at, not the one got from
     * socket
     */
    public void setListeningPort(int port) {
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("invalid port: " + port);
        this.port = port;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#getInetSocketAddress()
     */
    public InetSocketAddress getInetSocketAddress() throws IllegalStateException {
        return new InetSocketAddress(getInetAddress(), getPort());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#getInetAddress()
     */
    public InetAddress getInetAddress() throws IllegalStateException {
        if (socket == null) {
            throw new IllegalStateException("Not initialized");
        }
        return socket.getInetAddress();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#getSocket()
     */
    public Socket getSocket() throws IllegalStateException {
        if (socket == null) {
            throw new IllegalStateException("Not initialized");
        }
        return socket;
    }

    /** Sets the socket this is using. */
    protected void setSocket(Socket socket) {
        this.socket = socket;
        getConnectionBandwidthStatistics().setTlsOption(
                SSLUtils.isTLSEnabled(socket),
                SSLUtils.getSSLBandwidthTracker(socket));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#isStable()
     */
    public boolean isStable() {
        return isStable(System.currentTimeMillis());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#isStable(long)
     */
    public boolean isStable(long millis) {
        return (millis - getConnectionTime()) / 1000 > 5;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#getPropertyWritten(java.lang.String)
     */
    public String getPropertyWritten(String name) {
        return getConnectionCapabilities().getHeadersWritten().props().getProperty(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#isOpen()
     */
    public boolean isOpen() {
        return !closed.get();
    }

    /**
     * Returns the time this connection was established, in milliseconds since
     * January 1, 1970.
     * 
     * @return the time this connection was established
     */
    public long getConnectionTime() {
        return connectionTime;
    }

    /** Returns the ConnectType this was created with. */
    protected ConnectType getConnectType() {
        return connectType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#close()
     */
    public final void close() {
        // return if it was already closed.
        if (closed.getAndSet(true))
            return;

        IOUtils.close(socket);
        closeImpl();
    }

    protected abstract void closeImpl();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#isWriteDeflated()
     */
    public boolean isWriteDeflated() {
        return getConnectionCapabilities().getHeadersWritten().isDeflateEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#isReadDeflated()
     */
    public boolean isReadDeflated() {
        return getConnectionCapabilities().getHeadersRead().isDeflateEnabled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#isTLSCapable()
     */
    public boolean isTLSCapable() {
        if (!getConnectionCapabilities().isCapabilitiesVmSet() && isTLSEncoded())
            return true;
        else if (getConnectionCapabilities().getCapability(ConnectionCapabilities.Capability.TLS) >= 1)
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#isTLSEncoded()
     */
    public boolean isTLSEncoded() {
        return connectType == ConnectType.TLS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#allowNewPings()
     */
    public boolean allowNewPings() {
        synchronized (pingLock) {
            long curTime = System.currentTimeMillis();

            // don't allow new pings if the connection could drop any second
            if (!isStable(curTime))
                return false;
            if (curTime < nextPingTime) {
                return false;
            }
            nextPingTime = System.currentTimeMillis() + 2500;
            return true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#allowNewPongs()
     */
    public boolean allowNewPongs() {
        synchronized (pongLock) {
            long curTime = System.currentTimeMillis();

            // don't allow new pongs if the connection could drop any second
            if (!isStable(curTime))
                return false;
            if (curTime < nextPongTime) {
                return false;
            }

            int interval;

            // if the connection is young, give it a lot of pongs, otherwise
            // be more conservative
            if (curTime - getConnectionTime() < 10000) {
                interval = 300;
            } else {
                interval = 12000;
            }
            nextPongTime = curTime + interval;

            return true;
        }
    }
    
    protected void processReadMessage(Message m) {
        simpleProtocolBandwidthTracker.addRead(m.getTotalLength());
    }
    
    protected void processWrittenMessage(Message m) {
        simpleProtocolBandwidthTracker.addWritten(m.getTotalLength());
    }
    
    protected void initializeHandshake() throws IOException {
        // Check to see if close() was called while the socket was initializing
        if (!isOpen()) {
            IOUtils.close(getSocket()); // TODO: why?
            throw CONNECTION_CLOSED;
        }

        // Check to see if this is an attempt to connect to ourselves
        InetAddress localAddress = getSocket().getLocalAddress();
        if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue()
                && getSocket().getInetAddress().equals(localAddress)
                && getPort() == ConnectionSettings.PORT.getValue()) {
            throw new IOException("Connection to self");
        }

        // Notify the acceptor of our address.
        // TODO: move out of here!
        acceptor.setAddress(localAddress);
    }
    
    protected byte getSoftMax() {
        return softMax;
    }

    protected void handshakeInitialized(Handshaker handshaker) {
        setHeaders(handshaker.getReadHeaders(), handshaker.getWrittenHeaders());
        connectionTime = System.currentTimeMillis();

        // Now set the soft max TTL that should be used on this connection.
        // The +1 on the soft max for "good" connections is because the message
        // may come from a leaf, and therefore can have an extra hop.
        // "Good" connections are connections with features such as
        // intra-Ultrapeer QRP passing.
        softMax = ConnectionSettings.SOFT_MAX.getValue();
        if (getConnectionCapabilities().isGoodUltrapeer()
                || getConnectionCapabilities().isGoodLeaf()) {
            // we give these an extra hop because they might be sending
            // us traffic from their leaves
            softMax++;
        }

        updateAddress(handshaker.getReadHeaders());
    }

    /**
     * Determines if the address should be changed and changes it if necessary.
     */
    // TODO: this really shouldn't be here -- use a listener pattern instead
    private void updateAddress(HandshakeResponse readHeaders) {
        String ip = readHeaders.getProperty(HeaderNames.REMOTE_IP);
        if (ip == null) {
            return;
        }

        InetAddress ia = null;
        try {
            ia = InetAddress.getByName(ip);
        } catch (UnknownHostException uhe) {
            return; // invalid.
        }

        // invalid or private, exit
        if (!NetworkUtils.isValidAddress(ia) || NetworkUtils.isPrivateAddress(ia))
            return;

        // If we're forcing, change that if necessary.
        if (ConnectionSettings.FORCE_IP_ADDRESS.getValue()) {
            StringSetting addr = ConnectionSettings.FORCED_IP_ADDRESS_STRING;
            if (!ip.equals(addr.getValue())) {
                addr.setValue(ip);
                networkManager.addressChanged();
            }
        }
        // Otherwise, if our current address is invalid, change.
        else if (!NetworkUtils.isValidAddress(networkManager.getAddress())) {
            // will auto-call addressChanged.
            acceptor.setAddress(ia);
        }

        acceptor.setExternalAddress(ia);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#handleSimppVM(com.limegroup.gnutella.messages.vendor.SimppVM)
     */
    public void handleSimppVM(SimppVM simppVM) throws IOException {
        send(simppVM);
    }

    // overrides Object.toString
    public String toString() {
        return "CONNECTION: host=" + host + " port=" + port;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#getLocalePref()
     */
    public String getLocalePref() {
        return getConnectionCapabilities().getHeadersRead().getLocalePref();
    }

    public ConnectionCapabilities getConnectionCapabilities() {
        return connectionCapabilities;
    }

    public ConnectionBandwidthStatistics getConnectionBandwidthStatistics() {
        return connectionBandwidthStatistics;
    }
}
