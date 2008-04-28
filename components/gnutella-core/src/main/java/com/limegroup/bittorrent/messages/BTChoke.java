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
	 * @return ByteBuffer for this message
	 */
	@Override
    public ByteBuffer getPayload() {
		return EMPTY_PAYLOAD;
	}
	
	@Override
    public String toString() {
		return "BTChoke" ;
	}
}
