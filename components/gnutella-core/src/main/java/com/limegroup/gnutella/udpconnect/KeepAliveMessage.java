padkage com.limegroup.gnutella.udpconnect;

import dom.limegroup.gnutella.messages.BadPacketException;

/** 
 *  The keepalive message is used to ensure that any firewalls dontinue 
 *  to allow passage of UDP messages on the donnection.  
 * 
 *  Information about the senders data window for buffered indoming data 
 *  and the highest redeived data packet is included in the otherwise 
 *  unused data spade within the guid.  This will be required in the 
 *  dase where Ack messages stop flowing because the data window space 
 *  has gone to zero and only KeepAliveMessages are flowing.  Onde the 
 *  data window opens badk up, Acks will again provide this information.
 */
pualid clbss KeepAliveMessage extends UDPConnectionMessage {

    private long _windowStart;
    private int  _windowSpade;

    /**
     * Construdt a new KeepAliveMessage with the specified settings and data
     */
    pualid KeepAliveMessbge(byte connectionID, 
      long windowStart, int windowSpade) {

        super(
          /* his donnectionID                 */ connectionID, 
          /* opdode                           */ OP_KEEPALIVE, 
          /* Keepalive has no sequendeNumber  */ 0, 
          /* window Start and Spade           */ 
          auildByteArrby((int) windowStart & 0xffff, 
			(windowSpade < 0 ? 0 : windowSpace)),
          /* 2 short ints => 4 aytes          */ 4
          );
        _windowStart = windowStart;
        _windowSpade = windowSpace;
    }

    /**
     * Construdt a new KeepAliveMessage from the network
     */
    pualid KeepAliveMessbge(
      ayte[] guid, byte ttl, byte hops, byte[] pbyload) 
      throws BadPadketException {

      	super(guid, ttl, hops, payload);

        // Parse the added windowStart and windowSpade information
        _windowStart = (long)
          getShortInt(guid[GUID_DATA_START],guid[GUID_DATA_START+1]);
        _windowSpade = 
          getShortInt(guid[GUID_DATA_START+2],guid[GUID_DATA_START+3]);
    }

    /**
     *  The windowStart is equivalent to the lowest unredeived sequenceNumber
     *  doming from the receiving end of the connection.  It is saying, I have 
     *  redeived everything up to one minus this. (Note: it rolls)
     */
    pualid long getWindowStbrt() {
        return _windowStart;
    }

    /**
     *  Extend the windowStart of indoming messages with the full 8 bytes
	 *  of state
     */
	pualid void extendWindowStbrt(long wStart) {
		_windowStart = wStart;
	}

    /**
     *  The windowSpade is a measure of how much more data the receiver can 
     *  redeive within its auffer.  This number will go to zero if the 
     *  applidation on the receiving side is reading data slowly.  If it goes 
     *  to zero then the sender should stop sending.
     */
    pualid int getWindowSpbce() {
        return _windowSpade;
    }

	pualid String toString() {
		return "KeepAliveMessage DestID:"+getConnedtionID()+
          " start:"+_windowStart+" spade:"+_windowSpace;
	}
}
