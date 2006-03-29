package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * indicates that we will not upload anything to the remote host
 */
public class BTPiece extends BTMessage {
	private int _pieceNum;

	private int _offset;

	private byte[] _data;

	/**
	 * Constructs new BTPiece message
	 */
	private BTPiece(int pieceNum, int offset, byte[] data) {
		super(PIECE);
		_pieceNum = pieceNum;
		_offset = offset;
		_data = data;
	}

	/**
	 * read message from network
	 * 
	 * @param payload
	 *            the payload of the message wrapped in a ByteBuffer.
	 * @return new BTPiece message
	 * @throws BadBTMessageException
	 */
	public static BTPiece readMessage(ByteBuffer payload)
			throws BadBTMessageException {

		if (payload.remaining() < 8)
			throw new BadBTMessageException(
					"unexpected payload in piece message: "
							+ new String(payload.array()));

		payload.order(ByteOrder.BIG_ENDIAN);

		int pieceNum = payload.getInt();

		if (pieceNum < 0)
			throw new BadBTMessageException(
					"invalid piece number in request message: " + pieceNum);

		int offset = payload.getInt();

		byte[] data = new byte[payload.remaining()];
		payload.get(data);
		return new BTPiece(pieceNum, offset, data);
	}

	/**
	 * factory method
	 * 
	 * @param pieceNum
	 *            the number of the piece that was requested
	 * @param offset
	 *            the offset of the requested range wrt. to the beginning of the
	 *            piece
	 * @param data
	 *            the data that was requested
	 * @return new instance of BTPiece
	 */
	public static BTPiece createMessage(int pieceNum, int offset, byte[] data) {
		return new BTPiece(pieceNum, offset, data);
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
	 * @return requested data
	 */
	public byte[] getData() {
		return _data;
	}

	public ByteBuffer getPayload() {
		ByteBuffer payload = ByteBuffer.allocate(_data.length + 8);
		payload.order(ByteOrder.BIG_ENDIAN);
		payload.putInt(_pieceNum);
		payload.putInt(_offset);
		payload.put(_data);
		payload.clear();
		return payload;
	}
	
	public String toString() {
		return "BTPiece (" + _pieceNum + ";" + _offset + ";" + _data.length + ")" ;
	}
}
