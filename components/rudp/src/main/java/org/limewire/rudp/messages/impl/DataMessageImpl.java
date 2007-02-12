package org.limewire.rudp.messages.impl;

import java.nio.ByteBuffer;

import org.limewire.rudp.messages.DataMessage;

import com.limegroup.gnutella.messages.BadPacketException;

/** The data message is used to communicate data on the connection.
 */
public class DataMessageImpl extends RUDPMessageImpl implements DataMessage {

	private final ByteBuffer chunk;

    /**
     * Construct a new DataMessage with the specified data.
     */
    public DataMessageImpl(byte connectionID, long sequenceNumber, ByteBuffer chunk) {
        super(connectionID, OP_DATA, sequenceNumber, chunk.array(), chunk.remaining());
        this.chunk = chunk;
    }
    
    public DataMessageImpl(byte connectionID, long sequenceNumber, byte[] data, int len) {
        super(connectionID, OP_DATA, sequenceNumber, data, len);
        this.chunk = null;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.DataMessage#getChunk()
     */
    public ByteBuffer getChunk() {
        return chunk;
    }

    /**
     * Construct a new DataMessage from the network.
     */
    public DataMessageImpl(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, payload);
        this.chunk = null;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.DataMessage#getData1Chunk()
     */
    public ByteBuffer getData1Chunk() {
        return _data1;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.DataMessage#getData2Chunk()
     */
    public ByteBuffer getData2Chunk() {
        return _data2;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.DataMessage#getDataAt(int)
     */
    public byte getDataAt(int i) {
        if (i < MAX_GUID_DATA) 
            return _data1.get(i + _data1.position());
        else
            return _data2.get(i-MAX_GUID_DATA + _data2.position());
    }

	public String toString() {
		return "DataMessage DestID:"+getConnectionID()+" len:"+
          getDataLength()+" seq:"+getSequenceNumber();
	}
}
