package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** The Syn message begins a reliable udp connection by pinging the other host
 *  and by communicating the desired identifying connectionID.
 */
public class SynMessage extends UDPConnectionMessage {

	private byte _senderConnectionID;
    private int  _protocolVersionNumber;

    /**
     * Construct a new SynMessage with the specified settings and data
     */
    public SynMessage(byte connectionID) {

        super(
          /* his connectionID           */ (byte)0, 
          /* opcode                     */ OP_SYN, 
          /* sequenceNumber             */ 0, 
          /* my data is my connectionID and the protocol version number */ 
          buildByteArray(connectionID, PROTOCOL_VERSION_NUMBER),
          /* data length                */ 3
          );
		  _senderConnectionID    = connectionID;
          _protocolVersionNumber = PROTOCOL_VERSION_NUMBER;
    }

    /**
     * Construct a new SynMessage with both my Connection ID and theirs
     */
    public SynMessage(byte connectionID, byte theirConnectionID) {

        super(
          /* his connectionID           */ theirConnectionID, 
          /* opcode                     */ OP_SYN, 
          /* sequenceNumber             */ 0, 
          /* my data is my connectionID and the protocol version number */ 
          buildByteArray(connectionID, PROTOCOL_VERSION_NUMBER),
          /* data length                */ 3
          );
          _senderConnectionID    = connectionID;
          _protocolVersionNumber = PROTOCOL_VERSION_NUMBER;
    }


    /**
     * Construct a new SynMessage from the network
     */
    public SynMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
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
		return "SynMessage DestID:"+getConnectionID()+
		  " SrcID:"+_senderConnectionID+" vNo:"+_protocolVersionNumber;
	}
}
