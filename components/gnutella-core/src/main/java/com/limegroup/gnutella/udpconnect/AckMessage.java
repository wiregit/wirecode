
// Edited for the Learning branch

package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/**
 * Acknowledge other types of UDP connection messages with an Ack message.
 * 
 * 
 */
public class AckMessage extends UDPConnectionMessage {

    /** */
    private long _windowStart;

    /** */
    private int  _windowSpace;

    /**
     * Make a new Ack message for us to send.
     * This is the message maker.
     * 
     * @param connectionID   The connection ID the remote computer chose for this UDP connection
     * @param sequenceNumber The sequence number of this packet
     * @param windowStart    The lowest-numbered message we still need
     * @param windowSpace    The number of bytes we can receive right now, 0 if we're full
     */
    public AckMessage(byte connectionID, long sequenceNumber, long windowStart, int windowSpace) throws BadPacketException {

        // Call the UDPConnectionMessage constructor to make the message
        super(
            connectionID,   // The connection ID the remote computer chose
            OP_ACK,         // 0x1, this is an Ack message
            sequenceNumber, // The sequence number

            // 4 bytes of data, the lowest-numbered message we still need in the first 2 bytes, and the number of bytes of space we have in the 2 bytes after that
            buildByteArray((int)windowStart & 0xffff, (windowSpace < 0 ? 0 : windowSpace)), 4); // Payload length 4, fits in the GUID area

        // Save the given information in this object also
        _windowStart = windowStart;
        _windowSpace = windowSpace;
    }

    /**
     * Parse data we received into a new AckMessage object.
     * This is the message parser.
     * 
     * @param guid    The first 16 bytes of the Gnutella message header, where the GUID should be
     * @param ttl     The ttl we read
     * @param hops    The hops we read
     * @param payload The payload data after the 23-byte Gnutella message header
     */
    public AckMessage(byte[] guid, byte ttl, byte hops, byte[] payload) throws BadPacketException {

        // Call the UDPConnectionMessage and Message constructors to parse and save the information common to all UDP connection packets
      	super(guid, ttl, hops, payload);

        /*
         * An Ack message carries 4 bytes of data, like this:
         * 
         * aabb
         * 
         * a is 2 bytes with the window start, the packet number the sending computer needs next.
         * b is 2 bytes with the window space, the number of bytes of space the sending computer can receive right now, or 0 if it's full.
         * 
         * The 4 bytes of data fit into the area where the GUID should be.
         */

        // Parse the window start and space numbers in the data the Ack message carries
        _windowStart = (long)getShortInt(guid[GUID_DATA_START], guid[GUID_DATA_START + 1]); // The data starts 4 bytes from the start
        _windowSpace = getShortInt(guid[GUID_DATA_START + 2], guid[GUID_DATA_START + 3]);
    }

    /**
     * Get the lowest sequence number of the packets the computer that sent this Ack message still needs.
     * For instance, if a computer has received packets 1 2 4 5 6 7 8 9, it will send an Ack with a window start of 3.
     * 
     * 
     * The windowStart is equivalent to the lowest unreceived sequenceNumber
     * coming from the receiving end of the connection.  It is saying, I have 
     * received everything up to one minus this. (Note: it rolls)
     */
    public long getWindowStart() {
        
        // lowest numbrered windows packet
        
        return _windowStart;
    }

    /**
     * 
     * 
     * Extend the windowStart of incoming messages with the full 8 bytes
	 * of state
     */
	public void extendWindowStart(long wStart) {
        
		_windowStart = wStart;
	}

    /**
     * The windowSpace is a measure of how much more data the receiver can 
     * receive within its buffer.  This number will go to zero if the 
     * application on the receiving side is reading data slowly.  If it goes 
     * to zero then the sender should stop sending.
     */
    public int getWindowSpace() {
        
        return _windowSpace;
    }

    /**
     * 
     */
	public String toString() {

		return "AckMessage DestID:" + getConnectionID() + " start:" + _windowStart + " space:" + _windowSpace + " seq:" + getSequenceNumber();
	}
}
