package com.limegroup.gnutella.torrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.gnutella.torrent.BTMetaInfo;
import com.limegroup.gnutella.torrent.BadBTMessageException;

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

	private BTBitField(byte[] bitfield) {
		super(BITFIELD);
		_payload = ByteBuffer.wrap(bitfield);
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
		return new BTBitField(bitfield);
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
