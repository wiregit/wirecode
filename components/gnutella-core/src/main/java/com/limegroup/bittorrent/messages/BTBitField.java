package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.bittorrent.BTMetaInfo;

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
		return new BTBitField(payload);
	}

	private BTBitField(ByteBuffer payload) {
		super(BITFIELD);
		_payload = payload;
	}

	/**
	 * factory method, creates a new BitField message
	 * 
	 * @param info
	 *            the <tt>BTMetaInfo</tt> from which to request the actual
	 *            bitfield
	 * @return new instance of BTBitField
	 */
	public static BTBitField createMessage(BTMetaInfo info) {
		byte[] bitfield = info.createBitField();
		return new BTBitField(ByteBuffer.wrap(bitfield));
	}

	public ByteBuffer getPayload() {
		_payload.clear();
		return _payload;
	}
	
	public String toString() {
		return "BTBitfield";
	}
}
