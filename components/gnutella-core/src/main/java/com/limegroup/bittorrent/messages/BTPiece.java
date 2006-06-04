
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.bittorrent.BTInterval;

/**
 * BitTorrent programs give one another pieces of files in Piece messages.
 * A Piece message is at least 13 bytes long:
 * 
 * LLLLTPPPPOOOOdatadatadatadata...
 * 
 * LLLL, PPPP, and OOOO are 4-byte ints in big endian order.
 * L is the length of the rest of the message.
 * T is the type byte, 0x07 identifies this as a Piece message.
 * P is the piece number the data belongs to.
 * O is the offset in bytes into that piece where the data goes.
 * datadatadata... is the file data, you can tell how long it is from L.
 * 
 * A Piece message contains a location of data, and the data itself.
 * The data location is made up of the piece number, and a rage of data within that piece.
 * BTPiece has a BTInterval named in that keeps that information.
 * The data itself is in a byte array named _data.
 */
public class BTPiece extends BTMessage {

	/** The piece number and range within it that this Piece message carries. */
	private BTInterval in;

	/** The file piece. */
	private byte[] _data;

	/**
	 * Make a new BTPiece object that represents a BitTorrent Piece message.
	 * 
	 * @param in   A BTInterval with the piece number and range of bytes inside
	 * @param data The file data that makes up that piece
	 */
	public BTPiece(BTInterval in, byte[] data) {

		// Have the BTMessage constructor save the type byte 0x07 for a Piece message
		super(PIECE);

		// Save the given interval and byte array
		this.in = in;
		_data = data;
	}

	/**
	 * Parse the data of a Piece message from a remote computer into a BTPiece object.
	 * A whole Piece message looks like this:
	 * 
	 * LLLLtPPPPOOOOdatadatadatadata...
	 * 
	 * payload is a ByteBuffer with position and limit clipped around PPPPLLLLdatadata....
	 * readMessage() moves position to limit.
	 * 
	 * Copies the file data from the given ByteBuffer into a new byte array named _data.
	 * 
	 * @param payload A ByteBuffer with position and limit clipped around the payload of a Piece message.
	 *                This is the part after LLLLT, like PPPPLLLLdatadatadata....
	 */
	public static BTPiece readMessage(ByteBuffer payload) throws BadBTMessageException {

		// Make sure the ByteBuffer we got has at least enough room for the location information PPPPOOOO
		if (payload.remaining() < 8) throw new BadBTMessageException("unexpected payload in piece message: " + new String(payload.array()));

		// Have the ByteBuffer read 4-byte ints in big endian order
		payload.order(ByteOrder.BIG_ENDIAN);

		// Read the piece number PPPP
		int pieceNum = payload.getInt();
		if (pieceNum < 0) throw new BadBTMessageException("invalid piece number in request message: " + pieceNum);

		// Read the offset OOOO
		int offset = payload.getInt();
		if (!payload.hasRemaining()) throw new BadBTMessageException("empty piece message " + pieceNum);

		// Read the file data after that
		byte[] data = new byte[payload.remaining()]; // Make a byte array exactly the right size
		payload.get(data); // get() moves data from payload to data

		// Make a new BTPiece object with the information we read
		return new BTPiece(
			new BTInterval(               // Make a new BTInterval which will keep the location information
				offset,                   // (2) How far into the piece the data starts
				offset + data.length - 1, // (3) The index of the last byte of data, -1 because BTInterval.high points to the last byte, not beyond it
				pieceNum),                // (1) The file piece number the data belongs to
			data);                        // The file data we read
	}

	/**
	 * Get the location in the whole file of the data this Piece message carries.
	 * 
	 * @return A BTInterval with the piece number and range within that piece
	 */
	public BTInterval getInterval() {

		// Return the BTInterval with the piece number and range information
		return in;
	}

	/**
	 * Get the file data this Piece message carries.
	 * 
	 * @return A byte array carrying the data
	 */
	public byte[] getData() {

		// Return the byte array of file data
		return _data;
	}

	/**
	 * Compose the payload of this Piece message, like:
	 * 
	 * PPPPOOOOdatadatadatadatadata...
	 * 
	 * PPPP and OOOO are 4-byte ints in big endian order.
	 * PPPP is the piece number, and OOOO is the distance into the piece that the data starts.
	 * After that is the data.
	 * 
	 * @return A ByteBuffer with the piece number, offset distance, and data
	 */
	public ByteBuffer getPayload() {

		// Make a byte buffer big enough to hold the 8 bytes at the start and all the data
		ByteBuffer payload = ByteBuffer.allocate(_data.length + 8);
		payload.order(ByteOrder.BIG_ENDIAN); // Have putInt() write ints in big endian order

		// In the first 8 bytes, write the piece number and the index of the data in that piece
		payload.putInt(in.getId());
		payload.putInt(in.low);

		// After that, write all the data
		payload.put(_data);

		// Set position and limit to clip around the whole buffer, and return it
		payload.clear();
		return payload;
	}

	/**
	 * Express this BTPiece object as text.
	 * Composes a String like "BTPiece (piece number;offset;data length)".
	 * 
	 * @return A String
	 */
	public String toString() {

		// Compose and return the text
		return "BTPiece (" + in.getId() + ";" + in.low + ";" + _data.length + ")";
	}
}
