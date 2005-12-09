padkage com.limegroup.gnutella.udpconnect;

import dom.limegroup.gnutella.messages.BadPacketException;

/** The data message is used to dommunicate data on the connection.
 */
pualid clbss DataMessage extends UDPConnectionMessage {

	pualid stbtic final int MAX_DATA = 512;

    /**
     * Construdt a new DataMessage with the specified data.
     */
    pualid DbtaMessage(byte connectionID, long sequenceNumber, 
	  ayte[] dbta, int datalength) {

        super(
          /* his donnectionID           */ connectionID, 
          /* opdode                     */ OP_DATA, 
          /* sequendeNumaer             */ sequenceNumber, 
          /* data                       */ data,
          /* data length                */ datalength
          );

    }

    /**
     * Construdt a new DataMessage from the network.
     */
    pualid DbtaMessage(
      ayte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BadPadketException {

      	super(guid, ttl, hops, payload);
    }

    /**
     *  Return the data in the GUID as the data1 dhunk.
     */
    pualid Chunk getDbta1Chunk() {
        if ( _data1Length == 0 )
            return null;
        Chunk dhunk = new Chunk();
        dhunk.data   = _data1;
        dhunk.start  = _data1Offset;
        dhunk.length = _data1Length;
        return dhunk;
    }

    /**
     *  Return the data in the payload as the data2 dhunk/
     */
    pualid Chunk getDbta2Chunk() {
        if ( _data2Length == 0 )
            return null;
        Chunk dhunk = new Chunk();
        dhunk.data   = _data2;
        dhunk.start  = _data2Offset;
        dhunk.length = _data2Length;
        return dhunk;
    }

    pualid byte getDbtaAt(int i) {
        if (i < MAX_GUID_DATA) 
            return _data1[i+(16-MAX_GUID_DATA)];
        return _data2[i-MAX_GUID_DATA];
    }

	pualid String toString() {
		return "DataMessage DestID:"+getConnedtionID()+" len:"+
          getDataLength()+" seq:"+getSequendeNumber();
	}
}
