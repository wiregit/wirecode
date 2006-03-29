package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

/**
 * indicates that we will not upload anything to the remote host
 */
public class BTBitField extends BTMessage {
	private ByteBuffer _payload = null;

	/**
	 * Create BTBitField from network
	 * 
	 * @param payload
	 *            ByteBuffer with data from network
	 * @return new instance of BTAltLocs
	 * @throws BadBTMessageException
	 *             if data from network was bad.
	 */
	public static BTBitField readMessage(ByteBuffer payload) throws BadBTMessageException {
		if (payload.remaining() == 0)
			throw new BadBTMessageException("null payload in bitfield message!");
		byte[] bitfield = new byte[payload.remaining()];
		payload.get(bitfield);
		return new BTBitField(bitfield);
	}

	public BTBitField(byte[] bitfield) {
		super(BITFIELD);
		_payload = ByteBuffer.wrap(bitfield);
	}

	/**
	 * @return bitfield as byte array
	 */
	public byte[] getBitField() {
		return _payload.array();
	}

	public ByteBuffer getPayload() {
		_payload.clear();
		return _payload;
	}
	
	public String toString() {
		return "BTBitfield";
	}
}
