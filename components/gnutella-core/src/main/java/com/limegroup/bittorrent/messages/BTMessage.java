
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import com.limegroup.bittorrent.statistics.BTMessageStat;
import com.limegroup.bittorrent.statistics.BTMessageStatBytes;

/**
 * BTMessage is the base class for objects like BTChoke and BTHave that represent BitTorrent messages.
 * 
 * Call the static method BTMessage.parseMessage(buffer, length) with a buffer that contains a BitTorrent message.
 * The static parseMessage() method will hand it off to a message type-specific method, like BTChoke.readMessage().
 * 
 * A BitTorrent message looks like this:
 * 
 * LLLLT
 * 
 * The first 4 bytes, LLLL, are an int in big endian byte order.
 * They tell the length of the message that follows them.
 * The first byte of the message body, T, tells what type of message it is.
 * 
 * A Keep Alive message is a special case.
 * The length LLLL is 0, and no data follows in accordance with that length.
 * BitTorrent programs send Keep Alive messages to keep a quiet socket from closing.
 */
public abstract class BTMessage {

	/** 0xff, the byte that identifies a BitTorrent Keep Alive message. */
	public static final byte KEEP_ALIVE = (byte)0xFF;

	/** 0x00, the byte that identifies a BitTorrent Choke message. */
	public static final byte CHOKE = 0x00;

	/** 0x01, the byte that identifies a BitTorrent Unchoke message. */
	public static final byte UNCHOKE = 0x01;

	/** 0x02, the byte that identifies a BitTorrent Interested message. */
	public static final byte INTERESTED = 0x02;

	/** 0x03, the byte that identifies a BitTorrent Not Interested message. */
	public static final byte NOT_INTERESTED = 0x03;

	/** 0x04, the byte that identifies a BitTorrent Have message. */
	public static final byte HAVE = 0x04;

	/** 0x05, the byte that identifies a BitTorrent Bitfield message. */
	public static final byte BITFIELD = 0x05;

	/** 0x06, the byte that identifies a BitTorrent Request message. */
	public static final byte REQUEST = 0x06;

	/** 0x07, the byte that identifies a BitTorrent Piece message. */
	public static final byte PIECE = 0x07;

	/** 0x08, the byte that identifies a BitTorrent Cancel message. */
	public static final byte CANCEL = 0x08;

	/**
	 * A ByteBuffer with no length that can hold no data.
	 * BitTorrent message objects that don't have payloads have getPayload() methods that return EMPTY_PAYLOAD.
	 */
	static final ByteBuffer EMPTY_PAYLOAD;

	// Setup EMPTY_PAYLOAD to point at a 0-length ByteBuffer
	static { EMPTY_PAYLOAD = ByteBuffer.allocate(0); }

	/** The byte that identifies what kind of BitTorrent message this is. */
	private final byte _type;

	/**
	 * Make a new BTMessage object to represent a BitTorrent message.
	 * 
	 * @param type The byte that identifies what type of BitTorrent message this is, like 0x00 Choke or 0x02 Interested
	 */
	BTMessage(byte type) {

		// Save the given type byte in this new BTMessage object
		_type = type;
	}

	/**
	 * Find out what kind of BitTorrent message this is.
	 * Reads the type byte, the first byte of the payload, which comes after the 4 size bytes.
	 * 
	 * @return The type byte, like 0x01 Choke or 0x02 Interested
	 */
	public byte getType() {

		// Return the type byte
		return _type;
	}

	/**
	 * Get the data of this BitTorrent message's payload, without the message length. (do)
	 * Classes that extend BTMessage must have getPayload() methods.
	 * 
	 * @return The message payload data in a ByteBuffer object
	 */
	public abstract ByteBuffer getPayload();

