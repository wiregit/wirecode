
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * A BitTorrent program sends a Choke message to a remote computer that it's not going to give any data.
 * 
 * A Choke message looks like this:
 * 
 * LLLLT
 * 
 * LLLL is the length, 1, stored in 4 bytes in big endian order.
 * T is the type byte, 0x00 for a Choke message.
 * There is no payload beyond this.
 * 
 * All Choke messages are the same.
 * As an optimization, this class just makes one BTChoke object.
 * The static createMessage() and readMessage() methods return references to it.
 */
public class BTChoke extends BTMessage {

	/** Make the single BTChoke object that represents the Choke messages we send and receive. */
	private static final BTChoke INSTANCE = new BTChoke();

	/**
	 * Make a Choke message to send to a remote computer.
	 * 
	 * @return A reference to the program's BTChoke object
	 */
	public static BTChoke createMessage() {

		// Return a reference to the single BTChoke object
		return INSTANCE;
	}

	/**
	 * Make the program's single BTChoke object.
	 * This object will represent all the Choke messages we send and receive.
	 */
	private BTChoke() {

		// Call the BTMessage constructor, giving it the type byte 0x00 Choke
		super(CHOKE);
	}

	/**
	 * Parse the data of a Choke message from a remote computer into a BTChoke object.
	 * This is the message parser.
	 * 
	 * Only BTMessage.parseMessage() calls this method.
	 * 
	 * A BitTorrent Choke message is 5 bytes, like "LLLLT".
	 * LLLL is the length, 1, in big endian in 4 bytes.
	 * T is the type byte, 0x00 for a Choke message.
	 * 
	 * All Choke messages are the same.
	 * It doesn't make sense to make a BTChoke object for each one.
	 * So, readMessage() returns a reference to the single BTChoke object the program makes.
	 * 
	 * @param payload A ByteBuffer with the data of a Choke message a remote computer sent us.
	 *                We've already read the LLLL prefix and the T type byte.
	 *                This should be the whole message, nothing comes after this.
	 */
	public static BTChoke readMessage(ByteBuffer payload) throws BadBTMessageException {

		// Make sure there's nothing after LLLLT in this Choke message
		if (payload.remaining() != 0) { // Make sure position has reached limit in the given ByteBuffer

			// This Choke message has some more data in it, document it in an exception
			byte[] msg = new byte[payload.remaining()];
			payload.get(msg);
			throw new BadBTMessageException("unexpected payload in choke message: " + new String(msg));
		}

		// Return a reference to the single BTChoke object
		return INSTANCE;
	}

	/**
	 * Get the payload of this Choke message.
	 * 
	 * @return A reference to an empty ByteBuffer, because Choke messages have no payload
	 */
	public ByteBuffer getPayload() {

		// Return a reference to the empty ByteBuffer we made
		return EMPTY_PAYLOAD;
	}

	/**
	 * Express this Choke message as text.
	 * 
	 * @return The String "BTChoke"
	 */
	public String toString() {

		// Return the text "BTChoke"
		return "BTChoke";
	}
}
