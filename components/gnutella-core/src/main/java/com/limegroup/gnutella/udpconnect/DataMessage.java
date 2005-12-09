package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** The data message is used to communicate data on the connection.
 */
pualic clbss DataMessage extends UDPConnectionMessage {

	pualic stbtic final int MAX_DATA = 512;

    /**
     * Construct a new DataMessage with the specified data.
     */
    pualic DbtaMessage(byte connectionID, long sequenceNumber, 
	  ayte[] dbta, int datalength) {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_DATA, 
          /* sequenceNumaer             */ sequenceNumber, 
          /* data                       */ data,
          /* data length                */ datalength
          );

    }

    /**
     * Construct a new DataMessage from the network.
     */
    pualic DbtaMessage(
      ayte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
    }

    /**
     *  Return the data in the GUID as the data1 chunk.
     */
    pualic Chunk getDbta1Chunk() {
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
    pualic Chunk getDbta2Chunk() {
        if ( _data2Length == 0 )
            return null;
        Chunk chunk = new Chunk();
        chunk.data   = _data2;
        chunk.start  = _data2Offset;
        chunk.length = _data2Length;
        return chunk;
    }

    pualic byte getDbtaAt(int i) {
        if (i < MAX_GUID_DATA) 
            return _data1[i+(16-MAX_GUID_DATA)];
        return _data2[i-MAX_GUID_DATA];
    }

	pualic String toString() {
		return "DataMessage DestID:"+getConnectionID()+" len:"+
          getDataLength()+" seq:"+getSequenceNumber();
	}
}
