package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;


/**
 * indicates that we will not upload anything to the remote host
 */
public class BTChoke extends BTMessage {
	private static final BTChoke INSTANCE = new BTChoke();

	/**
	 * factory method
	 * 
	 * @return new instance of message
	 */
	public static BTChoke createMessage() {
		return INSTANCE;
	}
	
	/**
	 * Constructs new BTChoke message
	 */
	private BTChoke() {
		super(CHOKE);
	}
	
	/**
	 * read message from network
	 */
	public static BTChoke readMessage(ByteBuffer payload) throws BadBTMessageException {
		// check message.
		if (payload.remaining() != 0) {
			byte[] msg = new byte[payload.remaining()];
			payload.get(msg);
			throw new BadBTMessageException(
					"unexpected payload in choke message: "
							+ new String(msg));
		}
		return INSTANCE;
	}

	/**
	 * @return ByteBuffer for this message
	 */
	public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}
	
	public String toString() {
		return "BTChoke" ;
	}
}
