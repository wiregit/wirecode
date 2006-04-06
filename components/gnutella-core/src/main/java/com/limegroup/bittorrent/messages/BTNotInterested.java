package com.limegroup.gnutella.torrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.gnutella.torrent.BadBTMessageException;

/**
 * indicates that the sender is interested in downloading from the remote host
 */
public class BTNotInterested extends BTMessage {
	private static final BTNotInterested INSTANCE = new BTNotInterested();

	/**
	 * factory method
	 * 
	 * @return new instance of message
	 */
	public static BTNotInterested createMessage() {
		return INSTANCE;
	}

	/**
	 * Constructs new BTInterested message
	 */
	BTNotInterested() {
		super(NOT_INTERESTED);
	}

	/**
	 * read message from network
	 */
	public static BTNotInterested readMessage(ByteBuffer payload) throws BadBTMessageException {
		// check message.
		if (payload.remaining() != 0) {
			byte[] msg = new byte[payload.remaining()];
			payload.get(msg);
			throw new BadBTMessageException(
					"unexpected payload in notinterested message: "
							+ new String(msg));
		}
		return INSTANCE;
	}

	public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}

	public String toString() {
		return "BTNotInterested" ;
	}
	
}
