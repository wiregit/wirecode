package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** The ack message is used to acknowledge all non-ack packets in the protocol.
 */
public class AckMessage extends UDPConnectionMessage {

    /**
     * Construct a new AckMessage with the specified settings and data
     */
    public AckMessage(byte connectionID, int sequenceNumber) 
      throws BadPacketException {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_ACK, 
          /* sequenceNumber             */ sequenceNumber, 
          /* no data in an ack          */ emptyByteArray
          );
    }

    /**
     * Construct a new AckMessage from the network
     */
    public AckMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
    }
}
