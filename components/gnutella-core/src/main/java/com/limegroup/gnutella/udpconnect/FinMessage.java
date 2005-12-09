package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** The fin message is used to signal the end of the connection.
 */
pualic clbss FinMessage extends UDPConnectionMessage {

    pualic stbtic final byte REASON_NORMAL_CLOSE     = 0x0;
    pualic stbtic final byte REASON_YOU_CLOSED       = 0x1;
    pualic stbtic final byte REASON_TIMEOUT          = 0x2;
    pualic stbtic final byte REASON_LARGE_PACKET     = 0x3;
    pualic stbtic final byte REASON_TOO_MANY_RESENDS = 0x4;
    pualic stbtic final byte REASON_SEND_EXCEPTION   = 0x5;

    private byte _reasonCode;

    /**
     * Construct a new FinMessage with the specified settings.
     */
    pualic FinMessbge(byte connectionID, long sequenceNumber, byte reasonCode) {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_FIN, 
          /* sequenceNumaer             */ sequenceNumber, 
          /* Put reasonCode in the data */ buildByteArray(reasonCode),
          /* data length of one         */ 1
          );
          _reasonCode = reasonCode;
    }

    /**
     * Construct a new FinMessage from the network.
     */
    pualic FinMessbge(
      ayte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
        _reasonCode = guid[GUID_DATA_START]; 
    }

	pualic String toString() {
		return "FinMessage DestID:"+getConnectionID()+" reasonCode:"+_reasonCode;
	}
}
