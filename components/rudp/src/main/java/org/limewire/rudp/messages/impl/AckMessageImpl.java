package org.limewire.rudp.messages.impl;

import org.limewire.rudp.messages.AckMessage;

import com.limegroup.gnutella.messages.BadPacketException;

/** The ack message is used to acknowledge all non-ack packets in the protocol.
 */
public class AckMessageImpl extends RUDPMessageImpl implements AckMessage {

    private long _windowStart;
    private int  _windowSpace;

    /**
     * Construct a new AckMessage with the specified settings and data
     */
    public AckMessageImpl(byte connectionID, long sequenceNumber, 
      long windowStart, int windowSpace) {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_ACK, 
          /* sequenceNumber             */ sequenceNumber, 
          /* window Start and Space     */ 
          buildByteArray((int) windowStart & 0xffff, 
			(windowSpace < 0 ? 0 : windowSpace)),
          /* 2 short ints => 4 bytes    */ 4
          );
        _windowStart = windowStart;
        _windowSpace = windowSpace;
    }

    /**
     * Construct a new AckMessage from the network
     */
    public AckMessageImpl(
      byte[] guid, byte ttl, byte hops, byte[] payload) 
      throws BadPacketException {

      	super(guid, ttl, hops, payload);

        // Parse the added windowStart and windowSpace information
        _windowStart = getShortInt(guid[GUID_DATA_START],guid[GUID_DATA_START+1]);
        _windowSpace = getShortInt(guid[GUID_DATA_START+2],guid[GUID_DATA_START+3]);
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.AckMessage#getWindowStart()
     */
    public long getWindowStart() {
        return _windowStart;
    }

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.AckMessage#extendWindowStart(long)
     */
	public void extendWindowStart(long wStart) {
		_windowStart = wStart;
	}

    /* (non-Javadoc)
     * @see org.limewire.rudp.messages.impl.AckMessage#getWindowSpace()
     */
    public int getWindowSpace() {
        return _windowSpace;
    }

	public String toString() {
		return "AckMessage DestID:"+getConnectionID()+
		  " start:"+_windowStart+" space:"+_windowSpace+
		  " seq:"+getSequenceNumber();
	}
}
