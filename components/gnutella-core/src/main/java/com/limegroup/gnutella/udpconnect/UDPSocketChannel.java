package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;

import org.limewire.nio.BufferUtils;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.InterestReadChannel;
import org.limewire.nio.channel.InterestWriteChannel;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.TransportListener;
import org.limewire.nio.observer.WriteObserver;


/**
 * Interface between reading channels & UDP's data.
 * Analagous to SocketChannel combined w/ a SocketInterestReadAdapter & SocketInterestWriteAdapter.
 * 
 * This class _is_ the SocketChannel for UDP, except because we wrote it,
 * we can make it implement InterestReadChannel & InterestWriteChannel, so
 * we don't need the additional InterestAdapter.
 */
class UDPSocketChannel extends SocketChannel implements InterestReadChannel,
                                                        InterestWriteChannel,
                                                        ChunkReleaser {
    
    /** The processor this channel is writing to / reading from. */
    private final UDPConnectionProcessor processor;
    
    /** The <tt>TransportListener</tt> to notify for pending events */
    private final TransportListener listener;
    
    /** The Socket object this UDPSocketChannel is used for. */
    private Socket socket;
    
    /** The DataWindow containing incoming read data. */
    private final DataWindow readData;
    
    /** The WriteObserver that last requested interest from us. */
    private volatile WriteObserver writer;
    
    /** The list of buffered chunks that need to be written out. */
    private ArrayList<ByteBuffer> chunks;
    
    /** The current chunk we're writing to. */
    private ByteBuffer activeChunk;
    
    /** Whether or not we've handled one write yet. */
    private boolean writeHandled = false;
    
    /** A lock to hold while manipulating chunks or activeChunk. */
    private final Object writeLock = new Object();
    
    /** Whether or not we've propogated the shutdown to other writers. */
    private boolean shutdown = false;
    
    UDPSocketChannel(SelectorProvider provider, TransportListener listener) {
        super(provider);
        this.listener = listener;
        this.processor = new UDPConnectionProcessor(this);
        this.readData = processor.getReadWindow();
        this.chunks = new ArrayList<ByteBuffer>(5);
        allocateNewChunk();
        try {
            configureBlocking(false);
        } catch(IOException iox) {
            throw new RuntimeException(iox);
        }
    }
    
    // for testing.
    UDPSocketChannel(UDPConnectionProcessor processor) {
        super(null);
        this.listener = null;
        this.processor = processor;
        this.readData = processor.getReadWindow();
        this.chunks = new ArrayList<ByteBuffer>(5);
        allocateNewChunk();
        try {
            configureBlocking(false);
        } catch(IOException iox) {
            throw new RuntimeException(iox);
        }
    }
    
    UDPConnectionProcessor getProcessor() {
        return processor;
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
        DataMessage msg = record.msg;
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
        activeChunk = NIODispatcher.instance().getBufferCache().getHeap(UDPConnectionProcessor.DATA_CHUNK_SIZE);
    }
    
    /**
     * Releases a chunk.
     */
    public void releaseChunk(ByteBuffer chunk) {
        NIODispatcher.instance().getBufferCache().release(chunk);
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
                rChunk = chunks.remove(0);
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

    /** Closes the processor. */
    protected void implCloseSelectableChannel() throws IOException {        
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
    
    public InetSocketAddress getRemoteSocketAddress() {
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

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw new IOException("unsupported");
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new IOException("unsupported");
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        // does nothing.
    }    
    
    void eventPending() {
    	if (listener != null)
    		listener.eventPending();
    }
}
