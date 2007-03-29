package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private final SSLContext context;
    private final Executor executor;
    private SSLEngine engine;
    private ByteBuffer readIncoming;
    private ByteBuffer readOutgoing;
    private ByteBuffer writeOutgoing;
    private volatile InterestReadableByteChannel readSink;
    private volatile InterestWritableByteChannel writeSink;
    private WriteObserver writeWanter;
    private volatile boolean needsHandshakeWrap = false;
    private volatile boolean readDataLeft = false;
    
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
        readIncoming = ByteBuffer.allocate(session.getApplicationBufferSize());
        writeOutgoing = ByteBuffer.allocate(session.getPacketBufferSize());
        if(LOG.isTraceEnabled())
            LOG.trace("Initialized engine: " + engine + ", session: " + session);
    }

    public int read(ByteBuffer dst) throws IOException {
        int transferred = 0;
        
        // If data was left in readOutgoing, pre-transfer it.
        if(readOutgoing != null && readOutgoing.position() > 0) {
            transferred += BufferUtils.transfer(readOutgoing, dst);
        }        
        
        while(true) {
            // If we're not handshaking and there's no space to read into, exit early.
            if(!dst.hasRemaining() && engine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) {
                LOG.debug("No space to read into, leaving");
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
            transferred += result.bytesProduced();
            SSLEngineResult.Status status = result.getStatus();
            
            // If dst didn't have enough space, use an intermediate buffer.
            if(status == Status.BUFFER_OVERFLOW) {
                if(readOutgoing == null)
                    readOutgoing = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
                result = engine.unwrap(readIncoming, readOutgoing);
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
            // ensure we're ready for everything.
            readSink.interestRead(true);
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
        int consumed = 0;
        // do...while because we want to force one write even with empty buffers
        do {
            boolean wasEmpty = writeOutgoing.position() == 0;
            SSLEngineResult result = engine.wrap(src, writeOutgoing);
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

    public void shutdown() {
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
        readSink.interestRead(status);
    }

    public synchronized void interestWrite(WriteObserver observer, boolean status) {
        this.writeWanter = status ? observer : null;
        InterestWritableByteChannel source = writeSink;
        if(source != null)
            source.interestWrite(this, true);
    }

}
