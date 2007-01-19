package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnsupportedAddressTypeException;

import org.limewire.nio.channel.InterestReadChannel;
import org.limewire.nio.channel.InterestWriteChannel;
import org.limewire.nio.observer.ConnectObserver;

/**
 * A Socket that does all of its connecting/reading/writing using NIO.
 * 
 * Input/OutputStreams are provided to be used for blocking I/O (although internally non-blocking I/O is used). To
 * switch to using event-based reads, setReadObserver can be used, and read-events will be passed to the ReadObserver. A
 * ChannelReadObserver must be used so that the Socket can set the appropriate underlying channel.
 */
public class NIOSocket extends AbstractNBSocket {

    /** The underlying channel the socket is using */
    private final SocketChannel channel;

    /** The Socket that this delegates to */
    private final Socket socket;

    /**
     * The host we're connected to. (Necessary because Sockets retrieved from channels null out the host when
     * disconnected)
     */
    private InetAddress connectedTo;
    
    /** The port we're connecting/connected to. */
    private int port;

    /**
     * Constructs an NIOSocket using a pre-existing Socket. To be used by NIOServerSocket while accepting incoming
     * connections.
     */
    NIOSocket(Socket s) {
        channel = s.getChannel();
        socket = s;
        setInitialReader();
        setInitialWriter();
        NIODispatcher.instance().register(channel, this);
        connectedTo = s.getInetAddress();
        port = s.getPort();
    }

    /** Creates an unconnected NIOSocket. */
    public NIOSocket() throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        init();
        setInitialReader();
        setInitialWriter();
    }

    /** Creates an NIOSocket and connects (with no timeout) to addr/port */
    public NIOSocket(InetAddress addr, int port) throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        init();
        setInitialReader();
        setInitialWriter();
        connect(new InetSocketAddress(addr, port));
    }

    /** Creates an NIOSocket locally bound to localAddr/localPort and connects (with no timeout) to addr/port */
    public NIOSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        init();
        setInitialReader();
        setInitialWriter();
        bind(new InetSocketAddress(localAddr, localPort));
        connect(new InetSocketAddress(addr, port));
    }

    /** Creates an NIOSocket and connects (with no timeout) to addr/port */
    public NIOSocket(String addr, int port) throws UnknownHostException, IOException {
        this(InetAddress.getByName(addr), port);
    }

    /** Creates an NIOSocket locally bound to localAddr/localPort and connects (with no timeout) to addr/port */
    public NIOSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        this(InetAddress.getByName(addr), port, localAddr, localPort);
    }

    /**
     * Performs initialization for this NIOSocket. Currently just makes the channel non-blocking.
     */
    private void init() throws IOException {
        channel.configureBlocking(false);
    }

    /** Binds the socket to the SocketAddress */
    public void bind(SocketAddress endpoint) throws IOException {
        socket.bind(endpoint);
    }

    /** Stores the connecting address so we can retrieve it later. */
    public boolean connect(SocketAddress addr, int timeout, ConnectObserver observer) {
        InetSocketAddress a = (InetSocketAddress)addr;
        connectedTo = a.getAddress();
        port = a.getPort();
        return super.connect(addr, timeout, observer);
    }

    /**
     * Retrieves the host this is connected to. The separate variable for storage is necessary because Sockets created
     * with SocketChannel.open() return null when there's no connection.
     */
    public InetAddress getInetAddress() {
        return connectedTo;
    }
    
    /**
     * Returns the port this socket is connecting or connected to.
     */
    public int getPort() {
        return port;
    }

    /** Constructs an InterestReadChannel adapter around the SocketChannel. */
    protected InterestReadChannel getBaseReadChannel() {
        return new SocketInterestReadAdapter(channel);
    }

    /** Constructs an InterestWriteChannel adapter around the SocketChannel. */
    protected InterestWriteChannel getBaseWriteChannel() {
        return new SocketInterestWriteAdapter(channel);
    }

    /** Shuts down input, output & the socket. */
    protected void shutdownImpl() {
        try {
            shutdownInput();
        } catch (IOException ignored) {
        }

        try {
            shutdownOutput();
        } catch (IOException ignored) {
        }

        try {
            socket.close();
        } catch (IOException ignored) {
        } catch (Error ignored) {
        }
    }

    // /////////////////////////////////////////////
    // / BELOW ARE ALL WRAPPERS FOR SOCKET.
    // /////////////////////////////////////////////

    public SocketChannel getChannel() {
        return socket.getChannel();
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }

    public InetAddress getLocalAddress() {
        try {
            return socket.getLocalAddress();
        } catch (Error osxSucks) {
            // On OSX 10.3 w/ Java 1.4.2_05, if the connection dies
            // prior to this method being called, an Error is thrown.
            try {
                return InetAddress.getLocalHost();
            } catch (UnknownHostException uhe) {
                return null;
            }
        } catch (UnsupportedAddressTypeException uate) {
            SocketAddress localAddr = socket.getLocalSocketAddress();
            throw new RuntimeException("wrong address type: " + (localAddr == null ? null : localAddr.getClass()), uate);
        }
    }

    public boolean getOOBInline() throws SocketException {
        return socket.getOOBInline();
    }

    public int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }

    public boolean getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }

    public int getSendBufferSize() throws SocketException {
        return socket.getSendBufferSize();
    }

    public int getSoLinger() throws SocketException {
        return socket.getSoLinger();
    }

    public int getSoTimeout() throws SocketException {
        return socket.getSoTimeout();
    }

    public boolean getTcpNoDelay() throws SocketException {
        return socket.getTcpNoDelay();
    }

    public int getTrafficClass() throws SocketException {
        return socket.getTrafficClass();
    }

    public boolean isBound() {
        return socket.isBound();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public boolean isInputShutdown() {
        return socket.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return socket.isOutputShutdown();
    }

    public void sendUrgentData(int data) {
        throw new UnsupportedOperationException("No urgent data.");
    }

    public void setKeepAlive(boolean on) throws SocketException {
        socket.setKeepAlive(on);
    }

    public void setOOBInline(boolean on) throws SocketException {
        socket.setOOBInline(on);
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        socket.setReuseAddress(on);
    }

    public void setSendBufferSize(int size) throws SocketException {
        socket.setSendBufferSize(size);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        socket.setSoLinger(on, linger);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        socket.setTcpNoDelay(on);
    }

    public void setTrafficClass(int tc) throws SocketException {
        socket.setTrafficClass(tc);
    }

    public void shutdownInput() throws IOException {
        socket.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        socket.shutdownOutput();
    }

    public String toString() {
        return "NIOSocket::" + connectedTo + ", channel: " + channel.toString();
    }
}