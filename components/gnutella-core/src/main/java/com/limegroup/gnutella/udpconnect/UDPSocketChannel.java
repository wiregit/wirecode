package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;

import com.limegroup.gnutella.io.BufferUtils;
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
 * 
 * TODO: extend SocketChannel instead of SelectableChannel.
 *       (should be done after writing & connecting are implemented)
 */
class UDPSocketChannel extends SelectableChannel implements InterestReadChannel, InterestWriteChannel {
    
    /** The SelectionKey associated with this channel. */
    private SelectionKey key;
    /** The processor this channel is writing to / reading from. */
    private final UDPConnectionProcessor processor;
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
    
    UDPSocketChannel(UDPConnectionProcessor processor, DataWindow window) {
        this.processor = processor;
        this.readData = window;
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
        if ( (priorSpace == 0 && read > 0)|| 
             (priorSpace <= UDPConnectionProcessor.SMALL_SEND_WINDOW && 
              readData.getWindowSpace() > UDPConnectionProcessor.SMALL_SEND_WINDOW) ) {
            processor.sendKeepAlive();
        }
        
        if(read == 0 && processor.isClosed())
            return -1;
        else
            return read;       
    }
    
    /**
     * Transfers the chunks in the DataRecord's msg to the ByteBuffer.
     * Sets the record as being succesfully read after all data is
     * read from it.
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
    
    public int write(ByteBuffer src) throws IOException {
        if(processor.isClosed())
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
     *  Allocates a chunk for writing to and reset written amount.
     */
    private void allocateNewChunk() {
        activeChunk = ByteBuffer.allocate(UDPConnectionProcessor.DATA_CHUNK_SIZE);
    }

    /**
     *  Package accessor for retrieving and freeing up chunks of data.
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
     *  Return how many pending chunks are waiting.
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
    public SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
        key = new UDPSelectionKey(processor, att);
        return key;
    }

    /** Returns OP_READ & OP_WRITE. */
    public int validOps() {
        return SelectionKey.OP_READ & SelectionKey.OP_WRITE;
    }

    /** Closes the processor. */
    protected void implCloseChannel() throws IOException {
        processor.close();
    }

    /// ********** InterestWriteChannel methods. ***************

    /**
     * Shuts down this channel & processor, notifying the last interested party
     * that we're now shutdown.
     */
    public void shutdown() {
        if(!isOpen())
            return;
        
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
}
