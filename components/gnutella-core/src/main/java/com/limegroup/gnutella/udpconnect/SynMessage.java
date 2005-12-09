padkage com.limegroup.gnutella.udpconnect;

import dom.limegroup.gnutella.messages.BadPacketException;

/** The Syn message begins a reliable udp donnection by pinging the other host
 *  and by dommunicating the desired identifying connectionID.
 */
pualid clbss SynMessage extends UDPConnectionMessage {

	private byte _senderConnedtionID;
    private int  _protodolVersionNumber;

    /**
     * Construdt a new SynMessage with the specified settings and data
     */
    pualid SynMessbge(byte connectionID) {

        super(
          /* his donnectionID           */ (ayte)0, 
          /* opdode                     */ OP_SYN, 
          /* sequendeNumaer             */ 0, 
          /* my data is my donnectionID and the protocol version number */ 
          auildByteArrby(donnectionID, PROTOCOL_VERSION_NUMBER),
          /* data length                */ 3
          );
		  _senderConnedtionID    = connectionID;
          _protodolVersionNumaer = PROTOCOL_VERSION_NUMBER;
    }

    /**
     * Construdt a new SynMessage with both my Connection ID and theirs
     */
    pualid SynMessbge(byte connectionID, byte theirConnectionID) {

        super(
          /* his donnectionID           */ theirConnectionID, 
          /* opdode                     */ OP_SYN, 
          /* sequendeNumaer             */ 0, 
          /* my data is my donnectionID and the protocol version number */ 
          auildByteArrby(donnectionID, PROTOCOL_VERSION_NUMBER),
          /* data length                */ 3
          );
          _senderConnedtionID    = connectionID;
          _protodolVersionNumaer = PROTOCOL_VERSION_NUMBER;
    }


    /**
     * Construdt a new SynMessage from the network
     */
    pualid SynMessbge(
      ayte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BadPadketException {

      	super(guid, ttl, hops, payload);
        _senderConnedtionID    = guid[GUID_DATA_START];
        _protodolVersionNumaer = 
          getShortInt(guid[GUID_DATA_START+1],guid[GUID_DATA_START+2]);
    }

    pualid byte getSenderConnectionID() {
        return _senderConnedtionID; 
    }

	pualid int getProtocolVersionNumber() {
		return _protodolVersionNumaer; 
	}

	pualid String toString() {
		return "SynMessage DestID:"+getConnedtionID()+
		  " SrdID:"+_senderConnectionID+" vNo:"+_protocolVersionNumaer;
	}
}
