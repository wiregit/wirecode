package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;

import com.limegroup.gnutella.io.BufferUtils;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.NIODispatcher;

/**
 * Interface between reading channels & UDP's data.  Analagous to SocketInterestReadAdapter,
 * but uses a DataWindow instead of a Socket to retrieve the data.
 */
class UDPSocketChannel extends SelectableChannel implements InterestReadChannel {
    
    private UDPSelectionKey key;
    private UDPMultiplexor multiplexor;
    private final UDPConnectionProcessor processor;
    private final DataWindow data;
    
    UDPSocketChannel(UDPMultiplexor multiplexor, UDPConnectionProcessor processor, DataWindow window) {
        this.processor = processor;
        this.data = window;
        this.multiplexor = multiplexor;
    }

    /**
     * Sets interest on or off in the processor.
     */
    public void interest(boolean status) {
        NIODispatcher.instance().interestRead(this, status);
    }

    /**
     * Reads all possible data from the DataWindow into the ByteBuffer,
     * sending a keep alive if more space became available.
     */
    public int read(ByteBuffer to) throws IOException {
        int read = 0;
        DataRecord currentRecord = data.getReadableBlock();
        while (currentRecord != null) {
            read += transfer(currentRecord, to);
            if (!to.hasRemaining())
                break;

            // If to still has room left, we must have written
            // all we could from the record, so we assign a new one.
            // Fetch a block from the receiving window.
            currentRecord = data.getReadableBlock();
        }
        
        // Now that we've transferred all we can to the buffer, clear up
        // the space & send a keep-alive if necessary
        // Record how much space was previously available in the receive window
        int priorSpace = data.getWindowSpace();

        // Remove all records we just read from the receiving window
        data.clearEarlyReadBlocks();   

        // If the receive window opened up then send a special 
        // KeepAliveMessage so that the window state can be 
        // communicated.
        if ( (priorSpace == 0 && read > 0)|| 
             (priorSpace <= UDPConnectionProcessor.SMALL_SEND_WINDOW && 
              data.getWindowSpace() > UDPConnectionProcessor.SMALL_SEND_WINDOW) ) {
            processor.sendKeepAlive();
        }
        
        if(read == 0 && !isOpen())
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

    public Object blockingLock() {
        throw new UnsupportedOperationException();
    }

    public SelectableChannel configureBlocking(boolean block) throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean isBlocking() {
        return false;
    }

    public boolean isRegistered() {
        return key != null;
    }

    public SelectionKey keyFor(Selector sel) {
        return key;
    }

    public SelectorProvider provider() {
        throw new UnsupportedOperationException();
    }

    public SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
        key = new UDPSelectionKey(processor);
        key.attach(att);
        processor.setConnectionId(multiplexor.register(processor, key));
        return key;
    }

    public int validOps() {
        return SelectionKey.OP_READ;
    }

    protected void implCloseChannel() throws IOException {
        processor.close();
    }
}
