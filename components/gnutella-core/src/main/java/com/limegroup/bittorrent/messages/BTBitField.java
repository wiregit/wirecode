
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.bittorrent.BTMetaInfo;

/**
 * Send a BitTorrent Bit Field message to tell another computer what file pieces you have.
 * 
 * A Bit Field message looks like this:
 * 
 * LLLLTbitfieldbitfieldbitfield...
 * 
 * L is the length, and T is 0x05 to identify this as a Bit Field message.
 * After that is the bit field.
 * 
 * In the bit field, each bit represents a file piece.
 * A bit is set to 1 if the sender has the piece, 0 if it doesn't.
 * The bit field describes the entire file.
 * A really big file will have bigger pieces, making the total number of them less.
 */
public class BTBitField extends BTMessage {

	/** The payload of this Bit Field message, which is the bit field. */
	private ByteBuffer _payload = null;

	/**
	 * Make a BTBitField object from the data of a BitTorrent Bit Field message a remote computer sent us.
	 * This is the message parser.
	 * 
	 * Takes a ByteBuffer named payload with the position and limit clipped around the bitfield.
	 * Moves the position past it, to the limit.
	 * 
	 * @param payload The bit field payload of a Bit Field message, the part after "LLLLT"
	 * @return        A new BTBitField object that represents the message
	 */
	public static BTBitField readMessage(ByteBuffer payload) throws BadBTMessageException {

		// Make sure the caller gave us some data
		if (payload.remaining() == 0) throw new BadBTMessageException("null payload in bitfield message!");

		// Make a byte array exactly the right size to hold the bitfield, and copy it in
		byte[] bitfield = new byte[payload.remaining()];
		payload.get(bitfield); // Moves data from payload to bitfield

		// Make a new BTBitField with the byte array, and return it
		return new BTBitField(bitfield);
	}

	/**
	 * Make a new BTBitField object to represent a Bit Field message.
	 * 
	 * @param bitfield
	 */
	private BTBitField(byte[] bitfield) {

		// Save the type byte 0x05 for a Bit Field message
		super(BITFIELD);

		// Wrap the given byte array in a ByteBuffer, and save it as the payload
		_payload = ByteBuffer.wrap(bitfield);
	}

	/**
	 * Make a new Bit Field message for us to send.
	 * Only BTConnection.sendBitfield() calls this.
	 * 
	 * @param info A BTMetaInfo object we can get to create the bit field
	 * @return     A BTBitField object that represents the Bit Field message and contains the given bit field
	 */
	public static BTBitField createMessage(BTMetaInfo info) {

		// Have the given BTMetaInfo object create a bit field
		byte[] bitfield = info.createBitField();

		// Wrap it in a BTBitField object, and return it
		return new BTBitField(bitfield);
	}

	/**
	 * Get the bit field in this Bit Field message.
	 * The bit field has a bit for each piece in the file.
	 * If a bit is set to 1, that means the peer that sent the Bit Field message has that piece.
	 * 
	 * @return The bit field as a byte array
	 */
	public byte[] getBitField() {

		// Return the byte array inside the _payload ByteBuffer
		return _payload.array();
	}

	/**
	 * Get the payload of this Bit Field message, which is the bit field.
	 * The bit field has a bit for each piece in the file.
	 * If a bit is set to 1, that means the peer that send the Bit Field message has that piece.
	 * 
	 * @return The bit field in a ByteBuffer with position at the start and limit at the end
	 */
	public ByteBuffer getPayload() {

		// Move position to the start and limit to the end, and return the _payload ByteBuffer
		_payload.clear();
		return _payload;
	}

	/**
	 * Express this Bit Field message as text.
	 * 
	 * @return The String "BTBitfield", which doesn't actually contain any information from the bit field at all
	 */
	public String toString() {

		// Return the String "BTBitfield"
		return "BTBitfield";
	}
}
