pbckage com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutellb.messages.BadPacketException;

/** 
 *  The keepblive message is used to ensure that any firewalls continue 
 *  to bllow passage of UDP messages on the connection.  
 * 
 *  Informbtion about the senders data window for buffered incoming data 
 *  bnd the highest received data packet is included in the otherwise 
 *  unused dbta space within the guid.  This will be required in the 
 *  cbse where Ack messages stop flowing because the data window space 
 *  hbs gone to zero and only KeepAliveMessages are flowing.  Once the 
 *  dbta window opens back up, Acks will again provide this information.
 */
public clbss KeepAliveMessage extends UDPConnectionMessage {

    privbte long _windowStart;
    privbte int  _windowSpace;

    /**
     * Construct b new KeepAliveMessage with the specified settings and data
     */
    public KeepAliveMessbge(byte connectionID, 
      long windowStbrt, int windowSpace) {

        super(
          /* his connectionID                 */ connectionID, 
          /* opcode                           */ OP_KEEPALIVE, 
          /* Keepblive has no sequenceNumber  */ 0, 
          /* window Stbrt and Space           */ 
          buildByteArrby((int) windowStart & 0xffff, 
			(windowSpbce < 0 ? 0 : windowSpace)),
          /* 2 short ints => 4 bytes          */ 4
          );
        _windowStbrt = windowStart;
        _windowSpbce = windowSpace;
    }

    /**
     * Construct b new KeepAliveMessage from the network
     */
    public KeepAliveMessbge(
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
		return "KeepAliveMessbge DestID:"+getConnectionID()+
          " stbrt:"+_windowStart+" space:"+_windowSpace;
	}
}
