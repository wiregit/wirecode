package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.ChannelReader;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.util.BufferUtils;
import org.limewire.util.FileUtils;

/**
 * An SSL-capable layer that can transform incoming and outgoing
 * data according to the specified SSLContext and cipher suite.
 */
class SSLReadWriteChannel implements InterestReadableByteChannel, InterestWritableByteChannel,
                                 ChannelReader, ChannelWriter {
    
    private static final Log LOG = LogFactory.getLog(SSLReadWriteChannel.class);

    /** The context from which to retrieve a new SSLEngine. */
    private final SSLContext context;
    /** An executor to perform blocking tasks. */
    private final Executor executor;
    /** The engine managing this SSL session. */
    private SSLEngine engine;
    /** A temporary buffer which data is unwrapped to. */
    private ByteBuffer readIncoming;
    /** The buffer which the underlying readSink is read into. */
    private ByteBuffer readOutgoing;
    /** The buffer which we wrap writes to. */
    private ByteBuffer writeOutgoing;
    /** The underlying channel to read from. */
    private volatile InterestReadableByteChannel readSink;
    /** The underlying channel to write to. */
    private volatile InterestWritableByteChannel writeSink;
    /** The last WriteObserver that indicated write interested. */
    private WriteObserver writeWanter;
    /** True if handshaking indicated we need to immediately perform a wrap. */
    private volatile boolean needsHandshakeWrap = false;
    /** True if a read finished and data was still buffered. */
    private volatile boolean readDataLeft = false;
    /** True only after a single read has been performed. */
    private AtomicBoolean firstReadDone = new AtomicBoolean(false);
    
    /* Statistic gathering variables. */
    private volatile long readConsumed;
    private volatile long readProduced;
    private volatile long writeConsumed;
    private volatile long writeProduced;
    
    /**
     * Whether or not this has been shutdown.
     * Shutting down must be atomic wrt initializing, so that
     * we can guarantee all allocated buffers are released
     * properly.
     * 
     * Shutdown is volatile so read/write/handleWrite can quickly
     * get it w/o locking.
     */
    private volatile boolean shutdown = false;
    private final Object initLock = new Object();
    
    /**
     * The last state of who interested us in readinng must be kept,
     * so that after handshaking finishes, we can put reading into
     * the correct interest state.  otherwise, our options are:
     *  1) leave interest on, which could potentially loop forever
     *     if the connected socket closes.
     *  2) turn interest off, which could confuse any callers that
     *     had wanted to read data.
     * 
     * Note that we don't have to do this for writing because writing
     * can succesfully turn itself off.
     */
    private boolean readInterest = false;
    private final Object readInterestLock = new Object();
    
    public SSLReadWriteChannel(SSLContext context, Executor executor) {
        this.executor = executor;
        this.context = context;
    }
    
    /**
     * Initializes this TLSLayer, using the given address and
     * enabling the given cipherSuites.
     * 
     * If clientMode is disabled, client authentication can be turned on/off.
     * 
     * @param addr
     * @param cipherSuites
     */
    void initialize(SocketAddress addr, String[] cipherSuites, boolean clientMode, boolean needClientAuth) {
        synchronized(initLock) {
            if(shutdown) {
                LOG.debug("Not initializing because already shutdown.");
                return;
            }
            
            if(addr != null) {
                if(!(addr instanceof InetSocketAddress))
                    throw new IllegalArgumentException("unsupported SocketAddress");
                InetSocketAddress iaddr = (InetSocketAddress)addr;
                String host = iaddr.getAddress().getHostAddress();
                int port = iaddr.getPort();
                engine = context.createSSLEngine(host, port);
            } else {
                engine = context.createSSLEngine();
            }
            engine.setEnabledCipherSuites(cipherSuites);
            engine.setUseClientMode(clientMode);
            if(!clientMode) {
                engine.setWantClientAuth(needClientAuth);
                engine.setNeedClientAuth(needClientAuth);
            }
            SSLSession session = engine.getSession();
            readIncoming = NIODispatcher.instance().getBufferCache().getHeap(session.getApplicationBufferSize());
            writeOutgoing = NIODispatcher.instance().getBufferCache().getHeap(session.getPacketBufferSize());
            if(LOG.isTraceEnabled())
                LOG.trace("Initialized engine: " + engine + ", session: " + session);
        }
    }

    public int read(ByteBuffer dst) throws IOException {
        if(shutdown)
            throw new ClosedChannelException();
        
        int transferred = 0;
        
        // If data was left in readOutgoing, pre-transfer it.
        if(readOutgoing != null && readOutgoing.position() > 0) {
            transferred += BufferUtils.transfer(readOutgoing, dst);
        }
        
        while(true) {
            // If we're not handshaking and there's no space to read into, exit early.
            // Must check separately for 'first read' and 'not handshaking', because
            // the engine isn't put into handshaking mode until a single read is done.
            if(firstReadDone.getAndSet(true) && !dst.hasRemaining() && engine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) {
                LOG.debug("No room left to transfer data, exiting");
                return transferred;
            }

            int read = -1;
            while(readIncoming.hasRemaining() && (read = readSink.read(readIncoming)) > 0);
            // if we last read EOF & nothing was put in sourceBuffer, EOF
            if(read == -1 && readIncoming.position() == 0) {
                LOG.debug("Read EOF, no data to transfer.  Connection finished");
                return -1;
            }

            // If we couldn't read anything, there's nothing to unwrap.
            if(readIncoming.position() == 0) {
                LOG.debug("Unable to read anything, exiting read loop");
                return 0;
            }

            readIncoming.flip();

            // Try unwrapping directly into dst first.
            SSLEngineResult result = engine.unwrap(readIncoming, dst);
            readProduced += result.bytesProduced();
            readConsumed += result.bytesConsumed();
            transferred += result.bytesProduced();
            SSLEngineResult.Status status = result.getStatus();
            
            // If dst didn't have enough space, use an intermediate buffer.
            if(status == Status.BUFFER_OVERFLOW) {
                // Initialize readOutgoing only if not shutdown,
                // but grab the lock after we've checked to make sure
                // it's non-null, to avoid lock every read.
                if(readOutgoing == null) {
                    synchronized(initLock) {
                        if(!shutdown)
                            readOutgoing = NIODispatcher.instance().getBufferCache().getHeap(engine.getSession().getPacketBufferSize());
                    }
                }
                result = engine.unwrap(readIncoming, readOutgoing);
                readProduced += result.bytesProduced();
                readConsumed += result.bytesConsumed();
                status = result.getStatus();
                if(status == Status.BUFFER_OVERFLOW)
                    throw new IllegalStateException("not enough room in fallback TLS buffer!");
                transferred += BufferUtils.transfer(readOutgoing, dst);
            }
            
            if(readIncoming.hasRemaining()) {
                readDataLeft = true;
                readIncoming.compact();
            } else {
                readDataLeft = false;
                readIncoming.clear();
            }
            
            if(LOG.isDebugEnabled())
                LOG.debug("Read unwrap result: " + result);
            
            // If we were unable to interpret this packet because not enough was
            // read, then we must exit and wait for more to be read later.
            if(status == Status.BUFFER_UNDERFLOW)
                return transferred;
            
            // If the engine is closed and nothing was transferred,
            // return -1 to show the stream ended.  Otherwise return
            // however much we were able to already transfer.
            if(status == Status.CLOSED) {
                if(transferred == 0)
                    return -1;
                else
                    return transferred;
            }
            
            // We may be handshaking, which requires processing of data...
            if(!processHandshakeResult(true, false, result.getHandshakeStatus()))
                return transferred;
        }
    }
    
    /**
     * Processes a single handshake result.
     * If a delegated task is needed, returns false & schedules the task(s).
     * If writing is needed, returns false only if currently reading.
     * If reading is needed, returns false only if currently writing.
     * Otherwise, returns true.
     */
    private boolean processHandshakeResult(boolean reading, boolean writing, HandshakeStatus hs) {
        needsHandshakeWrap = false;
        switch(hs) {
        case NEED_TASK:
            needTask();
            return false;
        case NEED_WRAP:
            needsHandshakeWrap = true;
            writeSink.interestWrite(this, true);
            readSink.interestRead(true);
            return writing;
        case NEED_UNWRAP:
            readSink.interestRead(true);
            writeSink.interestWrite(null, false);
            // If we had previously buffered read data, force a read.
            if(readDataLeft && !reading)
                NIODispatcher.instance().invokeLater(new Runnable() {
                    public void run() {
                        try {
                            read(BufferUtils.getEmptyBuffer());
                        } catch(IOException iox) {
                            FileUtils.close(SSLReadWriteChannel.this);
                        }
                    }
                });
            return reading;
        case FINISHED:
            synchronized(readInterestLock) {
                // set interest to what our observer wanted.
                readSink.interestRead(readInterest);
            }
            writeSink.interestWrite(this, true);
        case NOT_HANDSHAKING:
        default: 
            // no change.
            return true;
        }
    }
    
    /** The engine needs to run some tasks before proceeding... */
    private void needTask() {
        readSink.interestRead(false);
        writeSink.interestWrite(null, false);
        // Run as many tasks as possible, and then add another
        // that will process the next state.
        while(true) {
            final Runnable runner = engine.getDelegatedTask();
            if(runner == null) {
                executor.execute(new Runnable() {
                    public void run() {
                        HandshakeStatus status = engine.getHandshakeStatus();
                        if(LOG.isDebugEnabled())
                            LOG.debug("Task(s) finished, status: " + status);
                        processHandshakeResult(false, false, status);
                    }
                });
                break;
            } else {
                executor.execute(runner);
            }
        }
    }

    public int write(ByteBuffer src) throws IOException {
        if(shutdown)
            throw new ClosedChannelException();
        
        int consumed = 0;
        // do...while because we want to force one write even with empty buffers
        do {
            boolean wasEmpty = writeOutgoing.position() == 0;
            SSLEngineResult result = engine.wrap(src, writeOutgoing);
            writeProduced += result.bytesProduced();
            writeConsumed += result.bytesConsumed();
            if(LOG.isDebugEnabled())
                LOG.debug("Wrap result: " + result);
            consumed += result.bytesConsumed();
            SSLEngineResult.Status status = result.getStatus();
            if(status == Status.CLOSED && !isOpen())
                throw new ClosedChannelException();
            if(!processHandshakeResult(false, true, result.getHandshakeStatus()))
                break;
            if(status == Status.BUFFER_OVERFLOW) {
                if(wasEmpty)
                    throw new IllegalStateException("outgoing TLS buffer not large enough!");
                else
                    break;
            }
        } while(src.hasRemaining());
        
        return consumed;
    }

    public boolean handleWrite() throws IOException {
        if(shutdown)
            throw new ClosedChannelException();
        
        InterestWritableByteChannel source = writeSink;
        if(source == null)
            throw new IllegalStateException("writing with no source.");
            
        while(true) {
            if(writeOutgoing.position() > 0) {
                // Step 1: See if there is any pending data to be written.
                writeOutgoing.flip();
                writeSink.write(writeOutgoing);
                if(writeOutgoing.hasRemaining()) {
                    writeOutgoing.compact();
                    return true; // there is still data that is pending a write.
                }            
                writeOutgoing.clear();
            }

            // Step 2: If we need to do a handshake wrap, do that.
            if(needsHandshakeWrap) {
                LOG.debug("Forcing a handshake wrap");
                write(BufferUtils.getEmptyBuffer());
                if(writeOutgoing.position() > 0)
                    continue;
            }
                            
            // Step 3: Tell any interested parties to write data.
            WriteObserver interested = writeWanter;
            if(interested != null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Telling interested parties to write.  (a " + interested + ")");
                interested.handleWrite();
            }
            
            // If no data after that, we've written everything we want -- exit.
            if (writeOutgoing.position() == 0) {
                // We have nothing left to write, however, it is possible
                // that between the above check for interested.handleWrite & here,
                // we got pre-empted and another thread turned on interest.
                synchronized (this) {
                    if (writeWanter == null) // no observer? good, we can turn interest off
                        source.interestWrite(this, false);
                    // else, we've got nothing to write, but our observer might.
                }
                return false;
            }
        }
    }

    /**
     * Releases any resources that were acquired by the channel.
     * If the underlying channels are still open, this method only propogates
     * the shutdown call, instead of shutting down this channel, as it can
     * still be used by other channels.
     */
    public void shutdown() {
        synchronized(initLock) {
            if(shutdown)
                return;
            
            if(!isOpen()) {
                LOG.debug("Shutting down SSL channel");
                shutdown = true;
            }
        }
        
        if(shutdown) {
            NIODispatcher.instance().invokeLater(new Runnable() {
                public void run() {
                    ByteBufferCache cache = NIODispatcher.instance().getBufferCache();
                    if(readIncoming != null)
                        cache.release(readIncoming);
                    if(readOutgoing != null)
                        cache.release(readOutgoing);
                    if(writeOutgoing != null)
                        cache.release(writeOutgoing);
                }
            });
        }
        
        Shutdownable observer = writeWanter;
        if(observer != null)
            observer.shutdown();
    }

    public InterestReadableByteChannel getReadChannel() {
        return readSink;
    }

    public void setReadChannel(InterestReadableByteChannel newChannel) {
        this.readSink = newChannel;
        
    }

    public InterestWritableByteChannel getWriteChannel() {
        return writeSink;
    }

    public void setWriteChannel(InterestWritableByteChannel newChannel) {
        this.writeSink = newChannel;
    }

    public void close() throws IOException {
        readSink.close();
        writeSink.close();
    }

    public boolean isOpen() {
        return readSink.isOpen() && writeSink.isOpen();
    }

    public void handleIOException(IOException iox) {
        shutdown();
    }
    
    public void interestRead(boolean status) {
        synchronized(readInterestLock) {
            readInterest = status;
            readSink.interestRead(status);
        }
    }

    public synchronized void interestWrite(WriteObserver observer, boolean status) {
        this.writeWanter = status ? observer : null;
        InterestWritableByteChannel source = writeSink;
        if(source != null)
            source.interestWrite(this, true);
    }
    
    /** Returns the total number of bytes that this has produced from unwrapping reads. */
    long getReadBytesProduced() {
        return readProduced;
    }
    
    /** Returns the total number of bytes that this has consumed while unwrapping reads. */
    long getReadBytesConsumed() {
        return readConsumed;
    }
    
    /** Returns the total number of bytes that this has produced from wrapping writes. */
    long getWrittenBytesProduced() {
        return writeProduced;
    }
    
    /** Returns the total number of bytes that this has consumed while wrapping writes. */
    long getWrittenBytesConsumed() {
        return writeConsumed;
    }
}
