package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * indicates that we will not upload anything to the remote host
 */
public class BTRequest extends BTMessage {
	/*
	 * if anyone requests more than 2^16 bytes we will rip his head off
	 */
	private static final int MAX_REQUEST_SIZE = 65536;

	private int _pieceNum;

	private int _offset;

	private int _length;
	
	private ByteBuffer _payload;

	/**
	 * Constructs new BTRequest message
	 */
	private BTRequest(int pieceNum, int offset, int length) {
		super(REQUEST);
		_pieceNum = pieceNum;
		_offset = offset;
		_length = length;
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

		int length = payload.getInt();
		if (length < 0 || length > MAX_REQUEST_SIZE)
			throw new BadBTMessageException(
					"invalid requested length in request message: " + length);
		
		return new BTRequest(pieceNum, offset, length);
	}

	/**
	 * factory method
	 * 
	 * @param pieceNum
	 *            the number of the piece to request
	 * @param offset
	 *            the offset of the requested range wrt. to the beginning of the
	 *            piece
	 * @param length
	 *            the length of the requested range in bytes
	 * @return new instance of BTRequest
	 */
	public static BTRequest createMessage(int pieceNum, int offset, int length) {
		return new BTRequest(pieceNum, offset, length);
	}

	/**
	 * @return piece number of the requested piece
	 */
	public int getPieceNum() {
		return _pieceNum;
	}

	/**
	 * @return offset wrt. the beginning of the piece
	 */
	public int getOffset() {
		return _offset;
	}

	/**
	 * @return requested amount of bytes
	 */
	public int getLength() {
		return _length;
	}

	public ByteBuffer getPayload() {
		if (_payload == null) {
			ByteBuffer buf = ByteBuffer.allocate(12);
			buf.order(ByteOrder.BIG_ENDIAN);
			buf.putInt(_pieceNum);
			buf.putInt(_offset);
			buf.putInt(_length);
			_payload = buf.asReadOnlyBuffer();
		}
		_payload.clear();
		return _payload;
	}
	
	public String toString() {
		return "BTRequest (" + _pieceNum + ";" + _offset + ";" + _length + ")" ;
	}
}
