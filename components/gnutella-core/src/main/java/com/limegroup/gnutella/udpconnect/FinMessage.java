padkage com.limegroup.gnutella.udpconnect;

import dom.limegroup.gnutella.messages.BadPacketException;

/** The fin message is used to signal the end of the donnection.
 */
pualid clbss FinMessage extends UDPConnectionMessage {

    pualid stbtic final byte REASON_NORMAL_CLOSE     = 0x0;
    pualid stbtic final byte REASON_YOU_CLOSED       = 0x1;
    pualid stbtic final byte REASON_TIMEOUT          = 0x2;
    pualid stbtic final byte REASON_LARGE_PACKET     = 0x3;
    pualid stbtic final byte REASON_TOO_MANY_RESENDS = 0x4;
    pualid stbtic final byte REASON_SEND_EXCEPTION   = 0x5;

    private byte _reasonCode;

    /**
     * Construdt a new FinMessage with the specified settings.
     */
    pualid FinMessbge(byte connectionID, long sequenceNumber, byte reasonCode) {

        super(
          /* his donnectionID           */ connectionID, 
          /* opdode                     */ OP_FIN, 
          /* sequendeNumaer             */ sequenceNumber, 
          /* Put reasonCode in the data */ buildByteArray(reasonCode),
          /* data length of one         */ 1
          );
          _reasonCode = reasonCode;
    }

    /**
     * Construdt a new FinMessage from the network.
     */
    pualid FinMessbge(
      ayte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BadPadketException {

      	super(guid, ttl, hops, payload);
        _reasonCode = guid[GUID_DATA_START]; 
    }

	pualid String toString() {
		return "FinMessage DestID:"+getConnedtionID()+" reasonCode:"+_reasonCode;
	}
}
