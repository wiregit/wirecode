package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImplFactory;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelReader;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.InterruptedIOException;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.NIOInputStream;
import com.limegroup.gnutella.io.NIOMultiplexor;
import com.limegroup.gnutella.io.NIOOutputStream;
import com.limegroup.gnutella.io.NoOpReader;
import com.limegroup.gnutella.io.NoOpWriter;
import com.limegroup.gnutella.io.ReadObserver;
import com.limegroup.gnutella.io.SoTimeout;
import com.limegroup.gnutella.io.ThrottleListener;
import com.limegroup.gnutella.io.WriteObserver;

/** 
 *  Create a reliable udp connection interface.
 */
public class UDPConnection extends Socket implements NIOMultiplexor, ReadObserver,
                                                     WriteObserver, SoTimeout,
                                                     ConnectObserver {
    public static final byte VERSION = (byte) 1;
    
    private static final Log LOG = LogFactory.getLog(UDPConnection.class);

    private final UDPSocketChannel channel;
	private final UDPConnectionProcessor processor;
    private final Object LOCK = new Object();
    private ReadObserver reader;
    private WriteObserver writer;
    private ConnectObserver connecter;
    private boolean shutdown = false;
    private int soTimeout = 1 * 60 * 1000; // default to 1 minute.
    
    /**
     * Creates an unconnected UDPConnection.
     * You must call connect(ip, port) to connect.
     * 
     * @throws IOException
     */
    public UDPConnection() throws IOException {
        // Handle the real work in the processor
        processor = new UDPConnectionProcessor();
        channel = processor.getChannel();
        channel.setSocket(this);
        reader = new NIOInputStream(this, this, channel);
        writer = new NIOOutputStream(this, channel);  
    }
    
    /**
     *  Create the UDPConnection.
     */
    public UDPConnection(String ip, int port) throws IOException {
        // Handle the real work in the processor
        this(InetAddress.getByName(ip), port);
    }     

    /**
     *  Create the UDPConnection.
     */
    public UDPConnection(InetAddress ip, int port) throws IOException {
		this();
        connect(new InetSocketAddress(ip, port));
    }   

	public InputStream getInputStream() throws IOException {
        if(isClosed())
            throw new IOException("Socket closed.");
        
        if(reader instanceof NIOInputStream) {
            NIODispatcher.instance().interestRead(channel, true);
            return ((NIOInputStream)reader).getInputStream();
        } else {
            throw new IllegalStateException("not an NIOInputStream!");
        }
	}

	public OutputStream getOutputStream() throws IOException {
        if(isClosed())
            throw new IOException("Socket closed.");
        
        if(writer instanceof NIOOutputStream) {
            NIODispatcher.instance().interestWrite(channel, true);
            return ((NIOOutputStream)writer).getOutputStream();
        } else {
            throw new IllegalStateException("not an NIOInputStream!");
        }
	}

	public void setSoTimeout(int timeout) throws SocketException {
        soTimeout = timeout;
	}

	public void close() {
		shutdown();
	}

    public InetAddress getInetAddress() {
        return processor.getSocketAddress().getAddress();
    }

    public InetAddress getLocalAddress() {
        return processor.getLocalAddress();
    }
    
    public int getSoTimeout() {
        return soTimeout;
    }
    
    //-------  Mostly Unimplemented  ----------------

    public UDPConnection(String host, int port, InetAddress localAddr,
      int localPort) throws IOException {
        throw new IOException("not implemented");
    }

    public UDPConnection(InetAddress address, int port, InetAddress localAddr,
      int localPort) throws IOException {
        throw new IOException("not implemented");
    }

    public UDPConnection(String host, int port, boolean stream) 
      throws IOException {
      throw new IOException("not implemented");
    }

    public UDPConnection(InetAddress host, int port, boolean stream) 
      throws IOException {
        throw new IOException("not implemented");
    }

    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }

    public void connect(SocketAddress addr, int timeout) throws IOException {
        BlockingConnecter connecter = new BlockingConnecter();
        synchronized(connecter) {
            if(!connect(addr, timeout, connecter)) {
                long then = System.currentTimeMillis();
                try {
                    connecter.wait();
                } catch(InterruptedException ie) {
                    shutdown();
                    throw new InterruptedIOException(ie);
                }
                
                if(!isConnected()) {
                    shutdown();
                    long now = System.currentTimeMillis();
                    if(timeout != 0 && now - then >= timeout)
                        throw new SocketTimeoutException("operation timed out (" + timeout + ")");
                    else
                        throw new ConnectException("Unable to connect!");
                }
            }
        }        
    }
    
    /**
     * Connects to the specified address within the given timeout (in milliseconds).
     * The given ConnectObserver will be notified of success or failure.
     * In the event of success, observer.handleConnect is called.  In a failure,
     * observer.shutdown is called.  observer.handleIOException is never called.
     *
     * Returns true if this was able to connect immediately.  The observer is still
     * notified about the success even it it was immediate.

     */
    public boolean connect(SocketAddress addr, int timeout, ConnectObserver observer) {
        InetSocketAddress iaddr = (InetSocketAddress)addr;
        this.connecter = observer;
        
        try {
            if (iaddr.isUnresolved())
                throw new IOException("unresolved: " + addr);
            
            if(channel.connect(addr)) {
                observer.handleConnect(this);
                return true;
            } else {
                NIODispatcher.instance().registerConnect(channel, this, timeout);
                return false;
            }
        } catch(IOException failed) {
            shutdown();
            return false;
        }
    }
        

    public void bind(SocketAddress bindpoint) throws IOException {
        throw new IOException("not implemented");
    }


    public int getPort() {
        return processor.getSocketAddress().getPort();
    }

    public int getLocalPort() {
        return UDPService.instance().getStableUDPPort();
    }

    public SocketAddress getRemoteSocketAddress() {
        return processor.getSocketAddress();
    }

    public SocketAddress getLocalSocketAddress() {
        return new InetSocketAddress(getLocalAddress(), getLocalPort());
    }

    public SocketChannel getChannel() {
        return null; // TODO: have an Adapter to the UDPSocketChannel
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        // does nothing
    }

    public boolean getTcpNoDelay() throws SocketException {
        return true;
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        // does nothing
    }

    public int getSoLinger() throws SocketException {
        return -1;
    }

    public void sendUrgentData (int data) throws IOException  {
        throw new IOException("not implemented");
    }

    public void setOOBInline(boolean on) throws SocketException {
        throw new SocketException("not implemented");
    }

    public boolean getOOBInline() throws SocketException {
        throw new SocketException("not implemented");
    }

    public synchronized void setSendBufferSize(int size)
      throws SocketException {
        throw new SocketException("not implemented");
    }

    public synchronized int getSendBufferSize() throws SocketException {
        throw new SocketException("not implemented");
    }

    public synchronized void setReceiveBufferSize(int size)
      throws SocketException{
        throw new SocketException("not implemented");
    }

    public synchronized int getReceiveBufferSize()
      throws SocketException{
        throw new SocketException("not implemented");
    }

    public void setKeepAlive(boolean on) throws SocketException {
        // ignore
    }

    public boolean getKeepAlive() throws SocketException {
        return true;
    }

    public void setTrafficClass(int tc) throws SocketException {
        throw new SocketException("not implemented");
    }

    public int getTrafficClass() throws SocketException {
        throw new SocketException("not implemented");
    }

    public void setReuseAddress(boolean on) throws SocketException {
        throw new SocketException("not implemented");
    }

    public boolean getReuseAddress() throws SocketException {
        throw new SocketException("not implemented");
    }

    public void shutdownInput() throws IOException {
        throw new SocketException("not implemented");
    }

    public void shutdownOutput() throws IOException {
        throw new IOException("not implemented");
    }

    public String toString() {
        return "UDPConnection:" + processor;
    }

    public boolean isConnected() {
        return processor.isConnected();
    }

    public boolean isBound() {
        return true;
    }

    public boolean isClosed() {
        return !processor.isConnected();
    }

    public boolean isInputShutdown() {
        return !processor.isConnected();
    }

    public boolean isOutputShutdown() {
        return !processor.isConnected();
    }

    public static void setSocketImplFactory(SocketImplFactory fac)
      throws IOException {
        throw new IOException("not implemented");
    }

    public void handleIOException(IOException iox) {
        shutdown();
    }

    public void shutdown() {
        synchronized(LOCK) {
            if(shutdown)
                return;
            shutdown = true;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Shutting down socket & streams for: " + this);
 
        try {
            processor.close();
        } catch(IOException ignored) {}
            
        reader.shutdown();
        writer.shutdown();
        if(connecter != null)
            connecter.shutdown();        
        
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                reader = new NoOpReader();
                writer = new NoOpWriter();
                connecter = null;
            }
        });
    }
    
    /**
     * Notification that a connect can occur.
     *
     * This passes it off on to the delegating connecter and then forgets the
     * connecter for the duration of the connection.
     */
    public void handleConnect(Socket s) throws IOException {
        // Clear out connector prior to calling handleConnect.
        // This is so that if handleConnect throws an IOX, the
        // observer won't be confused by having both handleConnect &
        // shutdown called.  It'll be one or the other.
        ConnectObserver observer = connecter;
        connecter = null;
        observer.handleConnect(this);
    }    

    public void handleRead() throws IOException {
        reader.handleRead();
    }

    public void setReadObserver(final ChannelReadObserver newReader) {
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                ReadObserver oldReader = reader;
                try {
                    reader = newReader;
                    ChannelReader lastChannel = newReader;
                    // go down the chain of ChannelReaders and find the last one to set our source
                    while(lastChannel.getReadChannel() instanceof ChannelReader)
                        lastChannel = (ChannelReader)lastChannel.getReadChannel();
                    
                    if(oldReader instanceof InterestReadChannel && oldReader != newReader) {
                        lastChannel.setReadChannel((InterestReadChannel)oldReader);
                        reader.handleRead(); // read up any buffered data.
                        oldReader.shutdown(); // shutdown the now unused reader.
                    }
                    
                    lastChannel.setReadChannel(channel);
                    NIODispatcher.instance().interestRead(channel, true);
                } catch(IOException iox) {
                    shutdown();
                    oldReader.shutdown(); // in case we lost it.
                }
            }
        });
    }

    public void setWriteObserver(final ChannelWriter newWriter) {
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                try {
                    if(writer.handleWrite())
                        throw new IllegalStateException("data still in old writer!");
                    writer.shutdown();

                    ChannelWriter lastChannel = newWriter;
                    while(lastChannel.getWriteChannel() instanceof ChannelWriter) {
                        lastChannel = (ChannelWriter)lastChannel.getWriteChannel();
                        if(lastChannel instanceof ThrottleListener)
                            ((ThrottleListener)lastChannel).setAttachment(UDPConnection.this);
                    }

                    writer = channel;
                    lastChannel.setWriteChannel(channel);
                } catch(IOException iox) {
                    shutdown();
                    newWriter.shutdown(); // in case we hadn't set it yet.
                }
            }
       });
    }

    public boolean handleWrite() throws IOException {
        return writer.handleWrite();
    }
    
    /** A ConnectObserver to use when someone wants to do a blocking connection. */
    private static class BlockingConnecter implements ConnectObserver {
        BlockingConnecter() {}
         
        public synchronized void handleConnect(Socket s) { notify(); }
        public synchronized void shutdown() { notify(); }
        
        // unused.
        public void handleIOException(IOException iox) {}
    }    
}
