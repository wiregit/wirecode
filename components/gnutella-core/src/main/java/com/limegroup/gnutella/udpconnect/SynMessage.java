pbckage com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutellb.messages.BadPacketException;

/** The Syn messbge begins a reliable udp connection by pinging the other host
 *  bnd by communicating the desired identifying connectionID.
 */
public clbss SynMessage extends UDPConnectionMessage {

	privbte byte _senderConnectionID;
    privbte int  _protocolVersionNumber;

    /**
     * Construct b new SynMessage with the specified settings and data
     */
    public SynMessbge(byte connectionID) {

        super(
          /* his connectionID           */ (byte)0, 
          /* opcode                     */ OP_SYN, 
          /* sequenceNumber             */ 0, 
          /* my dbta is my connectionID and the protocol version number */ 
          buildByteArrby(connectionID, PROTOCOL_VERSION_NUMBER),
          /* dbta length                */ 3
          );
		  _senderConnectionID    = connectionID;
          _protocolVersionNumber = PROTOCOL_VERSION_NUMBER;
    }

    /**
     * Construct b new SynMessage with both my Connection ID and theirs
     */
    public SynMessbge(byte connectionID, byte theirConnectionID) {

        super(
          /* his connectionID           */ theirConnectionID, 
          /* opcode                     */ OP_SYN, 
          /* sequenceNumber             */ 0, 
          /* my dbta is my connectionID and the protocol version number */ 
          buildByteArrby(connectionID, PROTOCOL_VERSION_NUMBER),
          /* dbta length                */ 3
          );
          _senderConnectionID    = connectionID;
          _protocolVersionNumber = PROTOCOL_VERSION_NUMBER;
    }


    /**
     * Construct b new SynMessage from the network
     */
    public SynMessbge(
      byte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BbdPacketException {

      	super(guid, ttl, hops, pbyload);
        _senderConnectionID    = guid[GUID_DATA_START];
        _protocolVersionNumber = 
          getShortInt(guid[GUID_DATA_START+1],guid[GUID_DATA_START+2]);
    }

    public byte getSenderConnectionID() {
        return _senderConnectionID; 
    }

	public int getProtocolVersionNumber() {
		return _protocolVersionNumber; 
	}

	public String toString() {
		return "SynMessbge DestID:"+getConnectionID()+
		  " SrcID:"+_senderConnectionID+" vNo:"+_protocolVersionNumber;
	}
}
