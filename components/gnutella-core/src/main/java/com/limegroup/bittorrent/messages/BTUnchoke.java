package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * indicates that we will not upload anything to the remote host
 */
public class BTUnchoke extends BTMessage {
	private static final BTUnchoke INSTANCE = new BTUnchoke();

	/**
	 * factory method
	 * 
	 * @return new instance of message
	 */
	public static BTUnchoke createMessage() {
		return INSTANCE;
	}
	
	/**
	 * Constructs new BTUnchoke message
	 */
	BTUnchoke() {
		super(UNCHOKE);
	}
	
	public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}
	
	public String toString() {
		return "BTUnchoke";
	}

}
