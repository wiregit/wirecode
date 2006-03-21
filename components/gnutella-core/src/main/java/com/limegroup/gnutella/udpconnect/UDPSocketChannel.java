package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.BufferUtils;
import com.limegroup.gnutella.io.ConnectableChannel;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.io.WriteObserver;

/**
 * Interface between reading channels & UDP's data.
 * Analagous to SocketChannel combined w/ a SocketInterestReadAdapter & SocketInterestWriteAdapter.
 * 
 * This class _is_ the SocketChannel for UDP, except because we wrote it,
 * we can make it implement InterestReadChannel & InterestWriteChannel, so
 * we don't need the additional InterestAdapter.
 */
class UDPSocketChannel extends SelectableChannel implements InterestReadChannel,
                                                            InterestWriteChannel,
                                                            ConnectableChannel {
    
    private static final Log LOG = LogFactory.getLog(UDPSocketChannel.class);
    
    /** The SelectionKey associated with this channel. */
    private SelectionKey key;
    
    /** The processor this channel is writing to / reading from. */
    private final UDPConnectionProcessor processor;
    
    /** The Socket object this UDPSocketChannel is used for. */
    private Socket socket;
    
    /** The DataWindow containing incoming read data. */
    private final DataWindow readData;
    
    /** The WriteObserver that last requested interest from us. */
    private volatile WriteObserver writer;
    
    /** The list of buffered chunks that need to be written out. */
    private ArrayList /* of ByteBuffer */ chunks;
    
    /** The current chunk we're writing to. */
    private ByteBuffer activeChunk;
    
    /** Whether or not we've handled one write yet. */
    private boolean writeHandled = false;
    
    /** A lock to hold while manipulating chunks or activeChunk. */
    private final Object writeLock = new Object();
    
    /** Whether or not we've propogated the shutdown to other writers. */
    private boolean shutdown = false;
    
    UDPSocketChannel() {
        this.processor = new UDPConnectionProcessor(this);
        this.readData = processor.getReadWindow();
        this.chunks = new ArrayList(5);
        allocateNewChunk();
    }

    /**
     * Sets read interest on or off in the processor.
     */
    public void interest(boolean status) {
        NIODispatcher.instance().interestRead(this, status);
    }
    
    /// ********** reading ***************

    /**
     * Reads all possible data from the DataWindow into the ByteBuffer,
     * sending a keep alive if more space became available.
     */
    public int read(ByteBuffer to) throws IOException {
        // It is possible that the channel is open but the processor
        // is closed.  In that case, this will return -1.
        // Once this closes, it throws CCE.
        if(!isOpen())
            throw new ClosedChannelException();
        
        synchronized (processor) {
            int read = 0;
            DataRecord currentRecord = readData.getReadableBlock();
            while (currentRecord != null) {
                read += transfer(currentRecord, to);
                if (!to.hasRemaining())
                    break;

                // If to still has room left, we must have written
                // all we could from the record, so we assign a new one.
                // Fetch a block from the receiving window.
                currentRecord = readData.getReadableBlock();
            }

            // Now that we've transferred all we can to the buffer, clear up
            // the space & send a keep-alive if necessary
            // Record how much space was previously available in the receive window
            int priorSpace = readData.getWindowSpace();

            // Remove all records we just read from the receiving window
            readData.clearEarlyReadBlocks();

            // If the receive window opened up then send a special
            // KeepAliveMessage so that the window state can be
            // communicated.
            if ((priorSpace == 0 && read > 0)
                    || (priorSpace <= UDPConnectionProcessor.SMALL_SEND_WINDOW &&
                        readData.getWindowSpace() > UDPConnectionProcessor.SMALL_SEND_WINDOW)) {
                processor.sendKeepAlive();
            }
        
            if(read == 0 && processor.isClosed())
                return -1;
            else
                return read;
        }
    }
    
    /**
     * Transfers the chunks in the DataRecord's msg to the ByteBuffer. Sets the record as being succesfully read after
     * all data is read from it.
     * 
     * @param record
     * @param to
     * @return
     */
    private int transfer(DataRecord record, ByteBuffer to) {
        DataMessage msg = (DataMessage)record.msg;
        int read = 0;
        
        ByteBuffer chunk = msg.getData1Chunk();
        if(chunk.hasRemaining())
            read += BufferUtils.transfer(chunk, to, false);
        
        if(chunk.hasRemaining())
            return read;
        
        chunk = msg.getData2Chunk();
        read += BufferUtils.transfer(chunk, to, false);
        
        if(!chunk.hasRemaining())
            record.read = true;
        
        return read;
    }
    
    /// ********** writing ***************
    
    /**
     * Writes all data in src into a list of internal chunks.
     * This will notify the processor if we have no pending chunks prior
     * to writing, so that it will know to retrieve some data.
     * Chunks will be created until the processor tells us we're at the limit,
     * at which point this will forcibly will return the amount of data that
     * could be written so far.
     * If all data is emptied from src, this will return that amount of data.
     */
    public int write(ByteBuffer src) throws IOException {
        // We cannot write if either the channel or the processor is closed.
        if(!isOpen() || processor.isClosed())
            throw new ClosedChannelException();
            
        synchronized(writeLock) {
            // If there was no data before this, then ensure a writer is awake
            if ( getNumberOfPendingChunks() == 0 ) {
                processor.wakeupWriteEvent(!writeHandled);
            }
            
            writeHandled = true;
            
            int wrote = 0;
            while (true) {
                if(src.hasRemaining()) {
                    if (activeChunk.hasRemaining()) {
                        wrote += BufferUtils.transfer(src, activeChunk, false);
                    } else if (chunks.size() < processor.getChunkLimit()) {
                        // If there is room for more chunks, allocate a new chunk
                        chunks.add(activeChunk);
                        allocateNewChunk();
                    } else {
                        return wrote;
                    }
                } else {
                    return wrote;
                }
            }
        }   
    }
    
    /**
     *  Allocates a chunk for writing to.
     */
    private void allocateNewChunk() {
        activeChunk = ByteBuffer.allocate(UDPConnectionProcessor.DATA_CHUNK_SIZE);
    }

    /**
     *  Gets the first chunk of data that should be written to the wire.
     *  Returns null if no data.
     */
    ByteBuffer getNextChunk() {
        synchronized(writeLock) {
            ByteBuffer rChunk;
            if ( chunks.size() > 0 ) {
                // Return the oldest chunk 
                rChunk = (ByteBuffer)chunks.remove(0);
                rChunk.flip();
            } else if (activeChunk.position() > 0) {
                rChunk = activeChunk;
                rChunk.flip();
                allocateNewChunk();
            } else {
                // If no data currently, return null
                rChunk = null;
            }            
            return rChunk;
        }
    }
    
    /**
     *  Return how many pending chunks are waiting on being written to the wire.
     */
    int getNumberOfPendingChunks() {
        synchronized(writeLock) { 
            // Add the number of list blocks
            int count = chunks.size();
            
            // Add one for the current block if data available.
            if (activeChunk.position() > 0)
                count++;
    
            return count;
        }
    }
    
    Object writeLock() {
        return writeLock;
    }

    /// ********** SelectableChannel methods. ***************
    
    /** Unsupported. */
    public Object blockingLock() {
        throw new UnsupportedOperationException();
    }

    /** Unsupported. */
    public SelectableChannel configureBlocking(boolean block) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Returns false. */
    public boolean isBlocking() {
        return false;
    }

    /** Returns true if register(...) was called. */
    public boolean isRegistered() {
        return key != null;
    }

    /** Returns the UDPSelectionKey that this created when it was registered. */
    public SelectionKey keyFor(Selector sel) {
        return key;
    }

    /** Unsupported. */
    public SelectorProvider provider() {
        throw new UnsupportedOperationException();
    }

    /** Creates a new UDPSelectionKey & attaches the attachment, then returns it. */
    public synchronized SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
        if(!isOpen())
            throw new ClosedChannelException();
        
        key = new UDPSelectionKey(processor, att, this, ops);        
        return key;
    }

    /** Returns OP_READ & OP_WRITE. */
    public int validOps() {
        return SelectionKey.OP_READ 
             & SelectionKey.OP_WRITE 
             & SelectionKey.OP_CONNECT;
    }

    /** Closes the processor. */
    protected void implCloseChannel() throws IOException {
        synchronized(this) {
            if(key != null)
                key.cancel();
        }
        
        processor.close();
    }

    /// ********** InterestWriteChannel methods. ***************

    /**
     * Shuts down this channel & processor, notifying the last interested party
     * that we're now shutdown.
     */
    public void shutdown() {
        synchronized(this) {
            if(shutdown)
                return;
            shutdown = true;
        }
        
        try {
            close();
        } catch(IOException ignored) {}
        
        Shutdownable chain = writer;
        if(chain != null)
            chain.shutdown();
        writer = null;
    }

    /** Sets interest on or off on the channel & stores the interested party for future writing. */
    public void interest(WriteObserver observer, boolean status) {
        if(isOpen()) { 
            writer = observer;
            NIODispatcher.instance().interestWrite(this, status);
        }
    }

    /** Sends a write up the chain to the last interest party. */
    public boolean handleWrite() throws IOException {
        WriteObserver chain = writer;
        if(chain != null)
            return chain.handleWrite();
        else
            return false;
    }

    /** Unused. */
    public void handleIOException(IOException iox) {
        throw new UnsupportedOperationException();
    }
    
    public SocketAddress getRemoteSocketAddress() {
        return processor.getSocketAddress();
    }

    public boolean connect(SocketAddress remote) throws IOException {
        processor.connect((InetSocketAddress)remote);
        return false;
    }

    public boolean finishConnect() throws IOException {
        return processor.prepareOpenConnection();
    }

    public boolean isConnected() {
        return processor.isConnected();
    }

    public boolean isConnectionPending() {
        return processor.isConnecting();
    }

    public Socket socket() {
        return socket;
    }

    void setSocket(Socket socket) {
        this.socket = socket;
    }
    

    
    //
    // -----------------------------------------------------------------
    
    protected void finalize() {
        if (isOpen()) {
            LOG.warn("finalizing an open UDPSocketChannel!");
            try {
                close();
            } catch (IOException ignored) {}
        }
    }    
}
