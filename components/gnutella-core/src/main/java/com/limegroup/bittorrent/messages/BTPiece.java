package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.bittorrent.BTInterval;

/**
 * indicates that we will not upload anything to the remote host
 */
public class BTPiece extends BTMessage {
	private BTInterval in;

	private final byte[] _data;

	/**
	 * Constructs new BTPiece message
	 */
	public BTPiece(BTInterval in, byte[] data) {
		super(PIECE);
		this.in = in;
		_data = data;
	}

	public BTInterval getInterval() {
		return in;
	}

	/**
	 * @return requested data
	 */
	public byte[] getData() {
		return _data;
	}

	public ByteBuffer getPayload() {
		ByteBuffer payload = ByteBuffer.allocate(_data.length + 8);
		payload.order(ByteOrder.BIG_ENDIAN);
		payload.putInt(in.getId());
		payload.putInt(in.low);
		payload.put(_data);
		payload.clear();
		return payload;
	}
	
	public String toString() {
		return "BTPiece (" + in.getId() + ";" + in.low + ";" + _data.length + ")" ;
	}
}
