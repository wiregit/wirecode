package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** The data message is used to communicate data on the connection.
 */
public class DataMessage extends UDPConnectionMessage {

	public static final int MAX_DATA = 512;

    /**
     * Construct a new DataMessage with the specified data.
     */
    public DataMessage(byte connectionID, long sequenceNumber, 
	  byte[] data, int datalength) {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_DATA, 
          /* sequenceNumber             */ sequenceNumber, 
          /* data                       */ data,
          /* data length                */ datalength
          );

    }

    /**
     * Construct a new DataMessage from the network.
     */
    public DataMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
    }

    /**
     *  Return the data in the GUID as the data1 chunk.
     */
    public Chunk getData1Chunk() {
        if ( _data1Length == 0 )
            return null;
        Chunk chunk = new Chunk();
        chunk.data   = _data1;
        chunk.start  = _data1Offset;
        chunk.length = _data1Length;
        return chunk;
    }

    /**
     *  Return the data in the payload as the data2 chunk/
     */
    public Chunk getData2Chunk() {
        if ( _data2Length == 0 )
            return null;
        Chunk chunk = new Chunk();
        chunk.data   = _data2;
        chunk.start  = _data2Offset;
        chunk.length = _data2Length;
        return chunk;
    }

	public String toString() {
		return "DataMessage DestID:"+getConnectionID()+" len:"+
          getDataLength()+" seq:"+getSequenceNumber();
	}
}
