package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** The Syn message begins a reliable udp connection by pinging the other host
 *  and by communicating the desired identifying connectionID.
 */
public class SynMessage extends UDPConnectionMessage {

	private byte _senderConnectionID;

    /**
     * Construct a new SynMessage with the specified settings and data
     */
    public SynMessage(byte connectionID) 
      throws BadPacketException {

        super(
          /* his connectionID           */ (byte)0, 
          /* opcode                     */ OP_SYN, 
          /* sequenceNumber             */ (byte)0, 
          /* my data is my connectionID */ buildByteArray(connectionID)
          );
    }

    /**
     * Construct a new SynMessage from the network
     */
    public SynMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
        _senderConnectionID = guid[GUID_DATA_START];
    }

	public byte getSenderConnectionID() {
		return _senderConnectionID; 
	}
}
