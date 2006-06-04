
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.bittorrent.BTInterval;

/**
 * A BitTorrent Request message lets a BitTorrent program request a part of a file from a peer.
 * A Request message is 17 bytes long:
 * 
 * LLLLTPPPPOOOOSSSS
 * 
 * LLLL, PPPP, OOOO, and SSSS are ints written in big endian order.
 * L is 13, the length of the bytes beyond.
 * T is 0x06, the byte code for a Request message.
 * P is the piece number, like 0 for the first piece.
 * The Request message requests S bytes a distance O into the piece.
 * 
 * For instance, here's a request for the 4 bytes that are 8 bytes into piece number 5:
 * 
 * PPPP 5  bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
 * OOOO 8  ------->
 * SSSS 4          rrrr
 */
public class BTRequest extends BTMessage {

	/** 65536 bytes, 65 KB, the maximum amount of data a remote computer can request from us. */
	private static final int MAX_REQUEST_SIZE = 65536;

	/** The piece number and range of bytes in the piece that this Request message is asking for. */
	private BTInterval in;

	/** A byte buffer with the payload of the message, just the "PPPPOOOOSSSS" part. */
	private ByteBuffer _payload;

	/**
	 * Make a new BTRequest object that represents a BitTorrent Request message.
	 * 
	 * @param in A BTInterval object that has the piece number and range of data within that piece
	 */
	public BTRequest(BTInterval in) {

		// Save the type, 0x06 for a Request message
		super(REQUEST);

		// Save the given BTInterval object, it has all the information for the payload
		this.in = in;
	}

	/**
	 * Make a BTRequest object that represents a BitTorrent Request message we've read from the network.
	 * We got it because a remote computer is requesting a part of a file from us.
	 * This is the message parser.
	 * 
	 * The whole message looks like "LLLLTPPPPOOOOSSSS".
	 * Call readMessage() with the payload ByteBuffer clipped beyond the length and type, around the 12 bytes "PPPPOOOOSSSS".
	 * P is the piece number, O is the length into the piece where the range starts, and S is the size from that point.
	 * 
	 * @param payload A ByteBuffer with position and limit clipped around the 12-byte payload beyond the length int and type byte
	 * @return        A new BTRequest object that represents the Request message
	 */
	public static BTRequest readMessage(ByteBuffer payload) throws BadBTMessageException {

		// Make sure we were given exactly 12 bytes
		if (payload.remaining() != 12) throw new BadBTMessageException("unexpected payload in request message: " + new String(payload.array()));

		// Tell the ByteBuffer to give us ints in big endian order
		payload.order(ByteOrder.BIG_ENDIAN);

		// First, read the piece number
		int pieceNum = payload.getInt(); // Reads the first 4 bytes as a big endian int, moving position past them
		if (pieceNum < 0) throw new BadBTMessageException("invalid piece number in request message: " + pieceNum);

		// Read the start distance
		int offset = payload.getInt();
		if (offset < 0) throw new BadBTMessageException("negative offset in mesage");

		// Read the size
		int length = payload.getInt();
		if (length <= 0 || length > MAX_REQUEST_SIZE) throw new BadBTMessageException("invalid requested length in request message: " + length);

		// Make a BTInterval object, and use it to make and return the new BTRequest object
		return new BTRequest(new BTInterval(
			offset,              // (2) The distance in the piece to the start of the range the Request message will ask for
			offset + length - 1, // (3) The number of bytes there the message is requesting, subtract 1 because BTInterval includes the last byte
			pieceNum));          // (1) The piece number
	}

	/**
	 * Get the range of data this Request message is asking for.
	 * Returns a BTInterval object, which uses index of first byte and index of last byte, not distance and size.
	 * 
	 * @return A BTInterval object that has the piece number and range within it
	 */
	public BTInterval getInterval() {

		// Return the BTInterval object inside this BTRequest object
		return in;
	}

	/**
	 * Compose the payload of this BitTorrent Request message.
	 * 
	 * An entire Request message is 18 bytes arranged like "LLLLTPPPPOOOOSSSS".
	 * getPayload() returns a 12-byte array with the payload after the length and type, just "PPPPOOOOSSSS".
	 * PPPP, OOOO, and SSSS are ints written in big endian order.
	 * PPPP is the piece number.
	 * OOOO is the distance in bytes from the start of the piece to the range this Request message is requesting.
	 * SSSS is the number of bytes at OOOO this Request message is requesting.
	 * 
	 * @return A 12-byte ByteBuffer with "PPPPOOOOSSSS", with position at the start and limit at the end
	 */
	public ByteBuffer getPayload() {

		// If we haven't composed this Request message's payload yet
		if (_payload == null) {

			// Compose the 12 bytes of payload data
			ByteBuffer buf = ByteBuffer.allocate(12); // Make a ByteBuffer that can hold 12 bytes
			buf.order(ByteOrder.BIG_ENDIAN);          // Have the putInt() method write ints in big endian order
			buf.putInt(in.getId());                   // Write the piece number, distance, and size
			buf.putInt(in.low);
			buf.putInt(in.high - in.low + 1);         // Add 1 because high points to the last byte in the range, not beyond it
			_payload = buf.asReadOnlyBuffer();        // Don't let the caller change the buffer we'll return
		}

		// Put position at the start and limit at the end, and return a reference to the buffer with the payload data
		_payload.clear();
		return _payload;
	}

	/**
	 * Express this BTRequest object as text.
	 * Makes a string like "BTRequest (25:5-10)".
	 * 25 is the piece number, and 5 is the distance in bytes into that piece that the range starts.
	 * 10 isn't the size, and it's not the end of the range either, it's the distance to the last byte in the range.
	 * 
	 * @return A String like "BTRequest (25:5-10)"
	 */
	public String toString() {

		// Compose the text, calling BTInterval.toString() by adding in to a String
		return "BTRequest (" + in + ")";
	}
}
