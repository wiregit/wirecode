package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * cancels a request we may have sent to the remote host
 */
public class BTCancel extends BTMessage {
	private ByteBuffer _payload = null;

	private int _pieceNum;

	private int _offset;

	private int _length;

	/**
	 * Constructs new BTCancel message
	 */
	private BTCancel(int pieceNum, int offset, int length) {
		super(CANCEL);
		_pieceNum = pieceNum;
		_offset = offset;
		_length = length;
	}

	/**
	 * creates BTCancel message from network
	 * 
	 * @param payload
	 *            the payload of the message wrapped in a ByteBuffer.
	 * @return new BTCancel message 
	 * 	
	 * @throws BadBTMessageException
	 */
	public static BTCancel readMessage(ByteBuffer payload) throws BadBTMessageException {
		if (payload.remaining() != 12)
			throw new BadBTMessageException(
					"unexpected payload in cancel message: "
							+ new String(payload.array()));

		payload.order(ByteOrder.BIG_ENDIAN);

		int pieceNum = payload.getInt();

		if (pieceNum < 0)
			throw new BadBTMessageException(
					"invalid piece number in cancel message: " + pieceNum);

		int offset = payload.getInt();

		int length = payload.getInt();
		
		return new BTCancel(pieceNum, offset, length);
	}

	/**
	 * factory method
	 * 
	 * @param pieceNum
	 *            the number of the piece we requested
	 * @param offset
	 *            the offset of the requested range wrt. to the beginning of the
	 *            piece
	 * @param length
	 *            the length of the requested range in bytes
	 * @return new instance of BTCancel
	 */
	public static BTCancel createMessage(int pieceNum, int offset, int length) {
		return new BTCancel(pieceNum, offset, length);
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
			_payload = ByteBuffer.allocate(12);
			_payload.order(ByteOrder.BIG_ENDIAN);
			_payload.putInt(_pieceNum);
			_payload.putInt(_offset);
			_payload.putInt(_length);
			_payload = _payload.asReadOnlyBuffer();
		}
		_payload.clear();
		return _payload;
	}
	

	public String toString() {
		return "BTCancel (" + _pieceNum + ";" + _offset + ";" + _length + ")" ;
	}
}
