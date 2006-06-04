
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A BitTorrent program sends a Have message to tell a computer it has a numbered piece.
 * 
 * In BitTorrent, a .torrent file has information about a file BitTorrent programs can download and share.
 * The .torrent file tells the shared file's size, how many pieces it's broken into, and the size of a piece.
 * Each piece has a number, and the first piece is number 0.
 * 
 * A Have message is 9 bytes long:
 * 
 * LLLLTPPPP
 * 
 * LLLL and NNNN are ints with big endian byte ordering.
 * LLLL is the length of the rest of the message, 5.
 * T is the type code, 0x04 for a Have message.
 * PPPP is the piece number.
 */
public class BTHave extends BTMessage {

	/**
	 * The payload of this Have message.
	 * An entire Have message looks like "LLLLTNNNN", with length, type, and piece number.
	 * The payload is just the piece number, "NNNN".
	 */
	private ByteBuffer _payload = null;

	/**
	 * The file piece number the computer that sends this Have message has.
	 * 
	 * In BitTorrent, a file is split into pieces.
	 * Each piece has a number, with the first piece being number 0.
	 * When a computer gets a piece and checks it's hash, it tells others with a Have message. (do)
	 */
	private final int _pieceNum;

	/**
	 * Make a new BTHave object to represent a BitTorrent Have message.
	 * 
	 * @param pieceNum The file piece number to write in the new message
	 */
	private BTHave(int pieceNum) {

		// Call the BTMessage constructor, giving it the type byte 0x04 Have
		super(HAVE);

		// Save the given piece number
		_pieceNum = pieceNum;
	}

	/**
	 * Parse the data of a Have message from a remote computer into a BTHave object.
	 * This is the message parser.
	 * 
	 * Only BTMessage.parseMessage() calls this method.
	 * 
	 * A BitTorrent Not Interested message is 9 bytes, like "LLLLTNNNN".
	 * LLLL is the length, 1, in big endian in 4 bytes.
	 * T is the type byte, 0x04 for a Have message.
	 * NNNN is the piece number.
	 * 
	 * @param payload A ByteBuffer with the data of a Not Interested message a remote computer sent us.
	 *                We've already read the LLLL prefix and the T type byte.
	 *                The 4 bytes of the piece number are still in the buffer.
	 *                This method moves position forward 4 bytes, to length, closing the buffer.
	 */
	public static BTHave readMessage(ByteBuffer payload) throws BadBTMessageException {

		// Make sure there are 4 bytes left in the buffer, this is the NNNN piece number
		if (payload.remaining() != 4) throw new BadBTMessageException("unexpected payload in have message: " + new String(payload.array()));

		// Read the piece number from the buffer
		payload.order(ByteOrder.BIG_ENDIAN);
		int pieceNum = payload.getInt(); // Moves position past the NNNN piece number, closing it to length

		/*
		 * Every integer in the BitTorrent peer protocol is unsigned.
		 * But, the int type in Java is signed.
		 * If the piece number we read is big enough to flip it to a negative number in Java, we treat it as invalid.
		 */

		// Make sure the piece number isn't so big that Java treats the int as negative
		if (pieceNum < 0) throw new BadBTMessageException("invalid piece number in have message: " + pieceNum);

		// Return a new BTHave object with the piece number we read
		return new BTHave(pieceNum);
	}

	/**
	 * Make a new BTHave message we can send to tell a remote computer a piece number we have.
	 * 
	 * @param pieceNum The file piece number we have
	 * @return         A new BTHave object that represents a BitTorrent Have message with the given piece number written in it
	 */
	public static BTHave createMessage(int pieceNum) {

		// Not used
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putInt(pieceNum);
		buf.clear();

		// Return a new BTHave object with the given piece number
		return new BTHave(pieceNum);
	}

	/**
	 * Get the piece number this Have message contains.
	 * This is a piece of the file the computer that sends this message has.
	 * The first piece is numbered 0.
	 * The size of the pieces are defined in the .torrent file.
	 * 
	 * @return The piece number
	 */
	public int getPieceNum() {

		// Return the piece number we saved or parsed
		return _pieceNum;
	}

	/**
	 * Compose the payload of this Have message.
	 * An entire Have message consists of 9 bytes:
	 * 
	 * LLLLTNNNN
	 * 
	 * LLLL is the size of the rest, 5.
	 * T is 0x04 for a Have message.
	 * NNNN is the piece number written in 4 bytes in big endian order.
	 * 
	 * getPayload() just returns NNNN, the part after the length and type.
	 * 
	 * @return A 4-byte ByteBuffer with position at the start and length at the end
	 */
	public ByteBuffer getPayload() {

		// If we haven't composed the payload for this Have message yet
		if (_payload == null) {

			// Compose it
			_payload = ByteBuffer.allocate(4);      // Make a ByteBuffer that holds exactly 4 bytes
			_payload.order(ByteOrder.BIG_ENDIAN);   // Have it write the numbers we put in it in big endian order
			_payload.putInt(_pieceNum);             // Write a 4-byte int, the piece number of this BTHave object
			_payload = _payload.asReadOnlyBuffer(); // Make a read-only buffer from that, and point _payload at it instead
		}

		// Move position to the start and length to the end to clip around the data in the ByteBuffer
		_payload.clear();

		// Return a reference to the ByteBuffer we prepared
		return _payload;
	}

	/**
	 * Express this BTHave object as text.
	 * Composes text like "BTHave (258)" if this Have message has a piece number of 258.
	 * 
	 * @return A String
	 */
	public String toString() {

		// Compose and return text that includes the piece number
		return "BTHave (" + _pieceNum + ")";
	}
}
