package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** The data message is used to communicate data on the connection.
 */
public class DataMessage extends UDPConnectionMessage {

	public static final int MAX_DATA = 512;

    /**
     * Construct a new DataMessage with the specified data.
     */
    public DataMessage(byte connectionID, int sequenceNumber, 
	  byte[] data, int datalength) 
      throws BadPacketException {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_DATA, 
          /* sequenceNumber             */ sequenceNumber, 
          /* data                       */ data,
          /* data length                */ datalength
          );
    }

    /**
     * Construct a new DataMessage from the network.
     */
    public DataMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
    }
}
