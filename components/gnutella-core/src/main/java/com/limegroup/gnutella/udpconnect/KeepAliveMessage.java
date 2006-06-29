package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/** 
 *  The keepalive message is used to ensure that any firewalls continue 
 *  to allow passage of UDP messages on the connection.  
 * 
 *  Information about the senders data window for buffered incoming data 
 *  and the highest received data packet is included in the otherwise 
 *  unused data space within the guid.  This will be required in the 
 *  case where Ack messages stop flowing because the data window space 
 *  has gone to zero and only KeepAliveMessages are flowing.  Once the 
 *  data window opens back up, Acks will again provide this information.
 */
public class KeepAliveMessage extends UDPConnectionMessage {

    private long _windowStart;
    private int  _windowSpace;

    /**
     * Construct a new KeepAliveMessage with the specified settings and data
     */
    public KeepAliveMessage(byte connectionID, 
      long windowStart, int windowSpace) {

        super(
          /* his connectionID                 */ connectionID, 
          /* opcode                           */ OP_KEEPALIVE, 
          /* Keepalive has no sequenceNumber  */ 0, 
          /* window Start and Space           */ 
          buildByteArray((int) windowStart & 0xffff, 
			(windowSpace < 0 ? 0 : windowSpace)),
          /* 2 short ints => 4 bytes          */ 4
          );
        _windowStart = windowStart;
        _windowSpace = windowSpace;
    }

    /**
     * Construct a new KeepAliveMessage from the network
     */
    public KeepAliveMessage(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);

        // Parse the added windowStart and windowSpace information
        _windowStart = getShortInt(guid[GUID_DATA_START],guid[GUID_DATA_START+1]);
        _windowSpace = getShortInt(guid[GUID_DATA_START+2],guid[GUID_DATA_START+3]);
    }

    /**
     *  The windowStart is equivalent to the lowest unreceived sequenceNumber
     *  coming from the receiving end of the connection.  It is saying, I have 
     *  received everything up to one minus this. (Note: it rolls)
     */
    public long getWindowStart() {
        return _windowStart;
    }

    /**
     *  Extend the windowStart of incoming messages with the full 8 bytes
	 *  of state
     */
	public void extendWindowStart(long wStart) {
		_windowStart = wStart;
	}

    /**
     *  The windowSpace is a measure of how much more data the receiver can 
     *  receive within its buffer.  This number will go to zero if the 
     *  application on the receiving side is reading data slowly.  If it goes 
     *  to zero then the sender should stop sending.
     */
    public int getWindowSpace() {
        return _windowSpace;
    }

	public String toString() {
		return "KeepAliveMessage DestID:"+getConnectionID()+
          " start:"+_windowStart+" space:"+_windowSpace;
	}
}
