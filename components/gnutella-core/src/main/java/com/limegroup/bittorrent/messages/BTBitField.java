package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.bittorrent.TorrentContext;

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
	public static BTBitField createMessage(TorrentContext context) {
		byte[] bitfield = context.getDiskManager().createBitField();
		return new BTBitField(ByteBuffer.wrap(bitfield));
	}

	@Override
    public ByteBuffer getPayload() {
		_bitfield.clear();
		return _bitfield;
	}
	
	@Override
    public String toString() {
		return "BTBitfield";
	}
}
