
// Commented for the Learning branch

package com.limegroup.gnutella.udpconnect;

import com.limegroup.gnutella.messages.BadPacketException;

/**
 * A computer sends an Ack message to acknowledge it's received a Syn message or a Data message.
 * 
 * A 23-byte Ack message has the following structure:
 * 
 * aa bc dd dd ee ee ff ff
 * gg gg gg gg gg gg gg gg
 * hh ii jj kk kk kk kk
 * 
 * a is the connection ID the remote computer assigned this connection.
 * b and c are 4-bit nibbles that fit together into a single byte.
 * b is the operation code, 1 identifies this as an Ack message.
 * c is the length of data in the GUID area, an Ack has 4 bytes there.
 * d is the sequence number, an Ack's sequence number is the same as the message it's acknowledging having received.
 * 
 * e through g are the 12 bytes in the GUID area we can start to put data, our 4 bytes of data fit into this area.
 * e through f are the 4 bytes of data specific to an Ack message.
 * e is the window start, the sequence number of the lowest numbered message the sending computer is missing.
 * f is the window space, the number of 512-byte messages the sending computer has space to receive right now.
 * g is the extra area in the GUID we don't need.
 * 
 * h is the Gnutella packet type, 0x41 identifies this as a UDP connection message.
 * i and j are the TTL and hops, 1 and 0, and not really useful or used.
 * k is the length of the payload beyond this 23-byte header, 0, there is no payload because all our data fits in the 12-byte GUID area e through g.
 */
public class AckMessage extends UDPConnectionMessage {

    /** The lowest numbered message the acknowledging computer is missing. */
    private long _windowStart;

    /** The number of messages the acknowledging computer has space to receive right now, 0 if its full. */
    private int _windowSpace;

    /**
     * Make a new Ack message for us to send.
     * This is the message maker.
     * 
     * Only UDPConnectionProcessor.safeSendAck() calls this.
     * 
     * @param connectionID   The connection ID the remote computer chose to identify this connection and the packets we send that are a part of it
     * @param sequenceNumber The sequence number of the message we're acknowledging we've received
     * @param windowStart    The lowest-numbered message we're missing
     * @param windowSpace    The number of messages we can receive right now, 0 if we're full
     */
    public AckMessage(byte connectionID, long sequenceNumber, long windowStart, int windowSpace) throws BadPacketException {

        // Call the UDPConnectionMessage constructor to make the message
        super(
            connectionID,   // The connection ID the remote computer chose for this connection
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
     * Only UDPConnectionMessage.createMessage() calls this.
     * 
     * @param guid    The first 16 bytes of the Gnutella message header, where the GUID would be
     * @param ttl     The ttl we read, we don't use it
     * @param hops    The hops we read, we don't use it
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
         * a is 2 bytes with the window start, the message number the remote computer needs next.
         * b is 2 bytes with the window space, the number of messages the remote computer can receive right now, or 0 if it's full.
         * 
         * The 4 bytes of data fit into the area where the GUID should be.
         */

        // Parse the window start and space numbers in the data the Ack message carries
        _windowStart = (long)getShortInt(guid[GUID_DATA_START], guid[GUID_DATA_START + 1]); // The data starts 4 bytes from the start
        _windowSpace = getShortInt(guid[GUID_DATA_START + 2], guid[GUID_DATA_START + 3]);
    }

    /**
     * Get the number of the earliest message the computer that sent this Ack message still needs.
     * For instance, if a computer has received packets 1 2 4 5 6 7 8 9, it will send an Ack with a window start of 3.
     * 
     * The windowStart is equivalent to the lowest unreceived sequenceNumber
     * coming from the receiving end of the connection.  It is saying, I have
     * received everything up to this. (Note: it rolls)
     */
    public long getWindowStart() {

        // Return the value we parsed or saved
        return _windowStart;
    }

    /**
     * Save the window start number we extended.
     * Saves the given value in _windowStart.
     * 
     * In the packet, the window start number is 2 bytes that go from 0x0000 to 0xffff, and then wrap around.
     * Our SequenceNumberExtender for this connection watches the numbers grow to the end, and handles it.
     * This lets it extend 2-byte truncated numbers into the full 8 bytes they were before the sending computer truncated them.
     * 
     * @param wStart The extended window start value
     */
	public void extendWindowStart(long wStart) {

        // Save the given value
		_windowStart = wStart;
	}

    /**
     * Find out how many more 512-byte messages the computer that made this Ack can take right now.
     * 
     * The windowSpace is a measure of how much more data the receiver can
     * receive within its buffer.  This number will go to zero if the
     * application on the receiving side is reading data slowly.  If it goes
     * to zero then the sender should stop sending.
     */
    public int getWindowSpace() {

        // Return the value we parsed or saved
        return _windowSpace;
    }

    /**
     * Express this Ack message as a String.
     * Composes text like "AckMessage DestID:1234 start:22 space:4 seq:" + getSequenceNumber();".
     * 
     * @return A String
     */
	public String toString() {

        // Compose and return the String
		return
            "AckMessage DestID:" + getConnectionID() +  // The connection ID the receiving computer assigned to the sending computer
            " start:"            + _windowStart      +  // The lowest numbered message the acknowledging computer is missing
            " space:"            + _windowSpace      +  // The number of messages the acknowledging computer has space to receive right now
            " seq:"              + getSequenceNumber(); // The sequence number this Ack packet is acknowledging we've received
	}
}
