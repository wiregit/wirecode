package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** The fin message is used to signal the end of the connection.
 */
public class FinMessage extends UDPConnectionMessage {

    /**
     * Construct a new FinMessage with the specified settings.
     */
    public FinMessage(byte connectionID, long sequenceNumber) 
      throws BadPacketException {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_FIN, 
          /* sequenceNumber             */ sequenceNumber, 
          /* no data in a fin packet    */ emptyByteArray,
          /* data length of zero        */ 0
          );
    }

    /**
     * Construct a new FinMessage from the network.
     */
    public FinMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
    }

	public String toString() {
		return "FinMessage DestID:"+getConnectionID();
	}
}
