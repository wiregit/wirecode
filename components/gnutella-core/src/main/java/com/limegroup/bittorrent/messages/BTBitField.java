package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.statistics.BTMessageStatBytes;
import com.limegroup.gnutella.util.BufferByteArrayOutputStream;

/**
 * A BitField message.  
 */
public class BTBitField extends BTMessage {
	
	/*
	 * This message is always sent only as the first message
	 * after establishing a connection.  Since it may be of any
	 * size, we parse it sequentially by adding data as it arrives
	 * on the network.
	 */
	
	/** the bitfield itself */
	private ByteBuffer _bitfield = null;
	
	/** Length of the bitfield */
	private final int length;
	
	/** 
	 * Buffer that grows while the BitField is read
	 * from network. 
	 */
	private BufferByteArrayOutputStream bbaos;
	
	private BTBitField(ByteBuffer payload) {
		super(BITFIELD);
		_bitfield = payload;
		length = _bitfield.remaining();
	}

	/**
	 * creates a BitField message that is ready to 
	 * be filled with data read from the network.
	 */
	public BTBitField(int length) {
		super(BITFIELD);
		this.length = length;
		bbaos = new BufferByteArrayOutputStream();
	}
	
	/**
	 * Adds data to the BitField from the given buffer. 
	 * @return whether the bitfield is ready to be processed
	 */
	public boolean addData(ByteBuffer buf) {
		if (bbaos == null)
			throw new IllegalStateException("adding data to finished bitfield");
		
		if (buf.hasRemaining()) {
			int limit = buf.limit();
			int toWrite = Math.min(buf.position() + length - bbaos.size(), 
					buf.limit());
			buf.limit(toWrite);
			bbaos.write(buf);
			buf.limit(limit);
			if (bbaos.size() == length) { 
				_bitfield = ByteBuffer.wrap(bbaos.toByteArray());
				bbaos = null;
				BTMessageStat.INCOMING_BITFIELD.incrementStat();
				BTMessageStatBytes.INCOMING_BITFIELD.addData(5 + 
						_bitfield.remaining());
			}
		}
		
		return bbaos == null;
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
