package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;


/**
 * indicates that the sender is interested in downloading from the remote host
 */
public class BTInterested extends BTMessage {
	private static final BTInterested INSTANCE = new BTInterested();

	/**
	 * factory method
	 * 
	 * @return new instance of message
	 */
	public static BTInterested createMessage() {
		return INSTANCE;
	}

	/**
	 * Constructs new BTInterested message
	 */
	private BTInterested() {
		super(INTERESTED);
	}

	/**
	 * read message from network
	 * 
	 * @param payload
	 *            ByteBuffer with data from network
	 * @return BTInterested message
	 * @throws BadBTMessageException
	 */
	public static BTInterested readMessage(ByteBuffer payload)
			throws BadBTMessageException {
		// check message.
		if (payload.remaining() != 0) {
			byte[] msg = new byte[payload.remaining()];
			payload.get(msg);
			throw new BadBTMessageException(
					"unexpected payload in interested message: "
							+ new String(msg));
		}
		return INSTANCE;
	}

	public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}

	public String toString() {
		return "BTInterested" ;
	}

}
