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

	public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}

	public String toString() {
		return "BTInterested" ;
	}

}
