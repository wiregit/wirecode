package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImplFactory;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelReader;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.NIOInputStream;
import com.limegroup.gnutella.io.NIOMultiplexor;
import com.limegroup.gnutella.io.NoOpReader;
import com.limegroup.gnutella.io.ReadObserver;
import com.limegroup.gnutella.io.SoTimeout;

/** 
 *  Create a reliable udp connection interface.
 */
public class UDPConnection extends Socket implements NIOMultiplexor, ReadObserver, SoTimeout {
    public static final byte VERSION = (byte) 1;
    
    private static final Log LOG = LogFactory.getLog(UDPConnection.class);

    private final UDPSocketChannel channel;
	private final UDPConnectionProcessor processor;
    private final Object LOCK = new Object();
    private ReadObserver reader;
    private boolean shutdown = false;

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
		// Handle the real work in the processor
		processor = new UDPConnectionProcessor();
        channel = processor.getChannel();
        reader = new NIOInputStream(this, this, channel);        
        NIODispatcher.instance().register(channel, this);
        processor.connect(ip, port);
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
		return processor.getOutputStream();
	}

	public void setSoTimeout(int timeout) throws SocketException {
		processor.setSoTimeout(timeout);
	}

	public void close() {
		shutdown();
	}

    public InetAddress getInetAddress() {
        return processor.getInetAddress();
    }

    public InetAddress getLocalAddress() {
        return processor.getLocalAddress();
    }
    
    public int getSoTimeout() {
    	return processor.getReadTimeout();
    }
    
    //-------  Mostly Unimplemented  ----------------

    public UDPConnection() throws IOException {
        throw new IOException("not implemented");
    }


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

    // These won't compile in Java 1.3
    //public void connect(SocketAddress endpoint) throws IOException {
        //throw new IOException("not implemented");
    //}

    //public void connect(SocketAddress endpoint, int timeout) 
      //throws IOException {
        //throw new IOException("not implemented");
    //}

    //public void bind(SocketAddress bindpoint) throws IOException {
        //throw new IOException("not implemented");
    //}


    public int getPort() {
        return processor.getPort();
    }

    public int getLocalPort() {
        return UDPService.instance().getStableUDPPort();
    }

    // These won't compile in Java 1.3
    //public SocketAddress getRemoteSocketAddress() {
        //return null;
    //}

    //public SocketAddress getLocalSocketAddress() {
        //return null;
    //}

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
    
    public void shutdownOutput() throws IOException
    {
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
        
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                reader = new NoOpReader();
            }
        });
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

    public void setWriteObserver(ChannelWriter writer) {
        throw new UnsupportedOperationException();
    }
}
