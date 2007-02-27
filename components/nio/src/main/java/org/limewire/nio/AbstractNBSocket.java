package org.limewire.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.ChannelReader;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.ReadObserver;
import org.limewire.nio.observer.ReadWriteObserver;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.nio.timeout.ReadTimeout;
import org.limewire.nio.timeout.SoTimeout;

/**
 * Implements all common functionality that a non-blocking socket must contain.
 * Specifically, this will handle the multiplexing aspect of handing off
 * reading, writing & connecting to other observers.  It will also handle correctly
 * traversing the chain of readers & writers so-as to set read up leftover data and
 * ensure all remaining data is written.   This also exposes a common blocking input
 * & output stream.
 */
public abstract class AbstractNBSocket extends NBSocket implements ConnectObserver, ReadWriteObserver,
                                                                  NIOMultiplexor, ReadTimeout, SoTimeout{
    
    private static final Log LOG = LogFactory.getLog(AbstractNBSocket.class);


    /** Lock for shutting down. */
    private final Object LOCK = new Object();

    /** The reader. */
    protected volatile ReadObserver reader;

    /** The writer. */
    protected volatile WriteObserver writer;

    /** The connecter. */
    protected volatile ConnectObserver connecter;
    
    /** An observer for being shutdown. */
    protected volatile Shutdownable shutdownObserver;

    /** Whether or not we've shutdown the socket. */
    private boolean shutdown = false;
    
    /**
     * Retrieves the channel which should be used as the base channel
     * for all reading operations.
     */
    protected abstract InterestReadableByteChannel getBaseReadChannel();
    
    /**
     * Retrives the channel which should be used as the base channel
     * for all writing operations.
     */
    protected abstract InterestWritableByteChannel getBaseWriteChannel();
    
    /**
     * Performs any operations required for shutting down this socket.
     * This will only be called once per Socket.
     */
    protected abstract void shutdownImpl();
    
    /**
     * Sets the initial reader value.
     */
    public final void setInitialReader() {
        reader = new NIOInputStream(this, this, getBaseReadChannel());
    }
    
    /**
     * Sets the initial writer value.
     */
    public final void setInitialWriter() {
        writer = new NIOOutputStream(this, getBaseWriteChannel());
    }
    
    /**
     * Sets the Shutdown observer.
     * This observer is useful for when the Socket is created,
     * but connect has not been called yet.  This observer will be
     * notified when the socket is shutdown.
     */
    public final void setShutdownObserver(Shutdownable observer) {
        shutdownObserver = observer;
    }
    
    /**
     * Sets the new ReadObserver.
     *
     * The deepest ChannelReader in the chain first has its source
     * set to the prior reader (assuming it implemented ReadableByteChannel)
     * and a read is notified, in order to read any buffered data.
     * The source is then set to the Socket's channel and interest
     * in reading is turned on.
     */
    public final void setReadObserver(final ChannelReadObserver newReader) {
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                ReadObserver oldReader = reader;
                try {
                    synchronized(LOCK) {
                        if(shutdown) {
                            newReader.shutdown();
                            return;
                        }
                        reader = newReader;
                    }
                    
                    // At this point, if the socket gets shutdown, we know the
                    // reader is going to be notified of the shutdown.
                    
                    ChannelReader lastChannel = newReader;
                    // go down the chain of ChannelReaders and find the last one to set our source
                    while(lastChannel.getReadChannel() instanceof ChannelReader)
                        lastChannel = (ChannelReader)lastChannel.getReadChannel();
                    
                    if(lastChannel instanceof ThrottleListener)
                    	((ThrottleListener)lastChannel).setAttachment(AbstractNBSocket.this);
                    
                    if(oldReader instanceof InterestReadableByteChannel && oldReader != newReader) {
                        lastChannel.setReadChannel((InterestReadableByteChannel)oldReader);
                        reader.handleRead(); // read up any buffered data.
                        oldReader.shutdown(); // shutdown the now unused reader.
                    }
                    
                    InterestReadableByteChannel source = getBaseReadChannel();
                    lastChannel.setReadChannel(source);
                    NIODispatcher.instance().interestRead(getChannel(), true);
                } catch(IOException iox) {
                    shutdown();
                    oldReader.shutdown(); // in case we lost it.
                }
            }
        });
    }
    
    /**
     * Sets the new WriteObserver.
     *
     * If a ThrottleWriter is one of the ChannelWriters, the attachment
     * of the ThrottleWriter is set to be this.
     *
     * The deepest ChannelWriter in the chain has its source set to be
     * a new InterestWriteChannel, which will be used as the hub to receive
     * and forward interest events from/to the channel.
     *
     * If this is called while the existing WriteObserver still has data left to
     * write, then an IllegalStateException is thrown.
     */
    public final void setWriteObserver(final ChannelWriter newWriter) {
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
                            ((ThrottleListener)lastChannel).setAttachment(AbstractNBSocket.this);
                    }

                    InterestWritableByteChannel source = getBaseWriteChannel();
                    
                    synchronized(LOCK) {
                        lastChannel.setWriteChannel(source);
                        if(shutdown) {
                            source.shutdown();
                            return;
                        }
                        writer = source;
                    }
                } catch(IOException iox) {
                    shutdown();
                    newWriter.shutdown(); // in case we hadn't set it yet.
                }
            }
       });
   }
    
    /**
     * Notification that a connect can occur.
     *
     * This passes it off on to the delegating connecter and then forgets the
     * connecter for the duration of the connection.
     */
    public final void handleConnect(Socket s) throws IOException {
        // Clear out connector prior to calling handleConnect.
        // This is so that if handleConnect throws an IOX, the
        // observer won't be confused by having both handleConnect &
        // shutdown called.  It'll be one or the other.
        ConnectObserver observer = connecter;
        connecter = null;
        observer.handleConnect(this);
    }
    
    /**
     * Notification that a read can occur.
     *
     * This passes it off to the delegating reader.
     */
    public final void handleRead() throws IOException {
        reader.handleRead();
    }
    
    /**
     * Notification that a write can occur.
     *
     * This passes it off to the delegating writer.
     */
    public final boolean handleWrite() throws IOException {
        return writer.handleWrite();
    }
    
    /** Closes the socket & all streams, waking up any waiting locks.  */
    public final void close() {
        shutdown();
    }
    
    /** Connects to addr with no timeout */
    public final void connect(SocketAddress addr) throws IOException {
        connect(addr, 0);
    }
    
    /** Connects to addr with the given timeout (in milliseconds) */
    public final void connect(SocketAddress addr, int timeout) throws IOException {
        BlockingConnecter connecter = new BlockingConnecter();
        synchronized(connecter) {
            if(!connect(addr, timeout, connecter)) {
                long then = System.currentTimeMillis();
                try {
                	// wait a little extra to allow other threads to notify
                    connecter.wait(timeout > 0 ? timeout + 1000 : timeout);
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
        synchronized(LOCK) {
            if(shutdown) {
                observer.shutdown();
                return false;
            }
            
            // Set the connectObserver within the lock so that the connecter
            // will not be set away from null after shutdown is called.
            this.connecter = observer;
        }
        
        // At this point, we know that if shutdown is called, the observer
        // will be notified of the shutdown.
        
        try {
            InetSocketAddress iaddr = (InetSocketAddress)addr;
            if (iaddr.isUnresolved())
                throw new IOException("unresolved: " + addr);
            
            if(getChannel().connect(addr)) {
                observer.handleConnect(this);
                return true;
            } else {
                NIODispatcher.instance().registerConnect(getChannel(), this, timeout);
                return false;
            }
        } catch(IOException failed) {
            NIODispatcher.instance().invokeReallyLater(new Runnable() {
                public void run() {
                    shutdown();
                }
            });
            return false;
        }
    }


    
    /**
     * Returns the InputStream from the NIOInputStream.
     *
     * Internally, this is a blocking Pipe from the non-blocking SocketChannel.
     */
    public final InputStream getInputStream() throws IOException {
        // Unlocked check real quickly.
        if(isClosed() || isShutdown())
            throw new IOException("Socket closed.");
        
        ReadObserver localReader;
        synchronized(LOCK) {
            if(isShutdown())
                throw new IOException("Socket closed.");
            localReader = reader;
        }
        
        if(localReader instanceof NIOInputStream) {
            // Ensure the stream is initialized before we interest it.
            InputStream stream = ((NIOInputStream)localReader).getInputStream();
            NIODispatcher.instance().interestRead(getChannel(), true);
            return stream;
        } else {
            Callable<InputStream> callable = new Callable<InputStream>() {
                public InputStream call() throws IOException {
                    NIOInputStream stream = new NIOInputStream(AbstractNBSocket.this, AbstractNBSocket.this, null).init();
                    setReadObserver(stream);
                    return stream.getInputStream();
                }
            };
            
            Future<InputStream> future = NIODispatcher.instance().submit(callable);
            try {
                return future.get();
            } catch(ExecutionException ee) {
                throw (IOException)new IOException().initCause(ee.getCause());
            } catch (InterruptedException ie) {
                throw (IOException)new IOException().initCause(ie.getCause());
            }
        }
    }
    
    /**
     * Returns the OutputStream from the NIOOutputStream.
     *
     * Internally, this is a blocking Pipe from the non-blocking SocketChannel.
     */
    public final OutputStream getOutputStream() throws IOException {
        // Unlocked check real quickly.
        if(isClosed() || isShutdown())
            throw new IOException("Socket closed.");
        
        // Retrieve the writer within the lock, guaranteeing
        // that it is not going to change to a NoOp if it was
        // shutdown after we got it.
        WriteObserver localWriter;
        synchronized(LOCK) {
            if(isShutdown())
                throw new IOException("Socket closed.");
            localWriter = writer;
        }
        
        if(localWriter instanceof NIOOutputStream)
            return ((NIOOutputStream)localWriter).getOutputStream();
        else
            throw new IllegalStateException("writer not NIOOutputStream, it's a: " + writer);
    }
    
    /** Gets the read timeout for this socket. */
    public final long getReadTimeout() {
        if(reader instanceof NIOInputStream) {
            return 0; // NIOInputStream handles its own timeouts.
        } else {
            try {
                return getSoTimeout();
            } catch(SocketException se) {
                return 0;
            }
        }
    }    
    
    /**
     * Notification that an IOException occurred while processing a
     * read, connect, or write.
     */
    public final void handleIOException(IOException iox) {
        shutdown();
    }
    
    /**
     * Shuts down this socket & all its streams.
     */
    public final void shutdown() {
        synchronized(LOCK) {
            if(shutdown)
                return;
            shutdown = true;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("Shutting down socket & streams for: " + this);
 
        shutdownImpl();
            
        try {
            getChannel().close();
        } catch(IOException ignored) {}
        
        reader.shutdown();
        writer.shutdown();
        if(connecter != null)
            connecter.shutdown();
        if(shutdownObserver != null)
            shutdownObserver.shutdown();
        
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                reader = new NoOpReader();
                writer = new NoOpWriter();
                connecter = null;
                shutdownObserver = null;
            }
        });
    }
    
    private boolean isShutdown() {
        synchronized(LOCK) {
            return shutdown;
        }
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