	/**
	 * Read the data of a BitTorrent message from a given ByteBuffer, parsing it into an object that extends BTMessage, and processing it.
	 * Only BTMessageReader.handleRead() calls this method.
	 * 
	 * @param in     A ByteBuffer that has the data of a BitTorrent message from a remote computer.
	 *               parseMessage() will start reading at in.position, and not go past in.length.
	 *               It will move in.position forward past the data of the message it reads. (do)
	 *               It will also compact the buffer, moving the data to the start. (do)
	 * @param length The length of the message.
	 *               The first 4 bytes in the in ByteBuffer contain this length number.
	 * @return       An object that extends BTMessage and represents the BitTorrent message we read.
	 */
	public static BTMessage parseMessage(ByteBuffer in, int length) throws BadBTMessageException {

		/*
		 * in case this is a keep alive message, length is not necessarily > 0
		 * the reason we do not return here is that we reset the buffer below.
		 */

		// Clip out the data in the in ByteBuffer
		in.flip(); // Move position to the start and limit to where position was

		// By default, set type for a Keep Alive message, which has no length
		byte type = KEEP_ALIVE;

		// If length is bigger than 0, this isn't a Keep Alive message, look in the message data to find out what kind of message it is
		if (length > 0) {

			// Read the first byte from the message payload, which tells the type
			type = in.get();
		}

		// Sort by what type of message it is
		switch (type) {

		// Keep Alive message
		case KEEP_ALIVE:

			// Record in statistics
			BTMessageStat.INCOMING_KEEP_ALIVE.incrementStat();
			BTMessageStatBytes.INCOMING_KEEP_ALIVE.addData(4); // A Keep Alive message takes up 4 bytes

			// It's not necessary to do anything else
			return null;

		// Choke message
		case CHOKE:

			// Record in statistics
			BTMessageStat.INCOMING_CHOKE.incrementStat();
			BTMessageStatBytes.INCOMING_CHOKE.addData(5); // A Choke message takes up 5 bytes

			// Give the message data to a method specific to its type
			return BTChoke.readMessage(in);

		// Unchoke message
		case UNCHOKE:

			// Record in statistics
			BTMessageStat.INCOMING_UNCHOKE.incrementStat();
			BTMessageStatBytes.INCOMING_UNCHOKE.addData(5); // An Unchoke message takes up 5 bytes

			// Give the message data to a method specific to its type
			return BTUnchoke.readMessage(in);

		// Interested message
		case INTERESTED:

			// Record in statistics
			BTMessageStat.INCOMING_INTERESTED.incrementStat();
			BTMessageStatBytes.INCOMING_INTERESTED.addData(5); // An Interested message takes up 5 bytes

			// Give the message data to a method specific to its type
			return BTInterested.readMessage(in);

		// Not Interested message
		case NOT_INTERESTED:

			// Record in statistics
			BTMessageStat.INCOMING_NOT_INTERESTED.incrementStat();
			BTMessageStatBytes.INCOMING_NOT_INTERESTED.addData(5); // A Not Interested message takes up 5 bytes

			// Give the message data to a method specific to its type
			return BTNotInterested.readMessage(in);

		// Have message
		case HAVE:

			// Record in statistics
			BTMessageStat.INCOMING_HAVE.incrementStat();
			BTMessageStatBytes.INCOMING_HAVE.addData(9); // A Have message takes up 9 bytes

			// Give the message data to a method specific to its type
			return BTHave.readMessage(in);

		// Bitfield message
		case BITFIELD:

			// Record in statistics
			BTMessageStat.INCOMING_BITFIELD.incrementStat();
			BTMessageStatBytes.INCOMING_BITFIELD.addData(5 + in.remaining()); // Pass the size of the Bitfield message to addData()

			// Give the message data to a method specific to its type
			return BTBitField.readMessage(in);

		// Request message
		case REQUEST:

			// Record in statistics
			BTMessageStat.INCOMING_REQUEST.incrementStat();
			BTMessageStatBytes.INCOMING_REQUEST.addData(17); // A Request message takes up 14 bytes TODO: why is this 17?

			// Give the message data to a method specific to its type
			return BTRequest.readMessage(in);

		// Piece message
		case PIECE:

			// Record in statistics
			BTMessageStat.INCOMING_PIECE.incrementStat();
			BTMessageStatBytes.INCOMING_PIECE.addData(5 + in.remaining()); // Pass the size of the piece message to addData()

			// Give the message data to a method specific to its type
			return BTPiece.readMessage(in);

		// Cancel message
		case CANCEL:

			// Record in statistics
			BTMessageStat.INCOMING_CANCEL.incrementStat();
			BTMessageStatBytes.INCOMING_CANCEL.addData(17); // A Cancel message takes up 14 bytes TODO: why is this 17?

			// Give the message data to a method specific to its type
			return BTCancel.readMessage(in);

		// The type byte has some other value
		default:

			// We only know what to do with the message types outlined above
			throw new BadBTMessageException("unknown message, type " + type);
		}
	}
}
