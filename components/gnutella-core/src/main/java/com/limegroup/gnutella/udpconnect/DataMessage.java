
// Edited for the Learning branch

package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/**
 * 
 * The data message is used to communicate data on the connection.
 */
public class DataMessage extends UDPConnectionMessage {

    //done

    /**
     * 512, don't put more than 512 bytes of data in a UDP packet to keep the packets we send around this size.
     * 
     * UDP packets can be any size.
     * But, if a UDP packet is too large for a router on the Internet, the router will cut it into smaller packets.
     * The receiving computer will get all the packet parts, and put them back together.
     * If any one of the packet parts is lost, the whole original packet is lost.
     * 
     * 512 bytes is small enough that it's unlikely any Internet router will cut the packet into smaller sizes.
     * By using a small packet size, we avoid this problem and keep things reliable.
     * 
     * 512 bytes is quite small, and even safe for modem connections.
     * We could probably safely change this to 1024 bytes, 1 KB.
     */
	public static final int MAX_DATA = 512;

    /**
     * Make a new DataMessage object to hold the given data in a UDP packet.
     * 
     * @param connectionID
     * @param sequenceNumber
     * @param data
     * @param datalength
     */
    public DataMessage(byte connectionID, long sequenceNumber, byte[] data, int datalength) {

        /*
         * his connectionID  connectionID   
         * opcode            OP_DATA 
         * sequenceNumber    sequenceNumber 
         * data              data
         * data length       datalength
         */

        super(
          connectionID,   
          OP_DATA, 
          sequenceNumber, 
          data,
          datalength);
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

    public byte getDataAt(int i) {
        if (i < MAX_GUID_DATA) 
            return _data1[i+(16-MAX_GUID_DATA)];
        return _data2[i-MAX_GUID_DATA];
    }

	public String toString() {
		return "DataMessage DestID:"+getConnectionID()+" len:"+
          getDataLength()+" seq:"+getSequenceNumber();
	}
}
