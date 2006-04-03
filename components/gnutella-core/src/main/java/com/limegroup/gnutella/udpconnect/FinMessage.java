
// Edited for the Learning branch

package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/**
 * 
 * 
 * The fin message is used to signal the end of the connection.
 * 
 * 
 */
public class FinMessage extends UDPConnectionMessage {

    /** 0x0, we closed for a normal reason (do), or haven't closed the UDP connection yet. */
    public static final byte REASON_NORMAL_CLOSE     = 0x0;

    /** 0x1 */
    public static final byte REASON_YOU_CLOSED       = 0x1;

    /** 0x2 */
    public static final byte REASON_TIMEOUT          = 0x2;

    /** 0x3 */
    public static final byte REASON_LARGE_PACKET     = 0x3;

    /** 0x4 */
    public static final byte REASON_TOO_MANY_RESENDS = 0x4;

    /** 0x5 */
    public static final byte REASON_SEND_EXCEPTION   = 0x5;

    private byte _reasonCode;

    /**
     * Construct a new FinMessage with the specified settings.
     */
    public FinMessage(byte connectionID, long sequenceNumber, byte reasonCode) {

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
    public FinMessage(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

      	super(guid, ttl, hops, payload);
        _reasonCode = guid[GUID_DATA_START];
    }

	public String toString() {

		return "FinMessage DestID:" + getConnectionID() + " reasonCode:" + _reasonCode;
	}
}
