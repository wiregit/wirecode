package com.limegroup.gnutella.torrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.gnutella.torrent.BTInterval;
import com.limegroup.gnutella.torrent.BadBTMessageException;

/**
 * indicates that we will not upload anything to the remote host
 */
public class BTRequest extends BTMessage {
	/*
	 * if anyone requests more than 2^16 bytes we will rip his head off
	 */
	private static final int MAX_REQUEST_SIZE = 65536;

	private BTInterval in;
	
	private ByteBuffer _payload;

	/**
	 * Constructs new BTRequest message
	 */
	public BTRequest(BTInterval in) {
		super(REQUEST);
		this.in = in;
	}

	/**
	 * read message from network
	 * 
	 * @param payload
	 *            the payload of the message wrapped in a ByteBuffer.
	 * @return new BTRequest
	 * @throws BadBTMessageException
	 */
	public static BTRequest readMessage(ByteBuffer payload) throws BadBTMessageException {
		if (payload.remaining() != 12)
			throw new BadBTMessageException(
					"unexpected payload in request message: "
							+ new String(payload.array()));

		payload.order(ByteOrder.BIG_ENDIAN);

		int pieceNum = payload.getInt();

		if (pieceNum < 0)
			throw new BadBTMessageException(
					"invalid piece number in request message: " + pieceNum);

		int offset = payload.getInt();
		if (offset < 0)
			throw new BadBTMessageException("negative offset in mesage");

		int length = payload.getInt();
		if (length <= 0 || length > MAX_REQUEST_SIZE)
			throw new BadBTMessageException(
					"invalid requested length in request message: " + length);
		
		return new BTRequest(new BTInterval(offset, offset + length - 1, pieceNum));
	}

	
	public BTInterval getInterval() {
		return in;
	}

	public ByteBuffer getPayload() {
		if (_payload == null) {
			ByteBuffer buf = ByteBuffer.allocate(12);
			buf.order(ByteOrder.BIG_ENDIAN);
			buf.putInt(in.getId());
			buf.putInt(in.low);
			buf.putInt(in.high - in.low + 1);
			_payload = buf.asReadOnlyBuffer();
		}
		_payload.clear();
		return _payload;
	}
	
	public String toString() {
		return "BTRequest (" + in + ")" ;
	}
}
