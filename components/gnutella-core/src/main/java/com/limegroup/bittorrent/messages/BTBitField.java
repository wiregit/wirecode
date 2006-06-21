package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.bittorrent.BTMetaInfo;

/**
 * A BitField message.  
 */
public class BTBitField extends BTMessage {
	
	/** the bitfield itself */
	private ByteBuffer _bitfield = null;

	
	public BTBitField(ByteBuffer payload) {
		super(BITFIELD);
		_bitfield = payload;
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
		_bitfield.clear();
		return _bitfield;
	}
	
	public String toString() {
		return "BTBitfield";
	}
}
