package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.statistics.BTMessageStatBytes;
import com.limegroup.gnutella.util.CircularByteBuffer;

public abstract class BTMessage {
	// private static final Log LOG = LogFactory.getLog(BTMessage.class);

	/*
	 * final message identifiers
	 */
	public static final byte KEEP_ALIVE = (byte) 0xFF;

	public static final byte CHOKE = 0x00;

	public static final byte UNCHOKE = 0x01;

	public static final byte INTERESTED = 0x02;

	public static final byte NOT_INTERESTED = 0x03;

	public static final byte HAVE = 0x04;

	public static final byte BITFIELD = 0x05;

	public static final byte REQUEST = 0x06;

	public static final byte PIECE = 0x07;

	public static final byte CANCEL = 0x08;

	/**
	 * Buffer used for messages without payload
	 * TODO: use the one from BufferUtils once backported
	 */
	public static final ByteBuffer EMPTY_PAYLOAD;

	// initialize ByteBuffer for this message
	static {
		EMPTY_PAYLOAD = ByteBuffer.allocate(0);
	}

	/*
	 * the identifier of a message
	 */
	private final byte _type;

	/**
	 * A private constructor that will be called by parseMessage()
	 * 
	 * @param type
	 *            a byte setting the type of the message
	 */
	BTMessage(byte type) {
		_type = type;
	}

	/**
	 * Accessor for the message's type
	 * 
	 * @return byte identifying the message's type
	 */
	public byte getType() {
		return _type;
	}

	/**
	 * Accessor for the message's payload without message length
	 * 
	 * @return ByteBuffer, the payload
	 */
	public abstract ByteBuffer getPayload();

	/**
	 * Notification that a keepAlive was received 
	 */
	public static void countKeepAlive() {
		BTMessageStat.INCOMING_KEEP_ALIVE.incrementStat();
		BTMessageStatBytes.INCOMING_KEEP_ALIVE.addData(4);
	}

	/**
	 * Reads a BTMessage from a given <tt>ByteBuffer</tt>. Removes the ranges
	 * read from the buffer and moves the rest to the front of the Buffer.
	 * 
	 * @param in
	 *            the <tt>ByteBuffer</tt> to read from
	 * @param length
	 *            an int specifying the message length. The 4 bytes at the
	 *            beginning of the buffer contain just this number
	 * @return new BTMessages of unknown type
	 * @throws BadBTMessageException
	 */
	public static BTMessage parseMessage(ByteBuffer in, int type)
			throws BadBTMessageException {
		// in case this is a keep alive message, length is not necessarily > 0
		// the reason we do not return here is that we reset the buffer below.
		
		switch (type) {
		case CHOKE:
			BTMessageStat.INCOMING_CHOKE.incrementStat();
			BTMessageStatBytes.INCOMING_CHOKE.addData(5);
			return BTChoke.createMessage();
		case UNCHOKE:
			BTMessageStat.INCOMING_UNCHOKE.incrementStat();
			BTMessageStatBytes.INCOMING_UNCHOKE.addData(5);
			return BTUnchoke.createMessage();
		case INTERESTED:
			BTMessageStat.INCOMING_INTERESTED.incrementStat();
			BTMessageStatBytes.INCOMING_INTERESTED.addData(5);
			return BTInterested.createMessage();
		case NOT_INTERESTED:
			BTMessageStat.INCOMING_NOT_INTERESTED.incrementStat();
			BTMessageStatBytes.INCOMING_NOT_INTERESTED.addData(5);
			return BTNotInterested.createMessage();
		case HAVE:
			BTMessageStat.INCOMING_HAVE.incrementStat();
			BTMessageStatBytes.INCOMING_HAVE.addData(9);
			return BTHave.readMessage(in);
		case BITFIELD:
			throw new BadBTMessageException("unexpected bitfield");
		case REQUEST:
			BTMessageStat.INCOMING_REQUEST.incrementStat();
			BTMessageStatBytes.INCOMING_REQUEST.addData(17);
			return BTRequest.readMessage(in);
		case PIECE:
			throw new IllegalArgumentException("do not parse pieces here");
		case CANCEL:
			BTMessageStat.INCOMING_CANCEL.incrementStat();
			BTMessageStatBytes.INCOMING_CANCEL.addData(17);
			return BTCancel.readMessage(in);
		default:
			throw new BadBTMessageException("unknown message, type " + type);
		}
	}
}
