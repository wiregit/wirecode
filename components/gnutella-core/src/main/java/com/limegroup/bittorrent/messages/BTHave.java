package com.limegroup.gnutella.torrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.limegroup.gnutella.torrent.BadBTMessageException;

/**
 * indicates that we will not upload anything to the remote host
 */
public class BTHave extends BTMessage {
	private ByteBuffer _payload = null;
	
	private final int _pieceNum;

	/**
	 * Constructs new BTHave message
	 */
	private BTHave(int pieceNum) {
		super(HAVE);
		_pieceNum = pieceNum;
	}

	/**
	 * read BTHave from network
	 * 
	 * @param payload
	 *            the data from the network
	 *          @return new BTHave message
	 * @throws BadBTMessageException
	 */
	public static BTHave readMessage(ByteBuffer payload) throws BadBTMessageException {

		if (payload.remaining() != 4)
			throw new BadBTMessageException(
					"unexpected payload in have message: "
							+ new String(payload.array()));

		payload.order(ByteOrder.BIG_ENDIAN);
		int pieceNum = payload.getInt();
		
		// actually every integer in the peer protocol is unsigned but we 
		// simply assume that every u_int > 2*10^9 is invalid. 
		if (pieceNum < 0)
			throw new BadBTMessageException(
					"invalid piece number in have message: " + pieceNum);
		return new BTHave(pieceNum);
	}

	/**
	 * factory method
	 * 
	 * @param pieceNum
	 *            the number of the piece to advertise
	 * @return new instance of BTHave
	 */
	public static BTHave createMessage(int pieceNum) {
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putInt(pieceNum);
		buf.clear();
		return new BTHave(pieceNum);
	}
	
	/**
	 * return piece number
	 */
	public int getPieceNum() {
		return _pieceNum;
	}

	public ByteBuffer getPayload() {
		if (_payload == null) {
			_payload = ByteBuffer.allocate(4);
			_payload.order(ByteOrder.BIG_ENDIAN);
			_payload.putInt(_pieceNum);
			_payload = _payload.asReadOnlyBuffer();
		}
		_payload.clear();
		return _payload;
	}
	
	public String toString() {
		return "BTHave (" + _pieceNum + ")" ;
	}
}
