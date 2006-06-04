
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * A BitTorrent program sends a Not Interested message when it's not interested in getting file pieces right now.
 * The program makes a single BTNotInterested object to represent the Not Interested messages we send and receive.
 * 
 * A Not Interested message looks like this:
 * 
 * LLLLT
 * 
 * LLLL is 4 bytes in big endian order that hold the length of the rest of the message, 1.
 * T is the type byte, 0x03 for Not Interested.
 */
public class BTNotInterested extends BTMessage {

	/** Make the single BTNotInterested object that represents the Not Interested messages we send and receive. */
	private static final BTNotInterested INSTANCE = new BTNotInterested();

	/**
	 * Make a Not Interested message to send to a remote computer.
	 * 
	 * @return A reference to the program's BTNotInterested object
	 */
	public static BTNotInterested createMessage() {

		// Return a reference to the single BTNotInterested object
		return INSTANCE;
	}

	/**
	 * Make the program's single BTNotInterested object.
	 * This object will represent all the Not Interested messages we send and receive.
	 */
	BTNotInterested() {

		// Call the BTMessage constructor, giving it the type byte 0x03 Not Interested
		super(NOT_INTERESTED);
	}

	/**
	 * Parse the data of a Not Interested message from a remote computer into a BTNotInterested object.
	 * This is the message parser.
	 * 
	 * Only BTMessage.parseMessage() calls this method.
	 * 
	 * A BitTorrent Not Interested message is 5 bytes, like "LLLLT".
	 * LLLL is the length, 1, in big endian in 4 bytes.
	 * T is the type byte, 0x03 for a Not Interested message.
	 * 
	 * All Not Interested messages are the same.
	 * It doesn't make sense to make a BTNotInterested object for each one.
	 * So, readMessage() returns a reference to the single BTNotInterested object the program makes.
	 * 
	 * @param payload A ByteBuffer with the data of a Not Interested message a remote computer sent us.
	 *                We've already read the LLLL prefix and the T type byte.
	 *                This should be the whole message, nothing comes after this.
	 */
	public static BTNotInterested readMessage(ByteBuffer payload) throws BadBTMessageException {

		// Make sure there's nothing after LLLLT in this Not Interested message
		if (payload.remaining() != 0) {

			// This Interested message has some more data in it, document it in an exception
			byte[] msg = new byte[payload.remaining()];
			payload.get(msg);
			throw new BadBTMessageException("unexpected payload in notinterested message: " + new String(msg));
		}

		// Return a reference to the single BTNotInterested object
		return INSTANCE;
	}

	/**
	 * Get the payload of this Not Interested message.
	 * 
	 * @return A reference to an empty ByteBuffer, because Not Interested messages have no payload
	 */
	public ByteBuffer getPayload() {

		// Return a reference to the empty ByteBuffer we made
		return EMPTY_PAYLOAD;
	}

	/**
	 * Express this Not Interested message as text.
	 * 
	 * @return The String "BTNotInterested"
	 */
	public String toString() {

		// Return the text "BTNotInterested"
		return "BTNotInterested";
	}
}
