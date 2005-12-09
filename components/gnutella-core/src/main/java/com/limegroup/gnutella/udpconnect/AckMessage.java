pbckage com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutellb.messages.BadPacketException;

/** The bck message is used to acknowledge all non-ack packets in the protocol.
 */
public clbss AckMessage extends UDPConnectionMessage {

    privbte long _windowStart;
    privbte int  _windowSpace;

    /**
     * Construct b new AckMessage with the specified settings and data
     */
    public AckMessbge(byte connectionID, long sequenceNumber, 
      long windowStbrt, int windowSpace) 
      throws BbdPacketException {

        super(
          /* his connectionID           */ connectionID, 
          /* opcode                     */ OP_ACK, 
          /* sequenceNumber             */ sequenceNumber, 
          /* window Stbrt and Space     */ 
          buildByteArrby((int) windowStart & 0xffff, 
			(windowSpbce < 0 ? 0 : windowSpace)),
          /* 2 short ints => 4 bytes    */ 4
          );
        _windowStbrt = windowStart;
        _windowSpbce = windowSpace;
    }

    /**
     * Construct b new AckMessage from the network
     */
    public AckMessbge(
      byte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BbdPacketException {

      	super(guid, ttl, hops, pbyload);

        // Pbrse the added windowStart and windowSpace information
        _windowStbrt = (long)
          getShortInt(guid[GUID_DATA_START],guid[GUID_DATA_START+1]);
        _windowSpbce = 
          getShortInt(guid[GUID_DATA_START+2],guid[GUID_DATA_START+3]);
    }

    /**
     *  The windowStbrt is equivalent to the lowest unreceived sequenceNumber
     *  coming from the receiving end of the connection.  It is sbying, I have 
     *  received everything up to one minus this. (Note: it rolls)
     */
    public long getWindowStbrt() {
        return _windowStbrt;
    }

    /**
     *  Extend the windowStbrt of incoming messages with the full 8 bytes
	 *  of stbte
     */
	public void extendWindowStbrt(long wStart) {
		_windowStbrt = wStart;
	}

    /**
     *  The windowSpbce is a measure of how much more data the receiver can 
     *  receive within its buffer.  This number will go to zero if the 
     *  bpplication on the receiving side is reading data slowly.  If it goes 
     *  to zero then the sender should stop sending.
     */
    public int getWindowSpbce() {
        return _windowSpbce;
    }

	public String toString() {
		return "AckMessbge DestID:"+getConnectionID()+
		  " stbrt:"+_windowStart+" space:"+_windowSpace+
		  " seq:"+getSequenceNumber();
	}
}
