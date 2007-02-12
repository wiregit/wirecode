package org.limewire.rudp.messages.impl;

import org.limewire.rudp.messages.KeepAliveMessage;

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
public class KeepAliveMessageImpl extends RUDPMessageImpl implements KeepAliveMessage {

    private long _windowStart;
    private int  _windowSpace;

    /**
     * Construct a new KeepAliveMessage with the specified settings and data
     */
    public KeepAliveMessageImpl(byte connectionID, 
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
    public KeepAliveMessageImpl(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);

        // Parse the added windowStart and windowSpace information
        _windowStart = getShortInt(guid[GUID_DATA_START],guid[GUID_DATA_START+1]);
        _windowSpace = getShortInt(guid[GUID_DATA_START+2],guid[GUID_DATA_START+3]);
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.KeepAliveMessage#getWindowStart()
     */
    public long getWindowStart() {
        return _windowStart;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.KeepAliveMessage#extendWindowStart(long)
     */
	public void extendWindowStart(long wStart) {
		_windowStart = wStart;
	}

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.KeepAliveMessage#getWindowSpace()
     */
    public int getWindowSpace() {
        return _windowSpace;
    }

	public String toString() {
		return "KeepAliveMessage DestID:"+getConnectionID()+
          " start:"+_windowStart+" space:"+_windowSpace;
	}
}
