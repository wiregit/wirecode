package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** 
 *  The keepalive message is used to ensure that any firewalls continue 
 *  to allow passage of UDP messages on the connection.
 */
public class KeepAliveMessage extends UDPConnectionMessage {

    /**
     * Construct a new KeepAliveMessage with the specified settings.
     */
    public KeepAliveMessage(byte connectionID) 
      throws BadPacketException {

        super(
          /* his connectionID                 */ connectionID, 
          /* opcode                           */ OP_KEEPALIVE, 
          /* Keepalive have no sequenceNumber */ (byte)0, 
          /* no data in a keepalive packet    */ emptyByteArray
          );
    }

    /**
     * Construct a new KeepAliveMessage from the network.
     */
    public KeepAliveMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);
    }
}
