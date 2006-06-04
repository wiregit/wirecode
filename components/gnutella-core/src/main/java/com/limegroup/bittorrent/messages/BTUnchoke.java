
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * The program's BTUnchoke object represents the BitTorrent Unchoke messages we send and receive.
 * 
 * BitTorent programs send choke messages to computers they won't give data.
 * An Unchoke message ends the choked status.
 * 
 * An Unchoke message looks like this:
 * 
 * LLLLT
 * 
 * LLLL is the length, 1, in 4 byte in big endian order.
 * T is the type, 0x01 for an Unchoke message.
 */
public class BTUnchoke extends BTMessage {

	/** Make the single BTUnchoke object that represents the Unchoke messages we send and receive. */
	private static final BTUnchoke INSTANCE = new BTUnchoke();

	/**
	 * Make a Unchoke message to send to a remote computer.
	 * 
	 * @return A reference to the program's BTUnchoke object
	 */
	public static BTUnchoke createMessage() {

		// Return a reference to the program's single BTUnchoke object
		return INSTANCE;
	}

	/**
	 * Make the program's single BTUnchoke object.
	 * This object will represent all the Unchoke messages we send and receive.
	 */
	BTUnchoke() {

		// Call the BTMessage constructor, giving it the type byte 0x01 Unchoke
		super(UNCHOKE);
	}

	/**
	 * Parse the data of an Unchoke message from a remote computer into a BTUnchoke object.
	 * This is the message parser.
	 * 
	 * Only BTMessage.parseMessage() calls this method.
	 * 
	 * A BitTorrent Choke message is 5 bytes, like "LLLLT".
	 * LLLL is the length, 1, in big endian in 4 bytes.
	 * T is the type byte, 0x01 for an Unchoke message.
	 * 
	 * All Unchoke messages are the same.
	 * It doesn't make sense to make a BTUnchoke object for each one.
	 * So, readMessage() returns a reference to the single BTUnchoke object the program makes.
	 * 
	 * @param payload A ByteBuffer with the data of an Unchoke message a remote computer sent us.
	 *                We've already read the LLLL prefix and the T type byte.
	 *                This should be the whole message, nothing comes after this.
	 */
	public static BTUnchoke readMessage(ByteBuffer payload) throws BadBTMessageException {

		// Make sure there's nothing after LLLLT in this Unchoke message
		if (payload.remaining() != 0) { // Make sure position has reached limit in the given ByteBuffer

			// This Choke message has some more data in it, document it in an exception
			byte[] msg = new byte[payload.remaining()];
			payload.get(msg);
			throw new BadBTMessageException("unexpected payload in unchoke message: " + new String(msg));
		}

		// Return a reference to the single BTUnchoke object
		return INSTANCE;
	}

	/**
	 * Get the payload of this Unchoke message.
	 * 
	 * @return A reference to an empty ByteBuffer, because Unchoke messages have no payload
	 */
	public ByteBuffer getPayload() {

		// Return a reference to the empty ByteBuffer we made
		return EMPTY_PAYLOAD;
	}

	/**
	 * Express this Unchoke message as text.
	 * 
	 * @return The String "BTUnchoke"
	 */
	public String toString() {

		// Return the text "BTUnchoke"
		return "BTUnchoke";
	}
}
