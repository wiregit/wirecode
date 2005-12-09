pbckage com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutellb.messages.BadPacketException;

/** The fin messbge is used to signal the end of the connection.
 */
public clbss FinMessage extends UDPConnectionMessage {

    public stbtic final byte REASON_NORMAL_CLOSE     = 0x0;
    public stbtic final byte REASON_YOU_CLOSED       = 0x1;
    public stbtic final byte REASON_TIMEOUT          = 0x2;
    public stbtic final byte REASON_LARGE_PACKET     = 0x3;
    public stbtic final byte REASON_TOO_MANY_RESENDS = 0x4;
    public stbtic final byte REASON_SEND_EXCEPTION   = 0x5;

    privbte byte _reasonCode;

    /**
     * Construct b new FinMessage with the specified settings.
     */
    public FinMessbge(byte connectionID, long sequenceNumber, byte reasonCode) {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_FIN, 
          /* sequenceNumber             */ sequenceNumber, 
          /* Put rebsonCode in the data */ buildByteArray(reasonCode),
          /* dbta length of one         */ 1
          );
          _rebsonCode = reasonCode;
    }

    /**
     * Construct b new FinMessage from the network.
     */
    public FinMessbge(
      byte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BbdPacketException {

      	super(guid, ttl, hops, pbyload);
        _rebsonCode = guid[GUID_DATA_START]; 
    }

	public String toString() {
		return "FinMessbge DestID:"+getConnectionID()+" reasonCode:"+_reasonCode;
	}
}
