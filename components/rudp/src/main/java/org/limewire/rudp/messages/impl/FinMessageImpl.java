package org.limewire.rudp.messages.impl;

import org.limewire.rudp.messages.FinMessage;

import com.limegroup.gnutella.messages.BadPacketException;

/** The fin message is used to signal the end of the connection.
 */
public class FinMessageImpl extends RUDPMessageImpl implements FinMessage {

    private byte _reasonCode;

    /**
     * Construct a new FinMessage with the specified settings.
     */
    public FinMessageImpl(byte connectionID, long sequenceNumber, byte reasonCode) {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_FIN, 
          /* sequenceNumber             */ sequenceNumber, 
          /* Put reasonCode in the data */ buildByteArray(reasonCode),
          /* data length of one         */ 1
          );
          _reasonCode = reasonCode;
    }

    /**
     * Construct a new FinMessage from the network.
     */
    public FinMessageImpl(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
        _reasonCode = guid[GUID_DATA_START]; 
    }

	public String toString() {
		return "FinMessage DestID:"+getConnectionID()+" reasonCode:"+_reasonCode;
	}
}
