package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * indicates that we will not upload anything to the remote host
 */
public class BTHave extends BTMessage {
	private ByteBuffer _payload = null;
	
	private final int _pieceNum;

	/**
	 * Constructs new BTHave message
	 */
	public BTHave(int pieceNum) {
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

		if (payload.remaining() < 4)
			throw new BadBTMessageException();

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
	 * return piece number
	 */
	public int getPieceNum() {
		return _pieceNum;
	}

	@Override
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
	
	@Override
    public String toString() {
		return "BTHave (" + _pieceNum + ")" ;
	}
}
