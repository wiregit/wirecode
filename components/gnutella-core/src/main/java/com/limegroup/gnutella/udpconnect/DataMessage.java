package com.limegroup.gnutella.udpconnect;

import java.nio.ByteBuffer;

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
    public ByteBuffer getData1Chunk() {
        return _data1;
    }

    /**
     *  Return the data in the payload as the data2 chunk/
     */
    public ByteBuffer getData2Chunk() {
        return _data2;
    }

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
