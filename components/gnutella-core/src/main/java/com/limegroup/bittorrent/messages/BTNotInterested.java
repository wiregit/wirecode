package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

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

	public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}

	public String toString() {
		return "BTNotInterested" ;
	}
	
}
