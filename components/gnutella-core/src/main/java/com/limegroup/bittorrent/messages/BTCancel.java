
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.bittorrent.BTInterval;

/**
 * A BitTorrent Cancel message cancels a data request a BitTorrent program previously sent.
 * A Cancel message is 17 bytes long:
 * 
 * LLLLTPPPPOOOOSSSS
 * 
 * LLLL, PPPP, OOOO, and SSSS are ints written in big endian order.
 * L is 13, the length of the bytes beyond.
 * T is 0x06, the byte code for a Cancel message.
 * P is the piece number, like 0 for the first piece.
 * The Cancel message cancels a request for S bytes a distance O into the piece.
 * 
 * For instance, here's a cancel for the 4 bytes that are 8 bytes into piece number 5:
 * 
 * PPPP 5  bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
 * OOOO 8  ------->
 * SSSS 4          rrrr
 */
public class BTCancel extends BTMessage {

	/** A byte buffer with the payload of the message, just the "PPPPOOOOSSSS" part. */
	private ByteBuffer _payload = null;

	/** The piece number and range of bytes in the piece that this Cancel message doesn't need any more. */
	private BTInterval in;

	/**
	 * Make a new BTCancel object that represents a BitTorrent Cancel message.
	 * 
	 * @param in A BTInterval object that has the piece number and range of data within that piece
	 */
	public BTCancel(BTInterval in) {

		// Save the type, 0x08 for a Cancel message
		super(CANCEL);

		// Save the given BTInterval object, it has all the information for the payload
		this.in = in;
	}

	/**
	 * Make a BTCancel object that represents a BitTorrent Cancel message we've read from the network.
	 * We got it because a remote computer is canceling a request for a part of a file.
	 * This is the message parser.
	 * 
	 * The whole message looks like "LLLLTPPPPOOOOSSSS".
	 * Call readMessage() with the payload ByteBuffer clipped beyond the length and type, around the 12 bytes "PPPPOOOOSSSS".
	 * P is the piece number, O is the length into the piece where the range starts, and S is the size from that point.
	 * 
	 * @param payload A ByteBuffer with position and limit clipped around the 12-byte payload beyond the length int and type byte
	 * @return        A new BTCancel object that represents the Cancel message
	 */
	public static BTCancel readMessage(ByteBuffer payload) throws BadBTMessageException {

		// Make sure we were given exactly 12 bytes
		if (payload.remaining() != 12) throw new BadBTMessageException("unexpected payload in cancel message: " + new String(payload.array()));

		// Tell the ByteBuffer to read ints in big endian order
		payload.order(ByteOrder.BIG_ENDIAN);

		// First, read the piece number
		int pieceNum = payload.getInt(); // Moves position forward 4 bytes, past the int
		if (pieceNum < 0) throw new BadBTMessageException("invalid piece number in cancel message: " + pieceNum);

		// After that is the start distance
		int offset = payload.getInt();

		// Last is the size
		int length = payload.getInt();
		if (length == 0) throw new BadBTMessageException("0 length in cancel message " + pieceNum);

		// Make a new BTInterval object with the piece number and range, and use it to make the BTCancel object
		return new BTCancel(new BTInterval(
			offset,              // (2) The distance in the piece to the start of the range the Cancel message will have
			offset + length - 1, // (3) The number of bytes there, subtract 1 because BTInterval includes the last byte
			pieceNum));          // (1) The piece number
	}

	/**
	 * Get the range of data this Cancel message is saying it no longer needs.
	 * Returns a BTInterval object, which uses index of first byte and index of last byte, not distance and size.
	 * 
	 * @return A BTInterval object that has the piece number and range within it
	 */
	public BTInterval getInterval() {

		// Return the BTInterval object inside this BTCancel object
		return in;
	}

	/**
	 * Compose the payload of this BitTorrent Cancel message.
	 * 
	 * An entire Cancel message is 17 bytes arranged "LLLLTPPPPOOOOSSSS".
	 * L, P, O, and S are 4-byte ints in big endian order.
	 * L is the length, T is the type, P is the piece number, O is the offset, and S is the size.
	 * getPayload() returns the payload after L and T, just "PPPPOOOOSSSS".
	 * 
	 * @return A ByteBuffer with 12 bytes, "PPPPOOOOSSSS".
	 */
	public ByteBuffer getPayload() {

		if (_payload == null) {

			// Make a ByteBuffer that can hold 12 bytes, and configure it to write ints in big endian order
			_payload = ByteBuffer.allocate(12);
			_payload.order(ByteOrder.BIG_ENDIAN);

			// Write the piece number, offset, and size
			_payload.putInt(in.getId());            // The piece number, 0 is the first piece of the file
			_payload.putInt(in.low);                // The distance in bytes from the start of that piece to the range the packet doesn't want any longer
			_payload.putInt(in.high - in.low + 1);  // The number of bytes there, add 1 because in.high reaches the last byte, it doesn't include it
			_payload = _payload.asReadOnlyBuffer(); // Replace _payload with a read-only copy of the buffer
		}

		// Set position at the start and length at the end, and return it
		_payload.clear();
		return _payload;
	}

	/**
	 * Express this BTCancel object as text.
	 * Makes a string like "BTCancel (25:5-10)".
	 * 25 is the piece number, and 5 is the distance in bytes into that piece that the range starts.
	 * 10 isn't the size, and it's not the end of the range either, it's the distance to the last byte in the range.
	 * 
	 * @return A String like "BTCancel (25:5-10)"
	 */
	public String toString() {

		// Compose the text, calling BTInterval.toString() by adding in to a String
		return "BTCancel (" + in + ")";
	}
}
