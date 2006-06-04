
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * A BitTorrent program sends an Interested message when it's interested in downloading parts of the file from the remote computer.
 * The program makes a single BTInterested object to represent the Interested messages we send and receive.
 * 
 * An Interested message looks like this:
 * 
 * LLLLT
 * 
 * LLLL is 4 bytes in big endian order that hold the length of the rest of the message, 1.
 * T is the type byte, 0x02 for Interested.
 */
public class BTInterested extends BTMessage {

	/** Make the single BTInterested object that represents the Interested messages we send and receive. */
	private static final BTInterested INSTANCE = new BTInterested();

	/**
	 * Make an Interested message to send to a remote computer.
	 * 
	 * @return A reference to the program's BTInterested object
	 */
	public static BTInterested createMessage() {

		// Return a reference to the single BTInterested object
		return INSTANCE;
	}

	/**
	 * Make the program's single BTInterested object.
	 * This object will represent all the Interested messages we send and receive.
	 */
	private BTInterested() {

		// Call the BTMessage constructor, giving it the type byte 0x02 Interested
		super(INTERESTED);
	}

	/**
	 * Parse the data of an Interested message from a remote computer into a BTInterested object.
	 * This is the message parser.
	 * 
	 * Only BTMessage.parseMessage() calls this method.
	 * 
	 * A BitTorrent Interested message is 5 bytes, like "LLLLT".
	 * LLLL is the length, 1, in big endian in 4 bytes.
	 * T is the type byte, 0x02 for an Interested message.
	 * 
	 * All Interested messages are the same.
	 * It doesn't make sense to make a BTInterested object for each one.
	 * So, readMessage() returns a reference to the single BTInterested object the program makes.
	 * 
	 * @param payload A ByteBuffer with the data of an Interested message a remote computer sent us.
	 *                We've already read the LLLL prefix and the T type byte.
	 *                This should be the whole message, nothing comes after this.
	 */
	public static BTInterested readMessage(ByteBuffer payload) throws BadBTMessageException {

		// Make sure there's nothing after LLLLT in this Interested message
		if (payload.remaining() != 0) { // Make sure position has reached limit in the given ByteBuffer

			// This Interested message has some more data in it, document it in an exception
			byte[] msg = new byte[payload.remaining()];
			payload.get(msg);
			throw new BadBTMessageException("unexpected payload in interested message: " + new String(msg));
		}

		// Return a reference to the single BTInterested object
		return INSTANCE;
	}

	/**
	 * Get the payload of this Interested message.
	 * 
	 * @return A reference to an empty ByteBuffer, because Interested messages have no payload
	 */
	public ByteBuffer getPayload() {

		// Return a reference to the empty ByteBuffer we made
		return EMPTY_PAYLOAD;
	}

	/**
	 * Express this Interested message as text.
	 * 
	 * @return The String "BTInterested"
	 */
	public String toString() {

		// Return the text "BTInterested"
		return "BTInterested";
	}
}
