package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.NBSocket;
import com.limegroup.gnutella.io.NIOMultiplexor;
import com.limegroup.gnutella.io.NIOServerSocket;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.io.ReadWriteObserver;

/** 
 * Returns two sockets A and B, so that the input of A is connected to 
 * the output of B and vice versa.  Typical use:
 * <pre>
 *     PipedSocketFactory factory=new PipedSocketFactory("1.1.1.1", "2.2.2.2");
 *     Connection cA=new Connection(factory.getSocketA());
 *     Connection cB=new Connection(factory.getSocketB());
 *     cA.send(m1);
 *     cB.receive();
 *     cB.send(m2);
 *     cA.receive();
 * </pre>
 */
public class PipedSocketFactory {
    
    private final ServerSocket ss;
    private final String hostA;
    private final String hostB;
    private NBSocket socketA;
    private NBSocket socketB;
    
        
    /**
     * @param hostA the address to use for socket A
     * @param hostB the address to use for socket B
     */
    public PipedSocketFactory(String hostA, String hostB) 
      throws IOException, UnknownHostException {
        this.hostA = hostA;
        this.hostB = hostB;
        ss = new NIOServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(0));
    }

    public Socket getSocketA() throws Exception {
        if(socketA == null)
            setupSockets();
        
        return new DelegatingSocket((NIOSocket)socketA, hostA, hostB);
    }

    public Socket getSocketB() throws Exception {
        if(socketB == null)
            setupSockets();
        
        return new DelegatingSocket((NIOSocket)socketB, hostB, hostA);
    }
    
    private void setupSockets() throws Exception {
        socketA = new NIOSocket(InetAddress.getLocalHost(), ss.getLocalPort());
        socketB = (NIOSocket)ss.accept();
    }
    
    private static class DelegatingSocket extends NBSocket implements NIOMultiplexor, ReadWriteObserver, ConnectObserver {
        private final NIOSocket delegate;
        private final String local;
        private final String remote;
        
        DelegatingSocket(NIOSocket delegate, String local, String remote) {
            this.delegate = delegate;
            this.local = local;
            this.remote = remote;
        }

        public void bind(SocketAddress bindpoint) throws IOException {
            delegate.bind(bindpoint);
        }

        public void close() throws IOException {
            delegate.close();
        }

        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            delegate.connect(endpoint, timeout);
        }

        public void connect(SocketAddress endpoint) throws IOException {
            delegate.connect(endpoint);
        }

        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        public SocketChannel getChannel() {
            return delegate.getChannel();
        }

        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName(remote);
            } catch(UnknownHostException uhe) {
                throw new RuntimeException(uhe);
            }
        }

        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }

        public boolean getKeepAlive() throws SocketException {
            return delegate.getKeepAlive();
        }

        public InetAddress getLocalAddress() {
            try {
                return InetAddress.getByName(local);
            } catch(UnknownHostException uhe) {
                throw new RuntimeException(uhe);
            }
        }

        public int getLocalPort() {
            return delegate.getLocalPort();
        }

        public SocketAddress getLocalSocketAddress() {
            return delegate.getLocalSocketAddress();
        }

        public boolean getOOBInline() throws SocketException {
            return delegate.getOOBInline();
        }

        public OutputStream getOutputStream() throws IOException {
            return delegate.getOutputStream();
        }

        public int getPort() {
            return delegate.getPort();
        }

        public int getReceiveBufferSize() throws SocketException {
            return delegate.getReceiveBufferSize();
        }

        public SocketAddress getRemoteSocketAddress() {
            return delegate.getRemoteSocketAddress();
        }

        public boolean getReuseAddress() throws SocketException {
            return delegate.getReuseAddress();
        }

        public int getSendBufferSize() throws SocketException {
            return delegate.getSendBufferSize();
        }

        public int getSoLinger() throws SocketException {
            return delegate.getSoLinger();
        }

        public int getSoTimeout() throws SocketException {
            return delegate.getSoTimeout();
        }

        public boolean getTcpNoDelay() throws SocketException {
            return delegate.getTcpNoDelay();
        }

        public int getTrafficClass() throws SocketException {
            return delegate.getTrafficClass();
        }

        public int hashCode() {
            return delegate.hashCode();
        }

        public boolean isBound() {
            return delegate.isBound();
        }

        public boolean isClosed() {
            return delegate.isClosed();
        }

        public boolean isConnected() {
            return delegate.isConnected();
        }

        public boolean isInputShutdown() {
            return delegate.isInputShutdown();
        }

        public boolean isOutputShutdown() {
            return delegate.isOutputShutdown();
        }

        public void sendUrgentData(int data) throws IOException {
            delegate.sendUrgentData(data);
        }

        public void setKeepAlive(boolean on) throws SocketException {
            delegate.setKeepAlive(on);
        }

        public void setOOBInline(boolean on) throws SocketException {
            delegate.setOOBInline(on);
        }

        public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
            delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
        }

        public void setReceiveBufferSize(int size) throws SocketException {
            delegate.setReceiveBufferSize(size);
        }

        public void setReuseAddress(boolean on) throws SocketException {
            delegate.setReuseAddress(on);
        }

        public void setSendBufferSize(int size) throws SocketException {
            delegate.setSendBufferSize(size);
        }

        public void setSoLinger(boolean on, int linger) throws SocketException {
            delegate.setSoLinger(on, linger);
        }

        public void setSoTimeout(int timeout) throws SocketException {
            delegate.setSoTimeout(timeout);
        }

        public void setTcpNoDelay(boolean on) throws SocketException {
            delegate.setTcpNoDelay(on);
        }

        public void setTrafficClass(int tc) throws SocketException {
            delegate.setTrafficClass(tc);
        }

        public void shutdownInput() throws IOException {
            delegate.shutdownInput();
        }

        public void shutdownOutput() throws IOException {
            delegate.shutdownOutput();
        }

        public String toString() {
            return delegate.toString();
        }

        public boolean connect(SocketAddress addr, int timeout, ConnectObserver observer) {
            return delegate.connect(addr, timeout, observer);
        }

        public void setReadObserver(ChannelReadObserver reader) {
            delegate.setReadObserver(reader);
        }

        public void setWriteObserver(ChannelWriter writer) {
            delegate.setWriteObserver(writer);
        }

        public void handleRead() throws IOException {
            delegate.handleRead();
        }

        public void handleIOException(IOException iox) {
            delegate.handleIOException(iox);
        }

        public void shutdown() {
            delegate.shutdown();
        }

        public boolean handleWrite() throws IOException {
            return delegate.handleWrite();
        }

        public void handleConnect(Socket socket) throws IOException {
            delegate.handleConnect(this);
        }
    }
}
